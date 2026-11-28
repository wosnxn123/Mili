package fun.bm.mili.carpet;

import fun.bm.mili.carpet.config.modules.EntityGameplayConfig;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityGameplayRulesTest {
    @Test
    void mixedOrbMergeConservesExperienceAndRejectsOverflow() {
        assertEquals(60, EntityGameplayRules.mergedExperienceValue(7, 3, 13, 3));
        assertEquals(Short.MAX_VALUE, EntityGameplayRules.mergedExperienceValue(32766, 1, 1, 1));
        assertEquals(-1, EntityGameplayRules.mergedExperienceValue(32766, 1, 1, 2));
        assertEquals(-1, EntityGameplayRules.mergedExperienceValue(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(-1, EntityGameplayRules.mergedExperienceValue(5, 0, 7, 1));
    }

    @Test
    void precisePlacementStaysInTheOriginalTargetCell() {
        BlockPos target = new BlockPos(-10, 100, 30);
        Vec3 placed = EntityGameplayRules.precisePlacement(target, new Vec3(-11.0, 0.0, 31.0), 100.25);
        assertEquals(target, BlockPos.containing(placed));
        assertEquals(100.25, placed.y);
        assertEquals(new Vec3(-9.5, 100.25, 30.5), EntityGameplayRules.precisePlacement(target, new Vec3(-9.5, 0.0, 30.5), 100.25));
    }

    @Test
    void groundSpeedLevelHasVanillaFallback() {
        assertEquals(0, EntityGameplayRules.groundSpeedLevel(null));
        assertEquals(0, EntityGameplayRules.groundSpeedLevel("VANILLA"));
        assertEquals(0, EntityGameplayRules.groundSpeedLevel("invalid"));
        assertEquals(5, EntityGameplayRules.groundSpeedLevel("Ⅴ"));
    }

    @Test
    void protocolEntriesAreUniqueAndUseVanillaDefaults() {
        var rules = new ArrayList<CarpetRule>();
        EntityGameplayRules.registerProtocolRules(rules);
        assertEquals(43, rules.size());
        assertTrue(rules.contains(CarpetRule.of("carpet", "creativeFlyDrag", "0.09")));
        assertEquals(rules.size(), rules.stream().map(CarpetRule::name).distinct().count());
        assertEquals(1.0, EntityGameplayConfig.creativeFlySpeed());
        assertTrue(rules.contains(CarpetRule.of("carpetorgaddition", "experienceOrbMerge", false)));
        assertTrue(rules.contains(CarpetRule.of("carpettisaddition", "preciseEntityPlacement", false)));
    }
}
