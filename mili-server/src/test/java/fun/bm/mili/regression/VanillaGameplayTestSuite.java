package fun.bm.mili.regression;

import fun.bm.mili.config.modules.fixes.VanillaLikeExperienceConfig;
import fun.bm.mili.config.modules.function.OldFeatureConfig;
import fun.bm.mili.utils.EndIslandGenerator;
import fun.bm.mili.utils.PlayerEnderPearls;
import io.papermc.paper.threadedregions.EntityScheduler;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

final class VanillaGameplayTestSuite {
    @Test
    void vanillaGameplayAndPearlPersistenceAreIndependentOptIns() {
        assertFalse(OldFeatureConfig.shouldGenerateEndRing);
        assertFalse(VanillaLikeExperienceConfig.enabled);
        assertFalse(VanillaLikeExperienceConfig.persistentEnderPearls);
    }

    @Test
    void endRingUsesSignedSquaredSectionDistance() {
        assertFalse(EndIslandGenerator.isInEndRing(46_340, 0));
        assertTrue(EndIslandGenerator.isInEndRing(46_341, 0));
        assertTrue(EndIslandGenerator.isInEndRing(-46_341, 0));
        assertTrue(EndIslandGenerator.isInEndRing(32_768, -32_768));
        assertTrue(EndIslandGenerator.isInEndRing(65_535, 0));
        assertFalse(EndIslandGenerator.isInEndRing(65_536, 0));
        assertEquals(80.0F, EndIslandGenerator.initialDensityOffset(0, 0));
        assertEquals(-100.0F, EndIslandGenerator.initialDensityOffset(46_340, 0));
        assertTrue(Float.isNaN(EndIslandGenerator.initialDensityOffset(46_341, 0)));
    }

    @Test
    void pendingRestoreIsDeduplicatedAndSurvivesAShortLogin() {
        PlayerEnderPearls store = new PlayerEnderPearls();
        UUID pearlId = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        tag.putString("ender_pearl_dimension", "minecraft:the_end");
        assertTrue(store.stageRestore(pearlId, tag));
        assertFalse(store.stageRestore(pearlId, new CompoundTag()));
        tag.putString("ender_pearl_dimension", "minecraft:overworld");
        assertTrue(store.schedulers().isEmpty());

        store.close();
        store.forget(pearlId, null);
        assertTrue(store.isClosing());
        assertFalse(store.stageRestore(UUID.randomUUID(), tag));
        CompoundTag snapshot = store.snapshots().getFirst();
        assertEquals("minecraft:the_end", snapshot.getStringOr("ender_pearl_dimension", ""));
        snapshot.putString("ender_pearl_dimension", "minecraft:the_nether");
        assertEquals("minecraft:the_end", store.snapshots().getFirst().getStringOr("ender_pearl_dimension", ""));
    }

    @Test
    void liveSnapshotReplacesPendingDataAndCannotBeChangedAfterQuit() {
        PlayerEnderPearls store = new PlayerEnderPearls();
        EntityScheduler scheduler = mock(EntityScheduler.class);
        UUID id = UUID.randomUUID();
        CompoundTag live = new CompoundTag();
        live.putInt("Age", 8);
        assertTrue(store.stageRestore(id, new CompoundTag()));
        assertTrue(store.publish(id, live, scheduler));
        store.forget(id, null);
        assertEquals(scheduler, store.schedulers().get(id));
        assertEquals(8, store.snapshots().getFirst().getIntOr("Age", -1));

        store.close();
        store.forget(id, scheduler);
        assertFalse(store.publish(id, new CompoundTag(), scheduler));
        assertEquals(8, store.snapshots().getFirst().getIntOr("Age", -1));
    }

    @Test
    void failedRestoreDoesNotResaveAStaleTag() {
        PlayerEnderPearls store = new PlayerEnderPearls();
        UUID id = UUID.randomUUID();
        assertTrue(store.stageRestore(id, new CompoundTag()));
        store.forget(id, null);
        assertTrue(store.snapshots().isEmpty());
        assertTrue(store.stageRestore(id, new CompoundTag()));
    }
}
