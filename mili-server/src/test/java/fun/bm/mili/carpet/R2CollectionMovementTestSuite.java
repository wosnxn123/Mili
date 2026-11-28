package fun.bm.mili.carpet;

import com.mojang.brigadier.CommandDispatcher;
import fun.bm.mili.carpet.config.modules.GameplayCompatConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class R2CollectionMovementTestSuite {
    @org.junit.jupiter.api.BeforeAll static void bootstrapRegistries() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test void xpUsesIndependentTickAndMonotonicWindows() {
        var window = new XpCounterWindow(100, 1_000_000L);
        window.record(7, 2);
        window.record(3, 2);
        var ticks = window.snapshot(1300, 120_001_000_000L, false, true);
        var real = window.snapshot(1300, 120_001_000_000L, true, false);
        assertEquals(20, ticks.total());
        assertEquals(1200.0, ticks.hourly(20), 1e-9);
        assertEquals(600.0, real.hourly(20), 1e-9);
        assertEquals(14L, ticks.byValue().get(7).longValue());
        assertTrue(real.byValue().isEmpty());
        window.record(7, 1);
        assertEquals(14L, ticks.byValue().get(7).longValue());
        assertTrue(Double.isFinite(window.snapshot(99, 0L, true, true).hourly(7)));
    }

    @Test void customDenominationsAreBoundedWithoutLosingTotals() {
        var window = new XpCounterWindow(0, 0L);
        for (int v = 1; v <= 100; v++) window.record(v, 1);
        var view = window.snapshot(2, 1L, false, true);
        assertEquals(5050, view.total());
        assertEquals(64, view.byValue().size());
        assertEquals(view.total(), view.other() + view.byValue().values().stream().mapToLong(Long::longValue).sum());
    }

    @Test void legacyBooleanAndOptInPlayerModeHaveDifferentConsumers() {
        var master = GameplayCompatConfig.blockDropsDirectlyEnterInventory;
        var custom = GameplayCompatConfig.miningDropPlayerControl;
        var xp = GameplayCompatConfig.hopperXpCounters;
        try {
            GameplayCompatConfig.hopperXpCounters = false;
            GameplayCompatConfig.blockDropsDirectlyEnterInventory = false;
            GameplayCompatConfig.miningDropPlayerControl = false;
            var defaults = new CommandDispatcher<CommandSourceStack>();
            CollectionGameplayRules.register(defaults);
            assertNull(defaults.getRoot().getChild("orange"));
            assertEquals("false", CollectionGameplayRules.miningDropMode());
            GameplayCompatConfig.blockDropsDirectlyEnterInventory = true;
            assertEquals("true", CollectionGameplayRules.miningDropMode());
            GameplayCompatConfig.miningDropPlayerControl = true;
            assertEquals("custom", CollectionGameplayRules.miningDropMode());
            var enabled = new CommandDispatcher<CommandSourceStack>();
            CollectionGameplayRules.register(enabled);
            assertNotNull(enabled.getRoot().getChild("orange").getChild("ruleself").getChild("player").getChild("blockDropsDirectlyEnterInventory").getChild("value"));
        } finally {
            GameplayCompatConfig.blockDropsDirectlyEnterInventory = master;
            GameplayCompatConfig.miningDropPlayerControl = custom;
            GameplayCompatConfig.hopperXpCounters = xp;
        }
    }

    @Test void changingModeOrLandingRemovesOnlyTheCreativeFlightModifier() {
        var player = org.mockito.Mockito.mock(net.minecraft.server.level.ServerPlayer.class);
        var level = org.mockito.Mockito.mock(net.minecraft.server.level.ServerLevel.class);
        var abilities = new net.minecraft.world.entity.player.Abilities();
        abilities.flying = true;
        var drag = new AttributeInstance(Attributes.AIR_DRAG_MODIFIER, ignored -> {});
        var base = drag.getValue();
        org.mockito.Mockito.when(player.level()).thenReturn(level);
        org.mockito.Mockito.when(player.getAbilities()).thenReturn(abilities);
        org.mockito.Mockito.when(player.getAttribute(Attributes.AIR_DRAG_MODIFIER)).thenReturn(drag);
        double prior = fun.bm.mili.carpet.config.modules.EntityGameplayConfig.creativeFlyDrag;
        try {
            fun.bm.mili.carpet.config.modules.EntityGameplayConfig.creativeFlyDrag = 0.5;
            org.mockito.Mockito.when(player.isCreative()).thenReturn(true);
            EntityGameplayRules.updateMovementAttributes(player);
            assertTrue(drag.getValue() > base);
            org.mockito.Mockito.when(player.onGround()).thenReturn(true);
            EntityGameplayRules.updateMovementAttributes(player);
            assertEquals(base, drag.getValue());
            org.mockito.Mockito.when(player.onGround()).thenReturn(false);
            org.mockito.Mockito.when(player.isCreative()).thenReturn(false);
            EntityGameplayRules.updateMovementAttributes(player);
            assertEquals(base, drag.getValue());
        } finally {
            fun.bm.mili.carpet.config.modules.EntityGameplayConfig.creativeFlyDrag = prior;
        }
    }

    @Test void vanillaAirDragComposesWithOtherModifiersAndRestoresThem() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        int[] changes = {0};
        var drag = new AttributeInstance(Attributes.AIR_DRAG_MODIFIER, unused -> ++changes[0]);
        var plugin = new AttributeModifier(Identifier.fromNamespaceAndPath("fixture", "existing"), 0.25D, AttributeModifier.Operation.ADD_VALUE);
        drag.addTransientModifier(plugin);
        for (double d : new double[]{0.0D, 0.02D, 0.5D, 1.0D}) {
            double factor = EntityGameplayRules.flightDragMultiplier(d);
            EntityGameplayRules.applyFlightDrag(drag, factor);
            assertEquals(Math.clamp(1.0D - d * 1.25D, 0.0D, 1.0D), Math.clamp(1.0D - (1.0F - 0.91F) * drag.getValue(), 0.0D, 1.0D), 1e-6);
            int before = changes[0];
            EntityGameplayRules.applyFlightDrag(drag, factor);
            assertEquals(before, changes[0]);
        }
        EntityGameplayRules.applyFlightDrag(drag, EntityGameplayRules.flightDragMultiplier(0.09D));
        assertEquals(1.25D, drag.getValue());
        assertSame(plugin, drag.getModifier(plugin.id()));
    }
}
