package fun.bm.mili.regression;

import fun.bm.mili.carpet.CarpetEntityRemoval;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class CreativeRemovalTestSuite {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void excludesPlayersSpectatorsAndRemovedTargets() {
        assertFalse(CarpetEntityRemoval.canRemove(mock(Player.class)));
        Entity target = mock(Entity.class);
        assertTrue(CarpetEntityRemoval.canRemove(target));
        when(target.isSpectator()).thenReturn(true);
        assertFalse(CarpetEntityRemoval.canRemove(target));
        when(target.isSpectator()).thenReturn(false);
        when(target.isRemoved()).thenReturn(true);
        assertFalse(CarpetEntityRemoval.canRemove(target));
    }

    @Test
    void usesDiscardRatherThanDeathAndRestoresFlagAfterCallbackFailure() {
        Entity target = mock(Entity.class, CALLS_REAL_METHODS);
        doAnswer(call -> {
            assertTrue(target.mili$isCleanRemoval());
            return null;
        }).when(target).remove(Entity.RemovalReason.DISCARDED, EntityRemoveEvent.Cause.PLUGIN);
        target.mili$discardCleanly();
        verify(target).remove(Entity.RemovalReason.DISCARDED, EntityRemoveEvent.Cause.PLUGIN);
        assertFalse(target.mili$isCleanRemoval());
        doThrow(new IllegalStateException("测试回调失败")).when(target).remove(Entity.RemovalReason.DISCARDED, EntityRemoveEvent.Cause.PLUGIN);
        assertThrows(IllegalStateException.class, target::mili$discardCleanly);
        assertFalse(target.mili$isCleanRemoval());
    }
}
