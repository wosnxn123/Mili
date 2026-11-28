package fun.bm.mili.carpet.timing;

import ca.spottedleaf.moonrise.common.util.TickThread;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fun.bm.mili.carpet.config.modules.GeneralCompatConfig;
import io.papermc.paper.threadedregions.RegionizedWorldData;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MicroTimingTest {
    private static final BlockPos POS = new BlockPos(4, 64, 4);
    private final boolean prior = GeneralCompatConfig.microTiming;

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach void restore() {
        GeneralCompatConfig.microTiming = prior;
        MicroTiming.resetWatches();
    }

    @Test void pendingBoundaryKeepsItsSlotAndViewsDoNotChangeTheJournal() {
        var journal = new TraceJournal();
        assertNotEquals(0, journal.reserve(2));
        int id = journal.begin(12, "block", "dispatch", "start");
        for (int i = 0; i < TraceJournal.CAPACITY; ++i) {
            if (journal.reserve(1) != 0) journal.point(12, "block", "neighbor", "same");
        }
        journal.end(12, "block", "dispatch", id, "returned");
        var all = journal.snapshot(TraceJournal.View.ALL);
        assertEquals(128, all.lines().size());
        assertEquals(2, all.omitted());
        assertTrue(all.lines().getLast().endsWith("-#1 returned"));
        assertTrue(journal.snapshot(TraceJournal.View.MERGED).lines().get(1).endsWith("×126"));
        assertEquals(3, journal.snapshot(TraceJournal.View.UNIQUE).lines().size());
        assertEquals(all, journal.snapshot(TraceJournal.View.ALL));
    }

    private static final class Session implements AutoCloseable {
        final ServerPlayer player = mock(ServerPlayer.class);
        final ServerLevel level = mock(ServerLevel.class);
        final CommandSource origin = mock(CommandSource.class);
        final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        final MockedStatic<TickThread> threads;

        Session() {
            when(player.commandSource()).thenReturn(origin);
            when(player.permissions()).thenReturn(PermissionSet.ALL_PERMISSIONS);
            when(player.level()).thenReturn(level);
            when(player.blockPosition()).thenReturn(POS);
            when(level.isInValidBounds(POS)).thenReturn(true);
            when(level.getChunkIfLoaded(POS)).thenReturn(mock(LevelChunk.class));
            MicroTiming.register(dispatcher);
            threads = mockStatic(TickThread.class);
            threads.when(() -> TickThread.isTickThreadFor(player)).thenReturn(true);
            threads.when(() -> TickThread.isTickThreadFor(level, POS)).thenReturn(true);
        }

        CommandSourceStack source(CommandSource sender, PermissionSet perms) {
            return new CommandSourceStack(sender, Vec3.atLowerCornerOf(POS), Vec2.ZERO, level, perms, null, player);
        }

        int run(String args) throws CommandSyntaxException {
            return dispatcher.execute("microtiming " + args, source(origin, PermissionSet.ALL_PERMISSIONS));
        }

        @Override public void close() { threads.close(); }
    }

    @Test void bothConfigCallbacksDiscardSubscriptionsEvenWithoutAnInterveningTick() throws Exception {
        try (var s = new Session()) {
            var config = new GeneralCompatConfig();
            GeneralCompatConfig.microTiming = false;
            assertEquals(0, s.run("watch 4 64 4"));
            GeneralCompatConfig.microTiming = true;
            assertEquals(1, s.run("watch 4 64 4"));
            GeneralCompatConfig.microTiming = false;
            config.onUnloaded(null);
            GeneralCompatConfig.microTiming = true;
            assertEquals(0, s.run("read"));
            assertEquals(1, s.run("watch 4 64 4 updates"));
            config.onLoaded(null, null);
            assertNull(MicroTiming.capture(s.level, POS, false, false));
            assertEquals(0, s.run("read"));
        }
    }

    @Test void recordingAndReadingRespectRegionSessionAndCurrentPermissions() throws Exception {
        GeneralCompatConfig.microTiming = true;
        try (var s = new Session()) {
            assertThrows(CommandSyntaxException.class, () -> s.dispatcher.execute("microtiming watch 4 64 4",
                s.source(s.origin, PermissionSet.NO_PERMISSIONS)));
            assertEquals(0, s.dispatcher.execute("microtiming watch 4 64 4",
                s.source(mock(CommandSource.class), PermissionSet.ALL_PERMISSIONS)));
            assertEquals(1, s.run("watch 4 64 4"));
            assertNull(MicroTiming.capture(s.level, POS, true, false));
            assertNull(MicroTiming.capture(s.level, POS.above(), false, false));
            s.threads.when(() -> TickThread.isTickThreadFor(s.level, POS)).thenReturn(false);
            assertNull(MicroTiming.capture(s.level, POS, false, false));
            assertEquals(0, s.run("read"));
            s.threads.when(() -> TickThread.isTickThreadFor(s.level, POS)).thenReturn(true);
            when(s.player.permissions()).thenReturn(PermissionSet.NO_PERMISSIONS);
            assertEquals(0, s.run("read"));
            when(s.player.permissions()).thenReturn(PermissionSet.ALL_PERMISSIONS);
            when(s.player.blockPosition()).thenReturn(POS.offset(9, 0, 0));
            assertEquals(0, s.run("read"));
            when(s.player.blockPosition()).thenReturn(POS);
            MicroTiming.disconnected(mock(ServerPlayer.class));
            assertEquals(1, s.run("read"));
            MicroTiming.disconnected(s.player);
            assertEquals(0, s.run("read"));
        }
    }

    @Test void nativeQueueReportsDuplicateButDispatchesOnlyOnce() throws Exception {
        GeneralCompatConfig.microTiming = true;
        try (var s = new Session()) {
            assertEquals(1, s.run("watch 4 64 4"));
            var ticks = new LevelTicks<Block>(key -> true, s.level, true);
            var region = mock(RegionizedWorldData.class);
            when(s.level.getCurrentWorldData()).thenReturn(region);
            when(region.getBlockLevelTicks()).thenReturn(ticks);
            ticks.addContainer(new ChunkPos(0, 0), new LevelChunkTicks<>());
            var tick = new ScheduledTick<>(Blocks.OBSERVER, POS, 66, TickPriority.HIGH, 42);
            int[] dispatched = {0};
            try (var scope = MicroTiming.phase(s.level, MicroTiming.Phase.BLOCK_TICKS)) {
                ticks.schedule(tick);
                ticks.schedule(tick);
                ticks.tick(66, 5, (pos, block) -> ++dispatched[0]);
            }
            assertEquals(1, dispatched[0]);
            clearInvocations(s.player);
            assertEquals(1, s.run("read unique"));
            var messages = ArgumentCaptor.forClass(Component.class);
            verify(s.player, atLeastOnce()).sendSystemMessage(messages.capture());
            var lines = messages.getAllValues().stream().map(Component::getString).toList();
            for (var expected : new String[]{"queued minecraft:observer", "duplicate minecraft:observer",
                "[scheduled block ticks] dispatch scheduled tick +#1", "dispatch scheduled tick -#1 callback returned"}) {
                assertTrue(lines.stream().anyMatch(line -> line.contains(expected)), () -> expected + " in " + lines);
            }
        }
    }
}
