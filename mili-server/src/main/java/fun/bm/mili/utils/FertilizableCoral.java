package fun.bm.mili.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

// Powered by fabric-carpet/src/main/java/carpet/helpers/FertilizableCoral.java
// Ported from Leaves
public interface FertilizableCoral extends BonemealableBlock {

    boolean isEnabled();

    @Override
    default boolean isValidBonemealTarget(@NotNull LevelReader world, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull BonemealSource source) {
        return isEnabled() && state.getValue(BaseCoralPlantTypeBlock.WATERLOGGED) && world.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    default boolean isBonemealSuccess(@NotNull Level world, RandomSource random, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull BonemealSource source) {
        ((ServerLevel) world).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.5, 0.3, 0.0);
        return random.nextFloat() < 0.15D;
    }

    @Override
    default void performBonemeal(@NotNull ServerLevel worldIn, RandomSource random, @NotNull BlockPos pos, @NotNull BlockState blockUnder, @NotNull BonemealSource source) {
        int variant = random.nextInt(3);
        MapColor color = blockUnder.getMapColor(worldIn, pos);
        BlockState properBlock = blockUnder;
        HolderSet.Named<Block> coralBlocks = worldIn.registryAccess().lookupOrThrow(Registries.BLOCK).getOrThrow(BlockTags.CORAL_BLOCKS);
        for (Holder<Block> block : coralBlocks) {
            properBlock = block.value().defaultBlockState();
            if (properBlock.getMapColor(worldIn, pos) == color) {
                break;
            }
        }
        worldIn.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_NONE);

        if (!CoralGrowth.place(variant, worldIn, random, pos, properBlock)) {
            worldIn.setBlock(pos, blockUnder, 3);
        } else {
            if (worldIn.getRandom().nextInt(10) == 0) {
                BlockPos randomPos = pos.offset(worldIn.getRandom().nextInt(16) - 8, worldIn.getRandom().nextInt(8), worldIn.getRandom().nextInt(16) - 8);
                if (coralBlocks.contains(worldIn.getBlockState(randomPos).typeHolder())) {
                    worldIn.setBlock(randomPos, Blocks.WET_SPONGE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }
}
