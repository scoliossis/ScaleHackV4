package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;

import java.util.concurrent.CompletableFuture;

public class SaveRoomCommand extends Command {
    @Override
    public String name() {
        return "saveroom";
    }

    @Override
    public boolean handleCommand(String text) {
        if (text.isEmpty()) return false;

        int lastSpace = text.lastIndexOf(" ");
        if (lastSpace == -1) return false;

        String secretCount = text.substring(lastSpace).trim();
        String roomNameAndType = text.substring(0, lastSpace);

        int secondLastSpace = text.substring(0, lastSpace).lastIndexOf(" ");
        if (secondLastSpace == -1) return false;
        String roomType = roomNameAndType.substring(secondLastSpace).trim();
        String roomName = text.substring(0, secondLastSpace).trim();

        if (roomName.isEmpty() || !secretCount.matches("\\d+")) return false;

        for (Room.Type type : Room.Type.values()) {
            if (type.name().equalsIgnoreCase(roomType)) {
                Room currentRoom = AreaUtil.getCurrentRoom();
                if (currentRoom == null) return false;

                currentRoom.createRoom(roomName, type, Integer.parseInt(secretCount));
                ChatUtil.chat(String.format("Saved room %s as type %s with %s secrets", roomName, roomType, secretCount));

                return true;
            }
        }

        return false;
    }

    @Override
    public void handleTab(String text, int pos, CompletableFuture<Suggestions> pendingSuggestions) {
        // todo ig
    }
}