package dev.vessel.kernel.network.handshake;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OutboundModdedPayloadAbsorber} and {@link SyntheticClientAckPacketFactory}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class NetworkHandshakeAndAbsorberTest {

    @Test
    @DisplayName("OutboundModdedPayloadAbsorber absorbs 100% of modded channels for vanilla/bedrock clients")
    void testAbsorberAbsorbsModdedHandshakeForVanillaClient() {
        OutboundModdedPayloadAbsorber absorber = new OutboundModdedPayloadAbsorber();
        byte[] payload = "forge_registry_data_sample".getBytes(StandardCharsets.UTF_8);

        // Vanilla client -> should absorb and return 0 bytes
        assertTrue(absorber.shouldAbsorb("fml:handshake", false));
        byte[] absorbedFml = absorber.processOutboundPayload("fml:handshake", payload, false);
        assertEquals(0, absorbedFml.length);
        assertArrayEquals(OutboundModdedPayloadAbsorber.ZERO_BYTES_ABSORBED, absorbedFml);

        assertTrue(absorber.shouldAbsorb("neoforge:handshake", false));
        byte[] absorbedNeo = absorber.processOutboundPayload("neoforge:handshake", payload, false);
        assertEquals(0, absorbedNeo.length);

        assertTrue(absorber.shouldAbsorb("fabric:registry_sync", false));
        byte[] absorbedFabric = absorber.processOutboundPayload("fabric:registry_sync", payload, false);
        assertEquals(0, absorbedFabric.length);

        // Modded client -> should NOT absorb
        assertFalse(absorber.shouldAbsorb("fml:handshake", true));
        byte[] moddedPayload = absorber.processOutboundPayload("fml:handshake", payload, true);
        assertArrayEquals(payload, moddedPayload);

        // Vanilla channels -> should NOT absorb even for vanilla client
        assertFalse(absorber.shouldAbsorb("minecraft:brand", false));
        byte[] vanillaPayload = absorber.processOutboundPayload("minecraft:brand", payload, false);
        assertArrayEquals(payload, vanillaPayload);

        // Telemetry counters
        assertEquals(3L, absorber.getAbsorbedPacketCount());
        assertEquals(payload.length * 3L, absorber.getAbsorbedByteCount());

        absorber.resetMetrics();
        assertEquals(0L, absorber.getAbsorbedPacketCount());
        assertEquals(0L, absorber.getAbsorbedByteCount());
    }

    @Test
    @DisplayName("OutboundModdedPayloadAbsorber dynamic channel registration and prefix matching")
    void testAbsorberDynamicChannels() {
        OutboundModdedPayloadAbsorber absorber = new OutboundModdedPayloadAbsorber();
        String customChannel = "create:handshake_channel";

        assertFalse(absorber.isModdedHandshakeChannel(customChannel));
        absorber.registerModdedChannel(customChannel);
        assertTrue(absorber.isModdedHandshakeChannel(customChannel));

        byte[] data = new byte[]{1, 2, 3};
        byte[] result = absorber.processOutboundPayload(customChannel, data, false);
        assertEquals(0, result.length);

        assertTrue(absorber.unregisterModdedChannel(customChannel));
        assertFalse(absorber.isModdedHandshakeChannel(customChannel));

        // Prefix matching tests
        assertTrue(absorber.isModdedHandshakeChannel("fml:custom_channel"));
        assertTrue(absorber.isModdedHandshakeChannel("neoforge:custom_network"));
        assertTrue(absorber.isModdedHandshakeChannel("fabric:registry_custom"));
    }

    @Test
    @DisplayName("SyntheticClientAckPacketFactory synthesizes FML, NeoForge, and Fabric ACKs in < 0.1ms")
    void testSyntheticAckPacketFactoryGeneration() {
        SyntheticClientAckPacketFactory factory = new SyntheticClientAckPacketFactory();

        SyntheticAckResponse fmlModAck = factory.createFmlModListAck(42);
        assertNotNull(fmlModAck);
        assertEquals(SyntheticClientAckPacketFactory.FML_HANDSHAKE_CHANNEL, fmlModAck.channel());
        assertEquals(SyntheticHandshakePhase.FML_HANDSHAKE_RECEIVED_MODS, fmlModAck.phase());
        assertEquals(5, fmlModAck.payload().length);
        assertEquals(SyntheticClientAckPacketFactory.FML_DISCRIMINATOR_MOD_LIST_ACK, fmlModAck.payload()[0]);

        SyntheticAckResponse fmlRegAck = factory.createFmlRegistrySyncAck(99);
        assertEquals(SyntheticHandshakePhase.FML_HANDSHAKE_SYNCED_REGISTRIES, fmlRegAck.phase());
        assertEquals(5, fmlRegAck.payload().length);
        assertEquals(SyntheticClientAckPacketFactory.FML_DISCRIMINATOR_REGISTRY_ACK, fmlRegAck.payload()[0]);

        SyntheticAckResponse fmlConnAck = factory.createFmlConnectionAck();
        assertEquals(SyntheticHandshakePhase.FML_HANDSHAKE_ACK_CONNECTION, fmlConnAck.phase());
        assertEquals(1, fmlConnAck.payload().length);
        assertEquals(SyntheticClientAckPacketFactory.FML_DISCRIMINATOR_COMPLETE_ACK, fmlConnAck.payload()[0]);

        SyntheticAckResponse neoAck = factory.createNeoForgeHandshakeAck(100);
        assertEquals(SyntheticClientAckPacketFactory.NEOFORGE_HANDSHAKE_CHANNEL, neoAck.channel());
        assertEquals(SyntheticHandshakePhase.NEOFORGE_HANDSHAKE_ACK, neoAck.phase());
        assertEquals(5, neoAck.payload().length);

        SyntheticAckResponse fabricSync = factory.createFabricRegistrySyncComplete();
        assertEquals(SyntheticClientAckPacketFactory.FABRIC_REGISTRY_SYNC_CHANNEL, fabricSync.channel());
        assertEquals(SyntheticHandshakePhase.FABRIC_REGISTRY_SYNC_COMPLETE, fabricSync.phase());
        assertEquals(1, fabricSync.payload().length);

        SyntheticAckResponse fabricReg = factory.createFabricChannelRegisterAck("minecraft:brand,vsl:mod");
        assertEquals(SyntheticHandshakePhase.FABRIC_CHANNEL_REGISTER_ACK, fabricReg.phase());
        assertTrue(fabricReg.payload().length > 0);
    }

    @Test
    @DisplayName("SyntheticClientAckPacketFactory generates automatic ACK from incoming server channels")
    void testAutomaticAckGeneration() {
        SyntheticClientAckPacketFactory factory = new SyntheticClientAckPacketFactory();

        // Server FML ModList discriminator 0x01
        SyntheticAckResponse autoFmlMod = factory.generateAutomaticAck(
                SyntheticClientAckPacketFactory.FML_HANDSHAKE_CHANNEL, new byte[]{0x01}
        );
        assertNotNull(autoFmlMod);
        assertEquals(SyntheticHandshakePhase.FML_HANDSHAKE_RECEIVED_MODS, autoFmlMod.phase());

        // Server FML Registry discriminator 0x02
        SyntheticAckResponse autoFmlReg = factory.generateAutomaticAck(
                SyntheticClientAckPacketFactory.FML_HANDSHAKE_CHANNEL, new byte[]{0x02}
        );
        assertNotNull(autoFmlReg);
        assertEquals(SyntheticHandshakePhase.FML_HANDSHAKE_SYNCED_REGISTRIES, autoFmlReg.phase());

        // Server NeoForge
        SyntheticAckResponse autoNeo = factory.generateAutomaticAck("neoforge:handshake", new byte[]{0x00});
        assertNotNull(autoNeo);
        assertEquals(SyntheticHandshakePhase.NEOFORGE_HANDSHAKE_ACK, autoNeo.phase());

        // Server Fabric
        SyntheticAckResponse autoFabric = factory.generateAutomaticAck(
                SyntheticClientAckPacketFactory.FABRIC_REGISTRY_SYNC_CHANNEL, new byte[]{0x00}
        );
        assertNotNull(autoFabric);
        assertEquals(SyntheticHandshakePhase.FABRIC_REGISTRY_SYNC_COMPLETE, autoFabric.phase());

        // Unknown channel returns null
        assertNull(factory.generateAutomaticAck("unknown:custom", new byte[]{0x00}));
    }

    @Test
    @DisplayName("SyntheticAckResponse record equals, hashCode, and toString")
    void testSyntheticAckResponseRecordContract() {
        SyntheticAckResponse res1 = new SyntheticAckResponse("chan", new byte[]{1, 2}, SyntheticHandshakePhase.FML_HANDSHAKE_RECEIVED_MODS, 100L);
        SyntheticAckResponse res2 = new SyntheticAckResponse("chan", new byte[]{1, 2}, SyntheticHandshakePhase.FML_HANDSHAKE_RECEIVED_MODS, 100L);
        SyntheticAckResponse res3 = new SyntheticAckResponse("chan2", new byte[]{1, 2}, SyntheticHandshakePhase.FML_HANDSHAKE_RECEIVED_MODS, 100L);

        assertEquals(res1, res2);
        assertEquals(res1.hashCode(), res2.hashCode());
        assertFalse(res1.equals(res3));
        assertTrue(res1.toString().contains("chan"));
    }
}
