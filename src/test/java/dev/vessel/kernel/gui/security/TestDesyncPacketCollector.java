package dev.vessel.kernel.gui.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Top-level test collector implementing DesyncPacketReceiver to avoid any anonymous or inner classes.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestDesyncPacketCollector implements DesyncPacketReceiver {

    private final List<TestSlotFlushRecord> slotFlushes = new ArrayList<>();
    private final List<TestCarriedFlushRecord> carriedFlushes = new ArrayList<>();

    @Override
    public void onSlotFlush(String playerId, int windowId, int slotIndex, String itemId, int count) {
        slotFlushes.add(new TestSlotFlushRecord(playerId, windowId, slotIndex, itemId, count));
    }

    @Override
    public void onCarriedFlush(String playerId, int windowId, String carriedItemId, int count) {
        carriedFlushes.add(new TestCarriedFlushRecord(playerId, windowId, carriedItemId, count));
    }

    public List<TestSlotFlushRecord> getSlotFlushes() {
        return Collections.unmodifiableList(slotFlushes);
    }

    public List<TestCarriedFlushRecord> getCarriedFlushes() {
        return Collections.unmodifiableList(carriedFlushes);
    }

    public void clear() {
        slotFlushes.clear();
        carriedFlushes.clear();
    }
}
