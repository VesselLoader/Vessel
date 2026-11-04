package dev.vessel.kernel.gpu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live hardware verification test validating that the VESSEL GPU subsystem properly detects host
 * graphics hardware (iGPU/dGPU), binds the appropriate sovereign execution strategy, and accelerates
 * parallel compute workloads (explosion raycasts and fluid spread) with zero crashes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class GpuHardwareAccelerationLiveTest {

    @Test
    public void testHostGpuHardwareDetectionAndTopology() {
        GpuHardwareDetector detector = new GpuHardwareDetector();
        GpuDeviceTopology topology = detector.detectHostTopology();

        assertNotNull(topology, "Topology must not be null");
        assertNotNull(topology.getResolvedStrategy(), "Resolved strategy must not be null");

        System.out.println("================================================================================");
        System.out.println("                 VESSEL SOVEREIGN GPU SUBSYSTEM TELEMETRY                       ");
        System.out.println("================================================================================");
        System.out.println(">> Resolved Hardware Strategy: " + topology.getResolvedStrategy());
        System.out.println(">> Discrete GPUs (dGPU) Count: " + topology.getDiscreteGpus().size());
        for (GpuDeviceInfo dgpu : topology.getDiscreteGpus()) {
            System.out.println("   * [dGPU] " + dgpu.deviceName() + " | Vendor: " + dgpu.vendor()
                    + " | Memory: " + (dgpu.vramBytes() / (1024 * 1024)) + " MB VRAM"
                    + " | Compute Units: " + dgpu.computeUnits());
        }

        System.out.println(">> Integrated GPUs (iGPU) Count: " + topology.getIntegratedGpus().size());
        for (GpuDeviceInfo igpu : topology.getIntegratedGpus()) {
            System.out.println("   * [iGPU] " + igpu.deviceName() + " | Vendor: " + igpu.vendor()
                    + " | UMA Shared Memory: " + (igpu.vramBytes() / (1024 * 1024)) + " MB"
                    + " | Compute Units: " + igpu.computeUnits());
        }

        System.out.println(">> Fallback Device: " + topology.getFallbackDevice().deviceName()
                + " | Available Cores: " + topology.getFallbackDevice().computeUnits());
        System.out.println("================================================================================");

        // Verification assertions
        assertNotNull(topology.getFallbackDevice(), "Fallback device must always exist");
        assertTrue(topology.getFallbackDevice().computeUnits() > 0, "Host must have at least 1 CPU core");
    }

    @Test
    public void testExplosionRaycastAccelerationThroughput() {
        GpuComputeDispatcher dispatcher = GpuComputeDispatcher.getActiveDispatcher();
        assertNotNull(dispatcher, "Active dispatcher must be initialized");

        int rayCount = 1024;
        float[] dirX = new float[rayCount];
        float[] dirY = new float[rayCount];
        float[] dirZ = new float[rayCount];
        float[] outDensities = new float[rayCount];

        // Generate normalized spherical ray directions
        for (int i = 0; i < rayCount; i++) {
            double theta = (2.0 * Math.PI * i) / rayCount;
            double phi = (Math.PI * i) / rayCount;
            dirX[i] = (float) (Math.sin(phi) * Math.cos(theta));
            dirY[i] = (float) (Math.cos(phi));
            dirZ[i] = (float) (Math.sin(phi) * Math.sin(theta));
        }

        // Measure batch execution time
        long startNanos = System.nanoTime();
        int processed = dispatcher.dispatchExplosionRaycasts(
                0.0f, 64.0f, 0.0f,
                50.0f, // Heavy explosion (Supercritical reactor meltdown or industrial blast)
                dirX, dirY, dirZ,
                16,
                outDensities
        );
        long elapsedNanos = System.nanoTime() - startNanos;

        assertEquals(rayCount, processed, "All 1,024 rays must be processed");
        for (int i = 0; i < rayCount; i++) {
            assertTrue(outDensities[i] >= 0.0f, "Density must be non-negative");
        }

        double elapsedMicros = elapsedNanos / 1000.0;
        double raysPerSecond = (rayCount / (double) elapsedNanos) * 1_000_000_000.0;

        System.out.println(">> EXPLOSION RAYCAST ACCELERATION BENCHMARK:");
        System.out.println("   * Processed Rays: " + processed + " rays");
        System.out.println("   * Latency: " + String.format("%.2f", elapsedMicros) + " us (" + elapsedNanos + " ns)");
        System.out.println("   * Throughput: " + String.format("%,.0f", raysPerSecond) + " rays/sec");
        System.out.println("   * Total Offloaded Telemetry Count: " + dispatcher.getExplosionProcessor().getTotalOffloadedRays());
    }

    @Test
    public void testFluidSpreadCellularAutomataAcceleration() {
        GpuComputeDispatcher dispatcher = GpuComputeDispatcher.getActiveDispatcher();

        int cellCount = 256;
        byte[] fluidLevels = new byte[cellCount];
        long[] passableMask = new long[cellCount / 64];
        byte[] outNextLevels = new byte[cellCount];

        // Seed initial fluid source levels
        for (int i = 0; i < cellCount; i++) {
            fluidLevels[i] = (byte) ((i % 8) + 1);
        }
        for (int i = 0; i < passableMask.length; i++) {
            passableMask[i] = -1L; // All voxels passable
        }

        long startNanos = System.nanoTime();
        int changed = dispatcher.dispatchFluidSpread(fluidLevels, passableMask, outNextLevels);
        long elapsedNanos = System.nanoTime() - startNanos;

        assertTrue(changed > 0, "Fluid simulation must update active cells");
        assertEquals(cellCount, dispatcher.getFluidProcessor().getTotalFluidStepsComputed(),
                "Telemetry must reflect total simulated cells");

        double elapsedMicros = elapsedNanos / 1000.0;
        System.out.println(">> FLUID CELLULAR AUTOMATA ACCELERATION BENCHMARK:");
        System.out.println("   * Total Cells: " + cellCount);
        System.out.println("   * Changed Cells: " + changed);
        System.out.println("   * Latency: " + String.format("%.2f", elapsedMicros) + " us (" + elapsedNanos + " ns)");
        System.out.println("   * Total Fluid Steps Telemetry: " + dispatcher.getFluidProcessor().getTotalFluidStepsComputed());
    }
}
