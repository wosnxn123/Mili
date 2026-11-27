/*
 * Based on Lophine's EndIslandGenerator by Helvetica Volubi (GPL-3.0).
 * Source: LophineLabs/Lophine at 7a51f49a512f1b5465f15a905d3704754e4bed02.
 */
package fun.bm.mili.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class EndIslandGenerator {
    private EndIslandGenerator() {
    }

    public static boolean isInEndRing(final int sectionX, final int sectionZ) {
        // Reproduce the sign of the old signed 32-bit squared distance, not distance overflow in individual coordinates.
        return (int) squaredDistance(sectionX, sectionZ) < 0;
    }

    public static float initialDensityOffset(final int sectionX, final int sectionZ) {
        long distance = squaredDistance(sectionX, sectionZ);
        return (int) distance < 0 ? Float.NaN : Mth.clamp(100.0F - Mth.sqrt(distance) * 8.0F, -100.0F, 80.0F);
    }

    private static long squaredDistance(final int x, final int z) {
        return (long) x * x + (long) z * z;
    }

    public static void clearOverflowBlocks(final ChunkAccess chunk) {
        if (chunk.getHighestFilledSectionIndex() == -1) {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        int minSectionX = chunkPos.x() << 1;
        int minSectionZ = chunkPos.z() << 1;
        for (int sectionX = minSectionX; sectionX <= minSectionX + 1; sectionX++) {
            for (int sectionZ = minSectionZ; sectionZ <= minSectionZ + 1; sectionZ++) {
                if (isInEndRing(sectionX, sectionZ)) {
                    int x = sectionX << 3;
                    int z = sectionZ << 3;
                    for (BlockPos pos : BlockPos.betweenClosed(x, chunk.getMinY(), z, x + 7, chunk.getMaxY(), z + 7)) {
                        if (chunk.getBlockState(pos).is(Blocks.END_STONE)) {
                            chunk.setBlockState(pos, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }
    }
}
