package com.jesz.createdieselgenerators.content.bulk_fermenter;

import com.zurrtum.create.infrastructure.items.ItemInventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * PORT-NOTE(26.1): a stable item container handed to Fabric's transfer API; the controller's combined inventory is
 * swapped in behind it (was an IItemHandlerModifiable wrapper). Without packager mode, an item type only starts a new
 * slot when no slot holds it yet (upstream refused to stack into a new slot once the existing stack was full).
 */
public class BulkFermenterInventoryWrapper implements ItemInventory {
    private Container itemHandler = new SimpleContainer(0);
    private BulkFermenterBlockEntity owner;

    public void setItemHandler(Container itemHandler, BulkFermenterBlockEntity owner) {
        this.itemHandler = itemHandler;
        this.owner = owner;
    }

    public Container getItemHandler() {
        return itemHandler;
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getContainerSize();
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setItem(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        ItemStack existing = getItem(slot);
        if (!existing.isEmpty())
            return ItemStack.isSameItemSameComponents(existing, stack);
        if (owner != null && owner.packagerMode)
            return true;
        for (int i = 0; i < getContainerSize(); i++) {
            if (i != slot && ItemStack.isSameItemSameComponents(getItem(i), stack))
                return false;
        }
        return true;
    }

    @Override
    public void setChanged() {
        itemHandler.setChanged();
    }
}
