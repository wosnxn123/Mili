/*
 * Paperclip - Paper Minecraft launcher
 * Copyright (c) 2019 Kyle Wood (DemonWav)
 * https://github.com/PaperMC/Paperclip
 * SPDX-License-Identifier: MIT
 *
 * Compatibility replacement for the pinned Riceear 3.0.18 PatchEntry.
 * Preserve its record ABI, hash checks, cache and classpath substitution.
 * Read patch bytes from the active launcher/update JAR, NOT the vanilla JAR.
 */
package moe.luminolmc.riceear;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import moe.luminolmc.riceear.update.AutoUpdate;
import riceear.libs.io.sigpipe.jbsdiff.InvalidHeaderException;
import riceear.libs.io.sigpipe.jbsdiff.Patch;
import riceear.libs.org.apache.commons.compress.compressors.CompressorException;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public record PatchEntry(
    String location,
    byte[] originalHash,
    byte[] patchHash,
    byte[] outputHash,
    String originalPath,
    String patchPath,
    String outputPath
) {
    private static boolean announced;

    public static PatchEntry[] parse(final BufferedReader reader) throws IOException {
        PatchEntry[] result = new PatchEntry[8];
        int index = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            final PatchEntry entry = parseLine(line);
            if (entry == null) continue;
            if (index == result.length) result = Arrays.copyOf(result, index * 2);
            result[index++] = entry;
        }
        return index == result.length ? result : Arrays.copyOf(result, index);
    }

    private static PatchEntry parseLine(final String line) {
        if (line.isBlank() || line.startsWith("#")) return null;
        final String[] parts = line.split("\t");
        if (parts.length != 7) throw new IllegalStateException("Invalid patch data line: " + line);
        return new PatchEntry(parts[0], Util.fromHex(parts[1]), Util.fromHex(parts[2]),
            Util.fromHex(parts[3]), parts[4], parts[5], parts[6]);
    }

    public void applyPatch(final Map<String, Map<String, URL>> urls, final Path originalRootDir,
                           final Path repoDir) throws IOException {
        final Path inputFile = originalRootDir.resolve("META-INF").resolve(this.location).resolve(this.originalPath);
        final Path targetDir = repoDir.resolve(this.location);
        final Path outputFile = targetDir.resolve(this.outputPath);
        if (Files.exists(outputFile) && Util.isFileValid(outputFile, this.outputHash)) {
            urls.get(this.location).put(this.originalPath, outputFile.toUri().toURL());
            return;
        }
        if (!announced) {
            Riceear.logger.info("Applying patches");
            announced = true;
        }
        Riceear.logger.info("  Applying patch to " + this.originalPath);
        final byte[] originalBytes = Util.readBytes(inputFile);
        if (!Util.isDataValid(originalBytes, this.originalHash)) {
            throw new IllegalStateException("Hash check of input file failed for " + this.originalPath);
        }

        // The original root is Mojang's bundler: it has input JARs but no fork patches.
        // Use the same active-target lookup that Riceear uses for patches.list.
        final String fullPatchPath = "/META-INF/" + Util.endingSlash(this.location) + this.patchPath;
        final byte[] patchBytes;
        try (InputStream stream = AutoUpdate.getResourceAsStreamFromTargetJar(fullPatchPath)) {
            if (stream == null) throw new IllegalStateException("Patch file not found: " + fullPatchPath);
            patchBytes = Util.readFully(stream);
        }
        if (!Util.isDataValid(patchBytes, this.patchHash)) {
            throw new IllegalStateException("Hash check of patch file failed for " + this.patchPath);
        }

        try {
            // Patched version paths may be the only entry in their nested directory.
            Files.createDirectories(outputFile.getParent());
            final ByteArrayOutputStream patched = new ByteArrayOutputStream();
            Patch.patch(originalBytes, patchBytes, patched);
            final byte[] outputBytes = patched.toByteArray();
            if (!Util.isDataValid(outputBytes, this.outputHash)) {
                throw new IllegalStateException("Patch not applied correctly for " + this.outputPath);
            }
            try (OutputStream output = Files.newOutputStream(outputFile, CREATE, WRITE, TRUNCATE_EXISTING)) {
                output.write(outputBytes);
            }
            urls.get(this.location).put(this.originalPath, outputFile.toUri().toURL());
        } catch (InvalidHeaderException | CompressorException e) {
            throw new IOException("Failed to apply patch", e);
        }
    }
}
