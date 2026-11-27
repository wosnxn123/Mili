package fun.bm.mili.update;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static fun.bm.mili.update.MiliReleaseSource.ASSET;
import static fun.bm.mili.update.MiliReleaseSource.COMMIT;
import static fun.bm.mili.update.MiliReleaseSource.DIGEST;
import static fun.bm.mili.update.MiliReleaseSource.VERSION;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class MiliUpdater {
    private static final String LAUNCHER_CLASS = "moe.luminolmc.riceear.Riceear";
    private static final String LAUNCHER_MAIN = "moe.luminolmc.riceear.Main";
    private static final int MAX_MANIFEST_BYTES = 64 * 1024;
    private final MiliReleaseSource source;
    private final Path updateDir;
    private final Path stageDir;
    private final Path corePath;
    private final Publish publish;

    MiliUpdater(MiliReleaseSource source, Path serverDirectory, Publish publish) throws IOException {
        this.source = source;
        this.updateDir = serverDirectory.toRealPath().resolve("auto_update");
        this.stageDir = updateDir.resolve("mili");
        this.corePath = updateDir.resolve("core.path");
        this.publish = publish;
    }

    public static String check(String current, boolean restart, String target, Publish publish) throws IOException {
        if (restart && (Boolean.getBoolean("riceear.disable.auto-update") || Boolean.getBoolean("leavesclip.disable.auto-update"))) {
            throw new IOException("Riceear restart selection is disabled by a JVM flag");
        }
        final var updater = new MiliUpdater(new MiliReleaseSource(new MiliReleaseSource.GitHub()), Path.of(""), publish);
        final boolean replace = !target.isBlank();
        Path launcher = null;
        Path runningServer = null;
        if (restart || replace) {
            try {
                launcher = codeSource(Class.forName(LAUNCHER_CLASS, false, ClassLoader.getSystemClassLoader()));
                if (replace) runningServer = codeSource(MiliUpdater.class);
            } catch (ClassNotFoundException | URISyntaxException | RuntimeException e) {
                throw new IOException("Cannot identify the running Riceear launcher or server JAR", e);
            }
        }
        return updater.run(current, restart, replace ? Path.of(target) : null, launcher, runningServer);
    }

    private static Path codeSource(Class<?> type) throws URISyntaxException {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
    }

    String run(String current, boolean restart, Path target, Path launcher, Path runningServer) throws IOException {
        if (restart && target != null) throw new IOException("Select only one Mili update activation mode");
        final var release = source.find(current);
        if (release == null) return "No newer verified Mili " + VERSION + " release found.";

        final Path staged = stageDir.resolve(release.commit()).resolve(release.sha256()).resolve(ASSET);
        final Pointer previous = restart ? readOwnedPointer() : null;
        if (previous != null) {
            if (previous.jar().equals(staged)) return "Mili release " + release.tag() + " is already selected for restart.";
            if (!source.strictlyAhead(previous.commit(), release.commit())) {
                throw new IOException("Release does not advance the Mili update already selected for restart");
            }
        }
        final String launcherVersion = restart ? restartVersion(launcher) : null;
        final BasicFileAttributes original = target == null ? null : validateTarget(target, launcher, runningServer);
        stage(release, staged, launcherVersion);
        if (restart) {
            writePointer(staged, previous);
            return "Mili release " + release.tag() + " will be used on restart: " + staged;
        }
        if (target != null) {
            final Path backup = replaceTarget(target.toAbsolutePath().normalize(), staged, release.sha256(), original);
            return "Replaced the Mili launcher with release " + release.tag() + ". Previous launcher backed up at " + backup;
        }
        return "Staged verified Mili release " + release.tag() + " at " + staged + " (no automatic activation).";
    }

    private void stage(MiliReleaseSource.Release release, Path staged, String launcherVersion) throws IOException {
        if (attributes(staged) != null) {
            verifyStaged(staged, release.sha256(), launcherVersion);
            return;
        }
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Mili auto-update stopped");
        ensureDirectory(updateDir);
        ensureDirectory(stageDir);
        final Path temp = Files.createTempFile(stageDir, "download-", ".tmp");
        try {
            final MessageDigest digest = sha256();
            try (var input = new DigestInputStream(source.download(release), digest);
                 var output = Files.newOutputStream(temp, WRITE)) {
                long remaining = release.size();
                final byte[] buffer = new byte[64 * 1024];
                while (remaining > 0) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Mili auto-update stopped");
                    final int read = input.read(buffer, 0, (int) Math.min(remaining, buffer.length));
                    if (read == -1) throw new IOException("Truncated Mili release asset");
                    output.write(buffer, 0, read);
                    remaining -= read;
                }
                if (input.read() != -1) throw new IOException("Mili release asset exceeds its declared size");
            }
            if (!HexFormat.of().formatHex(digest.digest()).equals(release.sha256())) throw new IOException("Mili release SHA-256 mismatch");
            verifyLauncherVersion(temp, launcherVersion);
            force(temp);
            publish.run(() -> {
                ensureDirectory(staged.getParent().getParent());
                ensureDirectory(staged.getParent());
                if (attributes(staged) != null) throw new IOException("Mili stage path changed during download");
                Files.move(temp, staged, ATOMIC_MOVE);
            });
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Pointer readOwnedPointer() throws IOException {
        final BasicFileAttributes existing = attributes(corePath);
        if (existing == null) return null;
        final String text = pointerText(existing);
        final Path jar;
        try {
            jar = Path.of(text);
        } catch (InvalidPathException e) {
            throw new IOException("Leaving malformed auto_update/core.path untouched", e);
        }
        if (!jar.isAbsolute() || !jar.normalize().equals(jar) || !jar.startsWith(stageDir)) {
            throw new IOException("Leaving external auto_update/core.path untouched");
        }
        final Path relative = stageDir.relativize(jar);
        if (relative.getNameCount() != 3 || !relative.getName(0).toString().matches(COMMIT)
                || !relative.getName(1).toString().matches(DIGEST) || !relative.getName(2).toString().equals(ASSET)) {
            throw new IOException("Leaving unrecognized auto_update/core.path untouched");
        }
        verifyStaged(jar, relative.getName(1).toString(), null);
        return new Pointer(jar, relative.getName(0).toString(), text, existing);
    }

    private String pointerText(BasicFileAttributes attributes) throws IOException {
        requireRegular(corePath, attributes);
        try (InputStream input = Files.newInputStream(corePath, NOFOLLOW_LINKS)) {
            final byte[] bytes = input.readNBytes(MAX_MANIFEST_BYTES + 1);
            if (bytes.length > MAX_MANIFEST_BYTES) throw new IOException("Oversized core.path");
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private void writePointer(Path staged, Pointer previous) throws IOException {
        final String text = staged.toString();
        if (!text.equals(text.trim()) || text.indexOf('\n') != -1 || text.indexOf('\r') != -1) {
            throw new IOException("Staged path cannot be represented in Riceear core.path");
        }
        final Path temp = Files.createTempFile(updateDir, "mili-core-", ".tmp");
        try {
            Files.writeString(temp, text);
            force(temp);
            publish.run(() -> {
                if (previous == null) {
                    // Atomic create-if-absent: never clobber a pointer created during the download.
                    Files.createLink(corePath, temp);
                } else {
                    final BasicFileAttributes now = attributes(corePath);
                    if (!sameFileState(previous.attributes(), now) || !previous.text().equals(pointerText(now))) {
                        throw new IOException("core.path changed during Mili update; leaving it untouched");
                    }
                    Files.move(temp, corePath, ATOMIC_MOVE, REPLACE_EXISTING);
                }
            });
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private BasicFileAttributes validateTarget(Path target, Path launcher, Path runningServer) throws IOException {
        if (attributes(corePath) != null) throw new IOException("Direct replacement refused while auto_update/core.path exists");
        target = target.toAbsolutePath().normalize();
        final BasicFileAttributes original = attributes(target);
        requireRegular(target, original);
        if (!target.equals(launcher) || !target.getFileName().toString().endsWith(".jar") || target.startsWith(updateDir)) {
            throw new IOException("Configured target is not the actual Riceear launcher outside auto_update");
        }
        requireRegular(runningServer, attributes(runningServer));
        if (!jarInfo(target).serverHash().equals(hash(runningServer))) {
            throw new IOException("Configured launcher JAR no longer matches the running Mili server");
        }
        return original;
    }

    private Path replaceTarget(Path target, Path staged, String expectedHash, BasicFileAttributes original) throws IOException {
        final Path parent = target.getParent();
        final Path backup = parent.resolve(target.getFileName() + ".mili-" + UUID.randomUUID() + ".bak");
        final Path backupTemp = Files.createTempFile(parent, ".mili-backup-", ".tmp");
        try {
            final Path replacement = Files.createTempFile(parent, ".mili-update-", ".tmp");
            try {
                Files.copy(target, backupTemp, REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINKS);
                final String oldHash = hash(backupTemp);
                if (!oldHash.equals(hash(target))) throw new IOException("Launcher changed while creating backup");
                Files.copy(staged, replacement, REPLACE_EXISTING, NOFOLLOW_LINKS);
                if (!expectedHash.equals(hash(replacement))) throw new IOException("Mili launcher copy failed SHA-256 verification");
                force(backupTemp);
                force(replacement);
                publish.run(() -> {
                    if (attributes(corePath) != null || !sameFileState(original, attributes(target))) {
                        throw new IOException("Launcher or core.path changed before replacement");
                    }
                    Files.move(backupTemp, backup, ATOMIC_MOVE);
                    Files.move(replacement, target, ATOMIC_MOVE, REPLACE_EXISTING);
                });
                return backup;
            } finally {
                Files.deleteIfExists(replacement);
            }
        } finally {
            Files.deleteIfExists(backupTemp);
        }
    }

    private static void verifyStaged(Path jar, String digest, String launcherVersion) throws IOException {
        requireRegular(jar, attributes(jar));
        if (!digest.equals(hash(jar))) throw new IOException("Mili staged JAR failed SHA-256 verification: " + jar);
        verifyLauncherVersion(jar, launcherVersion);
    }

    private static void verifyLauncherVersion(Path jar, String launcherVersion) throws IOException {
        final String stagedVersion = restartVersion(jar);
        if (launcherVersion != null && !launcherVersion.equals(stagedVersion)) {
            throw new IOException("Staged Mili JAR is not compatible with this Riceear launcher version");
        }
    }

    private static String restartVersion(Path path) throws IOException {
        final JarInfo info = jarInfo(path);
        if (!info.patched() || info.version().isBlank()) {
            throw new IOException("Riceear launcher does not support Mili staged patches: " + path);
        }
        return info.version();
    }

    private static JarInfo jarInfo(Path path) throws IOException {
        if (path == null) throw new IOException("No Riceear launcher path");
        requireRegular(path, attributes(path));
        try (var jar = new JarFile(path.toFile(), false)) {
            final var manifest = jar.getManifest();
            if (manifest == null || !LAUNCHER_MAIN.equals(manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS))) {
                throw new IOException("Not a Riceear Paperclip JAR: " + path);
            }
            final String version = jar.getJarEntry("META-INF/riceear-version") == null ? "" : jarEntry(jar, "META-INF/riceear-version");
            if (jar.getJarEntry("META-INF/patches.list") == null) {
                throw new IOException("Missing Riceear patch metadata: " + path);
            }
            String serverHash = null;
            for (String row : jarEntry(jar, "META-INF/versions.list").split("\\R")) {
                final String[] columns = row.split("\t", -1);
                if (columns.length == 3 && columns[1].equals(VERSION)) {
                    if (serverHash != null || !columns[0].matches(DIGEST)) throw new IOException("Invalid Riceear versions list");
                    serverHash = columns[0];
                }
            }
            if (serverHash == null) throw new IOException("No Minecraft " + VERSION + " server in Paperclip metadata");
            final boolean patched = jar.getJarEntry("META-INF/mili-launcher-patch") != null
                    && "active-target-jar-v1".equals(jarEntry(jar, "META-INF/mili-launcher-patch"));
            return new JarInfo(version, serverHash, patched);
        }
    }

    private static String jarEntry(JarFile jar, String name) throws IOException {
        final var entry = jar.getJarEntry(name);
        if (entry == null || entry.isDirectory()) throw new IOException("Missing Paperclip asset entry: " + name);
        try (InputStream input = jar.getInputStream(entry)) {
            final byte[] bytes = input.readNBytes(MAX_MANIFEST_BYTES + 1);
            if (bytes.length > MAX_MANIFEST_BYTES) throw new IOException("Oversized Paperclip asset entry: " + name);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static void ensureDirectory(Path path) throws IOException {
        if (!path.getParent().equals(path.getParent().toRealPath())) throw new IOException("Linked Mili update directory: " + path.getParent());
        try {
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException ignored) {
            // Check existing directories as well as a competing creation.
        }
        if (!Files.isDirectory(path, NOFOLLOW_LINKS) || !path.equals(path.toRealPath())) {
            throw new IOException("Unsafe Mili update directory: " + path);
        }
    }

    private static BasicFileAttributes attributes(Path path) throws IOException {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class, NOFOLLOW_LINKS);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    private static void requireRegular(Path path, BasicFileAttributes attributes) throws IOException {
        if (attributes == null || !attributes.isRegularFile() || !path.equals(path.toRealPath())) {
            throw new IOException("Missing, linked, or non-regular Mili update file: " + path);
        }
    }

    private static boolean sameFileState(BasicFileAttributes before, BasicFileAttributes after) {
        return after != null && after.isRegularFile() && before.size() == after.size()
                && before.lastModifiedTime().equals(after.lastModifiedTime())
                && Objects.equals(before.fileKey(), after.fileKey());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static String hash(Path path) throws IOException {
        final MessageDigest digest = sha256();
        try (var input = new DigestInputStream(Files.newInputStream(path, NOFOLLOW_LINKS), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void force(Path file) throws IOException {
        try (var channel = FileChannel.open(file, WRITE)) {
            channel.force(true);
        }
    }

    private record Pointer(Path jar, String commit, String text, BasicFileAttributes attributes) {}
    private record JarInfo(String version, String serverHash, boolean patched) {}

    @FunctionalInterface
    public interface Publish {
        void run(IoAction action) throws IOException;
    }

    @FunctionalInterface
    public interface IoAction {
        void run() throws IOException;
    }
}
