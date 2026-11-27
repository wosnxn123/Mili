/*
 * Based on Lophine Minecraft patch 0118 (7a51f49a), Helvetica Volubi and Anonymous Object,
 * GPL-3.0. Preserve the native Manhattan walk rather than copying the donor's search order.
 */
package fun.bm.mili.chunk;

import ca.spottedleaf.moonrise.common.util.TickThread;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Predicate;

public final class RepellentBlockSearch {
    private static final int HORIZONTAL_RANGE = 8;
    private static final int VERTICAL_RANGE = 4;

    private RepellentBlockSearch() {
    }

    public static Optional<BlockPos> find(ServerLevel level, BlockPos origin, Predicate<BlockState> matches) {
        // A 17x17 horizontal box always covers exactly two chunks in each axis.
        final int minX = (origin.getX() - HORIZONTAL_RANGE) >> 4;
        final int minZ = (origin.getZ() - HORIZONTAL_RANGE) >> 4;
        final int lowerY = origin.getY() - VERTICAL_RANGE;
        final int upperY = origin.getY() + VERTICAL_RANGE;
        final int minSection = Math.max(level.getMinSectionY(), lowerY >> 4);
        final int maxSection = Math.min(level.getMaxSectionY(), upperY >> 4);
        final boolean matchesOutsideBuildHeight = (lowerY < level.getMinY() || upperY > level.getMaxY())
            && matches.test(Blocks.AIR.defaultBlockState());
        int possibleMatches = 0;
        for (int i = 0; i < 4; ++i) {
            final int x = minX + (i & 1);
            final int z = minZ + (i >> 1);
            if (!TickThread.isTickThreadFor(level, x, z)) {
                continue;
            }
            final var chunk = level.getChunkIfLoaded(x, z);
            if (chunk == null) {
                continue;
            }
            if (matchesOutsideBuildHeight) {
                possibleMatches |= 1 << i;
                continue;
            }
            for (int y = minSection; y <= maxSection; ++y) {
                if (chunk.getSection(chunk.getSectionIndexFromSectionY(y)).maybeHas(matches)) {
                    possibleMatches |= 1 << i;
                    break;
                }
            }
        }
        if (possibleMatches == 0) {
            return Optional.empty();
        }
        final var search = level.findBlocksInBoxByManhattanDistance(origin, HORIZONTAL_RANGE, VERTICAL_RANGE);
        if (possibleMatches != 15) {
            final int mask = possibleMatches;
            search.filterPos(pos -> {
                final int x = (pos.getX() >> 4) - minX;
                final int z = (pos.getZ() >> 4) - minZ;
                return (mask & (1 << (x + 2 * z))) != 0;
            });
        }
        return search.filterState(matches).findFirst();
    }
}
