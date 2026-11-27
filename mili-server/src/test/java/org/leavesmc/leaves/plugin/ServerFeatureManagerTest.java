package org.leavesmc.leaves.plugin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
public class ServerFeatureManagerTest {
    private static final String LEAVESCLIP = "leavesclip.enable.mixin";
    private static final String RICEEAR = "riceear.enable.mixin";

    @BeforeAll
    static void initializeSingletonBeforeOverridingProperties() {
        assertNotNull(ServerFeatureManager.INSTANCE);
    }

    @ParameterizedTest
    @CsvSource({
            ",,false",
            "true,,true",
            ",true,true",
            "false,true,true",
            "true,false,true",
            "true,true,true",
            "false,false,false",
            "'','',false",
            "yes,1,false",
            "' true','true ',false",
            "TrUe,,true",
            ",TrUe,true"
    })
    void matchesLauncherMixinSwitch(String leavesclip, String riceear, boolean expected) {
        String oldLeavesclip = System.getProperty(LEAVESCLIP);
        String oldRiceear = System.getProperty(RICEEAR);
        try {
            setProperty(LEAVESCLIP, leavesclip);
            setProperty(RICEEAR, riceear);
            ServerFeatureManager manager = new ServerFeatureManager();

            assertEquals(expected, manager.isFeatureAvailable(Features.MIXIN));
            assertTrue(manager.isFeatureAvailable(Features.FAKEPLAYER));
            assertTrue(manager.isFeatureAvailable(Features.PHOTOGRAPHER));
        } finally {
            setProperty(LEAVESCLIP, oldLeavesclip);
            setProperty(RICEEAR, oldRiceear);
        }
    }

    private static void setProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
