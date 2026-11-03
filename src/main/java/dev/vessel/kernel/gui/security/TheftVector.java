package dev.vessel.kernel.gui.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of all 28 Virtual GUI item theft and duplication exploit vectors
 * identified across Minecraft server loaders and cheat clients.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum TheftVector {

    // 1. KEYBOARD SWAP EXPLOITS
    VECTOR_01_HOTBAR_SWAP(1, "Hotbar Key 1..9 Swap"),
    VECTOR_02_OFFHAND_SWAP(2, "Offhand Key F Swap"),

    // 2. DROP & THROW EXPLOITS
    VECTOR_03_THROW_SINGLE_KEY(3, "Throw Single Item Key Q"),
    VECTOR_04_THROW_STACK_KEY(4, "Throw Entire Stack Ctrl+Q"),
    VECTOR_05_OUT_OF_BOUNDS_BORDER_DROP(5, "Out-of-Bounds Border Drop"),

    // 3. RAPID BATCH & DRAG EXPLOITS
    VECTOR_06_SHIFT_QUICK_MOVE(6, "Shift-Click Quick Move"),
    VECTOR_07_DOUBLE_CLICK_COLLECT_ALL(7, "Double-Click Collect All"),
    VECTOR_08_DRAG_SPLIT_EVENLY(8, "Mouse Drag-Split Evenly"),
    VECTOR_09_DRAG_SPLIT_SINGLE(9, "Mouse Drag-Split Single"),
    VECTOR_10_DRAG_CLONE_CREATIVE(10, "Mouse Drag Clone"),

    // 4. DISCONNECT & WINDOW CLOSE EXPLOITS
    VECTOR_11_ESC_CLOSE_CURSOR_DROP(11, "Esc/Close with Item on Cursor"),
    VECTOR_12_DISCONNECT_WITH_CURSOR(12, "Network Disconnect with Item on Cursor"),
    VECTOR_13_CRASH_PACKET_WITH_CURSOR(13, "Instant Client Crash Packet with Carried Item"),
    VECTOR_14_OUT_OF_ORDER_CLOSE_DROP(14, "ServerboundContainerClosePacket out-of-order drop"),

    // 5. CLIENT DESYNC & CLIENT-MOD INJECTIONS
    VECTOR_15_MOUSE_WHEEL_SCROLL_SWAP(15, "Mouse Wheel Scroll Swap"),
    VECTOR_16_SHIFT_RIGHT_CLICK_BURST(16, "Shift + Right-Click Auto Sorting Burst"),
    VECTOR_17_FAST_SORT_INJECTION(17, "Inventory Profiles Next Fast Sort Injection"),
    VECTOR_18_MIDDLE_CLICK_CLONE(18, "Middle-Click Clone"),
    VECTOR_19_NEGATIVE_SLOT_PACKET_FLOOD(19, "Negative Slot ID Packet Flood"),
    VECTOR_20_SLOT_OVERRUN_WINDOW_SIZE(20, "Slot Index Greater Than Window Size Overrun"),
    VECTOR_21_HIGH_LATENCY_DESYNC(21, "High Latency Packet Reordering Desync"),

    // 6. CROSS-SLOT TRANSACTION CONFLICTS
    VECTOR_22_CURSOR_SWAP_VIRTUAL_TO_BACKPACK(22, "Swap Cursor Between Virtual Slot & Player Backpack"),
    VECTOR_23_PLACE_INTO_LOCKED_FRAME(23, "Place Real Mod Item Into Locked Virtual Frame Slot"),
    VECTOR_24_SHIFT_INTO_VIRTUAL_BUTTON(24, "Shift-Click From Backpack Into Virtual Action Button"),
    VECTOR_25_DOUBLE_CLICK_SUCKING_VIRTUAL(25, "Double-Click In Backpack Sucking Virtual Frame Items"),
    VECTOR_26_CONCURRENT_RACE_CONDITION(26, "Concurrent Transaction Race Condition"),
    VECTOR_27_BUTTON_PACKET_INJECTION(27, "Container Button Packet Injection"),
    VECTOR_28_GEYSER_FORM_DESYNC(28, "Geyser/Bedrock Form Transaction Desync");

    private final int vectorId;
    private final String description;

    TheftVector(int vectorId, String description) {
        this.vectorId = vectorId;
        this.description = description;
    }

    public int getVectorId() {
        return vectorId;
    }

    public String getDescription() {
        return description;
    }

    private static final Map<Integer, TheftVector> BY_ID;

    static {
        Map<Integer, TheftVector> map = new HashMap<>();
        for (TheftVector v : values()) {
            map.put(v.vectorId, v);
        }
        BY_ID = Collections.unmodifiableMap(map);
    }

    public static TheftVector fromId(int id) {
        TheftVector vector = BY_ID.get(id);
        if (vector == null) {
            throw new IllegalArgumentException("Unknown theft vector ID: " + id);
        }
        return vector;
    }
}
