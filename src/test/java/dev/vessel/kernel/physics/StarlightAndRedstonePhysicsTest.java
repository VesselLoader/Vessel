package dev.vessel.kernel.physics;

import dev.vessel.kernel.optimization.physics.AlternateCurrentBfsRedstoneSolver;
import dev.vessel.kernel.optimization.physics.PhysicsLightType;
import dev.vessel.kernel.optimization.physics.RedstoneSolverResult;
import dev.vessel.kernel.optimization.physics.StarlightZeroCopyLightEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite verifying zero-copy Starlight lighting propagation and non-recursive Alternate Current redstone solver.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class StarlightAndRedstonePhysicsTest {

    @Test
    @DisplayName("Verify Starlight nibble array read/write operations and bitwise layout")
    public void testStarlightNibbleReadWrite() {
        byte[] sectionData = StarlightZeroCopyLightEngine.createEmptySectionData(0);

        StarlightZeroCopyLightEngine.setLightLevel(sectionData, 0, 0, 0, 15);
        StarlightZeroCopyLightEngine.setLightLevel(sectionData, 1, 0, 0, 8);
        StarlightZeroCopyLightEngine.setLightLevel(sectionData, 15, 15, 15, 10);

        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 0, 0, 0)).isEqualTo(15);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 1, 0, 0)).isEqualTo(8);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 15, 15, 15)).isEqualTo(10);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 2, 0, 0)).isEqualTo(0);

        assertThatThrownBy(() -> StarlightZeroCopyLightEngine.getLightLevel(sectionData, -1, 0, 0))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> StarlightZeroCopyLightEngine.getLightLevel(sectionData, 0, 16, 0))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("Verify Starlight non-recursive BFS light propagation and removal")
    public void testStarlightPropagationAndRemoval() {
        StarlightZeroCopyLightEngine engine = new StarlightZeroCopyLightEngine(PhysicsLightType.BLOCK);
        byte[] sectionData = StarlightZeroCopyLightEngine.createEmptySectionData(0);

        // Propagate light source of level 15 at center (8, 8, 8)
        engine.propagateLight(sectionData, 8, 8, 8, 15);

        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 8, 8, 8)).isEqualTo(15);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 9, 8, 8)).isEqualTo(14);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 10, 8, 8)).isEqualTo(13);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 8, 9, 8)).isEqualTo(14);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 8, 8, 9)).isEqualTo(14);

        // Remove light source at (8, 8, 8)
        engine.removeLight(sectionData, 8, 8, 8);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 8, 8, 8)).isEqualTo(0);
        assertThat(StarlightZeroCopyLightEngine.getLightLevel(sectionData, 9, 8, 8)).isEqualTo(0);
    }

    @Test
    @DisplayName("Verify Alternate Current Redstone solver linear line propagation without DFS recursion")
    public void testAlternateCurrentLinearPropagation() {
        AlternateCurrentBfsRedstoneSolver solver = new AlternateCurrentBfsRedstoneSolver();
        TestSimpleRedstoneWorld world = new TestSimpleRedstoneWorld();

        // Create a line of 5 redstone wires from X=0 to X=4 at Y=64, Z=0
        for (int x = 0; x <= 4; x++) {
            world.addWire(x, 64, 0, 0);
        }

        // Apply external power 15 to wire at X=0
        world.setExternalPowerSource(0, 64, 0, 15);

        RedstoneSolverResult result = solver.solveNetwork(0, 64, 0, world, world);

        assertThat(result.nodesEvaluated()).isEqualTo(5);
        assertThat(result.wiresUpdated()).isEqualTo(5);
        assertThat(result.maxSignalDiscovered()).isEqualTo(15);

        assertThat(world.getWirePower(0, 64, 0)).isEqualTo(15);
        assertThat(world.getWirePower(1, 64, 0)).isEqualTo(14);
        assertThat(world.getWirePower(2, 64, 0)).isEqualTo(13);
        assertThat(world.getWirePower(3, 64, 0)).isEqualTo(12);
        assertThat(world.getWirePower(4, 64, 0)).isEqualTo(11);
    }

    @Test
    @DisplayName("Verify Coordinate packing and unpacking invariants in AlternateCurrentBfsRedstoneSolver")
    public void testCoordinatePacking() {
        int[] testCoords = { 0, 1, -1, 1000, -1000, 30000000, -30000000 };
        for (int x : testCoords) {
            for (int y : new int[]{ -64, 0, 64, 320 }) {
                for (int z : testCoords) {
                    long packed = AlternateCurrentBfsRedstoneSolver.packPos(x, y, z);
                    assertThat(AlternateCurrentBfsRedstoneSolver.unpackX(packed)).isEqualTo(x);
                    assertThat(AlternateCurrentBfsRedstoneSolver.unpackY(packed)).isEqualTo(y);
                    assertThat(AlternateCurrentBfsRedstoneSolver.unpackZ(packed)).isEqualTo(z);
                }
            }
        }
    }
}
