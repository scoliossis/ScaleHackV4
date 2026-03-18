package com.scale.scalehackv4.utils.skyblock.SecretsHelper;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.Rotation;
import lombok.AllArgsConstructor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

@AllArgsConstructor
// maybe add walk/aote node? but pearl is probably enough
public class Node {
    public BlockPos startPos;
    public BlockPos targetPos;
    public Rotation rotation;
    public Type type;

    @AllArgsConstructor
    // todo: move these predicates, idk where yet.
    public enum Type {
        ETHERWARP(e -> {
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                if (EtherwarpUtil.isEtherwarpItem(C.p().getInventory().getStack(i))) {
                    C.p().getInventory().setSelectedSlot(i);
                    return true;
                }
            }

            return false;
        }),
        PEARL(e -> {
            for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                if (C.p().getInventory().getStack(i).getItem() != Items.ENDER_PEARL) continue;

                Text itemName = C.p().getInventory().getStack(i).getComponents().get(DataComponentTypes.CUSTOM_NAME);
                if (itemName != null && itemName.getString().contains("eap")) continue; // eap, as in spirit Leap/infinileap

                C.p().getInventory().setSelectedSlot(i);
                return true;
            }

            return false;
        }),
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
        }),
        SECRET(e -> true);

        public final Predicate<?> getItem;
    }
}
