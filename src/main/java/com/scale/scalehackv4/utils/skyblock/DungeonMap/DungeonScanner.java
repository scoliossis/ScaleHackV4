package com.scale.scalehackv4.utils.skyblock.DungeonMap;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.PacketEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.skyblock.FinishedDungeonScanningEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import lombok.AllArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

@RegisterListeners
public class DungeonScanner {
    public record MapDivision(Direction.Axis axis, DoorType doorType) {}
    @AllArgsConstructor
    public enum DoorType {
        WALL(new Color(255, 255, 255)),
        FREE(new Color(169, 104, 5)),
        WITHER(new Color(113, 0, 213)),
        BLOOD(new Color(255, 0, 0)),;

        public final Color colour;
    }

    private static final HashMap<Integer, HashMap<Integer, Boolean>> UNSCANNED_CHUNKS = new HashMap<>();
    public static final HashMap<Integer, HashMap<Integer, MapDivision>> MAP_DIVISIONS = new HashMap<>();

    public static final ArrayList<Room> ROOMS = new ArrayList<>();
    private static final HashMap<Integer, HashMap<Integer, Boolean>> UNASSIGNED_CHUNKS = new HashMap<>();

    public static final int MAX_DUNGEON_DIAMETER = 190;
    public static Vector2f CURRENT_DUNGEON_DIAMETER = new Vector2f(MAX_DUNGEON_DIAMETER, MAX_DUNGEON_DIAMETER);
    protected static final int MAX_DUNGEON_CHUNKS = Math.ceilDiv(MAX_DUNGEON_DIAMETER, 16);
    public static final int DUNGEON_BLOCKS_OFFSET = 10;
    protected static final int DUNGEON_CHUNK_OFFSET = Math.ceilDiv(DUNGEON_BLOCKS_OFFSET, 16);

    private static boolean wasInDungeon = false;
    private static boolean finishedScanning = false;

    // infrequently posted event
    private final Event playerUpdateEvent = new PlayerUpdateEvent(null, params -> {
        if (!AreaUtil.isInDungeons()) {
            if (wasInDungeon) onLeaveDungeon();
            return false;
        }
        else if (!wasInDungeon) {
            wasInDungeon = true;
            onEnterDungeon();
        }

        // once a second is enough, its fast as is tho tbf
        if (C.p().age % 20 == 0) {
            if (!finishedScanning) {
                getSplits();
                registerRooms();
            }
            MapParser.parseMap(); // relatively slow process, maybe move to a separate thread
            setCurrentDungeonDiameter();

            if (!finishedScanning && finishedScanning()) {
                finishedScanning = !Bus.post(FinishedDungeonScanningEvent.class, null);
            }
        }

        return false;
    });


    private final Event updateSecretCount = new PacketEvent.Received(null, event -> {
        if (!(event.packet instanceof GameMessageS2CPacket packet)) return false;

        String text = Formatting.strip(packet.content().getString());
        if (!text.contains("Secrets")) return false;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return false;

        String split = text.split("Secrets")[0].trim();
        String secretCount = split.substring(split.lastIndexOf(" ") + 1).split("/")[0];

        currentRoom.secretsFound = Integer.parseInt(secretCount);

        return false;
    });

    private static void setCurrentDungeonDiameter() {
        Vector2f dungeonRect = getDungeonRect();
        CURRENT_DUNGEON_DIAMETER = new Vector2f(dungeonRect.x * 32 - 2, dungeonRect.y * 32 - 2);
    }

    protected static Vector2f getDungeonRect() {
        // yeah there are better ways to do this, eg. scanning player pos on the map, get percent and compare to percent of max
        Vector2f highestValue = new Vector2f(Integer.MIN_VALUE, Integer.MIN_VALUE);
        for (int x : MAP_DIVISIONS.keySet()) {
            for (int z : MAP_DIVISIONS.get(x).keySet()) {
                DungeonScanner.MapDivision mapDivision = MAP_DIVISIONS.get(x).get(z);
                if (mapDivision.doorType == DoorType.WALL) continue;
                int x2 = mapDivision.axis == Direction.Axis.X ? x+1 : x;
                int z2 = mapDivision.axis == Direction.Axis.Z ? z+1 : z;

                highestValue.x = Math.max(highestValue.x, x2);
                highestValue.y = Math.max(highestValue.y, z2);
            }
        }

        return new Vector2f(getRoomAmountFromChunks(highestValue.x), getRoomAmountFromChunks(highestValue.y));
    }

    private static float getRoomAmountFromChunks(float chunks) {
        return ((MAX_DUNGEON_CHUNKS - Math.abs(chunks)) / 2) + 1;
    }

    private static void onEnterDungeon() {
        for (int x = -MAX_DUNGEON_CHUNKS; x < -DUNGEON_CHUNK_OFFSET; x++) {
            for (int z = -MAX_DUNGEON_CHUNKS; z < -DUNGEON_CHUNK_OFFSET; z++) {
                UNSCANNED_CHUNKS.computeIfAbsent(x, k -> new HashMap<>()).put(z, true);
                UNASSIGNED_CHUNKS.computeIfAbsent(x, k -> new HashMap<>()).put(z, true);
            }
        }
    }

    public static ArrayList<Room> getConnectedRooms(int x, int z) {
        ArrayList<Room> connectedRooms = new ArrayList<>();

        for (Room room : ROOMS) {
            if (connectedRooms.size() == 2) continue;

            for (ChunkPos chunkPos : room.squares) {
                if (chunkPos.x == x-1 && chunkPos.z == z
                        || chunkPos.x == x+1 && chunkPos.z == z
                        || chunkPos.x == x && chunkPos.z == z+1
                        || chunkPos.x == x && chunkPos.z == z-1
                ) {
                    connectedRooms.add(room);
                    break;
                }
            }
        }

        return connectedRooms;
    }

    private static boolean finishedScanning() {
        if (ROOMS.isEmpty() || MAP_DIVISIONS.isEmpty()) return false;

        for (int x : MAP_DIVISIONS.keySet()) {
            for (int z : MAP_DIVISIONS.get(x).keySet()) {
                if (MAP_DIVISIONS.get(x).get(z).doorType == DoorType.WALL) continue;

                if (getConnectedRooms(x, z).size() < 2) return false;
            }
        }
        return true;
    }

    private static void getSplits() {
        int[] xValues = UNSCANNED_CHUNKS.keySet().stream().mapToInt(Integer::intValue).toArray();

        for (int x : xValues) {
            int[] zValues = UNSCANNED_CHUNKS.get(x).keySet().stream().mapToInt(Integer::intValue).toArray();

            for (int z : zValues) {
                if (checkSplit(new ChunkPos(x, z))) {
                    UNSCANNED_CHUNKS.get(x).remove(z);
                    if (UNSCANNED_CHUNKS.get(x).isEmpty()) UNSCANNED_CHUNKS.remove(x);
                }
            }
        }
    }

    // checks if the centre of the chunk is a split in the map (a door)
    private static boolean checkSplit(ChunkPos chunkPos) {
        BlockPos chunkCenter = getChunkCenter(chunkPos);
        BlockPos aboveDoorCenter = chunkCenter.offset(Direction.UP, 82);

        Block centreBlock = C.w().getBlockState(aboveDoorCenter).getBlock();
        if (WorldUtil.isChunkUnloaded(chunkPos)) return false;

        boolean isStartDoor = C.w().getBlockState(chunkCenter.offset(Direction.UP, 69)).getBlock() == Blocks.INFESTED_CHISELED_STONE_BRICKS;

        if (centreBlock == Blocks.AIR || isStartDoor) {
            boolean door = C.w().getBlockState(chunkCenter.offset(Direction.UP, 66)).getBlock() == Blocks.BEDROCK;
            BlockHitResult raycast = C.w().raycast(new RaycastContext(
                    // starts at 82 because 4 secret trap has protrusions at y81
                    new Vec3d(chunkCenter.getX(), door ? 82 : 159, chunkCenter.getZ()),
                    new Vec3d(chunkCenter.getX(), door ? 159 : 0, chunkCenter.getZ()),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    ShapeContext.absent()
            ));

            // check that there are no blocks in the line other than the door
            if (raycast.getType() != HitResult.Type.MISS && !isStartDoor) return true;

            boolean north = C.w().getBlockState(aboveDoorCenter.north()).getBlock() == Blocks.AIR
                    || (C.w().getBlockState(aboveDoorCenter.north()).getBlock() == Blocks.INFESTED_CHISELED_STONE_BRICKS && isStartDoor);

            DoorType doorType = isStartDoor ? DoorType.FREE : door ? getDoorType(chunkCenter) : DoorType.WALL;

            MAP_DIVISIONS.computeIfAbsent(chunkPos.x, k -> new HashMap<>())
                    .put(chunkPos.z, new MapDivision(north ? Direction.Axis.X : Direction.Axis.Z, doorType));

            assignChunk(chunkPos);
        }

        return true;
    }

    private static DoorType getDoorType(BlockPos pos) {
        Block block = C.w().getBlockState(pos.offset(Direction.UP, 69)).getBlock();
        if (block == Blocks.AIR) return DoorType.FREE;
        if (block == Blocks.COAL_BLOCK) return DoorType.WITHER;
        if (block == Blocks.RED_TERRACOTTA) return DoorType.BLOOD;
        return DoorType.WALL;
    }

    private static void assignChunk(ChunkPos chunkPos) {
        HashMap<Integer, Boolean> zChunks = UNASSIGNED_CHUNKS.get(chunkPos.x);
        if (zChunks == null) return;

        UNASSIGNED_CHUNKS.get(chunkPos.x).remove(chunkPos.z);

        if (UNASSIGNED_CHUNKS.get(chunkPos.x).isEmpty()) {
            UNASSIGNED_CHUNKS.remove(chunkPos.x);
        }
    }

    private static void registerRooms() {
        // make an array to stop concurrent modification exceptions
        for (int x : UNASSIGNED_CHUNKS.keySet().toArray(Integer[]::new)) {
            HashMap<Integer, Boolean> zChunks = UNASSIGNED_CHUNKS.get(x);

            // the hashmap is modified by assignChunk, causing there to be a loop of an x that is already assigned
            if (zChunks == null) continue;

            for (int z : zChunks.keySet().toArray(Integer[]::new)) {
                ChunkPos chunkPos = new ChunkPos(x, z);
                // dont try and scan a chunk if its already scanned, or cant be scanned
                if (isChunkScanned(chunkPos) || WorldUtil.isChunkUnloaded(chunkPos)) continue;

                Room room = getRoom(chunkPos);
                if (room == null) continue;

                ROOMS.add(room);
                for (ChunkPos square : room.squares) assignChunk(square);
            }
        }
    }

    private static Room getRoom(ChunkPos chunkPos) {
        ArrayList<ChunkPos> connectedSquares = addSquare(chunkPos, new ArrayList<>());

        return connectedSquares == null ? null : new Room(connectedSquares.toArray(ChunkPos[]::new));
    }

    // recursive function, flood fills to find connected squares
    private static ArrayList<ChunkPos> addSquare(ChunkPos chunkPos, ArrayList<ChunkPos> connectedSquares) {
        // if one of the chunks isnt loaded, we return null, cause we cant scan a room thats half loaded.
        if (connectedSquares == null) return null;
        if (isWall(chunkPos) || isChunkOutOfBounds(chunkPos) || connectedSquares.contains(chunkPos)) return connectedSquares;
        // this is below to allow edges to load even if the corner chunk isnt loaded
        if (WorldUtil.isChunkUnloaded(chunkPos)) return null;

        connectedSquares.add(chunkPos);

        // flood fill wowowowow
        connectedSquares = addSquare(new ChunkPos(chunkPos.x+1, chunkPos.z), connectedSquares);
        connectedSquares = addSquare(new ChunkPos(chunkPos.x-1, chunkPos.z), connectedSquares);

        connectedSquares = addSquare(new ChunkPos(chunkPos.x, chunkPos.z+1), connectedSquares);
        connectedSquares = addSquare(new ChunkPos(chunkPos.x, chunkPos.z-1), connectedSquares);

        return connectedSquares;
    }

    private static boolean isWall(ChunkPos chunkPos) {
        return MAP_DIVISIONS.getOrDefault(chunkPos.x, new HashMap<>()).getOrDefault(chunkPos.z, null) != null;
    }

    private static boolean isChunkScanned(ChunkPos chunkPos) {
        return isWall(chunkPos) || ROOMS.stream().anyMatch(e -> Arrays.stream(e.squares).anyMatch(cp -> cp.x == chunkPos.x && cp.z == chunkPos.z));
    }

    private static boolean isChunkOutOfBounds(ChunkPos chunkPos) {
        return (chunkPos.x < -MAX_DUNGEON_CHUNKS || chunkPos.z < -MAX_DUNGEON_CHUNKS)
                || (chunkPos.x >= -DUNGEON_CHUNK_OFFSET || chunkPos.z >= -DUNGEON_CHUNK_OFFSET);
    }

    public static ChunkPos getTopRoomChunk(Room room) {
        return Arrays.stream(room.squares)
                .sorted(Comparator.comparingInt(e -> e.x))
                .sorted(Comparator.comparingInt(e -> e.z))
                .toList().getFirst();
    }

    public static BlockPos getChunkCenter(ChunkPos chunkPos) {
        return new BlockPos((chunkPos.x * 16) + 7, 0, (chunkPos.z * 16) + 7);
    }

    private static void onLeaveDungeon() {
        wasInDungeon = false;
        UNSCANNED_CHUNKS.clear();
        MAP_DIVISIONS.clear();
        ROOMS.clear();
        CURRENT_DUNGEON_DIAMETER = new Vector2f(MAX_DUNGEON_DIAMETER, MAX_DUNGEON_DIAMETER);
        finishedScanning = false;
    }
}