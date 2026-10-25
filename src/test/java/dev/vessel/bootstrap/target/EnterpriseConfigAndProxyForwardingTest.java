package dev.vessel.bootstrap.target;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for Sub-Stage 6.4: {@link ServerBrandIdentifier}, {@link VesselLogger},
 * {@link VesselServerConfig}, {@link VesselFirstRunScaffolder},
 * {@link VesselShutdownCoordinator}, {@link VelocityModernForwardingHandler}, and
 * {@link ScheduledMaintenanceCoordinator}.
 *
 * @version 1.0.0
 */
class EnterpriseConfigAndProxyForwardingTest {

    @TempDir
    Path tempDir;

    @Test
    void testServerBrandIdentifierAndCustomPayloadEncoding() {
        ServerBrandIdentifier brandIdentifier = new ServerBrandIdentifier();
        assertEquals("VESSEL", brandIdentifier.getCanonicalServerBrand());

        byte[] payload = brandIdentifier.encodeBrandCustomPayload();
        byte[] expected = new byte[]{6, 'V', 'E', 'S', 'S', 'E', 'L'};
        assertArrayEquals(expected, payload);
    }

    @Test
    void testVesselLoggerAnsiColoringAndDuplicateSuppression() {
        VesselLogger logger = new VesselLogger();
        logger.info("BOOT", "Initializing microkernel");
        logger.info("BOOT", "Initializing microkernel");
        logger.warn("REMAP", "Single-source metadata fallback");
        logger.error("REGISTRY", "Duplicate key detected");
        logger.error("REGISTRY", "Duplicate key detected");

        assertEquals(2, logger.getSuppressedDuplicateLogCount());
        List<String> history = logger.getFormattedLogHistory();
        assertEquals(3, history.size());
        assertTrue(history.get(0).startsWith("\u001B[37m[VESSEL/INFO][BOOT]"));
        assertTrue(history.get(1).startsWith("\u001B[33m[VESSEL/WARN][REMAP]"));
        assertTrue(history.get(2).startsWith("\u001B[31m[VESSEL/ERROR][REGISTRY]"));
    }

    @Test
    void testFirstRunScaffolderAnd34SectionVesselYamlConfig() throws Exception {
        VesselFirstRunScaffolder scaffolder = new VesselFirstRunScaffolder();
        scaffolder.scaffoldServerDirectories(tempDir);

        assertTrue(Files.isDirectory(tempDir.resolve("mods")));
        assertTrue(Files.isDirectory(tempDir.resolve("plugins")));
        assertTrue(Files.isDirectory(tempDir.resolve("vessel-config")));
        assertTrue(Files.isDirectory(tempDir.resolve("vessel-config/defaults/forge")));
        assertTrue(Files.isDirectory(tempDir.resolve("vessel-config/overrides")));
        assertTrue(Files.exists(tempDir.resolve("eula.txt")));
        assertTrue(Files.exists(tempDir.resolve("vessel.yml")));

        List<String> requiredFiles = scaffolder.getRequiredVesselConfigFiles();
        assertEquals(8, requiredFiles.size());
        for (String fileName : requiredFiles) {
            assertTrue(Files.exists(tempDir.resolve("vessel-config").resolve(fileName)));
        }

        VesselServerConfig config = new VesselServerConfig();
        config.loadOrGenerateDefaults(tempDir);
        String yaml = config.generateDefaultAnnotatedVesselYaml();
        assertTrue(yaml.contains("# Section 1: branding"));
        assertTrue(yaml.contains("# Section 34: lua-computer-governor"));
        assertEquals("VESSEL", config.getString("branding.server-brand-name", "UNKNOWN"));
        assertEquals(250, config.getInt("lua-computer-governor.max-cpu-slice-micros-per-tick", 0));
        assertTrue(config.getBoolean("actor-scheduler.use-virtual-threads", false));
    }

    @Test
    void testVelocityHmacVerificationShutdownAndMaintenanceCountdown() throws Exception {
        VelocityModernForwardingHandler forwarding = new VelocityModernForwardingHandler();
        byte[] payload = "version=1;ip=203.0.113.42;uuid=123e4567-e89b-12d3-a456-426614174000".getBytes(StandardCharsets.UTF_8);
        String secret = "top-secret-velocity-key-2026";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] validSignature = mac.doFinal(payload);

        assertTrue(forwarding.verifyVelocityHmacSignature(payload, validSignature, secret));
        assertFalse(forwarding.verifyVelocityHmacSignature(payload, validSignature, "wrong-secret"));

        String bungeeHandshake = "play.example.com\u0000198.51.100.77\u0000123e4567e89b12d3a456426614174000";
        assertEquals("198.51.100.77", forwarding.extractForwardedPlayerIp(bungeeHandshake));
        assertEquals("203.0.113.42", forwarding.extractForwardedPlayerIp("version=1;ip=203.0.113.42;name=Shiroz"));

        VesselShutdownCoordinator shutdownCoordinator = new VesselShutdownCoordinator();
        List<Integer> order = new ArrayList<>();
        shutdownCoordinator.registerShutdownHook(() -> order.add(1));
        shutdownCoordinator.registerShutdownHook(() -> order.add(2));
        shutdownCoordinator.executeGracefulShutdown(tempDir);
        assertEquals(List.of(2, 1), order);
        assertEquals(2, shutdownCoordinator.getExecutedHookCount());
        assertTrue(shutdownCoordinator.isShutdownCompleted());

        ScheduledMaintenanceCoordinator maintenance = new ScheduledMaintenanceCoordinator();
        assertTrue(maintenance.shouldTriggerWarningCountdown(3, 55, 300));
        assertTrue(maintenance.shouldTriggerWarningCountdown(3, 59, 10));
        assertFalse(maintenance.shouldTriggerWarningCountdown(3, 59, 47));
        assertTrue(maintenance.formatRestartCountdownBroadcast(60).contains("1 minute (60s)"));
        assertTrue(maintenance.formatRestartCountdownBroadcast(5).contains("5 seconds."));
    }
}
