package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.commands.CommandManager;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.Node;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.NodeUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.Arrays;
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
        Rotation rotation = NodeUtil.flipRotation(RotationUtil.getClientRotation(), room.facing, Direction.NORTH);
        if (nodePos == null) return true;

        String command = text.substring(0, text.indexOf(" ")).trim().toLowerCase();
        String extraInfo = text.substring(text.indexOf(" ") + 1).trim();
        int extraInt = extraInfo.matches("\\d+") ? Integer.parseInt(extraInfo) : 0;

        switch (command) {
            case "etherwarp" -> {
                Vec3i etherwarpPos = EtherwarpUtil.getEtherwarpBlockSuccess(C.p().getPitch(), C.p().getYaw(), C.p().getEntityPos(), 57);
                if (etherwarpPos == null) {
                    ChatUtil.prefixMessage("Etherwarp goes nowhere.");
                    return true;
                }

                ChatUtil.prefixMessage("Etherwarp successfully found!");
                room.nodes.add(new Node(nodePos, getNodePos(room, new BlockPos(etherwarpPos)), rotation, Node.Type.ETHERWARP));
            }
            case "pearl" -> {
                ChatUtil.prefixMessage("Pearl spot added" + (extraInt == 0 ? "!" : " with a second pearl sent after " + extraInt + " ticks!"));
                // todo: calculate pearl trajectory
                room.nodes.add(new Node(nodePos, new BlockPos(extraInt, 0, 0), rotation, Node.Type.PEARL));
            }
            case "stonk" -> {
                BlockHitResult blockHitResult = WorldUtil.raytrace(C.p().getEyePos(), C.p().getBlockInteractionRange(), RotationUtil.getClientRotation());
                if (blockHitResult == null || blockHitResult.getBlockPos() == null || blockHitResult.getType() == HitResult.Type.MISS) {
                    ChatUtil.prefixMessage("We are breaking NOTHING :speaking_head: :burger:");
                    return true;
                }

                ChatUtil.prefixMessage("Stonk success");
                room.nodes.add(new Node(nodePos, getNodePos(room, blockHitResult.getBlockPos()), rotation, Node.Type.DUNGEON_BREAKER));
            }
            case "remove" -> {
                // removes last node at pos
                for (int i = room.nodes.size()-1; i >= 0; i--) {
                    if (room.nodes.get(i).startPos.equals(nodePos)) {
                        ChatUtil.prefixMessage("Removed node of type " + room.nodes.get(i).type.name() + " successfully!");
                        room.nodes.remove(i);
                        break;
                    }

                    if (i == 0) {
                        ChatUtil.prefixMessage("No node found at " + nodePos);
                    }
                }
            }
            case "secret" -> {
                int secretAmount = Math.min(1, extraInt);

                ChatUtil.prefixMessage(secretAmount + " secret await node(s) successfully added!");
                room.nodes.add(new Node(nodePos, new BlockPos(secretAmount, 0, 0), rotation, Node.Type.SECRET));
            }
            default -> {
                ChatUtil.prefixMessage("Invalid node type, try: " + Arrays.toString(Node.Type.values()));
            }
        }

        room.saveRoom();

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
            CommandManager.addCommand("etherwarp", range, pendingSuggestions);
            CommandManager.addCommand("pearl", range, pendingSuggestions);
            CommandManager.addCommand("stonk", range, pendingSuggestions);
            CommandManager.addCommand("secret", range, pendingSuggestions);
            CommandManager.addCommand("remove", range, pendingSuggestions);
        }
    }
}