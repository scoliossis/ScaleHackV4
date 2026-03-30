package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import lombok.AllArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.function.Predicate;

@AllArgsConstructor
public enum SkyblockItems {
    ETHERWARP(EtherwarpUtil::isEtherwarpItem),
    HYPERION(itemStack -> {
        if (itemStack.getItem() != Items.IRON_SWORD) return false;
        LoreComponent lore = itemStack.getComponents().get(DataComponentTypes.LORE);
        if (lore == null) return false;

        for (Text text : lore.lines()) {
            if (text.getString().contains("Wither Impact")) return true;
        }

        return false;
    }),
    PEARL(itemStack -> {
        if (itemStack.getItem() != Items.ENDER_PEARL) return false;

        Text itemName = itemStack.getComponents().get(DataComponentTypes.CUSTOM_NAME);
        // eap, as in spirit Leap/infinileap
        return itemName == null || !itemName.getString().contains("eap");
    }),
    SUPERBOOM(itemStack -> {
        if (itemStack.getItem() != Items.TNT) return false;

        Text itemName = itemStack.getComponents().get(DataComponentTypes.CUSTOM_NAME);
        // idealy they should have superboom, but infiniboom is probably fine sometimes
        return itemName == null || !itemName.getString().contains("boom TNT");
    }),
    SPIRIT_SCEPTRE(itemStack -> {
        if (itemStack.getItem() != Items.ALLIUM) return false;

        Text itemName = itemStack.getComponents().get(DataComponentTypes.CUSTOM_NAME);
        return itemName != null && itemName.getString().contains("Spirit Sceptre");
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
