/*
 * Region-owned snapshot design adapted from Lophine patch 0136 by Bacteriawa (GPL-3.0).
 * Source: LophineLabs/Lophine at 7a51f49a512f1b5465f15a905d3704754e4bed02.
 */
package fun.bm.mili.utils;

import io.papermc.paper.threadedregions.EntityScheduler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

public final class PlayerEnderPearls {
    private record Entry(CompoundTag tag, @Nullable EntityScheduler scheduler) {
    }

    private final Map<UUID, Entry> entries = new HashMap<>();
    private boolean closing;

    // Called only with a fully serialized pearl from its owning region. The published tag is never mutated.
    public synchronized boolean publish(final UUID id, final CompoundTag tag, final EntityScheduler scheduler) {
        if (this.closing) {
            return false;
        }
        this.entries.put(id, new Entry(tag, scheduler));
        return true;
    }

    public synchronized boolean stageRestore(final UUID id, final CompoundTag tag) {
        return !this.closing && this.entries.putIfAbsent(id, new Entry(tag.copy(), null)) == null;
    }

    public synchronized void forget(final UUID id, final @Nullable EntityScheduler scheduler) {
        if (!this.closing) {
            Entry entry = this.entries.get(id);
            if (entry != null && entry.scheduler() == scheduler) {
                this.entries.remove(id);
            }
        }
    }

    public synchronized boolean isClosing() {
        return this.closing;
    }

    public synchronized void close() {
        this.closing = true;
    }

    public synchronized Map<UUID, EntityScheduler> closeAndGetSchedulers() {
        if (this.closing) return Map.of();
        this.closing = true;
        return schedulers();
    }

    public synchronized Map<UUID, EntityScheduler> schedulers() {
        Map<UUID, EntityScheduler> result = new HashMap<>();
        this.entries.forEach((id, entry) -> {
            if (entry.scheduler() != null) {
                result.put(id, entry.scheduler());
            }
        });
        return result;
    }

    public synchronized List<CompoundTag> snapshots() {
        return this.entries.values().stream().map(entry -> entry.tag().copy()).toList();
    }
}
