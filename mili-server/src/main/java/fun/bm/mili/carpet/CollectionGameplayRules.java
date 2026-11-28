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
    private static final Map<ServerLevel, XpCounterWindow[]> XP = new WeakHashMap<>();
    private static final org.bukkit.NamespacedKey DROP_PREFERENCE = new org.bukkit.NamespacedKey("mili", "carpet_org_blockdropsdirectlyenterinventory");
    private CollectionGameplayRules() {}

    static String miningDropMode() {
        return !GameplayCompatConfig.blockDropsDirectlyEnterInventory ? "false" : GameplayCompatConfig.miningDropPlayerControl ? "custom" : "true";
    }

    private static boolean wantsDrops(ServerPlayer player) {
        return GameplayCompatConfig.blockDropsDirectlyEnterInventory && (!GameplayCompatConfig.miningDropPlayerControl
            || player.getBukkitEntity().getPersistentDataContainer().getOrDefault(DROP_PREFERENCE, org.bukkit.persistence.PersistentDataType.BYTE, (byte)0) != 0);
    }

    private static int preference(CommandSourceStack source, ServerPlayer target, Boolean enabled) {
        if (source.getEntity() != target && (!(target instanceof org.leavesmc.leaves.bot.ServerBot)
            || !fun.bm.mili.config.modules.function.FakeplayerConfig.canModifyConfig
            || !org.leavesmc.leaves.command.bot.BotCommand.hasPermission(source.getBukkitSender(), "config"))) {
            source.sendFailure(Component.literal("You may change only your own preference or that of a bot you can configure."));
            return 0;
        }
        if (!TickThread.isTickThreadFor(target)) {
            source.sendFailure(Component.literal("Change this preference in the player's current region."));
            return 0;
        }
        var data = target.getBukkitEntity().getPersistentDataContainer();
        if (Boolean.TRUE.equals(enabled)) data.set(DROP_PREFERENCE, org.bukkit.persistence.PersistentDataType.BYTE, (byte)1);
        else if (Boolean.FALSE.equals(enabled)) data.remove(DROP_PREFERENCE);
        boolean selected = data.getOrDefault(DROP_PREFERENCE, org.bukkit.persistence.PersistentDataType.BYTE, (byte)0) != 0;
        source.sendSuccess(() -> Component.literal(target.getScoreboardName() + " blockDropsDirectlyEnterInventory = " + selected), false);
        return 1;
    }

    /** Only items from the approved event list whose native spawn succeeded are eligible. */
    public static void offerMiningDrops(ServerPlayer player, List<ItemEntity> spawned) {
        if (spawned.isEmpty() || !TickThread.isTickThreadFor(player) || !wantsDrops(player)) return;
        final ServerLevel world = player.level();
        // Never clear a pickup delay. One bounded callback per block, not per item.
        final List<ItemEntity> drops = List.copyOf(spawned);
        player.getBukkitEntity().taskScheduler.schedule((ServerPlayer current) -> {
            if (!TickThread.isTickThreadFor(current) || !wantsDrops(current)) return;
            for (ItemEntity item : drops) {
                if (current != player || !current.isAlive() || current.isRemoved() || current.isSpectator() || current.level() != world) break;
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
            final int value = orb.getValue(), count = orb.count;
            if (value <= 0 || count <= 0) continue;
            orb.discard(org.bukkit.event.entity.EntityRemoveEvent.Cause.PICKUP);
            if (orb.isRemoved()) synchronized (XP) {
                var counters = XP.computeIfAbsent(level, ignored -> new XpCounterWindow[DyeColor.values().length]);
                if (counters[color.ordinal()] == null) counters[color.ordinal()] = new XpCounterWindow(level.getGameTime(), System.nanoTime());
                counters[color.ordinal()].record(value, count);
            }
        }
    }

    static long addWithoutOverflow(long total, long amount) { return amount > Long.MAX_VALUE - total ? Long.MAX_VALUE : total + amount; }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (GameplayCompatConfig.miningDropPlayerControl) {
            var rule = Commands.literal("blockDropsDirectlyEnterInventory")
                .executes(ctx -> preference(ctx.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"), null))
                .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                    .executes(ctx -> preference(ctx.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(ctx, "player"),
                        com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value"))));
            dispatcher.register(Commands.literal("orange").then(Commands.literal("ruleself")
                .requires(src -> GameplayCompatConfig.miningDropPlayerControl && GameplayCompatConfig.blockDropsDirectlyEnterInventory)
                .then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).then(rule))));
        }
        if (!GameplayCompatConfig.hopperXpCounters) return;
        var root = Commands.literal("xcounter").requires(source -> GameplayCompatConfig.hopperXpCounters
            && source.getBukkitSender().hasPermission("mili.commands.counter"));
        root.executes(ctx -> report(ctx.getSource(), null, false, false));
        root.then(Commands.literal("reset").requires(source -> source.getBukkitSender().hasPermission("mili.commands.counter.reset"))
            .executes(ctx -> report(ctx.getSource(), null, false, true)));
        for (DyeColor color : DyeColor.values()) root.then(Commands.literal(color.getName())
            .executes(ctx -> report(ctx.getSource(), color, false, false))
            .then(Commands.literal("realtime").executes(ctx -> report(ctx.getSource(), color, true, false)))
            .then(Commands.literal("reset").requires(source -> source.getBukkitSender().hasPermission("mili.commands.counter.reset"))
                .executes(ctx -> report(ctx.getSource(), color, false, true))));
        dispatcher.register(root);
    }

    private static XpCounterWindow.Snapshot sample(ServerLevel level, DyeColor color, boolean realtime, boolean detailed, boolean reset) {
        long tick = level.getGameTime(), now = System.nanoTime();
        synchronized (XP) {
            var colors = XP.get(level);
            if (colors == null || colors[color.ordinal()] == null) return null;
            var result = colors[color.ordinal()].snapshot(tick, now, realtime, detailed);
            if (reset) colors[color.ordinal()] = null;
            return result;
        }
    }

    public static List<Component> xpHud(ServerLevel level, String option) {
        if (!GameplayCompatConfig.hopperXpCounters) return List.of();
        var lines = new ArrayList<Component>();
        var seen = java.util.EnumSet.noneOf(DyeColor.class);
        for (String name : (option == null || option.isBlank() ? "white" : option).split(",")) {
            DyeColor color = DyeColor.byName(name.trim(), null);
            if (color == null || !seen.add(color)) continue;
            var view = sample(level, color, false, false, false);
            lines.add(Component.literal(view == null ? color.getName() + ": not started" : view.summary(color.getName(), false)));
        }
        return lines;
    }

    private static int report(CommandSourceStack source, DyeColor selected, boolean realtime, boolean reset) {
        int printed = 0;
        for (DyeColor color : DyeColor.values()) {
            if (selected != null && color != selected) continue;
            var view = sample(source.getLevel(), color, realtime, selected != null && !reset, reset);
            if (view == null) continue;
            ++printed;
            source.sendSuccess(() -> Component.literal(view.summary(color.getName(), realtime) + (reset ? " (reset)" : "")), false);
            view.byValue().forEach((value, xp) -> source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                " - orb value %d: %d XP, %.2f/h", value, xp, view.hourly(xp))), false));
            if (!reset && selected != null && view.other() != 0L) source.sendSuccess(() -> Component.literal(" - other denominations: " + view.other() + " XP"), false);
        }
        if (printed == 0) source.sendSuccess(() -> Component.literal("No XP counted in this world" + (selected == null ? "." : " for " + selected.getName() + ".")), false);
        return 1;
    }
}
