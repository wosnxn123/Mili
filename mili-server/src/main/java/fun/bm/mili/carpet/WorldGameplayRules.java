package fun.bm.mili.carpet;

import ca.spottedleaf.moonrise.common.util.TickThread;
import fun.bm.mili.carpet.config.modules.WorldGameplayConfig;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gamerules.GameRules;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

public final class WorldGameplayRules {
    private WorldGameplayRules() {
    }

    public static boolean ownsLoadedBox(final ServerLevel level, final int minX, final int minZ, final int maxX, final int maxZ) {
        for (int x = minX >> 4; x <= maxX >> 4; ++x) {
            for (int z = minZ >> 4; z <= maxZ >> 4; ++z) {
                if (!TickThread.isTickThreadFor(level, x, z) || level.getChunkIfLoaded(x, z) == null) return false;
            }
        }
        return true;
    }

    public static boolean drySaplingArea(final ServerLevel level, final BlockPos pos) {
        if (!ownsLoadedBox(level, pos.getX() - 4, pos.getZ() - 4, pos.getX() + 4, pos.getZ() + 4)) return false;
        for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 1, 4))) {
            if (level.getFluidState(nearby).is(FluidTags.WATER)) return false;
        }
        return true;
    }

    public static BlockInput softReplace(final BlockInput target, final BlockState previous) {
        BlockState result = target.getState();
        final var defined = new HashSet<>(target.getDefinedProperties());
        for (Property<?> property : previous.getProperties()) {
            if (result.hasProperty(property) && defined.add(property)) {
                result = copyProperty(result, previous, property);
            }
        }
        return new BlockInput(result, defined, target.tag);
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState target, BlockState previous, Property<T> property) {
        return target.trySetValue(property, previous.getValue(property));
    }

    public static void summonLightningTrap(final ServerLevel level, final BlockPos pos) {
        if (!GameplayCompatRules.ownsLoaded(level, pos) || !level.getGameRules().get(GameRules.SPAWN_MOBS)
            || level.getBlockState(pos.below()).is(BlockTags.LIGHTNING_RODS)) return;
        final double chance = level.paperConfig().entities.spawning.skeletonHorseThunderSpawnChance.or(0.01);
        if (!(level.getRandom().nextDouble() < level.getCurrentDifficultyAt(pos).getEffectiveDifficulty() * chance)) return;
        final var horse = EntityTypes.SKELETON_HORSE.create(level, EntitySpawnReason.EVENT);
        if (horse != null) {
            horse.setTrap(true);
            horse.setAge(0);
            horse.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            level.addFreshEntity(horse, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.LIGHTNING);
        }
    }

    public static boolean confirmStop(final CommandSourceStack source) {
        if (!WorldGameplayConfig.stopCommandDoubleConfirmation || !(source.getEntity() instanceof ServerPlayer player)) return true;
        if (consumeStopConfirmation(player, System.nanoTime())) return true;
        source.sendSuccess(() -> Component.literal("Run /stop again within 60 seconds to confirm shutdown."), false);
        return false;
    }

    private static final java.util.Map<Object, Long> STOP_CONFIRMATIONS = new java.util.IdentityHashMap<>();

    static boolean consumeStopConfirmation(final Object session, final long now) {
        final long window = TimeUnit.SECONDS.toNanos(60);
        synchronized (STOP_CONFIRMATIONS) {
            STOP_CONFIRMATIONS.values().removeIf(time -> now - time > window);
            if (STOP_CONFIRMATIONS.remove(session) != null) return true;
            STOP_CONFIRMATIONS.put(session, now);
            return false;
        }
    }

    public static int portalSearchSize() {
        return Math.max(21, WorldGameplayConfig.netherPortalMaxSize());
    }

    public static boolean ownsPortalPlane(final ServerLevel level, final BlockPos center, final net.minecraft.core.Direction.Axis axis, final int radius) {
        final int dx = axis == net.minecraft.core.Direction.Axis.X ? radius : 0;
        final int dz = axis == net.minecraft.core.Direction.Axis.Z ? radius : 0;
        return ownsLoadedBox(level, center.getX() - dx, center.getZ() - dz, center.getX() + dx, center.getZ() + dz);
    }

    public static boolean skipRedstoneUpwardShapeUpdate() {
        return fun.bm.mili.carpet.config.modules.GeneralCompatConfig.dustTrapdoorReintroduced
            || WorldGameplayConfig.simpleUpdateSkipper || WorldGameplayConfig.redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate;
    }

    public static boolean skipConfiguredUpwardBreak(final net.minecraft.world.level.Level level, final BlockPos pos) {
        return (WorldGameplayConfig.simpleUpdateSkipper || WorldGameplayConfig.redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate)
            && level instanceof ServerLevel world && GameplayCompatRules.ownsLoaded(world, pos);
    }

    public static java.util.Collection<BlockPos> wireUpdateOrder(final java.util.Set<BlockPos> nativeOrder,
        final net.minecraft.world.level.Level level, final BlockPos origin) {
        final String mode = WorldGameplayConfig.redstoneDustRandomUpdateOrder();
        if ("false".equals(mode)) return nativeOrder;
        final var random = java.util.concurrent.ThreadLocalRandom.current();
        final var result = new java.util.ArrayList<>(nativeOrder);
        if ("random_pos".equals(mode)) {
            final BlockPos sample = new BlockPos(random.nextInt(-30_000_000, 30_000_000),
                level.getMinY() + random.nextInt(level.getHeight()), random.nextInt(-30_000_000, 30_000_000));
            final var sampleOrder = new java.util.HashSet<BlockPos>();
            sampleOrder.add(sample);
            for (final var direction : net.minecraft.core.Direction.values()) sampleOrder.add(sample.relative(direction));
            int index = 0;
            for (final BlockPos pos : sampleOrder) {
                final int source = result.indexOf(origin.offset(pos.subtract(sample)));
                if (source >= 0) java.util.Collections.swap(result, index++, source);
            }
        } else {
            java.util.Collections.shuffle(result, random);
        }
        return result;
    }

    public static boolean suppressSculkVibration(final ServerLevel level, final BlockPos pos) {
        return WorldGameplayConfig.soundSuppressionSimulator && GameplayCompatRules.ownsLoaded(level, pos)
            && level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.LAPIS_ORE);
    }

    // EXTRA_HELPERS

    static void registerProtocolRules(final List<CarpetRule> rules) {
        rules.add(CarpetRule.of("carpet", "desertShrubs", WorldGameplayConfig.desertShrubs));
        rules.add(CarpetRule.of("carpetamsaddition", "fertilizableSmallFlower", WorldGameplayConfig.fertilizableSmallFlower));
        rules.add(CarpetRule.of("carpetamsaddition", "scheduledRandomTickAllPlants", WorldGameplayConfig.scheduledRandomTickAllPlants));
        rules.add(CarpetRule.of("carpetamsaddition", "scheduledRandomTickBamboo", WorldGameplayConfig.scheduledRandomTickBamboo));
        rules.add(CarpetRule.of("carpetamsaddition", "scheduledRandomTickCactus", WorldGameplayConfig.scheduledRandomTickCactus));
        rules.add(CarpetRule.of("carpetamsaddition", "scheduledRandomTickChorusFlower", WorldGameplayConfig.scheduledRandomTickChorusFlower));
        rules.add(CarpetRule.of("carpetamsaddition", "scheduledRandomTickStem", WorldGameplayConfig.scheduledRandomTickStem));
        rules.add(CarpetRule.of("carpetamsaddition", "scheduledRandomTickSugarCane", WorldGameplayConfig.scheduledRandomTickSugarCane));
        rules.add(CarpetRule.of("carpet", "fillUpdates", WorldGameplayConfig.fillUpdates));
        rules.add(CarpetRule.of("carpet", "summonNaturalLightning", WorldGameplayConfig.summonNaturalLightning));
        rules.add(CarpetRule.of("carpettisaddition", "failSoftBlockStateParsing", WorldGameplayConfig.failSoftBlockStateParsing));
        rules.add(CarpetRule.of("carpettisaddition", "fillCommandModeEnhance", WorldGameplayConfig.fillCommandModeEnhance()));
        rules.add(CarpetRule.of("carpettisaddition", "stopCommandDoubleConfirmation", WorldGameplayConfig.stopCommandDoubleConfirmation));
        rules.add(CarpetRule.of("carpet", "optimizedTNT", WorldGameplayConfig.optimizedTNT));
        rules.add(CarpetRule.of("carpet", "tntRandomRange", Double.toString(WorldGameplayConfig.tntRandomRange())));
        rules.add(CarpetRule.of("carpetamsaddition", "itemAntiExplosion", WorldGameplayConfig.itemAntiExplosion()));
        rules.add(CarpetRule.of("carpetorgaddition", "disableWindChargeEffect", WorldGameplayConfig.disableWindChargeEffect));
        rules.add(CarpetRule.of("carpettisaddition", "wetExplosionReintroduced", WorldGameplayConfig.wetExplosionReintroduced));
        rules.add(CarpetRule.of("carpettisaddition", "explosionPacketRange", Double.toString(WorldGameplayConfig.explosionPacketRange())));
        rules.add(CarpetRule.of("carpettisaddition", "entityTrackerDistance", WorldGameplayConfig.entityTrackerDistance()));
        rules.add(CarpetRule.of("carpettisaddition", "entityTrackerInterval", WorldGameplayConfig.entityTrackerInterval()));
        rules.add(CarpetRule.of("carpet", "chainStone", WorldGameplayConfig.chainStone()));
        rules.add(CarpetRule.of("carpetamsaddition", "cryingObsidianNetherPortal", WorldGameplayConfig.cryingObsidianNetherPortal));
        rules.add(CarpetRule.of("carpetamsaddition", "customizedNetherPortal", WorldGameplayConfig.customizedNetherPortal));
        rules.add(CarpetRule.of("carpettisaddition", "netherPortalMaxSize", WorldGameplayConfig.netherPortalMaxSize()));
        rules.add(CarpetRule.of("carpettisaddition", "violentNetherPortalCreation", WorldGameplayConfig.violentNetherPortalCreation()));
        rules.add(CarpetRule.of("carpetamsaddition", "redstoneComponentSound", WorldGameplayConfig.redstoneComponentSound));
        rules.add(CarpetRule.of("carpetorgaddition", "canActivatesObserver", WorldGameplayConfig.canActivatesObserver));
        rules.add(CarpetRule.of("carpetorgaddition", "simpleUpdateSkipper", WorldGameplayConfig.simpleUpdateSkipper));
        rules.add(CarpetRule.of("carpettisaddition", "redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate", WorldGameplayConfig.redstoneDustRepeaterComparatorIgnoreUpwardsStateUpdate));
        rules.add(CarpetRule.of("carpettisaddition", "redstoneDustRandomUpdateOrder", WorldGameplayConfig.redstoneDustRandomUpdateOrder()));
        rules.add(CarpetRule.of("carpettisaddition", "soundSuppressionSimulator", WorldGameplayConfig.soundSuppressionSimulator));
        rules.add(CarpetRule.of("carpettisaddition", "updateSkippingSimulator", WorldGameplayConfig.updateSkippingSimulator));
        rules.add(CarpetRule.of("carpet", "updateSuppressionBlock", WorldGameplayConfig.updateSuppressionBlock()));
    }
}
