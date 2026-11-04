package net.minecraftforge.items;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Universal multi-era standard implementation of {@link IItemHandlerModifiable}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ItemStackHandler implements IItemHandlerModifiable {

    protected final List<ItemStack> stacks;

    /**
     * Constructs a single-slot item handler.
     */
    public ItemStackHandler() {
        this(1);
    }

    /**
     * Constructs an item handler with the specified slot capacity.
     *
     * @param size slot capacity
     */
    public ItemStackHandler(int size) {
        int safeSize = Math.max(1, size);
        this.stacks = new ArrayList<>(safeSize);
        for (int i = 0; i < safeSize; i++) {
            this.stacks.add(ItemStack.EMPTY);
        }
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return stacks.get(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        stacks.set(slot, stack != null ? stack : ItemStack.EMPTY);
        onContentsChanged(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            if (!simulate) {
                stacks.set(slot, stack);
                onContentsChanged(slot);
            }
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        validateSlotIndex(slot);
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!simulate) {
            stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
        }
        return existing;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    protected void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            throw new IllegalArgumentException(
                    "Slot " + slot + " not in valid range - [0," + stacks.size() + ")");
        }
    }

    protected void onContentsChanged(int slot) {
    }
}
