package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.SelectedStackBridge;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayerInteractionManager.class)
public class SelectedStackBridgeMixin implements SelectedStackBridge {
    @Shadow
    private ItemStack selectedStack;

    @Shadow
    private int lastSelectedSlot;

    @Override
    public void bridge$setSelectedStack(ItemStack stack) {
        this.selectedStack = stack;
    }

    @Override
    public void bridge$setLastSelectedSlot(int slot) {
        this.lastSelectedSlot = slot;
    }

    @Override
    public int bridge$getLastSelectedSlot() {
        return this.lastSelectedSlot;
    }
}
