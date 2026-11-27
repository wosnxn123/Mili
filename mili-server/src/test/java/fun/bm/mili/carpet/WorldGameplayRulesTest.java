package fun.bm.mili.carpet;

import fun.bm.mili.carpet.config.modules.WorldGameplayConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldGameplayRulesTest {
    @Test
    void confirmationIsSingleUseAndScopedToTheSession() {
        final var firstConnection = new Object();
        final var newConnection = new Object();
        assertFalse(WorldGameplayRules.consumeStopConfirmation(firstConnection, 0));
        assertFalse(WorldGameplayRules.consumeStopConfirmation(newConnection, 1));
        assertTrue(WorldGameplayRules.consumeStopConfirmation(firstConnection, 2));
        assertFalse(WorldGameplayRules.consumeStopConfirmation(firstConnection, 3));
        assertFalse(WorldGameplayRules.consumeStopConfirmation(firstConnection, TimeUnit.SECONDS.toNanos(61)));
    }

    @Test
    void disabledAndRandomWireModesKeepTheNativeFanoutSet() {
        final var original = Set.of(BlockPos.ZERO, BlockPos.ZERO.north(), BlockPos.ZERO.below());
        final String previous = WorldGameplayConfig.redstoneDustRandomUpdateOrder;
        try {
            WorldGameplayConfig.redstoneDustRandomUpdateOrder = "false";
            assertSame(original, WorldGameplayRules.wireUpdateOrder(original, null, BlockPos.ZERO));
            WorldGameplayConfig.redstoneDustRandomUpdateOrder = "true";
            for (int iteration = 0; iteration < 8; ++iteration) {
                assertEquals(original, new HashSet<>(WorldGameplayRules.wireUpdateOrder(original, null, BlockPos.ZERO)));
            }
        } finally {
            WorldGameplayConfig.redstoneDustRandomUpdateOrder = previous;
        }
    }

    @Test
    void portalSearchBoundsAndModesAreNormalized() {
        final int previousSize = WorldGameplayConfig.netherPortalMaxSize;
        final String previousMode = WorldGameplayConfig.violentNetherPortalCreation;
        try {
            WorldGameplayConfig.netherPortalMaxSize = Integer.MAX_VALUE;
            assertEquals(64, WorldGameplayRules.portalSearchSize());
            WorldGameplayConfig.netherPortalMaxSize = 3;
            assertEquals(21, WorldGameplayRules.portalSearchSize());
            WorldGameplayConfig.violentNetherPortalCreation = "all";
            assertEquals("false", WorldGameplayConfig.violentNetherPortalCreation());
        } finally {
            WorldGameplayConfig.netherPortalMaxSize = previousSize;
            WorldGameplayConfig.violentNetherPortalCreation = previousMode;
        }
    }

    @Test
    void protocolPublishesUniqueManagersAndRules() {
        final var registered = new ArrayList<CarpetRule>();
        WorldGameplayRules.registerProtocolRules(registered);
        final Set<String> names = new HashSet<>();
        for (final var rule : registered) assertTrue(names.add(rule.identifier() + ":" + rule.name()));
        assertEquals(34, names.size());
        assertTrue(names.contains("carpet:fillUpdates"));
        assertFalse(names.contains("carpetamsaddition:stringDupeReintroduced"));
        assertTrue(names.contains("carpetorgaddition:canActivatesObserver"));
        assertTrue(names.contains("carpettisaddition:stopCommandDoubleConfirmation"));
    }
}
