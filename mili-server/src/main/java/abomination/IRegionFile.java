package abomination;

import ca.spottedleaf.moonrise.patches.chunk_system.storage.ChunkSystemRegionFile;
import net.minecraft.world.level.ChunkPos;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public interface IRegionFile extends ChunkSystemRegionFile, AutoCloseable {
    Path getPath();

    DataInputStream getChunkDataInputStream(ChunkPos pos) throws IOException;

    boolean doesChunkExist(ChunkPos pos) throws Exception;

    DataOutputStream getChunkDataOutputStream(ChunkPos pos) throws IOException;

    void flush() throws IOException;

    void clear(ChunkPos pos) throws IOException;

    boolean hasChunk(ChunkPos pos);

    void close() throws IOException;

    void write(ChunkPos pos, ByteBuffer buf) throws IOException;

    // Legacy oversized .mca recovery is owned by Moonrise's RegionFileRecovery
    // in 26.3. Linear formats never supported those sidecar files.
    boolean recalculateHeader() throws IOException;

    default int getRecalculateCount() {
        return 0;
    } // Luminol - Configurable region file format

    @Override
    default int moonrise$getRecalculateCount() {
        return getRecalculateCount();
    }

    @Override
    default boolean moonrise$recalculateHeader() throws IOException {
        return recalculateHeader();
    }

    @Override
    default Boolean moonrise$tryRecalcHeader(final int expectedRecalcCount) throws IOException {
        synchronized (this) {
            if (moonrise$getRecalculateCount() != expectedRecalcCount) {
                // The caller must retry its read when a concurrent repair changed the header.
                return null;
            }
            return Boolean.valueOf(moonrise$recalculateHeader());
        }
    }
}