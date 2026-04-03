package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.NodeUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.CompletableFuture;

public class AuraBlacklist extends Command {
    @Override
    public String name() {
        return "aurablacklist";
    }

    @Override
    public boolean handleCommand(String text) {
        Room room = AreaUtil.getCurrentRoom();
        if (room == null) return false;

        BlockPos nodePos = NodeUtil.reverseNodePos(C.p().getBlockPos().offset(Direction.DOWN, 1), room);
        if (text.contains("remove")) {
            room.secretAuraBlacklist.remove(nodePos);
            ChatUtil.chat("Removed " + nodePos + " from the aura blacklist.");
        }
        else {
            room.secretAuraBlacklist.add(nodePos);
            ChatUtil.chat("Added " + nodePos + " to the aura blacklist.");
        }


        room.saveRoom();

        return true;
    }

    @Override
    public void handleTab(String text, int pos, CompletableFuture<Suggestions> pendingSuggestions) {
        // todo ig
    }
}