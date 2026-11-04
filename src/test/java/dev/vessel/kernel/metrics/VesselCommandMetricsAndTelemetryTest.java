package dev.vessel.kernel.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.target.VesselServerConfig;
import dev.vessel.bootstrap.updater.HourlyGitHubAutoUpdateDaemon;
import dev.vessel.command.BuiltinSamplerProfiler;
import dev.vessel.command.VesselRootCommand;
import dev.vessel.kernel.testmatrix.TpsProfileSnapshot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests validating honest runtime metrics and telemetry across {@link VesselRootCommand}
 * and specialized auditor classes in {@link dev.vessel.kernel.metrics}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
class VesselCommandMetricsAndTelemetryTest {

    @Test
    void testLiveTickMetricsCalculationsAndWarmingUpState() {
        VesselLiveTickMetricsProvider.resetSamples();
        VesselLiveTickMetricsProvider provider = new VesselLiveTickMetricsProvider();

        // If no samples recorded yet or when samples are recorded
        if (!provider.hasTickSamples()) {
            assertEquals("[VESSEL] TPS: N/A (Server warming up) | MSPT: N/A (Server warming up)",
                    provider.formatSingleLineTps());
            List<String> warmingReport = provider.formatConsoleTpsReport(8);
            assertTrue(warmingReport.get(0).contains("N/A (Server warming up)"));
        }

        // Record a sequence of live tick durations
        VesselLiveTickMetricsProvider.recordTickDurationNanos(20_000_000L); // 20ms
        VesselLiveTickMetricsProvider.recordTickDurationNanos(25_000_000L); // 25ms
        VesselLiveTickMetricsProvider.recordTickDurationNanos(15_000_000L); // 15ms

        assertTrue(provider.hasTickSamples());
        TpsProfileSnapshot snap = provider.captureSnapshot();
        assertEquals(20.0, snap.currentTps(), 0.01);
        assertEquals(20.0, snap.meanTickDurationMs(), 0.1);
        assertTrue(snap.zeroLagCompliant());

        String singleLine = provider.formatSingleLineTps();
        assertTrue(singleLine.startsWith("[VESSEL] TPS: 20.00 | MSPT: 20.00ms"));
        assertFalse(singleLine.contains("N/A"));

        List<String> consoleReport = provider.formatConsoleTpsReport(16);
        assertEquals(2, consoleReport.size());
        assertTrue(consoleReport.get(0).contains("TPS: 20.00"));
        assertTrue(consoleReport.get(1).contains("16 threads"));
    }

    @Test
    void testEngineStateAuditorHonestStatusWithAutoUpdateDaemon(@TempDir Path tempDir) {
        VesselEngineStateAuditor auditor = new VesselEngineStateAuditor();

        // 1. Without daemon running
        assertFalse(auditor.isAutoUpdateDaemonActive());
        assertEquals("INACTIVE (Daemon not started)", auditor.resolveAutoUpdateStatus());
        String statusStandard = auditor.resolveOptimizationSuiteStatus(false);
        assertEquals("STANDARD (Core Microkernel Engines Active)", statusStandard);

        String singleLine = auditor.formatSingleLineStatus(false);
        assertTrue(singleLine.contains("Auto-Update: INACTIVE"));

        // 2. Start daemon
        HourlyGitHubAutoUpdateDaemon daemon = new HourlyGitHubAutoUpdateDaemon();
        daemon.startHourlyBackgroundUpdater(tempDir, HourlyGitHubAutoUpdateDaemon.DEFAULT_GITHUB_PAGES_MANIFEST_URL);
        try {
            assertTrue(auditor.isAutoUpdateDaemonActive());
            assertTrue(auditor.resolveAutoUpdateStatus().contains("ACTIVE"));
            String singleActive = auditor.formatSingleLineStatus(true);
            assertTrue(singleActive.contains("Auto-Update: ACTIVE"));

            List<String> consoleStatus = auditor.formatConsoleStatusReport(true, false, 5, 10);
            boolean foundAutoUpdate = false;
            for (String line : consoleStatus) {
                if (line.contains("Hourly Auto-Update : ACTIVE")) {
                    foundAutoUpdate = true;
                }
            }
            assertTrue(foundAutoUpdate, "Expected active auto-update in console status report");
        } finally {
            daemon.stopBackgroundUpdater();
        }

        // 3. After daemon stopped
        assertFalse(auditor.isAutoUpdateDaemonActive());
        assertEquals("INACTIVE (Daemon not started)", auditor.resolveAutoUpdateStatus());
    }

    @Test
    void testLiveConfigurationReloaderLoadsRealDiskFile(@TempDir Path tempDir) throws Exception {
        VesselLiveConfigurationReloader reloader = new VesselLiveConfigurationReloader();
        VesselServerConfig config = new VesselServerConfig();

        // Reload where vessel.yml does not exist yet -> generates defaults and loads 34 sections
        VesselConfigurationReloadResult r1 = reloader.reloadConfiguration(tempDir, config);
        assertTrue(r1.success());
        assertEquals(34, r1.sectionCount());
        assertTrue(r1.propertyCount() > 0);
        assertTrue(Files.exists(tempDir.resolve("vessel.yml")));

        String singleReport = reloader.formatSingleLineReload(r1);
        assertTrue(singleReport.startsWith("[VESSEL] Reloaded 34 canonical sections"));

        List<String> consoleReport = reloader.formatConsoleReloadReport(r1);
        assertEquals(2, consoleReport.size());
        assertTrue(consoleReport.get(0).contains("Hot-reloaded 34 enterprise configuration sections"));

        // Now modify vessel.yml on disk and re-load
        Path configFile = tempDir.resolve("vessel.yml");
        String currentContent = Files.readString(configFile, StandardCharsets.UTF_8);
        Files.writeString(configFile, currentContent + "\ncustom-section:\n  custom-key: custom-value\n", StandardCharsets.UTF_8);

        VesselConfigurationReloadResult r2 = reloader.reloadConfiguration(tempDir, config);
        assertTrue(r2.success());
        assertEquals("custom-value", config.getString("custom-section.custom-key", "default"));
    }

    @Test
    void testHeapMemoryInspectorProvidesRealDynamicMetrics() {
        VesselHeapMemoryInspector inspector = new VesselHeapMemoryInspector();

        assertTrue(inspector.getUsedMemoryBytes() > 0L);
        assertTrue(inspector.getTotalMemoryBytes() > 0L);
        assertTrue(inspector.getMaxMemoryBytes() > 0L);
        assertTrue(inspector.getUsedMemoryMb() <= inspector.getTotalMemoryMb());

        String singleLine = inspector.formatSingleLineHeap();
        assertTrue(singleLine.startsWith("[VESSEL] JVM Heap:"));
        assertFalse(singleLine.contains("0 leaked worlds")); // No hardcoded fake data

        List<String> report = inspector.formatConsoleHeapReport();
        assertEquals(2, report.size());
        assertTrue(report.get(0).contains("[VESSEL/HEAP] JVM Memory :"));
        assertFalse(report.get(0).contains("0 leaked worlds"));
        assertFalse(report.get(1).contains("0 leaked worlds"));
    }

    @Test
    void testProfileMetricsProviderHandlesStartStopStatus() {
        VesselProfileMetricsProvider provider = new VesselProfileMetricsProvider();
        VesselLiveTickMetricsProvider tickProvider = new VesselLiveTickMetricsProvider();

        // Start session
        String startSingle = provider.handleSingleLineCommand(new String[]{"profile", "start"});
        assertEquals("[VESSEL] Built-in Profiler: Sampling session STARTED", startSingle);
        assertTrue(BuiltinSamplerProfiler.getDefaultProfiler().isSamplingActive());

        // Record a sample
        BuiltinSamplerProfiler.getDefaultProfiler().recordMethodSample("dev.vessel.test#methodA", 1_000_000L);

        // Stop session
        List<String> stopLines = provider.handleConsoleCommand(new String[]{"profile", "stop"}, tickProvider);
        assertFalse(BuiltinSamplerProfiler.getDefaultProfiler().isSamplingActive());
        assertTrue(stopLines.get(0).contains("Session STOPPED"));
        assertTrue(stopLines.get(1).contains("Captured 1 distinct method frame(s)"));

        // Status check
        List<String> statusLines = provider.handleConsoleCommand(new String[]{"profile", "status"}, tickProvider);
        assertEquals(2, statusLines.size());
        assertTrue(statusLines.get(0).contains("Mode: IDLE"));
        assertFalse(statusLines.get(1).contains("3.7%")); // No hardcoded fake CPU 3.7%
    }

    @Test
    void testVesselRootCommandAllCanonicalSubcommandsExecution() {
        VesselRootCommand rootCommand = new VesselRootCommand();
        assertNotNull(rootCommand.getTickMetricsProvider());
        assertNotNull(rootCommand.getEngineAuditor());
        assertNotNull(rootCommand.getConfigReloader());
        assertNotNull(rootCommand.getHeapInspector());
        assertNotNull(rootCommand.getProfileProvider());
        assertNotNull(rootCommand.getChunkAuditor());
        assertNotNull(rootCommand.getEntityAuditor());
        assertNotNull(rootCommand.getDumpExporter());

        List<String> all13 = List.of(
                "version", "tps", "mods", "plugins", "top", "profile", "heap",
                "chunks", "entities", "pregen", "status", "reload", "dump");

        for (String sub : all13) {
            String output = rootCommand.executeCommand(new String[]{sub});
            assertTrue(output.startsWith("[VESSEL]"), "Failed output prefix for subcommand: " + sub);
            assertFalse(output.contains("Unknown subcommand"), "Unrecognized subcommand: " + sub);
            assertFalse(output.contains("40/40 Engines: ONLINE"), "Found fake engine status in: " + output);
            assertFalse(output.contains("0 leaked worlds"), "Found fake leaked worlds in: " + output);
        }

        // Live console commands
        List<String> statusLines = rootCommand.executeLiveConsoleCommand(
                new String[]{"status"}, "VESSEL-TEST", "26.3", List.of(), 0, false, false);
        assertFalse(statusLines.isEmpty());
        assertTrue(statusLines.get(1).contains("VESSEL Sovereign Engine Status : ONLINE"));

        List<String> tpsLines = rootCommand.executeLiveConsoleCommand(
                new String[]{"tps"}, "VESSEL-TEST", "26.3", List.of(), 0, false, false);
        assertFalse(tpsLines.isEmpty());
        assertTrue(tpsLines.get(0).contains("[VESSEL/TPS]"));

        List<String> heapLines = rootCommand.executeLiveConsoleCommand(
                new String[]{"heap"}, "VESSEL-TEST", "26.3", List.of(), 0, false, false);
        assertFalse(heapLines.isEmpty());
        assertTrue(heapLines.get(0).contains("[VESSEL/HEAP]"));

        // Spark commands
        List<String> sparkTps = rootCommand.executeSparkConsoleCommand(new String[]{"tps"}, "1.10.119", "26.3");
        assertFalse(sparkTps.isEmpty());
        assertTrue(sparkTps.get(0).contains("[spark/VESSEL]"));

        List<String> sparkHealth = rootCommand.executeSparkConsoleCommand(new String[]{"health"}, "1.10.119", "26.3");
        assertFalse(sparkHealth.isEmpty());
        assertTrue(sparkHealth.get(0).contains("[spark/VESSEL]"));
    }
}
