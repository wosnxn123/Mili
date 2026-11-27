/*
 * Based on Lophine's PortalSearchChunkLoadCoordinator, by Bacteriawa.
 * LophineLabs/Lophine, 7a51f49a, Minecraft patch 0027; GPL-3.0.
 */
package fun.bm.mili.portal;

import ca.spottedleaf.concurrentutil.util.Priority;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Shares successful in-flight Moonrise submissions, not the tickets or portal results. */
public final class PortalSearchChunkLoads {
    private final AreaLoader loader;
    private final ConcurrentHashMap<Area, Pending> pending = new ConcurrentHashMap<>();

    public PortalSearchChunkLoads(final ServerLevel level) {
        this((area, complete) -> level.moonrise$loadChunksAsync(area.minX, area.maxX, area.minZ, area.maxZ,
            ChunkStatus.EMPTY, Priority.HIGH, complete));
    }

    PortalSearchChunkLoads(final AreaLoader loader) {
        this.loader = loader;
    }

    public void load(final BlockPos pos, final int radius, final Consumer<List<ChunkAccess>> onLoad) {
        final Area area = new Area((pos.getX() - radius) >> 4, (pos.getX() + radius) >> 4,
            (pos.getZ() - radius) >> 4, (pos.getZ() + radius) >> 4);
        final Pending existing = this.pending.get(area);
        if (existing != null) {
            this.joinOrSubmit(area, existing, onLoad);
            return;
        }
        final Pending candidate = new Pending(onLoad);
        final Pending previous = this.pending.putIfAbsent(area, candidate);
        if (previous != null) {
            this.joinOrSubmit(area, previous, onLoad);
            return;
        }
        try {
            this.loader.load(area, chunks -> this.complete(area, candidate, chunks));
        } catch (RuntimeException | Error ex) {
            candidate.close();
            this.pending.remove(area, candidate);
            throw ex;
        }
        candidate.open();
    }

    private void joinOrSubmit(final Area area, final Pending existing, final Consumer<List<ChunkAccess>> onLoad) {
        // Never queue behind a submission that could still throw, or behind a completed callback.
        if (!existing.add(onLoad)) {
            this.loader.load(area, onLoad);
        }
    }

    private void complete(final Area area, final Pending load, final List<ChunkAccess> chunks) {
        final List<Consumer<List<ChunkAccess>>> callbacks = load.close();
        this.pending.remove(area, load);
        Throwable first = null;
        for (final var callback : callbacks) {
            try {
                // Run before returning to Moonrise, while it still holds the load tickets.
                callback.accept(chunks);
            } catch (RuntimeException | Error ex) {
                if (first == null) {
                    first = ex;
                } else if (first != ex) {
                    first.addSuppressed(ex);
                }
            }
        }
        if (first instanceof RuntimeException ex) {
            throw ex;
        }
        if (first instanceof Error ex) {
            throw ex;
        }
    }

    record Area(int minX, int maxX, int minZ, int maxZ) {
    }

    @FunctionalInterface
    interface AreaLoader {
        void load(Area area, Consumer<List<ChunkAccess>> complete);
    }

    private static final class Pending {
        private final List<Consumer<List<ChunkAccess>>> callbacks = new ArrayList<>(2);
        private boolean open;
        private boolean closed;

        Pending(final Consumer<List<ChunkAccess>> first) {
            this.callbacks.add(first);
        }

        synchronized void open() {
            this.open = !this.closed;
        }

        synchronized boolean add(final Consumer<List<ChunkAccess>> callback) {
            if (!this.open) {
                return false;
            }
            this.callbacks.add(callback);
            return true;
        }

        synchronized List<Consumer<List<ChunkAccess>>> close() {
            if (this.closed) {
                return List.of();
            }
            this.closed = true;
            this.open = false;
            return this.callbacks;
        }
    }
}
