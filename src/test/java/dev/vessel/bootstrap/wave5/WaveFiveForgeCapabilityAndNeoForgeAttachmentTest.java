package dev.vessel.bootstrap.wave5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;
import net.neoforged.neoforge.attachment.AttachmentHolderMap;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 5 verifying MinecraftForge {@link Capability},
 * {@link CapabilityToken}, and {@link LazyOptional}, alongside NeoForge {@link AttachmentType},
 * {@link AttachmentHolderMap}, {@link PayloadRegistrar}, and {@link IPayloadContext}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveFiveForgeCapabilityAndNeoForgeAttachmentTest {

    private boolean invalidationNotified;
    private int syncedManaPayloadValue;

    public Integer supplyDefaultEnergy() {
        return 10000;
    }

    public void onEnergyLazyOptionalInvalidated(LazyOptional<Integer> invalidated) {
        this.invalidationNotified = !invalidated.isPresent();
    }

    public Integer supplyDefaultMana() {
        return 100;
    }

    public void handleNeoForgeManaPayload(Object payload, IPayloadContext ctx) {
        if (payload instanceof Integer manaVal) {
            this.syncedManaPayloadValue = manaVal;
        }
        ctx.enqueueWork(this::recordMainThreadSyncTask);
    }

    public void recordMainThreadSyncTask() {
        // Executed on IPayloadContext main thread queue
    }

    @Test
    void testForgeLazyOptionalCapabilityAndNeoForgeAttachmentsAndPayloads() {
        Capability<Integer> energyCap = new Capability<>("forge:energy");
        CapabilityToken<Integer> token = new CapabilityToken<>("forge:energy");
        assertEquals("forge:energy", energyCap.getName());
        assertTrue(energyCap.isRegistered());
        assertEquals("forge:energy", token.getTypeIdentifier());

        LazyOptional<Integer> lazyEnergy = LazyOptional.of(this::supplyDefaultEnergy);
        lazyEnergy.addListener(this::onEnergyLazyOptionalInvalidated);

        assertTrue(lazyEnergy.isPresent());
        assertEquals(10000, lazyEnergy.orElse(0));
        assertEquals(10000, energyCap.orEmpty(energyCap, lazyEnergy).orElse(0));

        Capability<String> otherCap = new Capability<>("forge:item_handler");
        assertFalse(energyCap.orEmpty(otherCap, lazyEnergy).isPresent());

        lazyEnergy.invalidate();
        assertTrue(invalidationNotified);
        assertFalse(lazyEnergy.isPresent());
        assertEquals(0, lazyEnergy.orElse(0));

        AttachmentType<Integer> manaAttachment =
                AttachmentType.of("vessel:mana", this::supplyDefaultMana);
        assertEquals("vessel:mana", manaAttachment.getKey());

        AttachmentHolderMap holder = new AttachmentHolderMap();
        assertFalse(holder.hasData(manaAttachment));
        assertEquals(100, holder.getData(manaAttachment));
        assertTrue(holder.hasData(manaAttachment));
        assertEquals(250, holder.setData(manaAttachment, 250));
        assertEquals(250, holder.getData(manaAttachment));
        assertEquals(250, holder.removeData(manaAttachment));
        assertFalse(holder.hasData(manaAttachment));

        PayloadRegistrar registrar = new PayloadRegistrar("1.0.0");
        assertEquals("1.0.0", registrar.getVersion());
        registrar.playToServer("vessel:sync_mana", this::handleNeoForgeManaPayload);
        assertTrue(registrar.isRegistered("vessel:sync_mana"));

        IPayloadContext ctx = new IPayloadContext(UUID.randomUUID(), "1.0.0");
        boolean dispatched = registrar.dispatch("vessel:sync_mana", 500, ctx);
        assertTrue(dispatched);
        assertEquals(500, syncedManaPayloadValue);
        assertEquals(1, ctx.getExecutedTaskCount());
    }
}
