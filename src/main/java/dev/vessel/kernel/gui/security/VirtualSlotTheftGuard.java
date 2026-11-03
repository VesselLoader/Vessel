package dev.vessel.kernel.gui.security;

import java.util.Objects;
import java.util.Optional;

/**
 * Central security arbiter intercepting container clicks and thwarting all 28 virtual GUI theft vectors.
 * Enforces immutable slot policies and coordinates immediate client desync flushes and exploit audit logging.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VirtualSlotTheftGuard {

    private final ContainerClickActionClassifier classifier;
    private final SlotImmediateDesyncFlusher desyncFlusher;
    private final ExploitAuditLogger auditLogger;

    /**
     * Constructs a VirtualSlotTheftGuard with default dependencies.
     */
    public VirtualSlotTheftGuard() {
        this(new ContainerClickActionClassifier(), new SlotImmediateDesyncFlusher(), new ExploitAuditLogger());
    }

    /**
     * Constructs a VirtualSlotTheftGuard with explicit dependencies.
     *
     * @param classifier the click classifier
     * @param desyncFlusher the desync flusher
     * @param auditLogger the security audit logger
     */
    public VirtualSlotTheftGuard(
            ContainerClickActionClassifier classifier,
            SlotImmediateDesyncFlusher desyncFlusher,
            ExploitAuditLogger auditLogger
    ) {
        this.classifier = Objects.requireNonNull(classifier, "classifier cannot be null");
        this.desyncFlusher = Objects.requireNonNull(desyncFlusher, "desyncFlusher cannot be null");
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger cannot be null");
    }

    public ContainerClickActionClassifier getClassifier() {
        return classifier;
    }

    public SlotImmediateDesyncFlusher getDesyncFlusher() {
        return desyncFlusher;
    }

    public ExploitAuditLogger getAuditLogger() {
        return auditLogger;
    }

    /**
     * Evaluates a container click against slot classification and active security policy.
     *
     * @param context the click context snapshot
     * @param slotClassification the target slot classification
     * @param policy the active security policy for the target slot
     * @return the security decision (ALLOW, REJECT_AND_FLUSH, or TRIGGER_ACTION_ONLY)
     */
    public SecurityDecision evaluate(
            ContainerClickContext context,
            VirtualSlotClassification slotClassification,
            VirtualSlotSecurityPolicy policy
    ) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(slotClassification, "slotClassification cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");

        // 1. Detect any intentional theft vector
        Optional<TheftVector> detectedVector = classifier.detectTheftVector(context, slotClassification);
        if (detectedVector.isPresent()) {
            TheftVector vector = detectedVector.get();
            auditLogger.logExploitAttempt(context.playerId(), vector, context.slotIndex(), context.clickTypeName());
            desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
            return SecurityDecision.REJECT_AND_FLUSH;
        }

        // 2. Classify semantic intent
        ClickActionIntent intent = classifier.classifyIntent(context);

        // 3. Handle Malformed / Out of Bounds / Protocol Attacks
        if (intent == ClickActionIntent.OUT_OF_BOUNDS_DROP || intent == ClickActionIntent.SLOT_OVERRUN
                || intent == ClickActionIntent.RAPID_BURST_SORT || intent == ClickActionIntent.CONCURRENT_CONFLICT
                || intent == ClickActionIntent.BUTTON_PACKET_INJECTION) {
            desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
            return SecurityDecision.REJECT_AND_FLUSH;
        }

        // 4. Handle Window Close / Disconnect
        if (intent == ClickActionIntent.WINDOW_CLOSE) {
            if (context.carriedItemVirtual()) {
                desyncFlusher.flushCarried(context.playerId(), 0, ContainerClickContext.ITEM_EMPTY, 0);
                return SecurityDecision.REJECT_AND_FLUSH;
            }
            return SecurityDecision.ALLOW;
        }

        // 5. Enforce Virtual UI Slot Rules
        if (slotClassification == VirtualSlotClassification.BUTTON) {
            if (intent == ClickActionIntent.NORMAL_PICKUP && !context.carriedItemVirtual()) {
                desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
                return SecurityDecision.TRIGGER_ACTION_ONLY;
            }
            desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
            return SecurityDecision.REJECT_AND_FLUSH;
        }

        if (slotClassification == VirtualSlotClassification.PROGRESS_BAR
                || slotClassification == VirtualSlotClassification.DECORATIVE_FRAME) {
            desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
            return SecurityDecision.REJECT_AND_FLUSH;
        }

        // 6. Enforce Functional Mod Machinery Output Slot Rules
        if (slotClassification == VirtualSlotClassification.FUNCTIONAL_MOD_OUTPUT) {
            if (!policy.allowPlace() && !ContainerClickContext.ITEM_EMPTY.equals(context.carriedItemId())) {
                // Prohibit inserting into output slot
                desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
                return SecurityDecision.REJECT_AND_FLUSH;
            }
            if (intent == ClickActionIntent.SWAP_HOTBAR_1_TO_9 || intent == ClickActionIntent.SWAP_OFFHAND_KEY_F) {
                // Prohibit swapping hotbar/offhand into machine output
                desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
                return SecurityDecision.REJECT_AND_FLUSH;
            }
            return SecurityDecision.ALLOW;
        }

        // 7. Enforce Real Player Backpack Slot Rules
        if (slotClassification == VirtualSlotClassification.REAL_PLAYER_BACKPACK) {
            if (context.carriedItemVirtual()) {
                // Prohibit transferring virtual item from cursor into player backpack
                desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
                return SecurityDecision.REJECT_AND_FLUSH;
            }
            return SecurityDecision.ALLOW;
        }

        // 8. General Policy Fallback
        if (!isOperationPermittedByPolicy(intent, policy)) {
            desyncFlusher.flushFullSync(context.playerId(), 0, context.slotIndex(), context.targetItemId(), context.carriedItemId());
            return SecurityDecision.REJECT_AND_FLUSH;
        }

        return SecurityDecision.ALLOW;
    }

    private boolean isOperationPermittedByPolicy(ClickActionIntent intent, VirtualSlotSecurityPolicy policy) {
        return switch (intent) {
            case NORMAL_PICKUP -> policy.allowPickup() || policy.allowPlace();
            case SWAP_HOTBAR_1_TO_9 -> policy.allowHotbarSwap();
            case SWAP_OFFHAND_KEY_F -> policy.allowOffhandSwap();
            case DROP_SINGLE_Q, DROP_STACK_CTRL_Q -> policy.allowDropThrow();
            case SHIFT_TRANSFER -> policy.allowQuickMoveShift();
            case DOUBLE_CLICK_GATHER -> policy.allowPickupAllDouble();
            case DRAG_SPLIT -> policy.allowDragSplit();
            case CLONE_CREATIVE -> false;
            default -> false;
        };
    }
}
