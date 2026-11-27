package fun.bm.mili.carpet;

import fun.bm.mili.api.carpet.CarpetApi.Rule;
import fun.bm.mili.api.carpet.CarpetRuleUpdate.Status;
import fun.bm.mili.carpet.config.modules.GeneralCompatConfig;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class CarpetRuleApiTestSuite {
    @Test
    void persistsBeforeUpdatingLiveValueAndRejectsUnknownKeys() {
        boolean saved = GeneralCompatConfig.flippinCactus;
        try {
            GeneralCompatConfig.flippinCactus = false;
            Rule before = new Rule("carpet", "flippinCactus", "false");
            assertThrows(IllegalStateException.class, () -> CarpetRuleMutations.apply(before,"true",true,(k,v) -> false));
            assertFalse(GeneralCompatConfig.flippinCactus);
            var update = CarpetRuleMutations.apply(before,"TRUE",true,(k,v) -> k.equals("carpet.general.flippinCactus") && Boolean.TRUE.equals(v));
            assertEquals(Status.UPDATED, update.status());
            assertTrue(GeneralCompatConfig.flippinCactus);
            assertEquals("true", update.rule().value());
            assertEquals(Status.UNCHANGED, CarpetRuleMutations.apply(update.rule(),"true",false,(k,v) -> fail()).status());
            assertThrows(IllegalArgumentException.class, () -> CarpetRuleMutations.apply(before,"typo",false,(k,v) -> fail()));
            assertThrows(IllegalArgumentException.class, () -> CarpetRuleMutations.apply(new Rule("carpet", "../api.allowed_rule_writers", ""),"x",true,(k,v) -> fail()));
        } finally { GeneralCompatConfig.flippinCactus = saved; }
    }

    @Test
    void restartOnlyRulesNeverChangeLiveAndInvalidNumbersNeverSave() {
        boolean value = GeneralCompatConfig.largeBarrel;
        Rule before = new Rule("carpettisaddition", "largeBarrel", Boolean.toString(value));
        String requested = Boolean.toString(!value);
        assertThrows(IllegalArgumentException.class, () -> CarpetRuleMutations.apply(before,requested,false,(k,v) -> fail()));
        var change = CarpetRuleMutations.apply(before,requested,true,(k,v) -> k.equals("carpet.general.largeBarrel") && v.equals(!value));
        assertEquals(Status.SAVED_FOR_RESTART, change.status());
        assertEquals(before, change.rule());
        assertEquals(value, GeneralCompatConfig.largeBarrel);
        assertThrows(IllegalArgumentException.class, () -> CarpetRuleMutations.apply(new Rule("carpet", "snowMeltMinLightLevel", "12"),"17",true,(k,v) -> fail()));
    }

    @Test
    void anUnrelatedCallerCannotActAsAnotherPlugin() {
        Plugin owner = mock(Plugin.class);
        var result = MiliCarpetApi.INSTANCE.setRule(owner,"flippinCactus","true",true);
        assertInstanceOf(SecurityException.class, assertThrows(CompletionException.class, result::join).getCause());
    }
}
