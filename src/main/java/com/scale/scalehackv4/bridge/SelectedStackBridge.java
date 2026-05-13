package com.scale.scalehackv4.bridge;

import net.minecraft.item.ItemStack;

public interface SelectedStackBridge {
    static SelectedStackBridge from(Object instance) {
        return (SelectedStackBridge) instance;
    }

    void bridge$setSelectedStack(ItemStack stack);
    void bridge$setLastSelectedSlot(int slot);
    int bridge$getLastSelectedSlot();
}
