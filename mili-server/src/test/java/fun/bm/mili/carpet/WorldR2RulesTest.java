package fun.bm.mili.carpet;

import fun.bm.mili.carpet.config.modules.WorldR2Config;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.monster.Creeper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldR2RulesTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void independentPortalPoliciesOnlyGateNewFrames() {
        assertTrue(WorldR2Rules.mayCreateNetherPortal(true, false, true, true));
        assertTrue(WorldR2Rules.mayCreateNetherPortal(false, false, false, true));
        assertFalse(WorldR2Rules.mayCreateNetherPortal(false, false, true, false));
        assertTrue(WorldR2Rules.mayCreateNetherPortal(false, true, false, false));
        assertFalse(WorldR2Rules.mayCreateNetherPortal(false, true, false, true));
        assertFalse(WorldR2Rules.mayCreateNetherPortal(false, true, true, false));
        assertFalse(WorldR2Rules.mayCreateNetherPortal(false, true, true, true));
    }

    @Test
    void loaderTicketsHaveDedicatedLifetimeAndNoPersistence() {
        final int prior = WorldR2Config.blockChunkLoaderTimeController;
        try {
            WorldR2Config.blockChunkLoaderTimeController = 0;
            assertEquals(1, WorldR2Config.blockChunkLoaderTimeController());
            WorldR2Config.blockChunkLoaderTimeController = Integer.MAX_VALUE;
            assertEquals(300, WorldR2Config.blockChunkLoaderTimeController());
        } finally {
            WorldR2Config.blockChunkLoaderTimeController = prior;
        }
        var ticket = TicketType.MILI_BLOCK_LOADER;
        assertNotSame(TicketType.PORTAL, ticket);
        assertNotSame(TicketType.FORCED, ticket);
        assertTrue(ticket.hasTimeout());
        assertTrue(ticket.timeout() >= 1 && ticket.timeout() <= 300);
        assertTrue(ticket.canExpireIfUnloaded());
        assertFalse(ticket.persist());
        assertEquals(64, WorldR2Rules.MAX_BLOCK_LOADER_CENTERS);
    }

    @Test
    void dragonHeadQuotaIsReservedUntilDeathIsAcceptedOrCancelled() {
        final Creeper creeper = org.mockito.Mockito.mock(Creeper.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        org.mockito.Mockito.doReturn(false).when(creeper).isPowered();
        assertFalse(creeper.miliReserveDragonSkull());
        org.mockito.Mockito.doReturn(true).when(creeper).isPowered();
        assertTrue(creeper.miliReserveDragonSkull());
        assertFalse(creeper.carpetCanDropSkull());
        assertFalse(creeper.miliReserveDragonSkull());
        creeper.miliFinishDragonSkull(false);
        assertTrue(creeper.miliReserveDragonSkull());
        creeper.miliFinishDragonSkull(true);
        assertFalse(creeper.miliReserveDragonSkull());
    }

    @Test
    void publishesExactlyTheImplementedWorldRules() {
        var rules = new ArrayList<CarpetRule>();
        WorldR2Rules.appendRules(rules);
        assertEquals(8, rules.size());
        assertEquals(Set.of("carpet:movableBlockEntities", "carpetamsaddition:onlyPlayerCanCreateNetherPortal",
            "carpetamsaddition:itemEntityCreateNetherPortalDisabled", "carpettisaddition:renewableDragonHead",
            "carpetamsaddition:noteBlockChunkLoader", "carpetamsaddition:pistonBlockChunkLoader",
            "carpetamsaddition:bellBlockChunkLoader", "carpetamsaddition:blockChunkLoaderTimeController"),
            rules.stream().map(rule -> rule.identifier() + ":" + rule.name()).collect(Collectors.toSet()));
    }
}
