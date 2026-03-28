package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import lombok.AllArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.function.Predicate;

@AllArgsConstructor
public enum SkyblockItems {
    ETHERWARP(EtherwarpUtil::isEtherwarpItem),
    PEARL(itemStack -> {
        if (itemStack.getItem() != Items.ENDER_PEARL) return false;

        Text itemName = itemStack.getComponents().get(DataComponentTypes.CUSTOM_NAME);
        // eap, as in spirit Leap/infinileap
        return itemName == null || !itemName.getString().contains("eap");
    }),
    DUNGEON_BREAKER(itemStack -> {
        Text itemName = itemStack.getComponents().get(DataComponentTypes.CUSTOM_NAME);
        if (itemName == null) return false;

        return itemName.getString().contains("Dungeonbreaker");
    });

    public final Predicate<ItemStack> isItem;

    public int getItemStack() {
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            if (isItem.test(C.p().getInventory().getStack(i))) return i;
        }
        return -1;
    }

    public boolean switchToItem() {
        int itemStack = getItemStack();
        if (itemStack == -1) return false;

        C.p().getInventory().setSelectedSlot(itemStack);
        return true;
    }
}
