package fun.bm.mili.utils;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 26.2 vanilla coral growth algorithms used by Carpet's fertilizable coral.
 *
 * <p>26.3 replaced the old CoralFeature hierarchy with configured, placed-feature
 * records and removed the mushroom variant. Keep the coloured bonemeal feature
 * here instead of depending on a removed NMS superclass or a worldgen context.
 * Random draw order, placement flags and the three shapes are kept unchanged.
 */
final class CoralGrowth {
    private CoralGrowth() {
    }

    static boolean place(final int variant, final LevelAccessor level, final RandomSource random,
                         final BlockPos origin, final BlockState state) {
        return switch (variant) {
            case 0 -> placeClaw(level, random, origin, state);
            case 1 -> placeTree(level, random, origin, state);
            case 2 -> placeMushroom(level, random, origin, state);
            default -> throw new IllegalArgumentException("Unknown coral variant: " + variant);
        };
    }

    private static boolean placeCoralBlock(final LevelAccessor level, final RandomSource random,
                                           final BlockPos pos, final BlockState state) {
        BlockPos above = pos.above();
        BlockState targetBlockState = level.getBlockState(pos);
        if (!(targetBlockState.is(Blocks.WATER) || targetBlockState.is(BlockTags.CORALS))
            || !level.getBlockState(above).is(Blocks.WATER)) {
            return false;
        }

        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (random.nextFloat() < 0.25F) {
            BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.CORALS, random)
                .map(Holder::value)
                .ifPresent(block -> level.setBlock(above, block.defaultBlockState(), Block.UPDATE_CLIENTS));
        } else if (random.nextFloat() < 0.05F) {
            level.setBlock(above,
                Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, random.nextInt(4) + 1),
                Block.UPDATE_CLIENTS);
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() < 0.2F) {
                BlockPos relativePos = pos.relative(direction);
                if (level.getBlockState(relativePos).is(Blocks.WATER)) {
                    BuiltInRegistries.BLOCK.getRandomElementOf(BlockTags.WALL_CORALS, random).map(Holder::value).ifPresent(coral -> {
                        BlockState coralFanState = coral.defaultBlockState();
                        if (coralFanState.hasProperty(BaseCoralWallFanBlock.FACING)) {
                            coralFanState = coralFanState.setValue(BaseCoralWallFanBlock.FACING, direction);
                        }
                        level.setBlock(relativePos, coralFanState, Block.UPDATE_CLIENTS);
                    });
                }
            }
        }
        return true;
    }

    private static boolean placeClaw(final LevelAccessor level, final RandomSource random,
                                     final BlockPos origin, final BlockState state) {
        if (!placeCoralBlock(level, random, origin, state)) {
            return false;
        }

        Direction clawDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int nBranches = random.nextInt(2) + 2;
        List<Direction> possibleDirections = Util.toShuffledList(
            Stream.of(clawDirection, clawDirection.getClockWise(), clawDirection.getCounterClockWise()), random);
        for (Direction branchDirection : possibleDirections.subList(0, nBranches)) {
            BlockPos.MutableBlockPos mutPos = origin.mutable();
            int sidewayLength = random.nextInt(2) + 1;
            mutPos.move(branchDirection);
            int inwardLength;
            Direction segmentDirection;
            if (branchDirection == clawDirection) {
                segmentDirection = clawDirection;
                inwardLength = random.nextInt(3) + 2;
            } else {
                mutPos.move(Direction.UP);
                Direction[] possibleSegments = new Direction[]{branchDirection, Direction.UP};
                segmentDirection = Util.getRandom(possibleSegments, random);
                inwardLength = random.nextInt(3) + 3;
            }

            for (int i = 0; i < sidewayLength && placeCoralBlock(level, random, mutPos, state); i++) {
                mutPos.move(segmentDirection);
            }
            mutPos.move(segmentDirection.getOpposite());
            mutPos.move(Direction.UP);
            for (int i = 0; i < inwardLength; i++) {
                mutPos.move(clawDirection);
                if (!placeCoralBlock(level, random, mutPos, state)) {
                    break;
                }
                if (random.nextFloat() < 0.25F) {
                    mutPos.move(Direction.UP);
                }
            }
        }
        return true;
    }

    private static boolean placeTree(final LevelAccessor level, final RandomSource random,
                                     final BlockPos origin, final BlockState state) {
        BlockPos.MutableBlockPos mutPos = origin.mutable();
        int trunkHeight = random.nextInt(3) + 1;
        for (int i = 0; i < trunkHeight; i++) {
            if (!placeCoralBlock(level, random, mutPos, state)) {
                return true;
            }
            mutPos.move(Direction.UP);
        }

        BlockPos trunkTopPos = mutPos.immutable();
        int nBranches = random.nextInt(3) + 2;
        List<Direction> directions = Direction.Plane.HORIZONTAL.shuffledCopy(random);
        for (Direction branchDirection : directions.subList(0, nBranches)) {
            mutPos.set(trunkTopPos);
            mutPos.move(branchDirection);
            int branchHeight = random.nextInt(5) + 2;
            int segmentLength = 0;
            for (int j = 0; j < branchHeight && placeCoralBlock(level, random, mutPos, state); j++) {
                segmentLength++;
                mutPos.move(Direction.UP);
                if (j == 0 || segmentLength >= 2 && random.nextFloat() < 0.25F) {
                    mutPos.move(branchDirection);
                    segmentLength = 0;
                }
            }
        }
        return true;
    }

    private static boolean placeMushroom(final LevelAccessor level, final RandomSource random,
                                         final BlockPos origin, final BlockState state) {
        int height = random.nextInt(3) + 3;
        int width = random.nextInt(3) + 3;
        int length = random.nextInt(3) + 3;
        int sinkValue = random.nextInt(3) + 1;
        BlockPos.MutableBlockPos mutPos = origin.mutable();
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = 0; z <= length; z++) {
                    mutPos.set(x + origin.getX(), y + origin.getY(), z + origin.getZ());
                    mutPos.move(Direction.DOWN, sinkValue);
                    if ((x != 0 && x != width || y != 0 && y != height)
                        && (z != 0 && z != length || y != 0 && y != height)
                        && (x != 0 && x != width || z != 0 && z != length)
                        && (x == 0 || x == width || y == 0 || y == height || z == 0 || z == length)
                        && !(random.nextFloat() < 0.1F)) {
                        placeCoralBlock(level, random, mutPos, state);
                    }
                }
            }
        }
        // Vanilla's mushroom and tree variants report success even if obstructed.
        return true;
    }
}
