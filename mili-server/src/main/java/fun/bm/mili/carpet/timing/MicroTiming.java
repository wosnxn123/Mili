package fun.bm.mili.carpet.timing;

import ca.spottedleaf.moonrise.common.util.TickThread;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fun.bm.mili.carpet.config.modules.GeneralCompatConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;
import org.jspecify.annotations.Nullable;

/**
 * Local, opt-in event tracing. No region/world enumeration or remote fetching.
 * Event semantics were compared with Lophine 0134 (7a51f49), not its global logger or marker implementation.
 */
public final class MicroTiming {
    private static final int MAX_WATCHES = 16;
    private static final int PAGE_SIZE = 12;
    private static final int MAX_PAGES = Math.ceilDiv(TraceJournal.CAPACITY, PAGE_SIZE);
    private static final int RANGE = 8;
    private static final long LEASE = TimeUnit.SECONDS.toNanos(30);
    private static final ThreadLocal<PhaseScope> PHASE = new ThreadLocal<>();
    // Publication shares watch metadata only; journals are never accessed by management or another region.
    private static volatile List<Watch> watches = List.of();

    private MicroTiming() {}

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        final var read = Commands.literal("read").executes(ctx -> read(ctx.getSource(), TraceJournal.View.ALL, 1));
        for (final var view : TraceJournal.View.values()) {
            read.then(Commands.literal(view.name().toLowerCase(Locale.ROOT))
                .executes(ctx -> read(ctx.getSource(), view, 1))
                .then(Commands.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, MAX_PAGES))
                    .executes(ctx -> read(ctx.getSource(), view, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "page")))));
        }
        dispatcher.register(Commands.literal("microtiming")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("watch").then(Commands.argument("pos", BlockPosArgument.blockPos())
                .executes(ctx -> watch(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"), false))
                .then(Commands.literal("updates").executes(ctx -> watch(ctx.getSource(), BlockPosArgument.getBlockPos(ctx, "pos"), true)))))
            .then(read)
            .then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource()))));
    }

    private static @Nullable ServerPlayer owner(final CommandSourceStack source) {
        final ServerPlayer player = source.getPlayer();
        if (player == null || source.source != player.commandSource() || !TickThread.isTickThreadFor(player)
            || !source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
            || !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            source.sendFailure(Component.literal("MicroTiming requires your own player session on its owning region and operator permission."));
            return null;
        }
        return player;
    }

    private static boolean local(final ServerPlayer player, final ServerLevel level, final BlockPos pos) {
        return player.level() == level && player.blockPosition().distSqr(pos) <= RANGE * RANGE
            && level.isInValidBounds(pos) && TickThread.isTickThreadFor(level, pos) && level.getChunkIfLoaded(pos) != null;
    }

    private static int watch(final CommandSourceStack source, final BlockPos pos, final boolean updates) {
        final ServerPlayer player = owner(source);
        if (player == null) {
            return 0;
        }
        if (!GeneralCompatConfig.microTiming) {
            source.sendFailure(Component.literal("MicroTiming is disabled."));
            return 0;
        }
        final ServerLevel level = (ServerLevel) player.level();
        if (!local(player, level, pos)) {
            source.sendFailure(Component.literal("Watch a loaded block within 8 blocks in your own region."));
            return 0;
        }
        if (!install(player, level, pos, updates)) {
            source.sendFailure(Component.literal("MicroTiming is disabled or all watch slots are in use."));
            return 0;
        }
        source.sendSystemMessage(Component.literal("Watching " + pos.toShortString() + " for 30 seconds ("
            + (updates ? "with neighbor/shape updates" : "basic") + "). Use /microtiming read or stop."));
        return 1;
    }

    private static int read(final CommandSourceStack source, final TraceJournal.View view, final int page) {
        final ServerPlayer player = owner(source);
        if (player == null) {
            return 0;
        }
        if (!GeneralCompatConfig.microTiming) {
            source.sendFailure(Component.literal("MicroTiming is disabled."));
            return 0;
        }
        final long now = System.nanoTime();
        for (final Watch watch : watches) {
            if (watch.owner != player || watch.expired(now)) {
                continue;
            }
            if (!local(player, watch.level, watch.pos)) {
                source.sendFailure(Component.literal("Return within 8 blocks of your watched block in its loaded owning region."));
                return 0;
            }
            final TraceJournal.Snapshot snapshot = watch.journal.snapshot(view);
            final int size = snapshot.lines().size();
            final int pages = Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
            if (page > pages) {
                source.sendFailure(Component.literal("No such MicroTiming page."));
                return 0;
            }
            source.sendSystemMessage(Component.literal("MicroTiming " + page + "/" + pages + " (" + size + " rows, "
                + snapshot.omitted() + " events omitted by budget)"));
            for (int i = (page - 1) * PAGE_SIZE; i < Math.min(size, page * PAGE_SIZE); ++i) {
                source.sendSystemMessage(Component.literal(snapshot.lines().get(i)));
            }
            return 1;
        }
        source.sendFailure(Component.literal("You have no active MicroTiming watch."));
        return 0;
    }

    private static int stop(final CommandSourceStack source) {
        final ServerPlayer player = owner(source);
        if (player == null) {
            return 0;
        }
        disconnected(player);
        source.sendSystemMessage(Component.literal("MicroTiming watch stopped."));
        return 1;
    }

    private static synchronized boolean install(final ServerPlayer owner, final ServerLevel level, final BlockPos pos, final boolean updates) {
        if (!GeneralCompatConfig.microTiming) {
            return false;
        }
        final long now = System.nanoTime();
        final List<Watch> next = new ArrayList<>();
        for (final Watch watch : watches) {
            if (watch.owner != owner && !watch.expired(now)) {
                next.add(watch);
            }
        }
        if (next.size() >= MAX_WATCHES) {
            return false;
        }
        next.add(new Watch(owner, level, pos.immutable(), updates, now + LEASE, new TraceJournal()));
        watches = List.copyOf(next);
        return true;
    }

    public static synchronized void disconnected(final ServerPlayer player) {
        if (!watches.isEmpty()) {
            watches = watches.stream().filter(watch -> watch.owner != player).toList();
        }
    }

    /** Optional config lifecycle callback: discard every old watch, including on off→on reloads. */
    public static synchronized void resetWatches() {
        watches = List.of();
    }

    private static synchronized void expire(final long now) {
        watches = watches.stream().filter(watch -> !watch.expired(now)).toList();
    }

    public static @Nullable PhaseScope phase(final ServerLevel level, final Phase phase) {
        final List<Watch> active = watches;
        if (active.isEmpty()) {
            return null;
        }
        if (!GeneralCompatConfig.microTiming) {
            // Do not revoke a new watch if another thread has already replaced this snapshot.
            synchronized (MicroTiming.class) {
                if (active == watches && !GeneralCompatConfig.microTiming) {
                    resetWatches();
                }
            }
            return null;
        }
        // Install appends watches in lease order; expiry and disconnect only remove entries.
        if (active.getFirst().expired(System.nanoTime())) {
            expire(System.nanoTime());
        }
        for (final Watch watch : watches) {
            if (watch.level == level) {
                final PhaseScope scope = new PhaseScope(level, phase, PHASE.get());
                PHASE.set(scope);
                return scope;
            }
        }
        return null;
    }

    /** Call only at the actual update site. Returns null before details are computed on a miss. */
    public static @Nullable Capture capture(final LevelAccessor level, final @Nullable BlockPos pos, final boolean updatesOnly, final boolean paired) {
        final List<Watch> active = watches;
        if (active.isEmpty() || pos == null || !GeneralCompatConfig.microTiming || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        ArrayList<TraceJournal> journals = null;
        for (final Watch watch : active) {
            if (watch.level != serverLevel || !watch.pos.equals(pos) || (updatesOnly && !watch.updates) || watch.expired(System.nanoTime())) {
                continue;
            }
            if (journals == null && !TickThread.isTickThreadFor(serverLevel, pos)) {
                return null;
            }
            if (watch.journal.reserve(paired ? 2 : 1) == 0) {
                continue;
            }
            if (journals == null) {
                journals = new ArrayList<>();
            }
            journals.add(watch.journal);
        }
        if (journals == null) {
            return null;
        }
        final PhaseScope phase = PHASE.get();
        return new Capture(journals, serverLevel.getRedstoneGameTime(), phase != null && phase.level == level ? phase.phase.label : "outside traced phase");
    }

    public static String blockKey(final Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    public static String blockEventDetails(final net.minecraft.world.level.BlockEventData event) {
        return blockKey(event.block()) + " a=" + event.paramA() + " b=" + event.paramB();
    }

    public static String tickDetails(final ScheduledTick<?> tick) {
        final Object type = tick.type();
        final String key = type instanceof Block block ? blockKey(block)
            : type instanceof Fluid fluid ? BuiltInRegistries.FLUID.getKey(fluid).toString() : "other";
        return key + " due(region tick)=" + tick.triggerTick() + " priority=" + tick.priority() + " order=" + tick.subTickOrder();
    }

    private record Watch(ServerPlayer owner, ServerLevel level, BlockPos pos, boolean updates, long expires, TraceJournal journal) {
        boolean expired(final long now) {
            return now - this.expires >= 0;
        }
    }

    public enum Phase {
        BLOCK_TICKS("scheduled block ticks"), FLUID_TICKS("scheduled fluid ticks"), BLOCK_EVENTS("block events"),
        RANDOM_TICKS("random ticks"), BLOCK_ENTITIES("block entities");

        private final String label;

        Phase(final String label) {
            this.label = label;
        }
    }

    public static final class PhaseScope implements AutoCloseable {
        private final ServerLevel level;
        private final Phase phase;
        private final @Nullable PhaseScope previous;

        private PhaseScope(final ServerLevel level, final Phase phase, final @Nullable PhaseScope previous) {
            this.level = level;
            this.phase = phase;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.previous == null) {
                PHASE.remove();
            } else {
                PHASE.set(this.previous);
            }
        }
    }

    public static final class Capture {
        private final List<TraceJournal> journals;
        private final long tick;
        private final String phase;

        private Capture(final List<TraceJournal> journals, final long tick, final String phase) {
            this.journals = journals;
            this.tick = tick;
            this.phase = phase;
        }

        public void point(final String event, final String detail) {
            for (final TraceJournal journal : this.journals) {
                journal.point(this.tick, this.phase, event, detail);
            }
        }

        public Action begin(final String event, final String detail) {
            final int[] ids = new int[this.journals.size()];
            for (int i = 0; i < ids.length; ++i) {
                ids[i] = this.journals.get(i).begin(this.tick, this.phase, event, detail);
            }
            return new Action(this, event, ids);
        }
    }

    public static final class Action implements AutoCloseable {
        private final Capture capture;
        private final String event;
        private final int[] ids;
        private String result = "aborted";

        private Action(final Capture capture, final String event, final int[] ids) {
            this.capture = capture;
            this.event = event;
            this.ids = ids;
        }

        public void result(final String result) {
            this.result = result;
        }

        @Override
        public void close() {
            for (int i = 0; i < this.ids.length; ++i) {
                this.capture.journals.get(i).end(this.capture.tick, this.capture.phase, this.event, this.ids[i], this.result);
            }
        }
    }
}
