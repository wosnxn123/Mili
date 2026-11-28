package fun.bm.mili.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static fun.bm.mili.update.MiliReleaseSource.API;
import static fun.bm.mili.update.MiliReleaseSource.ASSET;
import static fun.bm.mili.update.MiliReleaseSource.REPOSITORY;
import static org.junit.jupiter.api.Assertions.*;

class MiliAutoUpdateTestSuite {
    private static final String CURRENT = "1".repeat(40);
    private static final String RELEASE = "2".repeat(40);
    private static final String TIP = "3".repeat(40);
    private static final String ANNOTATED = "4".repeat(40);
    private static final String OTHER = "5".repeat(40);
    private static final String RICEEAR = "3.0.18";
    private static final String SHORT = CURRENT.substring(0, 7);
    private static final String BASE = "https://github.com/" + REPOSITORY;
    private static final byte[] SERVER = {1, 3, 5, 7};
    private static final byte[] CANDIDATE = jar("7".repeat(64), RICEEAR, true);

    @Test
    void validatesAnnotatedTagAndIgnoresTargetCommitish() throws IOException {
        final FakeGitHub api = stable();
        api.ref("tags/26.3", "tag", ANNOTATED);
        api.set("/git/tags/" + ANNOTATED, """
                {"sha":"%s","object":{"type":"commit","sha":"%s"}}
                """.formatted(ANNOTATED, RELEASE));
        assertEquals(RELEASE, api.source().find(SHORT).commit());
        api.set("/git/tags/" + ANNOTATED, """
                {"sha":"%s","object":{"type":"tree","sha":"%s"}}
                """.formatted(ANNOTATED, RELEASE));
        assertThrows(IOException.class, () -> api.source().find(SHORT));
    }

    @Test
    void onlyAcceptsMatchingShortTagAndStableRelease() throws IOException {
        final FakeGitHub api = stable();
        final String goodTag = "26.3-" + RELEASE.substring(0, 7);
        final String falseTag = "26.3-" + OTHER.substring(0, 7);
        api.ref("tags/" + goodTag, "commit", RELEASE);
        api.ref("tags/" + falseTag, "commit", RELEASE);
        final JsonObject draft = api.release("26.3");
        draft.addProperty("draft", true);
        final JsonObject prerelease = api.release("26.3");
        prerelease.addProperty("prerelease", true);
        api.releases(draft, prerelease, api.release("26.3.1"), api.release(goodTag + "-1"), api.release(falseTag), api.release(goodTag));
        assertEquals(goodTag, api.source().find(SHORT).tag());
        api.releases(draft, prerelease, api.release(falseTag));
        assertNull(api.source().find(SHORT));
    }

    @Test
    void releasedUpdaterAcceptsR2ContentTagButNotAnUnrelatedRevision() throws IOException {
        final FakeGitHub api = stable();
        final String tag = "26.3-" + RELEASE.substring(0, 12);
        api.ref("tags/26.3", "commit", CURRENT);
        api.ref("tags/" + tag, "commit", RELEASE);
        api.releases(api.release(tag), api.release("26.3"));
        assertEquals(RELEASE, api.source().find(SHORT).commit());
        api.ref("tags/" + tag, "commit", OTHER);
        assertNull(api.source().find(SHORT));
    }

    @Test
    void rejectsWrongRepoReleaseAndMalformedOrImpostorAssets() throws IOException {
        final FakeGitHub api = stable();
        for (String[] bad : new String[][]{
                {"name", "my-" + ASSET}, {"state", "new"}, {"digest", "sha256:123"},
                {"browser_download_url", "https://github.com/elsewhere/Mili/releases/download/26.3/" + ASSET},
                {"browser_download_url", BASE + "/releases/download/26.3/" + ASSET + "?other"}}) {
            final JsonObject release = api.release("26.3");
            asset(release).addProperty(bad[0], bad[1]);
            api.releases(release);
            assertNull(api.source().find(SHORT));
        }
        final JsonObject duplicate = api.release("26.3");
        duplicate.getAsJsonArray("assets").add(asset(duplicate).deepCopy());
        final JsonObject fork = api.release("26.3");
        fork.addProperty("url", "https://api.github.com/repos/LuminolMC/Luminol/releases/1");
        final JsonObject wrongTypes = api.release("26.3");
        wrongTypes.addProperty("prerelease", "false");
        api.releases(duplicate, fork, wrongTypes);
        assertNull(api.source().find(SHORT));
    }

    @Test
    void validatesOfficialLineAndForwardOnlyAncestry() throws IOException {
        final FakeGitHub api = stable();
        assertThrows(IOException.class, () -> api.source().find("not-a-commit"));
        api.compare(CURRENT, RELEASE, "behind", RELEASE);
        assertNull(api.source().find(SHORT));
        api.compare(CURRENT, RELEASE, "diverged", OTHER);
        assertNull(api.source().find(SHORT));
        api.compare(CURRENT, RELEASE, "ahead", OTHER);
        assertThrows(IOException.class, () -> api.source().find(SHORT));
        api.compare(CURRENT, RELEASE, "unknown", CURRENT);
        assertThrows(IOException.class, () -> api.source().find(SHORT));
        api.ahead(CURRENT, RELEASE);
        api.compare(RELEASE, TIP, "diverged", CURRENT);
        assertNull(api.source().find(SHORT));
        api.ahead(RELEASE, TIP);
        api.compare(CURRENT, TIP, "diverged", OTHER);
        assertThrows(IOException.class, () -> api.source().find(SHORT));
        api.ahead(CURRENT, TIP);
        api.ref("tags/26.3", "commit", CURRENT);
        assertNull(api.source().find(SHORT));
        api.set("/commits/" + SHORT, "{\"sha\":\"" + OTHER + "\"}");
        assertThrows(IOException.class, () -> api.source().find(SHORT));
        assertEquals(0, api.downloads);
    }

    @Test
    void refusesUnapprovedAssetRedirects() throws IOException {
        final URI origin = URI.create(BASE + "/releases/download/26.3/" + ASSET);
        assertEquals("release-assets.githubusercontent.com", MiliReleaseSource.assetRedirect(origin,
                "https://release-assets.githubusercontent.com/github-production-release-asset-2e65be/42?sig=fixture").getHost());
        for (String redirect : new String[]{"http://release-assets.githubusercontent.com/file",
                "https://release-assets.githubusercontent.com.evil.example/file", "https://release-assets.githubusercontent.com:444/file",
                "https://github.com/another/release", "https://user@release-assets.githubusercontent.com/file", "//evil.example/file"}) {
            assertThrows(IOException.class, () -> MiliReleaseSource.assetRedirect(origin, redirect));
        }
    }

    @Test
    void stageOnlyLeavesLegacyPointerAndIsReused(@TempDir Path directory) throws IOException {
        final FakeGitHub api = stable();
        final Path update = Files.createDirectory(directory.resolve("auto_update"));
        final Path core = Files.writeString(update.resolve("core.path"), "old-luminol.jar\n");
        final var updater = updater(api, directory);
        updater.run(SHORT, false, null, null, null);
        updater.run(SHORT, false, null, null, null);
        assertArrayEquals(CANDIDATE, Files.readAllBytes(staged(directory, RELEASE, CANDIDATE)));
        assertEquals(1, api.downloads);
        assertEquals("old-luminol.jar\n", Files.readString(core));
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, null, null));
        assertEquals("old-luminol.jar\n", Files.readString(core));
    }

    @Test
    void badDigestTruncationAndUnverifiedCompareCannotPublish(@TempDir Path directory) throws IOException {
        final FakeGitHub api = stable();
        final Path launcher = launcher(directory, RICEEAR);
        api.compare(CURRENT, RELEASE, "ahead", OTHER);
        final var updater = updater(api, directory);
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, launcher, null));
        assertFalse(Files.exists(directory.resolve("auto_update")));
        assertEquals(0, api.downloads);

        api.ahead(CURRENT, RELEASE);
        api.body = CANDIDATE.clone();
        api.body[7] ^= 1;
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, launcher, null));
        api.body = new byte[]{1};
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, launcher, null));
        assertFalse(Files.exists(staged(directory, RELEASE, CANDIDATE)));
        assertFalse(Files.exists(directory.resolve("auto_update/core.path")));
        try (var files = Files.list(directory.resolve("auto_update/mili"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void restartRequiresCompatibleLauncherAndAdvancesOwnedPointerOnly(@TempDir Path directory) throws IOException {
        final FakeGitHub api = stable();
        final Path launcher = launcher(directory, "3.0.17");
        final var updater = updater(api, directory);
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, launcher, null));
        final Path core = directory.resolve("auto_update/core.path");
        assertFalse(Files.exists(core));
        Files.write(launcher, jar(sha(SERVER), RICEEAR, false));
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, launcher, null));
        assertFalse(Files.exists(core));
        Files.write(launcher, jar(sha(SERVER), RICEEAR, true));
        updater.run(SHORT, true, null, launcher, null);
        assertEquals(staged(directory, RELEASE, CANDIDATE).toString(), Files.readString(core));

        api.ref("tags/26.3", "commit", TIP);
        updater.run(SHORT, true, null, launcher, null);
        final Path pending = staged(directory, TIP, CANDIDATE);
        assertEquals(pending.toString(), Files.readString(core));
        api.ref("tags/26.3", "commit", RELEASE);
        api.compare(TIP, RELEASE, "behind", RELEASE);
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, launcher, null));
        assertEquals(pending.toString(), Files.readString(core));
        Files.write(pending, new byte[]{0});
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, launcher, null));
        assertEquals(pending.toString(), Files.readString(core));
    }

    @Test
    void doesNotClobberPointerCreatedDuringDownload(@TempDir Path directory) throws IOException {
        final FakeGitHub api = stable();
        final Path launcher = launcher(directory, RICEEAR);
        final Path core = directory.resolve("auto_update/core.path");
        api.beforeDownload = () -> Files.writeString(core, "outside.jar");
        final var updater = updater(api, directory);
        assertThrows(IOException.class, () -> updater.run(SHORT, true, null, launcher, null));
        assertEquals("outside.jar", Files.readString(core));
    }

    @Test
    void onlyReplacesActualRunningLauncherAndPreservesBackup(@TempDir Path directory) throws IOException {
        final FakeGitHub api = stable();
        final Path launcher = Files.write(directory.resolve("launch.jar"), jar(sha(SERVER), null, false));
        final byte[] original = Files.readAllBytes(launcher);
        final Path running = Files.write(directory.resolve("running.jar"), SERVER);
        final Path unrelated = Files.write(directory.resolve("other.jar"), original);
        final var updater = updater(api, directory);
        assertThrows(IOException.class, () -> updater.run(SHORT, false, unrelated, launcher, running));
        assertEquals(0, api.downloads);
        updater.run(SHORT, false, launcher, launcher, running);
        assertArrayEquals(CANDIDATE, Files.readAllBytes(launcher));
        assertArrayEquals(original, Files.readAllBytes(unrelated));
        assertFalse(Files.exists(directory.resolve("auto_update/core.path")));
        try (var files = Files.list(directory)) {
            final var backups = files.filter(file -> file.getFileName().toString().endsWith(".bak")).toList();
            assertEquals(1, backups.size());
            assertArrayEquals(original, Files.readAllBytes(backups.getFirst()));
        }
        assertThrows(IOException.class, () -> updater.run(SHORT, false, launcher, launcher, running));
        assertArrayEquals(CANDIDATE, Files.readAllBytes(launcher));
    }

    @Test
    void canceledPublicationLeavesLauncherUnchanged(@TempDir Path directory) throws IOException {
        final FakeGitHub api = stable();
        final Path launcher = launcher(directory, RICEEAR);
        final Path running = Files.write(directory.resolve("running.jar"), SERVER);
        final byte[] original = Files.readAllBytes(launcher);
        final var writes = new AtomicInteger();
        final var updater = new MiliUpdater(api.source(), directory, action -> {
            if (writes.incrementAndGet() == 2) throw new IOException("Configuration reloaded");
            action.run();
        });
        assertThrows(IOException.class, () -> updater.run(SHORT, false, launcher, launcher, running));
        assertArrayEquals(original, Files.readAllBytes(launcher));
        try (var files = Files.list(directory)) {
            assertEquals(0, files.filter(file -> file.getFileName().toString().contains(".mili-")).count());
        }
    }

    private static Path launcher(Path directory, String version) throws IOException {
        return Files.write(directory.resolve("launch.jar"), jar(sha(SERVER), version, true));
    }

    private static MiliUpdater updater(FakeGitHub api, Path directory) throws IOException {
        return new MiliUpdater(api.source(), directory, MiliUpdater.IoAction::run);
    }

    private static Path staged(Path directory, String commit, byte[] bytes) throws IOException {
        return directory.toRealPath().resolve("auto_update/mili").resolve(commit).resolve(sha(bytes)).resolve(ASSET);
    }

    private static byte[] jar(String serverHash, String launcherVersion, boolean patched) {
        final var bytes = new ByteArrayOutputStream();
        final var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "moe.luminolmc.riceear.Main");
        try (var jar = new JarOutputStream(bytes, manifest)) {
            if (launcherVersion != null) entry(jar, "META-INF/riceear-version", launcherVersion);
            if (patched) entry(jar, "META-INF/mili-launcher-patch", "active-target-jar-v1");
            entry(jar, "META-INF/versions.list", serverHash + "\t26.3\t26.3/mili-server.jar\n");
            entry(jar, "META-INF/patches.list", "fixture-not-executed");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return bytes.toByteArray();
    }

    private static void entry(JarOutputStream jar, String name, String contents) throws IOException {
        jar.putNextEntry(new JarEntry(name));
        jar.write(contents.getBytes(StandardCharsets.UTF_8));
        jar.closeEntry();
    }

    private static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static JsonObject asset(JsonObject release) {
        return release.getAsJsonArray("assets").get(0).getAsJsonObject();
    }

    private static FakeGitHub stable() {
        final var api = new FakeGitHub();
        api.set("/commits/" + SHORT, "{\"sha\":\"" + CURRENT + "\"}");
        api.ref("heads/ver/26.3", "commit", TIP);
        api.ref("tags/26.3", "commit", RELEASE);
        api.ahead(CURRENT, RELEASE);
        api.ahead(CURRENT, TIP);
        api.ahead(RELEASE, TIP);
        api.releases(api.release("26.3"));
        return api;
    }

    private static final class FakeGitHub implements MiliReleaseSource.Transport {
        final Map<String, JsonElement> responses = new HashMap<>();
        byte[] body = CANDIDATE;
        int downloads;
        MiliUpdater.IoAction beforeDownload = () -> {};

        MiliReleaseSource source() { return new MiliReleaseSource(this); }

        @Override
        public JsonElement json(String path) throws IOException {
            final JsonElement result = responses.get(path);
            if (result == null) throw new IOException("Unmatched offline API request: " + path);
            return result;
        }

        @Override
        public InputStream download(URI uri) throws IOException {
            assertTrue(uri.toString().startsWith(BASE + "/releases/download/"));
            downloads++;
            beforeDownload.run();
            return new ByteArrayInputStream(body);
        }

        void set(String path, String json) {
            responses.put(path, JsonParser.parseString(json));
        }

        void releases(JsonObject... releases) {
            final JsonArray array = new JsonArray();
            for (JsonObject release : releases) array.add(release);
            responses.put("/releases?per_page=100", array);
        }

        JsonObject release(String tag) {
            return JsonParser.parseString("""
                    {"draft":false,"prerelease":false,"target_commitish":"not-a-branch-or-proof","tag_name":"%s",
                     "url":"%s/releases/42","html_url":"%s/releases/tag/%s",
                     "assets":[{"name":"%s","state":"uploaded","size":%d,"digest":"sha256:%s",
                                "browser_download_url":"%s/releases/download/%s/%s"}]}
                    """.formatted(tag, API, BASE, tag, ASSET, CANDIDATE.length, sha(CANDIDATE), BASE, tag, ASSET)).getAsJsonObject();
        }

        void ref(String name, String type, String sha) {
            set("/git/ref/" + name, """
                    {"ref":"refs/%s","object":{"type":"%s","sha":"%s"}}
                    """.formatted(name, type, sha));
        }

        void ahead(String base, String head) { compare(base, head, "ahead", base); }

        void compare(String base, String head, String status, String mergeBase) {
            set("/compare/" + base + "..." + head + "?per_page=1", """
                    {"status":"%s","ahead_by":1,"behind_by":0,"base_commit":{"sha":"%s"},"merge_base_commit":{"sha":"%s"}}
                    """.formatted(status, base, mergeBase));
        }
    }
}
