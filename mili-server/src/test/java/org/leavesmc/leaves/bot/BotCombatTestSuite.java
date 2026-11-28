package org.leavesmc.leaves.bot;

import ca.spottedleaf.moonrise.common.util.TickThread;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import io.papermc.paper.threadedregions.RegionizedWorldData;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.shapes.Shapes;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.leavesmc.leaves.bot.agent.Configs;
import org.leavesmc.leaves.bot.agent.configs.AffectsSpawningConfig;
import org.leavesmc.leaves.bot.agent.configs.CombatModeConfig.CombatMode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class BotCombatTestSuite {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void spawningConfigurationUpdatesThePaperPredicateAndPersists() {
        final ServerBot bot = mock(ServerBot.class);
        when(bot.isAlive()).thenReturn(true);
        final AffectsSpawningConfig config = new AffectsSpawningConfig();
        config.setValue(false);
        config.setBot(bot);
        assertFalse(EntitySelector.PLAYER_AFFECTS_SPAWNING.test(bot));
        config.setValue(true);
        assertTrue(EntitySelector.PLAYER_AFFECTS_SPAWNING.test(bot));

        // A direct Bukkit API write to the same flag remains authoritative and is saved.
        bot.affectsSpawning = false;
        final CompoundTag saved = config.save(new CompoundTag());
        config.setValue(true);
        config.load(saved);
        assertFalse(bot.affectsSpawning);
    }

    @Test
    void largeRangeSpawnerIgnoresBotsOutsideItsOwnedLoadedArea() {
        final ServerBot bot = mock(ServerBot.class);
        final ServerLevel level = mock(ServerLevel.class);
        final LevelChunk chunk = mock(LevelChunk.class);
        when(level.players()).thenReturn(List.of(bot));
        when(bot.level()).thenReturn(level);
        when(bot.chunkPosition()).thenReturn(new ChunkPos(0, 0));
        when(bot.isAlive()).thenReturn(true);
        doCallRealMethod().when(level).hasNearbyAlivePlayerThatAffectsSpawningForLargerRangeSpawner(0, 64, 0, 40);
        final var config = new AffectsSpawningConfig();
        config.setBot(bot);
        config.setValue(true);
        try (var ownership = mockStatic(TickThread.class)) {
            assertFalse(level.hasNearbyAlivePlayerThatAffectsSpawningForLargerRangeSpawner(0, 64, 0, 40));
            verify(bot, never()).distanceToSqr(0, 64, 0);
            ownership.when(() -> TickThread.isTickThreadFor(bot)).thenReturn(true);
            assertFalse(level.hasNearbyAlivePlayerThatAffectsSpawningForLargerRangeSpawner(0, 64, 0, 40));
            when(level.getChunkIfLoaded(0, 0)).thenReturn(chunk);
            assertTrue(level.hasNearbyAlivePlayerThatAffectsSpawningForLargerRangeSpawner(0, 64, 0, 40));
            config.setValue(false);
            assertFalse(level.hasNearbyAlivePlayerThatAffectsSpawningForLargerRangeSpawner(0, 64, 0, 40));
        }
    }

    @Test
    void cancelledLureMovementDoesNotMoveBot() {
        final ServerBot bot = mock(ServerBot.class);
        final ServerLevel level = mock(ServerLevel.class);
        final WorldBorder border = mock(WorldBorder.class);
        final LevelChunk chunk = mock(LevelChunk.class);
        final org.leavesmc.leaves.entity.bot.CraftBot player = mock(org.leavesmc.leaves.entity.bot.CraftBot.class);
        final Vec3 start = new Vec3(6, 65, 6);
        when(bot.position()).thenReturn(start);
        when(bot.onGround()).thenReturn(true);
        when(bot.getAbilities()).thenReturn(new Abilities());
        when(bot.getBoundingBox()).thenReturn(new AABB(5.7, 65, 5.7, 6.3, 66.8, 6.3));
        when(bot.getBukkitEntity()).thenReturn(player);
        when(player.getLocation()).thenReturn(new Location(null, 6, 65, 6));
        when(level.getWorldBorder()).thenReturn(border);
        when(border.isWithinBounds(any(AABB.class))).thenReturn(true);
        when(level.getChunkIfLoaded(anyInt(), anyInt())).thenReturn(chunk);
        when(level.getBlockCollisions(same(bot), any(AABB.class))).thenReturn(List.of(Shapes.block()));

        try (var ownership = mockStatic(TickThread.class);
             var events = mockConstruction(PlayerMoveEvent.class, (event, context) -> when(event.callEvent()).thenReturn(false))) {
            ownership.when(() -> TickThread.isTickThreadFor(same(level), anyInt(), anyInt())).thenReturn(true);
            BotCombat.stepTowards(bot, level, start.add(1, 0, 0), start, 16);
            assertEquals(1, events.constructed().size());
            verify(events.constructed().getFirst()).callEvent();
            verify(bot, never()).move(any(MoverType.class), any(Vec3.class));
        }
    }

    @Test
    void lureStepIsHorizontalAndCannotLeaveItsAnchorRadius() {
        assertEquals(new Vec3(0.1, 0.0, 0.0), BotCombat.lureStep(Vec3.ZERO, new Vec3(4, 5, 0), Vec3.ZERO, 2));
        final Vec3 outside = new Vec3(2.01, 0, 0);
        assertSame(Vec3.ZERO, BotCombat.lureStep(outside, new Vec3(5, 0, 0), Vec3.ZERO, 2));
        assertTrue(BotCombat.lureStep(outside, Vec3.ZERO, Vec3.ZERO, 2).x < 0);
    }

    @Test
    void guardReacquiresOwnedHostilesAndRechecksLoadedAreaAndBystanders() {
        final ServerBot bot = mock(ServerBot.class);
        final ServerLevel level = mock(ServerLevel.class);
        final WorldBorder border = mock(WorldBorder.class);
        final RegionizedWorldData region = mock(RegionizedWorldData.class);
        final Zombie target = mock(Zombie.class);
        final Zombie foreign = mock(Zombie.class);
        final Cow passive = mock(Cow.class);
        final ServerPlayer bystander = mock(ServerPlayer.class);
        final LevelChunk chunk = mock(LevelChunk.class);
        final ItemStack sword = mock(ItemStack.class);
        final AABB box = new AABB(4, 65, 4, 5, 67, 5);

        when(bot.level()).thenReturn(level);
        when(bot.getConfigValue(Configs.COMBAT_MODE)).thenReturn(CombatMode.GUARD);
        when(bot.getConfigValue(Configs.COMBAT_RANGE)).thenReturn(3.0);
        when(bot.getConfigValue(Configs.COMBAT_LURE_RANGE)).thenReturn(16.0);
        when(bot.isAlive()).thenReturn(true);
        when(bot.getMainHandItem()).thenReturn(sword);
        when(bot.getBoundingBox()).thenReturn(box.move(-1.0, 0.0, 0.0));
        when(bot.getAttackStrengthScale(0.5F)).thenReturn(1.0F);
        when(bot.isWithinAttackRange(same(sword), eq(box), eq(0.0))).thenReturn(true);
        when(bot.hasLineOfSight(target)).thenReturn(true);
        when(sword.is(ItemTags.SWORDS)).thenReturn(true);
        when(target.level()).thenReturn(level);
        when(target.isAlive()).thenReturn(true);
        when(target.isAttackable()).thenReturn(true);
        when(target.getEyePosition()).thenReturn(new Vec3(4, 66, 4));
        when(target.getBoundingBox()).thenReturn(box);
        when(bystander.getBoundingBox()).thenReturn(box);
        when(level.getWorldBorder()).thenReturn(border);
        when(border.isWithinBounds(any(AABB.class))).thenReturn(true);
        when(level.getCurrentWorldData()).thenReturn(region);
        when(level.getChunkIfLoaded(anyInt(), anyInt())).thenReturn(chunk);
        when(region.getLocalEntities()).thenReturn(List.of(bystander, passive, foreign, target));

        try (var ownership = mockStatic(TickThread.class)) {
            ownership.when(() -> TickThread.isTickThreadFor(bot)).thenReturn(true);
            ownership.when(() -> TickThread.isTickThreadFor(target)).thenReturn(true);
            ownership.when(() -> TickThread.isTickThreadFor(bystander)).thenReturn(true);
            ownership.when(() -> TickThread.isTickThreadFor(same(level), anyInt(), anyInt())).thenReturn(true);
            final BotCombat combat = new BotCombat();
            doAnswer(invocation -> {
                final Predicate<Entity> allowed = invocation.getArgument(1);
                assertTrue(allowed.test(target));
                assertFalse(allowed.test(bystander));
                when(level.getLocalPlayers()).thenReturn(List.of(bystander));
                assertFalse(allowed.test(target));
                when(level.getLocalPlayers()).thenReturn(List.of());
                when(level.getChunkIfLoaded(anyInt(), anyInt())).thenReturn(null);
                assertFalse(allowed.test(target));
                return null;
            }).when(bot).attackCombatTarget(same(target), any());
            combat.tick(bot);
            verify(bot).attackCombatTarget(same(target), any());
            verifyNoInteractions(passive, foreign);

            when(level.getChunkIfLoaded(anyInt(), anyInt())).thenReturn(chunk);
            when(region.getLocalEntities()).thenReturn(List.of(bystander, passive, foreign));
            combat.tick(bot);
            verify(bot, times(1)).attackCombatTarget(any(), any());
        }
    }

    @Test
    void nativePreAttackCancellationStopsDamageAndSwing() {
        final ServerBot bot = mock(ServerBot.class, invocation -> switch (invocation.getMethod().getName()) {
            case "attackCombatTarget", "attackSingleTarget" -> invocation.callRealMethod();
            default -> RETURNS_DEFAULTS.answer(invocation);
        });
        final Zombie target = mock(Zombie.class);
        when(target.isAttackable()).thenReturn(true);
        @SuppressWarnings("unchecked") final Predicate<Entity> allowed = mock(Predicate.class);
        when(allowed.test(target)).thenReturn(true);
        try (var events = mockConstruction(PrePlayerAttackEntityEvent.class,
            (event, context) -> when(event.callEvent()).thenReturn(false))) {
            bot.attackCombatTarget(target, allowed);
            assertEquals(1, events.constructed().size());
            verify(events.constructed().getFirst()).callEvent();
            verify(allowed, times(1)).test(target);
            verify(target, never()).hurtOrSimulate(any(), anyFloat());
            verify(bot, never()).swing(any(), any(), anyBoolean());
        }
    }
}
