package dev.vessel.kernel.modelengine.driver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Suppresses default vanilla entity loot drops (e.g. raw porkchop from pigs, rotten flesh from zombies)
 * and generates configured custom item drops on mob death.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DriverDropSuppressor {

    private boolean suppressDefaultDrops = true;
    private final List<CustomDropItem> customDrops = new ArrayList<>();

    /**
     * Constructs a DriverDropSuppressor with default drop suppression enabled.
     */
    public DriverDropSuppressor() {
    }

    /**
     * Adds a custom drop item definition.
     *
     * @param customDrop custom drop item
     */
    public synchronized void addCustomDrop(CustomDropItem customDrop) {
        Objects.requireNonNull(customDrop, "customDrop cannot be null");
        this.customDrops.add(customDrop);
    }

    /**
     * Removes all configured custom drops.
     */
    public synchronized void clearCustomDrops() {
        this.customDrops.clear();
    }

    /**
     * Evaluates whether vanilla loot tables or item entity spawning should be cancelled.
     *
     * @return true if default drops are suppressed
     */
    public boolean shouldSuppressVanillaLoot() {
        return this.suppressDefaultDrops;
    }

    /**
     * Rolls all configured custom drops for a given looting enchantment level upon mob death.
     *
     * @param lootingLevel looting enchantment level (0 if none)
     * @return list of generated item stacks as [itemId, count] pairs or empty list
     */
    public synchronized List<DroppedItemEntry> generateCustomDrops(int lootingLevel) {
        if (customDrops.isEmpty()) {
            return Collections.emptyList();
        }
        List<DroppedItemEntry> results = new ArrayList<>();
        for (int i = 0; i < customDrops.size(); i++) {
            CustomDropItem drop = customDrops.get(i);
            int count = drop.rollDropCount(lootingLevel);
            if (count > 0) {
                results.add(new DroppedItemEntry(drop.getItemId(), count));
            }
        }
        return Collections.unmodifiableList(results);
    }

    public boolean isSuppressDefaultDrops() {
        return suppressDefaultDrops;
    }

    public void setSuppressDefaultDrops(boolean suppressDefaultDrops) {
        this.suppressDefaultDrops = suppressDefaultDrops;
    }

    public synchronized List<CustomDropItem> getCustomDrops() {
        return Collections.unmodifiableList(new ArrayList<>(customDrops));
    }
}
