package dev.vessel.kernel.gui.security;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe delegate interfacing with modded machinery slots (e.g. Create millstone, Mekanism crusher).
 * Balances strict anti-theft locking with essential gameplay automation: permits valid ingredient
 * insertions into input slots and product extractions from output slots while strictly forbidding
 * foreign item insertions into output slots.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FunctionalModSlotDelegate {

    private final ConcurrentHashMap<String, Set<String>> machineAcceptedIngredients = new ConcurrentHashMap<>();

    /**
     * Registers accepted ingredient item IDs for a specific modded machine identifier.
     *
     * @param machineIdentifier canonical machine identifier (e.g. "create:millstone")
     * @param acceptedItemIds set of acceptable ingredient item IDs
     */
    public void registerMachineIngredients(String machineIdentifier, Set<String> acceptedItemIds) {
        Objects.requireNonNull(machineIdentifier, "machineIdentifier cannot be null");
        Objects.requireNonNull(acceptedItemIds, "acceptedItemIds cannot be null");
        machineAcceptedIngredients.put(machineIdentifier, Set.copyOf(acceptedItemIds));
    }

    /**
     * Evaluates whether an item may be placed into the target slot.
     *
     * @param slotType target slot classification
     * @param itemId canonical item identifier to insert
     * @param isVirtualItem whether item carries virtual marker
     * @return true if placement is permitted
     */
    public boolean mayPlace(VirtualSlotClassification slotType, String itemId, boolean isVirtualItem) {
        Objects.requireNonNull(slotType, "slotType cannot be null");
        Objects.requireNonNull(itemId, "itemId cannot be null");

        // Never allow placing virtual items into any slot
        if (isVirtualItem) {
            return false;
        }

        // Virtual slots never accept item placement
        if (slotType.isVirtual()) {
            return false;
        }

        // Functional outputs strictly reject all insertions
        if (slotType == VirtualSlotClassification.FUNCTIONAL_MOD_OUTPUT) {
            return false;
        }

        // Empty items represent no placement
        if (ContainerClickContext.ITEM_EMPTY.equals(itemId)) {
            return true;
        }

        // Inputs and mod storages accept valid real items
        return slotType == VirtualSlotClassification.FUNCTIONAL_MOD_INPUT
                || slotType == VirtualSlotClassification.FUNCTIONAL_MOD_STORAGE
                || slotType == VirtualSlotClassification.REAL_PLAYER_BACKPACK;
    }

    /**
     * Evaluates whether an item may be placed into a specific registered machine input slot.
     *
     * @param machineIdentifier canonical machine identifier
     * @param slotType target slot classification
     * @param itemId canonical item identifier to insert
     * @param isVirtualItem whether item carries virtual marker
     * @return true if placement is permitted
     */
    public boolean mayPlaceInMachine(
            String machineIdentifier,
            VirtualSlotClassification slotType,
            String itemId,
            boolean isVirtualItem
    ) {
        if (!mayPlace(slotType, itemId, isVirtualItem)) {
            return false;
        }

        if (slotType == VirtualSlotClassification.FUNCTIONAL_MOD_INPUT && machineIdentifier != null) {
            Set<String> accepted = machineAcceptedIngredients.get(machineIdentifier);
            if (accepted != null && !accepted.contains(itemId)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Evaluates whether an item may be picked up or extracted from the target slot.
     *
     * @param slotType target slot classification
     * @param itemId canonical item identifier sitting in slot
     * @return true if extraction is permitted
     */
    public boolean mayPickup(VirtualSlotClassification slotType, String itemId) {
        Objects.requireNonNull(slotType, "slotType cannot be null");
        Objects.requireNonNull(itemId, "itemId cannot be null");

        // Virtual slots never allow pickup
        if (slotType.isVirtual()) {
            return false;
        }

        // Empty slots have nothing to pick up
        if (ContainerClickContext.ITEM_EMPTY.equals(itemId)) {
            return false;
        }

        // Mod outputs, inputs, and storages permit extraction of real items
        return slotType.isModFunctional() || slotType.isPlayerBackpack();
    }
}
