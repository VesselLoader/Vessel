package dev.vessel.kernel.gui.security;

import java.util.Objects;
import java.util.Optional;

/**
 * Classifies raw container click packet context into high-level semantic action intents
 * and maps suspicious interactions to one of the 28 recognized theft attack vectors.
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public final class ContainerClickActionClassifier {

    public static final String ACTION_DISCONNECT = "DISCONNECT";
    public static final String ACTION_CRASH = "CRASH";
    public static final String ACTION_OUT_OF_ORDER_CLOSE = "OUT_OF_ORDER_CLOSE";
    public static final String ACTION_SCROLL_SWAP = "SCROLL_SWAP";
    public static final String ACTION_FAST_SORT = "FAST_SORT";
    public static final String ACTION_HIGH_LATENCY_DESYNC = "HIGH_LATENCY_DESYNC";
    public static final String ACTION_BUTTON_INJECTION = "BUTTON_INJECTION";
    public static final String ACTION_FORM_DESYNC = "FORM_DESYNC";

    public static final int DRAG_BUTTON_SPLIT_EVENLY = 0;
    public static final int DRAG_BUTTON_SPLIT_SINGLE = 4;
    public static final int DRAG_BUTTON_CLONE_CREATIVE = 8;

    /**
     * Resolves the primary action intent from the given interaction context.
     *
     * @param context the click context snapshot
     * @return classified semantic action intent
     */
    public ClickActionIntent classifyIntent(ContainerClickContext context) {
        Objects.requireNonNull(context, "context cannot be null");

        if (context.windowClosing()) {
            return ClickActionIntent.WINDOW_CLOSE;
        }
        if (context.concurrentRaceDetected()) {
            return ClickActionIntent.CONCURRENT_CONFLICT;
        }
        if (context.rapidBurstDetected()) {
            return ClickActionIntent.RAPID_BURST_SORT;
        }
        if (context.outOfBounds() || context.slotIndex() == ContainerClickContext.OUT_OF_BOUNDS_SLOT_ID) {
            return ClickActionIntent.OUT_OF_BOUNDS_DROP;
        }
        if (context.slotIndex() < 0 || (context.containerSize() > 0 && context.slotIndex() >= context.containerSize())) {
            return ClickActionIntent.SLOT_OVERRUN;
        }

        String type = context.clickTypeName().toUpperCase();
        return switch (type) {
            case ContainerClickContext.CLICK_TYPE_SWAP -> {
                if (context.button() == ContainerClickContext.OFFHAND_BUTTON_CODE) {
                    yield ClickActionIntent.SWAP_OFFHAND_KEY_F;
                }
                yield ClickActionIntent.SWAP_HOTBAR_1_TO_9;
            }
            case ContainerClickContext.CLICK_TYPE_THROW -> {
                if (context.button() == 1) {
                    yield ClickActionIntent.DROP_STACK_CTRL_Q;
                }
                yield ClickActionIntent.DROP_SINGLE_Q;
            }
            case ContainerClickContext.CLICK_TYPE_QUICK_MOVE -> ClickActionIntent.SHIFT_TRANSFER;
            case ContainerClickContext.CLICK_TYPE_PICKUP_ALL -> ClickActionIntent.DOUBLE_CLICK_GATHER;
            case ContainerClickContext.CLICK_TYPE_QUICK_CRAFT -> ClickActionIntent.DRAG_SPLIT;
            case ContainerClickContext.CLICK_TYPE_CLONE -> ClickActionIntent.CLONE_CREATIVE;
            case ContainerClickContext.CLICK_TYPE_PICKUP -> ClickActionIntent.NORMAL_PICKUP;
            case ACTION_BUTTON_INJECTION -> ClickActionIntent.BUTTON_PACKET_INJECTION;
            default -> ClickActionIntent.UNKNOWN;
        };
    }

    /**
     * Detects whether the current interaction context targets any of the 28 specific
     * theft exploit vectors defined in the VESSEL anti-theft specification.
     *
     * @param context the click context snapshot
     * @param slotClassification the target slot classification
     * @return Optional containing detected TheftVector, or empty if normal interaction
     */
    public Optional<TheftVector> detectTheftVector(ContainerClickContext context, VirtualSlotClassification slotClassification) {
        Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(slotClassification, "slotClassification cannot be null");

        String type = context.clickTypeName().toUpperCase();

        // 1. KEYBOARD SWAP EXPLOITS
        if (ContainerClickContext.CLICK_TYPE_SWAP.equals(type) && slotClassification.isVirtual()) {
            if (context.button() == ContainerClickContext.OFFHAND_BUTTON_CODE) {
                return Optional.of(TheftVector.VECTOR_02_OFFHAND_SWAP);
            }
            return Optional.of(TheftVector.VECTOR_01_HOTBAR_SWAP);
        }

        // 2. DROP & THROW EXPLOITS
        if (ContainerClickContext.CLICK_TYPE_THROW.equals(type) && slotClassification.isVirtual()) {
            if (context.button() == 1) {
                return Optional.of(TheftVector.VECTOR_04_THROW_STACK_KEY);
            }
            return Optional.of(TheftVector.VECTOR_03_THROW_SINGLE_KEY);
        }
        if ((context.outOfBounds() || context.slotIndex() == ContainerClickContext.OUT_OF_BOUNDS_SLOT_ID) && context.carriedItemVirtual()) {
            return Optional.of(TheftVector.VECTOR_05_OUT_OF_BOUNDS_BORDER_DROP);
        }

        // 3. RAPID BATCH & DRAG EXPLOITS
        if (context.rapidBurstDetected()) {
            if (ACTION_FAST_SORT.equalsIgnoreCase(context.clickTypeName())) {
                return Optional.of(TheftVector.VECTOR_17_FAST_SORT_INJECTION);
            }
            return Optional.of(TheftVector.VECTOR_16_SHIFT_RIGHT_CLICK_BURST);
        }
        if (ContainerClickContext.CLICK_TYPE_QUICK_MOVE.equals(type)) {
            if (slotClassification == VirtualSlotClassification.BUTTON) {
                return Optional.of(TheftVector.VECTOR_24_SHIFT_INTO_VIRTUAL_BUTTON);
            }
            if (slotClassification.isVirtual()) {
                return Optional.of(TheftVector.VECTOR_06_SHIFT_QUICK_MOVE);
            }
        }
        if (ContainerClickContext.CLICK_TYPE_PICKUP_ALL.equals(type)) {
            if (slotClassification.isVirtual()) {
                return Optional.of(TheftVector.VECTOR_07_DOUBLE_CLICK_COLLECT_ALL);
            }
            if (slotClassification.isPlayerBackpack() && context.targetItemVirtual()) {
                return Optional.of(TheftVector.VECTOR_25_DOUBLE_CLICK_SUCKING_VIRTUAL);
            }
        }
        if (ContainerClickContext.CLICK_TYPE_QUICK_CRAFT.equals(type) && slotClassification.isVirtual()) {
            if (context.button() == DRAG_BUTTON_SPLIT_SINGLE) {
                return Optional.of(TheftVector.VECTOR_09_DRAG_SPLIT_SINGLE);
            }
            if (context.button() == DRAG_BUTTON_CLONE_CREATIVE) {
                return Optional.of(TheftVector.VECTOR_10_DRAG_CLONE_CREATIVE);
            }
            return Optional.of(TheftVector.VECTOR_08_DRAG_SPLIT_EVENLY);
        }

        // 4. DISCONNECT & WINDOW CLOSE EXPLOITS
        if (context.windowClosing() && context.carriedItemVirtual()) {
            if (ACTION_DISCONNECT.equalsIgnoreCase(context.clickTypeName())) {
                return Optional.of(TheftVector.VECTOR_12_DISCONNECT_WITH_CURSOR);
            }
            if (ACTION_CRASH.equalsIgnoreCase(context.clickTypeName())) {
                return Optional.of(TheftVector.VECTOR_13_CRASH_PACKET_WITH_CURSOR);
            }
            if (ACTION_OUT_OF_ORDER_CLOSE.equalsIgnoreCase(context.clickTypeName())) {
                return Optional.of(TheftVector.VECTOR_14_OUT_OF_ORDER_CLOSE_DROP);
            }
            return Optional.of(TheftVector.VECTOR_11_ESC_CLOSE_CURSOR_DROP);
        }

        // 5. CLIENT DESYNC & CLIENT-MOD INJECTIONS
        if (ACTION_SCROLL_SWAP.equalsIgnoreCase(context.clickTypeName()) && slotClassification.isVirtual()) {
            return Optional.of(TheftVector.VECTOR_15_MOUSE_WHEEL_SCROLL_SWAP);
        }
        if (ContainerClickContext.CLICK_TYPE_CLONE.equals(type) && slotClassification.isVirtual()) {
            return Optional.of(TheftVector.VECTOR_18_MIDDLE_CLICK_CLONE);
        }
        if (context.slotIndex() < 0 && context.slotIndex() != ContainerClickContext.OUT_OF_BOUNDS_SLOT_ID && !context.windowClosing()) {
            return Optional.of(TheftVector.VECTOR_19_NEGATIVE_SLOT_PACKET_FLOOD);
        }
        if (context.containerSize() > 0 && context.slotIndex() >= context.containerSize()) {
            return Optional.of(TheftVector.VECTOR_20_SLOT_OVERRUN_WINDOW_SIZE);
        }
        if (ACTION_HIGH_LATENCY_DESYNC.equalsIgnoreCase(context.clickTypeName())) {
            return Optional.of(TheftVector.VECTOR_21_HIGH_LATENCY_DESYNC);
        }

        // 6. CROSS-SLOT TRANSACTION CONFLICTS
        if (slotClassification.isPlayerBackpack() && context.carriedItemVirtual()) {
            return Optional.of(TheftVector.VECTOR_22_CURSOR_SWAP_VIRTUAL_TO_BACKPACK);
        }
        if (slotClassification == VirtualSlotClassification.DECORATIVE_FRAME && !ContainerClickContext.ITEM_EMPTY.equals(context.carriedItemId())) {
            return Optional.of(TheftVector.VECTOR_23_PLACE_INTO_LOCKED_FRAME);
        }
        if (context.concurrentRaceDetected()) {
            return Optional.of(TheftVector.VECTOR_26_CONCURRENT_RACE_CONDITION);
        }
        if (ACTION_BUTTON_INJECTION.equalsIgnoreCase(context.clickTypeName())) {
            return Optional.of(TheftVector.VECTOR_27_BUTTON_PACKET_INJECTION);
        }
        if (context.bedrockClient() && ACTION_FORM_DESYNC.equalsIgnoreCase(context.clickTypeName())) {
            return Optional.of(TheftVector.VECTOR_28_GEYSER_FORM_DESYNC);
        }

        return Optional.empty();
    }
}
