package net.minecraft.world.level.block;

import fun.bm.mili.carpet.config.modules.GeneralCompatConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CactusRotationTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void placementRequiresBothTheRuleAndAnOffhandCactus() {
        Player player = mock(Player.class);
        boolean previous = GeneralCompatConfig.flippinCactus;
        try {
            // 无存档或数据包的测试环境中直接提供组件，不修改全局物品注册表。
            var cactus = new ItemStack(net.minecraft.core.Holder.direct(Items.CACTUS, net.minecraft.core.component.DataComponentMap.EMPTY), 1);
            when(player.getOffhandItem()).thenReturn(cactus);
            GeneralCompatConfig.flippinCactus = false;
            assertFalse(CarpetBlockRotator.flippinEligibility(player));
            GeneralCompatConfig.flippinCactus = true;
            assertFalse(CarpetBlockRotator.flippinEligibility(null));
            assertTrue(CarpetBlockRotator.flippinEligibility(player));
            when(player.getOffhandItem()).thenReturn(ItemStack.EMPTY);
            assertFalse(CarpetBlockRotator.flippinEligibility(player));
        } finally {
            GeneralCompatConfig.flippinCactus = previous;
        }
    }

    @Test
    void flipsOnlySingleSlabs() {
        Level level = mock(Level.class);
        Player player = mock(Player.class);
        BlockPos pos = new BlockPos(1, 64, 1);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        BlockState bottom = Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState top = bottom.setValue(SlabBlock.TYPE, SlabType.TOP);
        when(level.setBlock(pos, top, Block.UPDATE_CLIENTS | 1024)).thenReturn(true);
        assertTrue(CarpetBlockRotator.flipBlock(bottom, level, player, InteractionHand.MAIN_HAND, hit));
        verify(level).setBlock(pos, top, Block.UPDATE_CLIENTS | 1024);
        verify(level).setBlocksDirty(pos, bottom, top);
        clearInvocations(level);
        assertFalse(CarpetBlockRotator.flipBlock(bottom.setValue(SlabBlock.TYPE, SlabType.DOUBLE), level, player, InteractionHand.MAIN_HAND, hit));
        verifyNoInteractions(level);
    }
}
