package dev.vessel.kernel.tick;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FlatArrayMinHeapPathfinder} and {@link EntityActivationRange3dEvaluator}.
 * <p>
 * Validates zero-allocation A* path traversal, obstacle avoidance,
 * and 3D spherical EAR deep-cave deactivation.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class FlatArrayMinHeapPathfinderTest {

    @Test
    public void testCoordinatePackingAndUnpacking() {
        int x = 125;
        int y = 64;
        int z = -45;

        int packed = FlatArrayMinHeapPathfinder.packCoordinate(x, y, z);
        assertThat(FlatArrayMinHeapPathfinder.unpackX(packed)).isEqualTo(x);
        assertThat(FlatArrayMinHeapPathfinder.unpackY(packed)).isEqualTo(y);
        assertThat(FlatArrayMinHeapPathfinder.unpackZ(packed)).isEqualTo(z);
    }

    @Test
    public void testPathfinderStraightLine() {
        FlatArrayMinHeapPathfinder pathfinder = new FlatArrayMinHeapPathfinder();
        TestPathPassabilityPredicate passability = new TestPathPassabilityPredicate();

        PathResult result = pathfinder.findPath(0, 64, 0, 3, 64, 0, passability);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.status()).isEqualTo(PathSearchStatus.FOUND);
        assertThat(result.pathLength()).isEqualTo(4); // (0,64,0), (1,64,0), (2,64,0), (3,64,0)
        assertThat(result.totalCost()).isEqualTo(3.0f);
    }

    @Test
    public void testPathfinderObstacleAvoidance() {
        FlatArrayMinHeapPathfinder pathfinder = new FlatArrayMinHeapPathfinder();
        TestPathPassabilityPredicate passability = new TestPathPassabilityPredicate();

        // Place wall obstacle at (1, 64, 0)
        passability.addObstacle(1, 64, 0);

        PathResult result = pathfinder.findPath(0, 64, 0, 2, 64, 0, passability);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.status()).isEqualTo(PathSearchStatus.FOUND);
        // Traverses around obstacle, e.g. via (0,64,1) -> (1,64,1) -> (2,64,1) -> (2,64,0)
        assertThat(result.pathLength()).isGreaterThanOrEqualTo(4);
    }

    @Test
    public void testPathfinderUnreachableTarget() {
        FlatArrayMinHeapPathfinder pathfinder = new FlatArrayMinHeapPathfinder();
        TestPathPassabilityPredicate passability = new TestPathPassabilityPredicate();

        // Goal is an obstacle
        passability.addObstacle(5, 64, 5);

        PathResult result = pathfinder.findPath(0, 64, 0, 5, 64, 5, passability);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo(PathSearchStatus.UNREACHABLE);
    }

    @Test
    public void testEntityActivationRange3dEllipsoid() {
        EntityActivationRange3dEvaluator evaluator = new EntityActivationRange3dEvaluator();

        // Player at surface: (0, 64, 0)
        double px = 0.0, py = 64.0, pz = 0.0;

        // Monster nearby at same level: (10, 64, 0) -> horizontal dist 10 <= 32 -> should tick
        assertThat(evaluator.shouldTick(EarEntityTypeCategory.MONSTER, 10.0, 64.0, 0.0, px, py, pz, false)).isTrue();

        // Monster deep in cave: (0, -20, 0) -> vertical dist 84 > vertical radius 20 -> should NOT tick
        // Directly solves ERR-41 and ERR-42!
        assertThat(evaluator.shouldTick(EarEntityTypeCategory.MONSTER, 0.0, -20.0, 0.0, px, py, pz, false)).isFalse();
        assertThat(evaluator.deactivatedCount()).isGreaterThan(0L);

        // Immune entity deep in cave (e.g. taking damage) -> should tick regardless of distance
        assertThat(evaluator.shouldTick(EarEntityTypeCategory.MONSTER, 0.0, -20.0, 0.0, px, py, pz, true)).isTrue();
        assertThat(evaluator.immuneCount()).isGreaterThan(0L);

        // Evaluation result record
        Ear3dEvaluationResult evalResult = evaluator.evaluate(
                EarEntityTypeCategory.MONSTER, 0.0, -20.0, 0.0, px, py, pz, false
        );
        assertThat(evalResult.shouldTick()).isFalse();
        assertThat(evalResult.isImmune()).isFalse();
        assertThat(evalResult.category()).isEqualTo(EarEntityTypeCategory.MONSTER);
    }
}
