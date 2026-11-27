package fun.bm.mili.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortalSearchChunkLoadsTest {
    @Test
    void joinsOnlyIdenticalBoundsAndDoesNotCacheCompletedLoads() {
        var pending = new ArrayList<Consumer<List<ChunkAccess>>>();
        var loads = new PortalSearchChunkLoads((area, complete) -> pending.add(complete));
        var callbacks = new ArrayList<String>();
        loads.load(new BlockPos(-15, 0, 0), 0, chunks -> callbacks.add("first"));
        loads.load(new BlockPos(-1, 80, 15), 0, chunks -> callbacks.add("same bounds"));
        loads.load(new BlockPos(0, 80, 15), 0, chunks -> callbacks.add("different bounds"));
        assertEquals(2, pending.size());

        pending.getFirst().accept(List.of());
        assertEquals(List.of("first", "same bounds"), callbacks);
        pending.get(1).accept(List.of());
        assertEquals(List.of("first", "same bounds", "different bounds"), callbacks);

        loads.load(new BlockPos(-1, 0, 0), 0, chunks -> callbacks.add("new load"));
        assertEquals(3, pending.size());
    }

    @Test
    void aFailingCallbackDoesNotStopItsFollowers() {
        var pending = new ArrayList<Consumer<List<ChunkAccess>>>();
        var loads = new PortalSearchChunkLoads((area, complete) -> pending.add(complete));
        var failure = new IllegalStateException("callback failed");
        var callbacks = new AtomicInteger();
        loads.load(BlockPos.ZERO, 32, chunks -> { throw failure; });
        loads.load(BlockPos.ZERO, 32, chunks -> { throw failure; });
        loads.load(BlockPos.ZERO, 32, chunks -> callbacks.incrementAndGet());
        assertSame(failure, assertThrows(IllegalStateException.class, () -> pending.getFirst().accept(List.of())));
        assertEquals(1, callbacks.get());
    }

    @Test
    void failedSubmissionDoesNotCaptureOtherRequests() {
        var loadsStarted = new AtomicInteger();
        var callbacks = new AtomicInteger();
        var holder = new PortalSearchChunkLoads[1];
        holder[0] = new PortalSearchChunkLoads((area, complete) -> {
            if (loadsStarted.getAndIncrement() == 0) {
                holder[0].load(BlockPos.ZERO, 32, chunks -> callbacks.incrementAndGet());
                throw new IllegalStateException("submission failed");
            }
            complete.accept(List.of());
        });
        assertThrows(IllegalStateException.class, () -> holder[0].load(BlockPos.ZERO, 32, chunks -> callbacks.incrementAndGet()));
        assertEquals(1, callbacks.get());
        holder[0].load(BlockPos.ZERO, 32, chunks -> callbacks.incrementAndGet());
        assertEquals(3, loadsStarted.get());
        assertEquals(2, callbacks.get());
    }
}
