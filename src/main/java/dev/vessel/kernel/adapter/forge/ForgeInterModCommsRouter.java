package dev.vessel.kernel.adapter.forge;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deterministic router coordinating Forge Inter-Mod Communications (IMC) between cooperating mods.
 * <p>
 * Eliminates ECO-005 (IMC dispatch lockups and cross-thread access race conditions during initialization).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ForgeInterModCommsRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeInterModCommsRouter.class);

    private final Map<String, InterModCommsModQueue> queues = new ConcurrentHashMap<>();
    private volatile InterModCommsLifecycleStage currentStage = InterModCommsLifecycleStage.IDLE;

    /**
     * Constructs a new IMC router in IDLE stage.
     */
    public ForgeInterModCommsRouter() {
    }

    /**
     * Advances the router lifecycle stage.
     *
     * @param nextStage the new stage
     */
    public synchronized void transitionStage(InterModCommsLifecycleStage nextStage) {
        Objects.requireNonNull(nextStage, "nextStage must not be null");
        LOGGER.info("[VESSEL-IMC] Advancing IMC stage: {} -> {}", currentStage, nextStage);
        this.currentStage = nextStage;
    }

    /**
     * Returns current lifecycle stage.
     *
     * @return active stage
     */
    public InterModCommsLifecycleStage getCurrentStage() {
        return currentStage;
    }

    /**
     * Enqueues an IMC message from a sender mod to a recipient mod.
     *
     * @param senderModId sender mod namespace ID
     * @param targetModId target mod namespace ID
     * @param method      action/protocol method identifier
     * @param payload     message payload object
     * @return true if enqueued successfully
     */
    public boolean sendTo(String senderModId, String targetModId, String method, Object payload) {
        Objects.requireNonNull(senderModId, "senderModId must not be null");
        Objects.requireNonNull(targetModId, "targetModId must not be null");
        Objects.requireNonNull(method, "method must not be null");

        if (currentStage != InterModCommsLifecycleStage.ENQUEUE_OPEN
                && currentStage != InterModCommsLifecycleStage.IDLE) {
            String err = "Cannot enqueue IMC message while router is in stage " + currentStage;
            LOGGER.error("[VESSEL-IMC] [ECO-005] {}", err);
            throw new ForgeInterModCommsException(senderModId, targetModId, currentStage, err);
        }

        String normalizedTarget = targetModId.trim().toLowerCase(Locale.ROOT);
        InterModCommsMessage message = new InterModCommsMessage(
                senderModId.trim().toLowerCase(Locale.ROOT),
                normalizedTarget,
                method.trim().toLowerCase(Locale.ROOT),
                payload,
                System.nanoTime()
        );

        InterModCommsModQueue queue = queues.computeIfAbsent(normalizedTarget, InterModCommsModQueue::new);
        queue.enqueue(message);
        LOGGER.debug("[VESSEL-IMC] Enqueued message from '{}' to '{}' (method: '{}')",
                senderModId, targetModId, method);
        return true;
    }

    /**
     * Enqueues an IMC message using a supplier resolved at transmission time.
     *
     * @param senderModId     sender mod identifier
     * @param targetModId     target mod identifier
     * @param method          method identifier
     * @param payloadSupplier supplier yielding the payload
     * @return true if enqueued successfully
     */
    public boolean sendTo(String senderModId, String targetModId, String method, Supplier<?> payloadSupplier) {
        Objects.requireNonNull(payloadSupplier, "payloadSupplier must not be null");
        Object payload = payloadSupplier.get();
        return sendTo(senderModId, targetModId, method, payload);
    }

    /**
     * Retrieves all pending messages targeted at a specific recipient mod.
     *
     * @param targetModId recipient mod identifier
     * @return unmodifiable list of messages
     */
    public List<InterModCommsMessage> getMessages(String targetModId) {
        Objects.requireNonNull(targetModId, "targetModId must not be null");
        String normalized = targetModId.trim().toLowerCase(Locale.ROOT);
        InterModCommsModQueue queue = queues.get(normalized);
        if (queue == null) {
            return Collections.emptyList();
        }
        return queue.getAllMessages();
    }

    /**
     * Retrieves all pending messages targeted at a specific mod for a given method.
     *
     * @param targetModId recipient mod identifier
     * @param method      method identifier
     * @return unmodifiable list of matching messages
     */
    public List<InterModCommsMessage> getMessages(String targetModId, String method) {
        Objects.requireNonNull(targetModId, "targetModId must not be null");
        Objects.requireNonNull(method, "method must not be null");
        String normalized = targetModId.trim().toLowerCase(Locale.ROOT);
        InterModCommsModQueue queue = queues.get(normalized);
        if (queue == null) {
            return Collections.emptyList();
        }
        return queue.getMessagesByMethod(method);
    }

    /**
     * Streams pending messages for a recipient mod.
     *
     * @param targetModId recipient mod identifier
     * @return stream of messages
     */
    public Stream<InterModCommsMessage> streamMessages(String targetModId) {
        Objects.requireNonNull(targetModId, "targetModId must not be null");
        String normalized = targetModId.trim().toLowerCase(Locale.ROOT);
        InterModCommsModQueue queue = queues.get(normalized);
        if (queue == null) {
            return Stream.empty();
        }
        return queue.stream();
    }

    /**
     * Clears all message queues and resets stage to IDLE.
     */
    public void reset() {
        queues.clear();
        currentStage = InterModCommsLifecycleStage.IDLE;
    }
}
