package dev.vessel.kernel.network.handshake;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite verifying all 5 micro-classes of the Strict Client-Modded Peer Mode
 * handshake enforcement subsystem.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class ClientModdedPeerHandshakeTest {

    @Test
    @DisplayName("ClientPresentationMode: Enum contracts, CLI argument parsing, and configuration loading")
    void testClientPresentationModeContracts() {
        ClientPresentationMode vanilla = ClientPresentationMode.SOVEREIGN_VANILLA;
        ClientPresentationMode strict = ClientPresentationMode.STRICT_CLIENT_MODDED;

        // Invariants
        assertFalse(vanilla.enforcesClientMods());
        assertFalse(vanilla.bypassesServerPresentation());
        assertTrue(vanilla.allowsVanillaClients());
        assertEquals("SOVEREIGN_VANILLA", vanilla.getConfigurationName());

        assertTrue(strict.enforcesClientMods());
        assertTrue(strict.bypassesServerPresentation());
        assertFalse(strict.allowsVanillaClients());
        assertEquals("STRICT_CLIENT_MODDED", strict.getConfigurationName());

        // CLI parsing
        assertEquals(strict, ClientPresentationMode.fromCliArgs(new String[]{"--vessel-enforce-client-mods"}));
        assertEquals(strict, ClientPresentationMode.fromCliArgs(new String[]{"--nogui", "--vessel-strict-modded"}));
        assertEquals(strict, ClientPresentationMode.fromCliArgs(new String[]{"--vessel-native-modded"}));
        assertEquals(vanilla, ClientPresentationMode.fromCliArgs(new String[]{"--nogui", "--max-memory=4G"}));
        assertEquals(vanilla, ClientPresentationMode.fromCliArgs(new String[0]));
        assertEquals(vanilla, ClientPresentationMode.fromCliArgs(null));

        // Config parsing
        assertEquals(strict, ClientPresentationMode.fromConfigValue("STRICT_CLIENT_MODDED"));
        assertEquals(strict, ClientPresentationMode.fromConfigValue("strict_client_modded"));
        assertEquals(vanilla, ClientPresentationMode.fromConfigValue("SOVEREIGN_VANILLA"));
        assertEquals(vanilla, ClientPresentationMode.fromConfigValue(""));
        assertEquals(vanilla, ClientPresentationMode.fromConfigValue(null));
    }

    @Test
    @DisplayName("ModManifestAuditEvaluator: Registry SHA-256 computation, fast-path verification, and SemVer checks")
    void testModManifestAuditEvaluator() {
        ModManifestAuditEvaluator evaluator = new ModManifestAuditEvaluator();

        Map<String, String> serverMods = Map.of(
                "create", "6.0.1",
                "mekanism", "10.7.0",
                "irons_spellbooks", "3.4.0"
        );

        String hash = evaluator.computeModRegistryHash(serverMods);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 hex is 64 characters

        // Constant-time fast-path verification
        assertTrue(evaluator.verifyRegistryHash(hash, serverMods));
        assertFalse(evaluator.verifyRegistryHash("invalid_hash_string", serverMods));
        assertFalse(evaluator.verifyRegistryHash(null, serverMods));

        // SemVer and exact version evaluation
        assertTrue(evaluator.isVersionCompatible("6.0.1", "6.0.1", true));
        assertFalse(evaluator.isVersionCompatible("6.0.2", "6.0.1", true)); // exact fails
        assertTrue(evaluator.isVersionCompatible("6.0.2", "6.0.1", false)); // patch discrepancy allowed

        // Constraint syntax ">= 6.0.1"
        assertTrue(evaluator.isVersionCompatible("6.0.2", ">= 6.0.1", false));
        assertFalse(evaluator.isVersionCompatible("5.9.0", ">= 6.0.1", false));

        // Missing mods detection
        Map<String, String> clientWithMissing = Map.of(
                "create", "6.0.1"
        );
        List<String> missing = evaluator.findMissingMods(clientWithMissing, serverMods);
        assertEquals(2, missing.size()); // mekanism and irons_spellbooks missing

        // Version conflict detection
        Map<String, String> clientWithConflict = Map.of(
                "create", "6.0.1",
                "mekanism", "10.7.0",
                "irons_spellbooks", "3.2.1"
        );
        Map<String, String> conflicts = evaluator.findVersionConflicts(clientWithConflict, serverMods, false);
        assertEquals(1, conflicts.size());
        assertTrue(conflicts.containsKey("irons_spellbooks"));

        // Boolean evaluation
        assertFalse(evaluator.evaluate(clientWithMissing, serverMods, false));
        assertFalse(evaluator.evaluate(clientWithConflict, serverMods, false));

        Map<String, String> validClient = Map.of(
                "create", "6.0.1",
                "mekanism", "10.7.0",
                "irons_spellbooks", "3.4.0",
                "extra_client_mod", "1.0.0"
        );
        assertTrue(evaluator.evaluate(validClient, serverMods, false));
    }

    @Test
    @DisplayName("MissingModDisconnectionPayload: Rich text formatting, JSON payload, and wire bytes")
    void testMissingModDisconnectionPayload() {
        List<String> missing = List.of(
                "create (Required: >= 6.0.1 | Client: Not Installed)",
                "mekanism (Required: 10.7.0 | Client: Not Installed)"
        );
        Map<String, String> conflicts = Map.of(
                "irons_spellbooks", "Server: 3.4.0 | Client: 3.2.1 - Outdated"
        );
        String downloadUrl = "https://your-community.example.com/modpack";

        MissingModDisconnectionPayload payload = MissingModDisconnectionPayload.create(
                missing, conflicts, downloadUrl
        );

        assertNotNull(payload);
        assertTrue(payload.hasViolations());
        assertEquals(2, payload.getMissingMods().size());
        assertEquals(1, payload.getVersionConflicts().size());
        assertEquals(downloadUrl, payload.getDownloadUrl());

        // Verify rich text contents
        String richText = payload.getFormattedRichText();
        assertTrue(richText.contains(MissingModDisconnectionPayload.HEADER_BANNER));
        assertTrue(richText.contains(MissingModDisconnectionPayload.SUBTITLE_MISMATCH));
        assertTrue(richText.contains(MissingModDisconnectionPayload.SECTION_MISSING_MODS));
        assertTrue(richText.contains("create"));
        assertTrue(richText.contains(MissingModDisconnectionPayload.SECTION_VERSION_CONFLICTS));
        assertTrue(richText.contains("irons_spellbooks"));
        assertTrue(richText.contains(downloadUrl));

        // Verify JSON text component
        String json = payload.getJsonPayload();
        assertTrue(json.contains("\"text\":"));
        assertTrue(json.contains("\"color\":\"red\""));

        // Verify wire payload bytes match JSON
        byte[] wire = payload.getWirePayload();
        assertArrayEquals(json.getBytes(StandardCharsets.UTF_8), wire);

        // Equals and toString contract
        MissingModDisconnectionPayload payload2 = MissingModDisconnectionPayload.create(
                missing, conflicts, downloadUrl
        );
        assertEquals(payload, payload2);
        assertEquals(payload.hashCode(), payload2.hashCode());
        assertTrue(payload.toString().contains("missingCount=2"));
    }

    @Test
    @DisplayName("DirectModdedPacketRouter: Direct channel filtering, verified connection routing, and metrics")
    void testDirectModdedPacketRouter() {
        DirectModdedPacketRouter router = new DirectModdedPacketRouter();

        // Default direct channels check
        assertTrue(router.isDirectChannel("create:contraption_update"));
        assertTrue(router.isDirectChannel("mekanism:tile_network_sync"));
        assertTrue(router.isDirectChannel("irons_spellbooks:cast_spell"));
        assertFalse(router.isDirectChannel("minecraft:brand"));

        // Dynamic registration
        router.registerDirectChannel("custom_mod:data");
        assertTrue(router.isDirectChannel("custom_mod:data"));
        router.unregisterDirectChannel("custom_mod:data");
        assertFalse(router.isDirectChannel("custom_mod:data"));

        String connId = "session-100";
        byte[] samplePacket = new byte[]{0x01, 0x02, 0x03, 0x04};

        // Unregistered connection cannot route
        assertFalse(router.canRouteDirectly(connId, "create:contraption_update"));
        assertNull(router.routeOutboundPacket(connId, "create:contraption_update", samplePacket));

        // Registered in SOVEREIGN_VANILLA cannot route directly (must go through presentation layer)
        router.registerConnection(connId, ClientPresentationMode.SOVEREIGN_VANILLA);
        assertFalse(router.canRouteDirectly(connId, "create:contraption_update"));

        // Switch to STRICT_CLIENT_MODDED but unverified
        router.registerConnection(connId, ClientPresentationMode.STRICT_CLIENT_MODDED);
        assertFalse(router.canRouteDirectly(connId, "create:contraption_update"));

        // Mark verified -> now authorized for direct routing
        router.markConnectionVerified(connId);
        assertTrue(router.canRouteDirectly(connId, "create:contraption_update"));

        // Route packet directly
        byte[] routed = router.routeOutboundPacket(connId, "create:contraption_update", samplePacket);
        assertArrayEquals(samplePacket, routed);

        // Metrics verification
        assertEquals(1L, router.getRoutedPacketCount());
        assertEquals(4L, router.getRoutedByteCount());
        assertEquals(1, router.getActiveConnectionCount());
        assertEquals(1, router.getVerifiedConnectionCount());

        // Unregister
        router.unregisterConnection(connId);
        assertFalse(router.isConnectionVerified(connId));
        assertEquals(0, router.getActiveConnectionCount());

        router.resetMetrics();
        assertEquals(0L, router.getRoutedPacketCount());
    }

    @Test
    @DisplayName("ClientModdedHandshakeGateway: End-to-end multi-loader handshake lifecycle and verification")
    void testClientModdedHandshakeGatewayLifecycle() {
        ModManifestAuditEvaluator evaluator = new ModManifestAuditEvaluator();
        DirectModdedPacketRouter router = new DirectModdedPacketRouter();
        ClientModdedHandshakeGateway gateway = new ClientModdedHandshakeGateway(
                ClientPresentationMode.STRICT_CLIENT_MODDED,
                evaluator,
                router
        );

        gateway.addServerActiveMod("create", "6.0.1");
        gateway.addServerActiveMod("mekanism", "10.7.0");

        // Supported channels
        assertTrue(gateway.isModdedHandshakeChannel(ClientModdedHandshakeGateway.FML_HANDSHAKE_CHANNEL));
        assertTrue(gateway.isModdedHandshakeChannel(ClientModdedHandshakeGateway.NEOFORGE_HANDSHAKE_CHANNEL));
        assertTrue(gateway.isModdedHandshakeChannel(ClientModdedHandshakeGateway.FABRIC_NETWORKING_CHANNEL));
        assertFalse(gateway.isModdedHandshakeChannel("minecraft:custom_channel"));

        // Scenario 1: Handshake initiation for Strict Mode client
        String conn1 = "player-conn-1";
        String outboundQuery = gateway.initiateHandshake(conn1, ClientModdedHandshakeGateway.FML_HANDSHAKE_CHANNEL);
        assertEquals(ClientModdedHandshakeGateway.VESSEL_MOD_AUDIT_CHANNEL, outboundQuery);
        assertEquals(1, gateway.getActivePendingCount());
        assertFalse(gateway.isConnectionReadyForPlay(conn1));

        // Scenario 2: Client response with missing mods -> Rejection payload returned
        Map<String, String> badClientMods = Map.of("create", "6.0.1"); // missing mekanism
        MissingModDisconnectionPayload rejection = gateway.processModAuditResponse(conn1, badClientMods);
        assertNotNull(rejection);
        assertTrue(rejection.hasViolations());
        assertFalse(gateway.isConnectionReadyForPlay(conn1));
        assertFalse(router.isConnectionVerified(conn1));

        // Scenario 3: Valid client connects -> Verified and authorized for play
        String conn2 = "player-conn-2";
        gateway.initiateHandshake(conn2, ClientModdedHandshakeGateway.NEOFORGE_HANDSHAKE_CHANNEL);
        Map<String, String> goodClientMods = Map.of(
                "create", "6.0.1",
                "mekanism", "10.7.0"
        );
        MissingModDisconnectionPayload successResult = gateway.processModAuditResponse(conn2, goodClientMods);
        assertNull(successResult); // null indicates clean pass
        assertTrue(gateway.isConnectionReadyForPlay(conn2));
        assertTrue(router.isConnectionVerified(conn2));

        // Direct packet router can now route packets for conn2
        assertTrue(router.canRouteDirectly(conn2, "create:contraption_update"));

        // Scenario 4: Fast-path SHA-256 hash match
        String conn3 = "player-conn-3";
        gateway.initiateHandshake(conn3, ClientModdedHandshakeGateway.FABRIC_NETWORKING_CHANNEL);
        String serverHash = evaluator.computeModRegistryHash(gateway.getServerActiveMods());
        boolean hashVerified = gateway.processModAuditHashResponse(conn3, serverHash);
        assertTrue(hashVerified);
        assertTrue(gateway.isConnectionReadyForPlay(conn3));
        assertTrue(router.isConnectionVerified(conn3));

        // Scenario 5: Sovereign Vanilla Mode allows zero-mod client
        gateway.setPresentationMode(ClientPresentationMode.SOVEREIGN_VANILLA);
        String conn4 = "vanilla-player";
        gateway.initiateHandshake(conn4, ClientModdedHandshakeGateway.FML_HANDSHAKE_CHANNEL);
        MissingModDisconnectionPayload vanillaResult = gateway.processModAuditResponse(conn4, Map.of());
        assertNull(vanillaResult);
        assertTrue(gateway.isConnectionReadyForPlay(conn4));

        // Cleanup
        gateway.terminateConnection(conn1);
        gateway.terminateConnection(conn2);
        gateway.terminateConnection(conn3);
        gateway.terminateConnection(conn4);
    }
}
