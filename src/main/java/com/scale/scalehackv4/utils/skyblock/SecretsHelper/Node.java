package com.scale.scalehackv4.utils.skyblock.SecretsHelper;

import com.scale.scalehackv4.utils.C;
import lombok.AllArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

@AllArgsConstructor
public class Node {
    public BlockPos startPos;
    public BlockPos targetPos;
    public Type type;

    @AllArgsConstructor
    public enum Type {
        ETHERWARP(e -> {
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                if (EtherwarpUtil.isEtherwarpItem(C.p().getInventory().getStack(i))) {
                    C.p().getInventory().setSelectedSlot(i);
                    return true;
                }
            }

            return false;
        }, false),
        DUNGEON_BREAKER(e -> {
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                Text itemName = C.p().getInventory().getStack(i).getComponents().get(DataComponentTypes.CUSTOM_NAME);
                if (itemName == null) continue;

                if (itemName.getString().contains("Dungeonbreaker")) {
                    C.p().getInventory().setSelectedSlot(i);
                    return true;
                }
            }

            return false;
        }, false),
        CHEST(e -> true, true),
        WITHER_ESSENCE(e -> true, true),
        ITEM(e -> true, true),
        BAT(e -> true, true); // switch to hyp ig bro

        public final Predicate<?> getItem;
        public final boolean isSecret;
    }
}
