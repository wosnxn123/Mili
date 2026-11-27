/* SPDX-License-Identifier: LGPL-3.0-or-later
 * XP counter gameplay: Carpet TIS Addition (Fallen_Breath and contributors),
 * revision 62c3cb6fff26cd9c4e12aa616b403c184a4a6ca0, via Lophine 7a51f49a.
 * Native mining-drop collection: inspired by Carpet Org Addition via the same revision of Lophine.
 */
package fun.bm.mili.carpet;

import ca.spottedleaf.moonrise.common.util.TickThread;
import com.mojang.brigadier.CommandDispatcher;
import fun.bm.mili.carpet.config.modules.GameplayCompatConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.leavesmc.leaves.util.WoolUtils;

public final class CollectionGameplayRules {
    private static final Map<ServerLevel, long[]> XP = new WeakHashMap<>();
    private CollectionGameplayRules() {}

    /** Only items from the approved event list whose native spawn succeeded are eligible. */
    public static void offerMiningDrops(ServerPlayer player, List<ItemEntity> spawned) {
        if (spawned.isEmpty() || !GameplayCompatConfig.blockDropsDirectlyEnterInventory || !TickThread.isTickThreadFor(player)) return;
        final ServerLevel world = player.level();
        // Never clear a pickup delay. One bounded callback per block, not per item.
        final List<ItemEntity> drops = List.copyOf(spawned);
        player.getBukkitEntity().taskScheduler.schedule((ServerPlayer current) -> {
            for (ItemEntity item : drops) {
                if (current != player || !GameplayCompatConfig.blockDropsDirectlyEnterInventory || !current.isAlive() || current.isRemoved()
                    || current.isSpectator() || !TickThread.isTickThreadFor(current) || current.level() != world) break;
                if (TickThread.isTickThreadFor(item) && item.level() == world && item.isAlive() && !item.isRemoved()
                    && !item.hasPickUpDelay() && item.distanceToSqr(current) <= 64.0 && world.getEntity(item.getId()) == item) {
                    item.playerTouch(current); // Permissions, pickup events, ownership, and partial/full inventory use the native path.
                }
            }
        }, null, 12L);
    }

    private static DyeColor color(HopperBlockEntity hopper) {
        if (!GameplayCompatConfig.hopperXpCounters || !(hopper.getLevel() instanceof ServerLevel level)
            || !GameplayCompatRules.ownsLoaded(level, hopper.getBlockPos()) || hopper.isRemoved()) return null;
        final var state = hopper.getBlockState();
        if (!(state.getBlock() instanceof HopperBlock) || !state.getValue(HopperBlock.ENABLED)) return null;
        final BlockPos target = hopper.getBlockPos().relative(state.getValue(HopperBlock.FACING));
        return GameplayCompatRules.ownsLoaded(level, target) ? WoolUtils.getWoolColorAtPosition(level, target) : null;
    }

    public static boolean needsXpTicks(HopperBlockEntity hopper) { return color(hopper) != null; }

    public static void collectXp(HopperBlockEntity hopper) {
        final DyeColor color = color(hopper);
        if (color == null || !(hopper.getLevel() instanceof ServerLevel level)) return;
        final var mouth = hopper.getSuckAabb().move(hopper.getLevelX() - 0.5, hopper.getLevelY() - 0.5, hopper.getLevelZ() - 0.5);
        if (!TickThread.isTickThreadFor(level, mouth)) return;
        final var orbs = new ArrayList<ExperienceOrb>();
        level.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), mouth,
            orb -> TickThread.isTickThreadFor(orb) && orb.isAlive() && !orb.isRemoved(), orbs, 16);
        for (ExperienceOrb orb : orbs) {
            if (!TickThread.isTickThreadFor(orb) || orb.isRemoved() || orb.level() != level || !orb.getBoundingBox().intersects(mouth)) continue;
            if (color(hopper) != color) break;
            final long amount = (long) orb.getValue() * orb.count;
            if (orb.getValue() <= 0 || orb.count <= 0) continue;
            orb.discard(org.bukkit.event.entity.EntityRemoveEvent.Cause.PICKUP);
            if (orb.isRemoved()) synchronized (XP) {
                final long[] counters = XP.computeIfAbsent(level, ignored -> new long[DyeColor.values().length]);
                counters[color.ordinal()] = addWithoutOverflow(counters[color.ordinal()], amount);
            }
        }
    }

    static long addWithoutOverflow(long total, long amount) { return amount > Long.MAX_VALUE - total ? Long.MAX_VALUE : total + amount; }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!GameplayCompatConfig.hopperXpCounters) return; // Leave this command name available to plugins by default.
        var root = Commands.literal("xcounter").requires(source -> GameplayCompatConfig.hopperXpCounters
            && source.getBukkitSender().hasPermission("mili.commands.counter"));
        root.executes(context -> report(context.getSource(), null, false));
        root.then(Commands.literal("reset").requires(source -> source.getBukkitSender().hasPermission("mili.commands.counter.reset"))
            .executes(context -> report(context.getSource(), null, true)));
        for (DyeColor color : DyeColor.values()) root.then(Commands.literal(color.getName())
            .executes(context -> report(context.getSource(), color, false))
            .then(Commands.literal("reset").requires(source -> source.getBukkitSender().hasPermission("mili.commands.counter.reset"))
                .executes(context -> report(context.getSource(), color, true))));
        dispatcher.register(root);
    }

    private static int report(CommandSourceStack source, DyeColor selected, boolean reset) {
        final List<String> lines = new ArrayList<>();
        synchronized (XP) {
            final long[] counters = XP.get(source.getLevel());
            for (DyeColor color : DyeColor.values()) {
                if (selected != null && selected != color) continue;
                long value = counters == null ? 0L : counters[color.ordinal()];
                if (reset && counters != null) counters[color.ordinal()] = 0L;
                if (selected != null || value != 0L) lines.add(color.getName() + ": " + value + " XP" + (reset ? " (reset)" : ""));
            }
        }
        if (lines.isEmpty()) lines.add("No XP counted in this world.");
        for (String line : lines) source.sendSuccess(() -> Component.literal(line), false);
        return 1;
    }
}
