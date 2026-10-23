package dev.vessel.adapter.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Coordinates Forge and NeoForge lifecycle setup queues ({@code FMLCommonSetupEvent#enqueueWork})
 * and provides the canonical list of replayable registration events per Lesson L26.
 *
 * @version 1.0.0
 */
public final class ForgeLifecycleAdapter {

    private static final List<String> REPLAYABLE_REGISTRATION_EVENTS = List.of(
            "RegisterEvent",
            "NewRegistryEvent",
            "DataPackRegistryEvent.NewRegistry",
            "EntityAttributeCreationEvent",
            "EntityAttributeModificationEvent",
            "SpawnPlacementRegisterEvent",
            "RegisterCapabilitiesEvent",
            "RegisterPayloadHandlersEvent"
    );

    private final Queue<Runnable> commonSetupWorkQueue = new ConcurrentLinkedQueue<>();

    /**
     * Creates a new Forge/NeoForge lifecycle adapter.
     */
    public ForgeLifecycleAdapter() {
    }

    /**
     * Enqueues a deferred setup task submitted by a mod during {@code FMLCommonSetupEvent}.
     *
     * @param modId the owning mod namespace ID
     * @param work  the setup work runnable
     */
    public void enqueueCommonSetupWork(String modId, Runnable work) {
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(work, "work must not be null");
        commonSetupWorkQueue.add(work);
    }

    /**
     * Executes and drains all queued {@code FMLCommonSetupEvent} tasks synchronously.
     *
     * @return the number of setup tasks executed
     */
    public int flushCommonSetupQueue() {
        int executed = 0;
        Runnable task;
        while ((task = commonSetupWorkQueue.poll()) != null) {
            task.run();
            executed++;
        }
        return executed;
    }

    /**
     * Returns the immutable list of Forge/NeoForge registration events that support
     * deterministic retroactive replay during hybrid bootstrap (Lesson L26).
     *
     * @return list of replayable registration event names
     */
    public List<String> getReplayableRegistrationEvents() {
        return new ArrayList<>(REPLAYABLE_REGISTRATION_EVENTS);
    }
}
