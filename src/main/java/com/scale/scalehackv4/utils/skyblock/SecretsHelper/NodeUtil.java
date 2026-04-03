package com.scale.scalehackv4.utils.skyblock.SecretsHelper;

import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class NodeUtil {
    public static BlockPos getNodePos(BlockPos node, Room room) {
        BlockPos corner = getRoomDefaultCorner(room);
        BlockPos nodeRotated = rotateNode(node, Direction.NORTH, room.facing);
        return corner.add(nodeRotated.getX(), nodeRotated.getY(), nodeRotated.getZ());
    }

    public static BlockPos reverseNodePos(BlockPos pos, Room room) {
        BlockPos roomCorner = NodeUtil.getRoomDefaultCorner(room);
        BlockPos relativePos = pos.subtract(roomCorner);

        return NodeUtil.rotateNode(relativePos, room.facing, Direction.NORTH);
    }


    public static BlockPos getRoomDefaultCorner(Room room) {
        return DungeonScanner.getChunkCenter(room.squares[room.lowestHashIndex]);
    }

    public static BlockPos rotateNode(BlockPos node, Direction from, Direction to) {
        // tuff name
        int horizontalQuarterTurns = Math.floorMod(to.getHorizontalQuarterTurns() - from.getHorizontalQuarterTurns(), 4);

        // java 17 or wtv is so fancy i love this
        return switch (horizontalQuarterTurns) {
            case 1 -> new BlockPos(-node.getZ(), node.getY(), node.getX());
            case 2 -> new BlockPos(-node.getX(), node.getY(), -node.getZ());
            case 3 -> new BlockPos(node.getZ(), node.getY(), -node.getX());
            default -> node;
        };
    }

    public static Rotation flipRotation(Rotation rotation, Direction from, Direction to) {
        // tuff name
        int horizontalQuarterTurns = Math.floorMod(to.getHorizontalQuarterTurns() - from.getHorizontalQuarterTurns(), 4);

        // java 17 or wtv is so fancy i love this
        return switch (horizontalQuarterTurns) {
            case 1 -> rotation.add(new Rotation(90, 0));
            case 2 -> rotation.add(new Rotation(180, 0));
            case 3 -> rotation.add(new Rotation(270, 0));
            default -> rotation;
        };
    }
}
