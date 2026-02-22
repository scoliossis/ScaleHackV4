package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.commands.CommandManager;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
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
import org.apache.commons.lang3.EnumUtils;

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
        if (nodePos == null) return true;

        String command = text.substring(0, text.indexOf(" ")).trim().toLowerCase();
        switch (command) {
            case "etherwarp" -> {
                Vec3i etherwarpPos = EtherwarpUtil.getEtherwarpBlockSuccess(C.p().getPitch(), C.p().getYaw(), C.p().getEntityPos(), 57);
                if (etherwarpPos == null) {
                    ChatUtil.prefixMessage("Etherwarp goes nowhere.");
                    return true;
                }

                ChatUtil.prefixMessage("Etherwarp successfully found!");
                room.nodes.add(new Node(nodePos, getNodePos(room, new BlockPos(etherwarpPos.getX(), etherwarpPos.getY(), etherwarpPos.getZ())), Node.Type.ETHERWARP));
            }
            case "stonk" -> {
                BlockHitResult blockHitResult = WorldUtil.raytrace(C.p().getEyePos(), C.p().getBlockInteractionRange(), RotationUtil.getClientRotation());
                if (blockHitResult == null || blockHitResult.getBlockPos() == null || blockHitResult.getType() == HitResult.Type.MISS) {
                    ChatUtil.prefixMessage("We are breaking NOTHING :speaking_head: :burger:");
                    return true;
                }

                ChatUtil.prefixMessage("Stonk success");
                room.nodes.add(new Node(nodePos, getNodePos(room, blockHitResult.getBlockPos()), Node.Type.DUNGEON_BREAKER));
            }
            case "remove" -> {
                // removes last node at pos
                for (int i = room.nodes.size()-1; i >= 0; i--) {
                    if (room.nodes.get(i).startPos.equals(nodePos)) {
                        room.nodes.remove(i);
                        break;
                    }
                }
            }
            default -> {
                if (!EnumUtils.isValidEnum(Node.Type.class, command.toUpperCase())) {
                    ChatUtil.prefixMessage("Invalid node type, try: " + Arrays.toString(Node.Type.values()));
                    return true;
                }

                Node.Type nodeType = Node.Type.valueOf(command.toUpperCase());

                ChatUtil.prefixMessage(nodeType.name() + " node successfully added!");
                room.nodes.add(new Node(nodePos, BlockPos.ORIGIN, nodeType));
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
            CommandManager.addCommand("stonk", range, pendingSuggestions);
            for (Node.Type type : Node.Type.values()) {
                if (type.isSecret) CommandManager.addCommand(type.name().toLowerCase(), range, pendingSuggestions);
            }
            CommandManager.addCommand("remove", range, pendingSuggestions);
        }
    }
}