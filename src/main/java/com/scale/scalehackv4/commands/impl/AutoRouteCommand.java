package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.commands.CommandManager;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.NodeUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.CompletableFuture;

public class AutoRouteCommand extends Command {
    @Override
    public String name() {
        return "route";
    }

    @Override
    public boolean handleCommand(String text) {
        if (text.isEmpty()) return false;

        // adds the space just in case!
        text += " ";

        Room room = AreaUtil.getCurrentRoom();
        if (room == null) {
            ChatUtil.prefixMessage("Not in a valid dungeon room!");
            return true;
        }
        BlockPos playerPos = C.p().getBlockPos().offset(Direction.DOWN, 1);
        BlockPos nodePos = getNodePos(room, playerPos);
        if (nodePos == null) return true;

        switch (text.substring(0, text.indexOf(" ")).trim().toLowerCase()) {
            case "add" -> {
                room.nodes.add(nodePos);
                room.saveRoom();
                ChatUtil.prefixMessage(String.format("Added node at %s", playerPos));
            }
            case "remove" -> {
                if (room.nodes.remove(nodePos)) {
                    room.saveRoom();
                    ChatUtil.prefixMessage(String.format("Removed node at %s", playerPos));
                }
                else {
                    ChatUtil.prefixMessage(String.format("No node exists at %s", playerPos));
                }
            }
        }

        return true;
    }

    private static BlockPos getNodePos(Room room, BlockPos pos) {
        BlockPos roomCorner = NodeUtil.getRoomDefaultCorner(room);
        BlockPos relativePos = pos.subtract(roomCorner);

        return NodeUtil.rotateNode(relativePos, room.facing, Direction.NORTH);
    }

    @Override
    public void handleTab(String text, int pos, CompletableFuture<Suggestions> pendingSuggestions) {
        if (!text.contains(" ")) {
            StringRange range = StringRange.between(pos, pos+text.length());
            CommandManager.addCommand("add", range, pendingSuggestions);
            CommandManager.addCommand("remove", range, pendingSuggestions);
        }
    }
}