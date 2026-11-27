package fun.bm.mili.carpet;

import fun.bm.mili.carpet.config.modules.GameplayCompatConfig;
import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.MoonPhase;
import org.junit.jupiter.api.Test;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;
import static org.junit.jupiter.api.Assertions.*;

final class GameplayCompatTestSuite {
    @Test
    void publishesDistinctManagerRulesAndBoundsDistances() {
        var entries = new ArrayList<CarpetRule>();
        GameplayCompatRules.registerProtocolRules(entries);
        WorldGameplayRules.registerProtocolRules(entries);
        EntityGameplayRules.registerProtocolRules(entries);
        var names = new HashSet<String>();
        for (var entry : entries) assertTrue(names.add(entry.name()), entry::name);
        assertTrue(entries.contains(CarpetRule.of("carpetamsaddition", "useItemCooldownDisabled", false)));
        double previous = GameplayCompatConfig.xpTrackingDistance;
        try {
            GameplayCompatConfig.xpTrackingDistance = Double.NaN;
            assertEquals(8.0D, GameplayCompatConfig.xpTrackingDistance());
            GameplayCompatConfig.xpTrackingDistance = 900;
            assertEquals(128.0D, GameplayCompatConfig.xpTrackingDistance());
        } finally { GameplayCompatConfig.xpTrackingDistance = previous; }
    }

    @Test
    void xpCounterSaturatesAndDoesNotClaimTheCommandByDefault() {
        assertEquals(137L, CollectionGameplayRules.addWithoutOverflow(100L, 37L));
        assertEquals(Long.MAX_VALUE, CollectionGameplayRules.addWithoutOverflow(Long.MAX_VALUE - 1, 3L));
        boolean old = GameplayCompatConfig.hopperXpCounters;
        try {
            GameplayCompatConfig.hopperXpCounters = false;
            var commands = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
            CollectionGameplayRules.register(commands);
            assertNull(commands.getRoot().getChild("xcounter"));
        } finally { GameplayCompatConfig.hopperXpCounters = old; }
    }

    @Test
    void appliesOnlyTheConfiguredMoonAndVoidRules() {
        boolean oldMoon = GameplayCompatConfig.fullMoonEveryDay;
        String oldVoid = GameplayCompatConfig.voidDamageIgnorePlayer;
        try {
            GameplayCompatConfig.fullMoonEveryDay = false;
            assertEquals(4, MoonPhase.NEW_MOON.index());
            GameplayCompatConfig.fullMoonEveryDay = true;
            assertEquals(0, MoonPhase.NEW_MOON.index());
            GameplayCompatConfig.voidDamageIgnorePlayer = "creative, spectator";
            assertTrue(GameplayCompatRules.ignoresVoidDamage(GameType.SPECTATOR));
            assertFalse(GameplayCompatRules.ignoresVoidDamage(GameType.SURVIVAL));
        } finally {
            GameplayCompatConfig.fullMoonEveryDay = oldMoon;
            GameplayCompatConfig.voidDamageIgnorePlayer = oldVoid;
        }
    }
}
