package fun.bm.mili.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class PathRecomputationTestSuite {
    private static final Node END = new Node(6, -4, 10);
    private Mob mob;
    private Navigation navigation;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        mob = mock(Mob.class);
        when(mob.getX()).thenReturn(-5.0);
        when(mob.getY()).thenReturn(5.0);
        when(mob.getZ()).thenReturn(-9.0);
        navigation = new Navigation(mob);
    }

    @Test
    void usesStrictBlockCenterDistanceAndRemainingNodesWithoutIntegerOverflow() {
        for (int remaining : new int[] {1, 7, 46_340, 46_341, 50_000}) {
            // The end node and mock mob give a midpoint at (0.5, 0.5, 0.5).
            // Use different coordinates on earlier nodes to catch accidental use of the next node.
            List<Node> nodes = new ArrayList<>(Collections.nCopies(remaining + 3, new Node(60, 70, 80)));
            nodes.set(nodes.size() - 1, END);
            Path path = new Path(nodes, BlockPos.ZERO, true);
            path.setNextNodeIndex(3);
            navigation.use(path, false);

            assertCase(path, false, new BlockPos(remaining - 1, 0, 0), true);
            assertCase(path, false, new BlockPos(remaining, 0, 0), false);
            assertCase(path, false, new BlockPos(remaining + 1, 0, 0), false);
            assertCase(path, false, new BlockPos(0, -(remaining - 1), 0), true);
            assertCase(path, false, new BlockPos(0, -remaining, 0), false);
            assertCase(path, false, new BlockPos(0, 0, remaining), false);
        }
    }

    @Test
    void returnsFalseForDelayedNullDoneAndEmptyPaths() {
        Path oneNode = new Path(List.of(END), BlockPos.ZERO, true);
        navigation.use(oneNode, true);
        assertCase(oneNode, true, BlockPos.ZERO, false);

        navigation.use(null, false);
        assertCase(null, false, BlockPos.ZERO, false);

        oneNode.setNextNodeIndex(1);
        navigation.use(oneNode, false);
        assertCase(oneNode, false, BlockPos.ZERO, false);

        Path empty = new Path(List.of(), BlockPos.ZERO, false);
        navigation.use(empty, false);
        assertCase(empty, false, BlockPos.ZERO, false);
        // The public setter permits this cursor; isDone() no longer masks the empty-path guard.
        empty.setNextNodeIndex(-1);
        assertFalse(empty.isDone());
        assertCase(empty, false, BlockPos.ZERO, false);
    }

    @Test
    void matchesTheOriginalWhenTheMidpointIsNotOnABlockCenter() {
        when(mob.getX()).thenReturn(-5.375);
        when(mob.getY()).thenReturn(4.25);
        when(mob.getZ()).thenReturn(-9.625);
        Path path = new Path(List.of(END, END, END), BlockPos.ZERO, true);
        path.setNextNodeIndex(1);
        navigation.use(path, false);

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    assertEquals(original(path, false, pos), navigation.shouldRecomputePath(pos), pos::toString);
                }
            }
        }
    }

    private void assertCase(Path path, boolean delayed, BlockPos pos, boolean expected) {
        assertEquals(expected, original(path, delayed, pos), pos::toString);
        assertEquals(expected, navigation.shouldRecomputePath(pos), pos::toString);
    }

    private boolean original(Path path, boolean delayed, BlockPos pos) {
        if (delayed || path == null || path.isDone() || path.getNodeCount() == 0) {
            return false;
        }
        Node target = path.getEndNode();
        Vec3 midpoint = new Vec3((target.x + mob.getX()) / 2.0, (target.y + mob.getY()) / 2.0, (target.z + mob.getZ()) / 2.0);
        return pos.closerToCenterThan(midpoint, path.getNodeCount() - path.getNextNodeIndex());
    }

    private static final class Navigation extends PathNavigation {
        Navigation(Mob mob) {
            super(mob, null);
        }

        void use(Path path, boolean delayed) {
            this.path = path;
            this.hasDelayedRecomputation = delayed;
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            return new PathFinder(new WalkNodeEvaluator(), maxVisitedNodes);
        }

        @Override
        protected Vec3 getTempMobPos() {
            return Vec3.ZERO;
        }

        @Override
        protected boolean canUpdatePath() {
            return false;
        }

        @Override
        public boolean canNavigateGround() {
            return false;
        }
    }
}
