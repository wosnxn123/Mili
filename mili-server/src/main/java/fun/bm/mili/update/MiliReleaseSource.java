package fun.bm.mili.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class MiliReleaseSource {
    static final String VERSION = "26.3";
    static final String ASSET = "mili-26.3-paperclip.jar";
    static final String REPOSITORY = "wosnxn123/Mili";
    static final String API = "https://api.github.com/repos/" + REPOSITORY;
    private static final String WEB = "https://github.com/" + REPOSITORY;
    static final long MAX_JAR_BYTES = 512L * 1024 * 1024;
    static final String COMMIT = "[0-9a-f]{40}";
    static final String DIGEST = "[0-9a-f]{64}";
    private static final String SHORT_COMMIT = "[0-9a-f]{7,40}";
    private static final int MAX_JSON_BYTES = 8 * 1024 * 1024;
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    private final Transport transport;

    MiliReleaseSource(Transport transport) {
        this.transport = transport;
    }

    Release find(String buildCommit) throws IOException {
        buildCommit = buildCommit.toLowerCase(Locale.ROOT);
        if (!buildCommit.matches(SHORT_COMMIT)) {
            throw new IOException("Missing or invalid Mili build commit");
        }
        final String current = fullSha(object("/commits/" + buildCommit).get("sha"));
        if (!current.startsWith(buildCommit)) throw new IOException("GitHub resolved a different Mili build commit");
        final Ref branch = ref("heads/ver/" + VERSION);
        if (!branch.type().equals("commit") || !onLine(current, branch.sha())) {
            throw new IOException("The running commit is not on the official Mili " + VERSION + " history");
        }

        final JsonElement releases = transport.json("/releases?per_page=100");
        if (!releases.isJsonArray()) throw new IOException("Invalid GitHub releases response");
        for (JsonElement entry : releases.getAsJsonArray()) {
            if (!(entry instanceof JsonObject release)) continue;
            final Asset asset = parseAsset(release);
            if (asset == null) continue;
            final String commit = resolveTag(asset.tag());
            if (!asset.tag().equals(VERSION) && !commit.startsWith(asset.tag().substring(VERSION.length() + 1))) continue;
            if (!strictlyAhead(current, commit) || !onLine(commit, branch.sha())) continue;
            return new Release(asset.tag(), commit, asset.sha256(), asset.size(), asset.url());
        }
        return null;
    }

    InputStream download(Release release) throws IOException {
        return transport.download(release.url());
    }

    boolean strictlyAhead(String base, String head) throws IOException {
        if (!base.matches(COMMIT) || !head.matches(COMMIT)) throw new IOException("Invalid full commit ID");
        if (base.equals(head)) return false;
        final JsonObject comparison = object("/compare/" + base + "..." + head + "?per_page=1");
        final String status = string(comparison.get("status"));
        if (status.equals("behind") || status.equals("diverged")) return false;
        if (!status.equals("ahead")) throw new IOException("Unknown GitHub commit comparison status: " + status);
        if (!(comparison.get("base_commit") instanceof JsonObject baseCommit)
                || !(comparison.get("merge_base_commit") instanceof JsonObject mergeBase)
                || !base.equals(fullSha(baseCommit.get("sha")))
                || !base.equals(fullSha(mergeBase.get("sha")))
                || integer(comparison.get("ahead_by")) <= 0 || integer(comparison.get("behind_by")) != 0) {
            throw new IOException("GitHub did not prove the requested commit ancestry");
        }
        return true;
    }

    private boolean onLine(String commit, String tip) throws IOException {
        return commit.equals(tip) || strictlyAhead(commit, tip);
    }

    private String resolveTag(String tag) throws IOException {
        Ref ref = ref("tags/" + tag);
        for (int depth = 0; depth < 8; depth++) {
            if (ref.type().equals("commit")) return ref.sha();
            if (!ref.type().equals("tag")) break;
            final JsonObject annotated = object("/git/tags/" + ref.sha());
            if (!ref.sha().equals(fullSha(annotated.get("sha")))) throw new IOException("Wrong annotated tag object");
            ref = refObject(annotated.get("object"));
        }
        throw new IOException("Release tag does not resolve to a commit: " + tag);
    }

    private Ref ref(String name) throws IOException {
        final JsonObject response = object("/git/ref/" + name);
        if (!string(response.get("ref")).equals("refs/" + name)) throw new IOException("Wrong GitHub ref: " + name);
        return refObject(response.get("object"));
    }

    private Ref refObject(JsonElement value) throws IOException {
        if (!(value instanceof JsonObject object)) throw new IOException("Invalid Git ref object");
        return new Ref(string(object.get("type")), fullSha(object.get("sha")));
    }

    private JsonObject object(String path) throws IOException {
        if (transport.json(path) instanceof JsonObject object) return object;
        throw new IOException("Invalid GitHub object at " + path);
    }

    private static Asset parseAsset(JsonObject release) {
        if (!isFalse(release.get("draft")) || !isFalse(release.get("prerelease"))) return null;
        final String tag = string(release.get("tag_name"));
        if (!tag.equals(VERSION) && !tag.matches("26\\.3-" + SHORT_COMMIT)) return null;
        if (!string(release.get("html_url")).equals(WEB + "/releases/tag/" + tag)
                || !string(release.get("url")).matches(java.util.regex.Pattern.quote(API) + "/releases/[0-9]+")
                || !release.has("assets") || !release.get("assets").isJsonArray()) return null;
        JsonObject match = null;
        for (JsonElement item : release.getAsJsonArray("assets")) {
            if (item instanceof JsonObject asset && ASSET.equals(string(asset.get("name")))) {
                if (match != null) return null;
                match = asset;
            }
        }
        if (match == null || !string(match.get("state")).equals("uploaded")) return null;
        final String digest = string(match.get("digest")).toLowerCase(Locale.ROOT);
        final long size = integer(match.get("size"));
        final String url = WEB + "/releases/download/" + tag + "/" + ASSET;
        if (!digest.matches("sha256:" + DIGEST) || size <= 0 || size > MAX_JAR_BYTES
                || !url.equals(string(match.get("browser_download_url")))) return null;
        return new Asset(tag, digest.substring("sha256:".length()), size, URI.create(url));
    }

    private static boolean isFalse(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && !value.getAsBoolean();
    }

    private static String string(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : "";
    }

    private static long integer(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return -1;
        try {
            return value.getAsBigDecimal().longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return -1;
        }
    }

    private static String fullSha(JsonElement value) throws IOException {
        final String sha = string(value).toLowerCase(Locale.ROOT);
        if (!sha.matches(COMMIT)) throw new IOException("Invalid GitHub commit or tag SHA");
        return sha;
    }

    record Release(String tag, String commit, String sha256, long size, URI url) {}
    private record Asset(String tag, String sha256, long size, URI url) {}
    private record Ref(String type, String sha) {}

    interface Transport {
        JsonElement json(String path) throws IOException;
        InputStream download(URI url) throws IOException;
    }

    static final class GitHub implements Transport {
        @Override
        public JsonElement json(String path) throws IOException {
            final HttpURLConnection connection = open(URI.create(API + path), "application/vnd.github+json");
            try {
                if (connection.getResponseCode() != 200) throw new IOException("GitHub API returned HTTP " + connection.getResponseCode());
                try (InputStream input = connection.getInputStream()) {
                    final byte[] bytes = input.readNBytes(MAX_JSON_BYTES + 1);
                    if (bytes.length > MAX_JSON_BYTES) throw new IOException("GitHub response exceeds size limit");
                    final JsonElement result = JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonElement.class);
                    if (result == null) throw new IOException("Empty GitHub response");
                    return result;
                }
            } finally {
                connection.disconnect();
            }
        }

        @Override
        public InputStream download(URI uri) throws IOException {
            for (int redirects = 0; redirects < 4; redirects++) {
                final HttpURLConnection connection = open(uri, "application/octet-stream");
                boolean keepOpen = false;
                try {
                    final int status = connection.getResponseCode();
                    if (status == 200) {
                        final InputStream stream = connection.getInputStream();
                        keepOpen = true;
                        return new FilterInputStream(stream) {
                            @Override
                            public void close() throws IOException {
                                try {
                                    super.close();
                                } finally {
                                    connection.disconnect();
                                }
                            }
                        };
                    }
                    if (status != 301 && status != 302 && status != 303 && status != 307 && status != 308) {
                        throw new IOException("GitHub asset returned HTTP " + status);
                    }
                    uri = assetRedirect(uri, connection.getHeaderField("Location"));
                } finally {
                    if (!keepOpen) connection.disconnect();
                }
            }
            throw new IOException("Too many GitHub asset redirects");
        }

        private static HttpURLConnection open(URI uri, String accept) throws IOException {
            final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", accept);
            connection.setRequestProperty("User-Agent", "Mili Auto Update");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(30_000);
            return connection;
        }
    }

    static URI assetRedirect(URI previous, String location) throws IOException {
        if (location == null) throw new IOException("Missing GitHub asset redirect location");
        final URI next = previous.resolve(location);
        if (!"https".equalsIgnoreCase(next.getScheme()) || next.getRawUserInfo() != null || next.getPort() != -1
                || next.getFragment() != null || !("release-assets.githubusercontent.com".equalsIgnoreCase(next.getHost())
                || "objects.githubusercontent.com".equalsIgnoreCase(next.getHost()))) {
            throw new IOException("Untrusted GitHub asset redirect");
        }
        return next;
    }
}
