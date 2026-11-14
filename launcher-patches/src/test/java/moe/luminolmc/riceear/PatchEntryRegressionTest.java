package moe.luminolmc.riceear;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import moe.luminolmc.riceear.update.AutoUpdate;
import riceear.libs.io.sigpipe.jbsdiff.Diff;

/** Offline regression test: no Minecraft main, network, world or EULA. */
public final class PatchEntryRegressionTest {
    private static int assertions;

    private static byte[] hash(byte[] value) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(value);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        assertions++;
    }

    private static void selectTarget(Path target) throws Exception {
        Field path = AutoUpdate.class.getDeclaredField("autoUpdateCorePath");
        Field enabled = AutoUpdate.class.getDeclaredField("useAutoUpdateJar");
        path.setAccessible(true);
        enabled.setAccessible(true);
        path.set(null, target);
        enabled.setBoolean(null, true);
    }

    private static void jar(Path destination, String entry, byte[] data) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination))) {
            zip.putNextEntry(new ZipEntry(entry));
            zip.write(data);
            zip.closeEntry();
        }
    }

    private static void expectFailure(PatchEntry entry, Path original, Path repo,
                                      String message) throws Exception {
        Map<String, Map<String, URL>> urls = new HashMap<>();
        urls.put(entry.location(), new HashMap<>());
        try {
            entry.applyPatch(urls, original, repo);
            throw new AssertionError("Expected failure: " + message);
        } catch (IllegalStateException e) {
            check(e.getMessage().contains(message), "Unexpected failure: " + e);
        }
        check(urls.get(entry.location()).isEmpty(), "Failed patch registered a classpath entry");
    }

    public static void main(String[] args) throws Exception {
        Path parent = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(parent);
        Path work = Files.createTempDirectory(parent, "patch-entry-");
        byte[] original = "original vanilla input\n".getBytes(StandardCharsets.UTF_8);
        byte[] expected = "rebuilt fork output with changes\n".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Diff.diff(original, expected, buffer);
        byte[] delta = buffer.toByteArray();
        Path vanilla = work.resolve("vanilla.jar");
        jar(vanilla, "META-INF/versions/26.3/server.jar", original);
        Path target = work.resolve("active-update.jar");
        jar(target, "META-INF/versions/26.3/server.jar.patch", delta);
        selectTarget(target);
        String line = String.join("\t", "versions", HexFormat.of().formatHex(hash(original)),
            HexFormat.of().formatHex(hash(delta)), HexFormat.of().formatHex(hash(expected)),
            "26.3/server.jar", "26.3/server.jar.patch", "26.3/fork.jar");
        PatchEntry[] parsed = PatchEntry.parse(new BufferedReader(new StringReader("# comment\n\n" + (line + "\n").repeat(10))));
        check(parsed.length == 10, "Parser lost entries while growing its initial array");
        check(PatchEntry.parse(new BufferedReader(new StringReader("\n# only comments\n"))).length == 0, "Empty manifest failed");
        try {
            PatchEntry.parse(new BufferedReader(new StringReader("invalid\n")));
            throw new AssertionError("Malformed manifest accepted");
        } catch (IllegalStateException e) {
            check(e.getMessage().contains("Invalid patch data line"), "Unexpected parser error");
        }
        try (FileSystem fs = FileSystems.newFileSystem(vanilla)) {
            Path root = fs.getPath("/");
            check(!Files.exists(root.resolve("META-INF/versions/26.3/server.jar.patch")), "Fixture accidentally seeded patch in vanilla");
            PatchEntry entry = parsed[0];
            Path repo = work.resolve("output");
            Map<String, Map<String, URL>> urls = new HashMap<>();
            urls.put("versions", new HashMap<>());
            entry.applyPatch(urls, root, repo);
            Path output = repo.resolve("versions/26.3/fork.jar");
            check(java.util.Arrays.equals(Files.readAllBytes(output), expected), "Active target patch was not applied");
            check(urls.get("versions").get("26.3/server.jar").equals(output.toUri().toURL()), "Wrong classpath substitution");
            selectTarget(work.resolve("missing-target.jar"));
            entry.applyPatch(urls, root, repo);
            check(java.util.Arrays.equals(Files.readAllBytes(output), expected), "Valid cache was not reused without reading patches");
            selectTarget(target);
            Files.writeString(output, "corrupt cache");
            entry.applyPatch(urls, root, repo);
            check(java.util.Arrays.equals(Files.readAllBytes(output), expected), "Corrupt cache was not rebuilt");
            byte[] invalidHash = new byte[32];
            expectFailure(new PatchEntry("versions", invalidHash, hash(delta), hash(expected),
                entry.originalPath(), entry.patchPath(), entry.outputPath()), root, work.resolve("bad-original"), "Hash check of input file failed");
            expectFailure(new PatchEntry("versions", hash(original), invalidHash, hash(expected),
                entry.originalPath(), entry.patchPath(), entry.outputPath()), root, work.resolve("bad-patch"), "Hash check of patch file failed");
            expectFailure(new PatchEntry("versions", hash(original), hash(delta), invalidHash,
                entry.originalPath(), entry.patchPath(), entry.outputPath()), root, work.resolve("bad-output"), "Patch not applied correctly");
            check(!Files.exists(work.resolve("bad-output/versions/26.3/fork.jar")), "Invalid output was published");
            expectFailure(new PatchEntry("versions", hash(original), hash(delta), hash(expected),
                entry.originalPath(), "missing.patch", entry.outputPath()), root, work.resolve("missing-patch"), "Patch file not found");
        }
        System.out.println("PASS: " + assertions + " offline PatchEntry assertions (active update JAR, parser, cache and three hash gates)");
    }
}
