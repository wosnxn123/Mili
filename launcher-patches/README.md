# Riceear 3.0.18 compatibility patch

The checksum-pinned Hyacinthusclip 3.0.18 asset actually contains the Riceear
launcher. Its `PatchEntry.applyPatch` reads fork patch payloads from the vanilla
Mojang bundler, even though those payloads are in the active launcher/update JAR.
A fresh installation consequently fails with `NoSuchFileException`.

This directory supplies an ABI-compatible `moe.luminolmc.riceear.PatchEntry`
replacement. It reads the original input from Mojang's JAR and patch bytes through
`AutoUpdate.getResourceAsStreamFromTargetJar`, the same lookup used for
`patches.list`. Original-input, patch and reconstructed-output SHA-256 checks,
valid-cache reuse and classpath substitution remain intact.

Both Bundler and Paperclip packaging compile and embed the replacement plus
`META-INF/LICENSE-mili-launcher.txt` and the `META-INF/mili-launcher-patch` marker.
The original downloaded launcher and its pinned checksum are not modified.

## Verification

```sh
./gradlew :mili-server:verifyLauncherPatch --no-configuration-cache
python3 -I scripts/smoke_test_launcher.py path/to/mili-paperclip.jar
```

The first task runs 17 offline regression assertions using miniature vanilla and
active-update JAR fixtures. The second runs the real Riceear patch-only path in a
fresh directory, downloads only the hash-checked official vanilla bundler if
needed, seeds unchanged dependencies and verifies every reconstructed output.
GitHub Actions runs the second check before artifact upload and release.

Neither check starts Minecraft, creates a world or accepts the EULA. Full server,
Mixin and staged-update end-to-end tests are separate. Regression test classes are
not packaged into the release JAR.

The compatibility source retains the original MIT notices in `LICENSE.txt`;
this does not change the license of the rest of the project.
