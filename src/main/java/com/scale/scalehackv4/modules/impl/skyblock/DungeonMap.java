package com.scale.scalehackv4.modules.impl.skyblock;

import com.google.gson.internal.LinkedTreeMap;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import lombok.AllArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@RegisterModule(
        name = "Dungeon Map",
        category = Category.SKYBLOCK
)
public class DungeonMap extends Module {
    private static final HashMap<Integer, HashMap<Integer, Boolean>> UNSCANNED_CHUNKS = new HashMap<>();
    private static final HashMap<Integer, HashMap<Integer, MapDivision>> MAP_DIVISIONS = new HashMap<>();

    public static final ArrayList<Room> ROOMS = new ArrayList<>();
    private static final HashMap<Integer, HashMap<Integer, Boolean>> UNASSIGNED_CHUNKS = new HashMap<>();

    private static final int MAX_DUNGEON_DIAMETER = 190;
    private static final int MAX_DUNGEON_CHUNKS = Math.ceilDiv(MAX_DUNGEON_DIAMETER, 16);
    private static final int DUNGEON_BLOCKS_OFFSET = 10;
    private static final int DUNGEON_CHUNK_OFFSET = Math.ceilDiv(DUNGEON_BLOCKS_OFFSET, 16);

    private static final Color CLEAR_ROOM_COLOUR = new Color(169, 104, 5);

    private static final int DUNGEON_MAP_DIAMETER = 100;
    private static final int ROOM_RADIUS = 7;
    private static final int DOOR_RADIUS = 3;

    private static boolean wasInDungeon = false;

    private static final File DUNGEON_ROOMS_FILE = new File(Main.BASE_FOLDER_PATH + "/dungeons/dungeon_rooms.json");

    // blocks for the hashing algorithm to ignore because they can be in different positions randomly
    private static final List<Block> BLACKLISTED_BLOCKS = List.of(
            Blocks.RED_WOOL, // waterboard
            Blocks.JUNGLE_PLANKS, // boulder
            Blocks.BIRCH_PLANKS, // boulder
            Blocks.POLISHED_ANDESITE // ice fill
    );

    @AllArgsConstructor
    private enum DoorType {
        WALL(new Color(255, 255, 255)),
        FREE(CLEAR_ROOM_COLOUR),
        WITHER(new Color(0, 0, 0)),
        BLOOD(new Color(255, 0, 0)),;

        private final Color colour;
    }
    private record MapDivision(Direction.Axis axis, DoorType doorType) {}

    public static class Room {
        public ChunkPos[] squares;
        public Direction facing;

        public String name = "";
        public int secrets = -1;
        public Type type = Type.MOB;
        public int[] hashes;

        public Room(ChunkPos[] squares) {
            this.squares = squares;
            getHashes();
            getRoom();
        }

        private void getRoom() {
            HashMap<String, LinkedTreeMap<String, Object>> rooms = getRooms();

            LinkedTreeMap<String, Object> room = rooms.get(Arrays.toString(hashes));
            if (room == null) return;

            name = (String) room.getOrDefault("name", "");
            secrets = (int) Double.parseDouble(room.getOrDefault("secrets", -1).toString());
            type = Type.valueOf((String) room.getOrDefault("type", "MOB"));
        }

        public void createRoom(String name, Type type, int secrets) {
            HashMap<String, LinkedTreeMap<String, Object>> rooms = getRooms();

            this.name = name;
            this.secrets = secrets;
            this.type = type;

            LinkedTreeMap<String, Object> room = new LinkedTreeMap<>();
            room.put("name", name);
            room.put("type", type.name());
            room.put("secrets", secrets);

            rooms.put(Arrays.toString(hashes), room);

            try {
                Files.write(DUNGEON_ROOMS_FILE.toPath(), C.gson.toJson(rooms).getBytes());
                System.out.println("Saved room: " + name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static HashMap<String, LinkedTreeMap<String, Object>> getRooms() {
            try {
                if (Files.exists(DUNGEON_ROOMS_FILE.toPath())) {
                    HashMap<String, LinkedTreeMap<String, Object>> rooms = C.gson.fromJson(FileUtils.readFileToString(DUNGEON_ROOMS_FILE, Charset.defaultCharset()), HashMap.class);
                    if (rooms != null) return rooms;
                }
            } catch (IOException ignored) {}

            DUNGEON_ROOMS_FILE.getParentFile().mkdirs();
            return new HashMap<>();
        }

        private void getHashes() {
            ChunkPos[] squaresSorted = Arrays.stream(squares)
                    .sorted(Comparator.comparingInt(e -> e.z))
                    .sorted(Comparator.comparingInt(e -> e.x))
                    .toArray(ChunkPos[]::new);

            // gets the centre of each chunk in the room
            int[] chunkHashes = new int[squares.length];
            int lowestHashIndex = 0;
            for (int i = 0; i < squares.length; i++) {
                ChunkPos square = squaresSorted[i];
                BlockPos middle = getChunkCenter(square);

                chunkHashes[i] = hashXZ(middle.getX(), middle.getZ());
                if (chunkHashes[i] < chunkHashes[lowestHashIndex]) lowestHashIndex = i;
            }

            // combine the hashes into a string and sort the hashes to always be the same no matter the orientation of the room
            int roomHash = Arrays.stream(chunkHashes).sorted().mapToObj(String::valueOf).collect(Collectors.joining()).hashCode();

            // arbitrary number offset, 2 just seems nice and round
            // always offsets from the same point, meaning the result is only affected by the orientation of the room
            BlockPos centerBlock = getChunkCenter(squaresSorted[lowestHashIndex]);
            BlockPos offset1 = centerBlock.add(2, 0, 2);
            BlockPos offset2 = centerBlock.add(-2, 0, -2);

            this.hashes = new int[] {
                    roomHash,
                    // create all the possible hashes for the rotations
                    hashXZ(offset1.getX(), offset1.getZ()),
                    hashXZ(offset2.getX(), offset1.getZ()),
                    hashXZ(offset2.getX(), offset2.getZ()),
                    hashXZ(offset1.getX(), offset2.getZ())
            };

            // todo: get the direction before sorting hashes
            this.hashes = Arrays.stream(this.hashes).sorted().toArray();
        }

        private static int hashXZ(int x, int z) {
            String hashString = "";
            for (int y = 0; y < 255; y++) {
                BlockState state = C.w().getBlockState(new BlockPos(x, y, z));
                Block block = state.getBlock();
                // ignore non full blocks as they can sometimes not be loaded cause of render distance
                if (state.isOpaqueFullCube() && !BLACKLISTED_BLOCKS.contains(block)) {
                    hashString += block.getTranslationKey();
                }
            }

            return hashString.hashCode();
        }

        @AllArgsConstructor
        public enum Type {
            MOB(CLEAR_ROOM_COLOUR),
            MINI(CLEAR_ROOM_COLOUR),
            YELLOW(new Color(0xFFE95F)),
            BLOOD(new Color(0xFF0202)),
            FAIRY(new Color(0xFF00FE)),
            PUZZLE(new Color(0x6B3AFA)),
            SPAWN(new Color(0x257506)),
            TRAP(new Color(0xFFA002));

            public final Color colour;
        }
    }

    public final Draggable dungeonMap = new Draggable(
            "Dungeon Map",
            () -> {
                Render2dUtil.drawRect(-1, -1, DUNGEON_MAP_DIAMETER+1, DUNGEON_MAP_DIAMETER+1, new Color(22, 22, 22, 170));

                for (int x : MAP_DIVISIONS.keySet().stream().sorted().toList()) {
                    for (int z : MAP_DIVISIONS.get(x).keySet().stream().sorted().toList()) {
                        MapDivision mapDivision = MAP_DIVISIONS.get(x).get(z);

                        if (mapDivision.doorType == DoorType.WALL) continue;

                        BlockPos chunkCenter = getChunkCenter(new ChunkPos(x, z));

                        int x2d = pos3dTo2d(chunkCenter.getX()) - DOOR_RADIUS;
                        int y2d = pos3dTo2d(chunkCenter.getZ()) - DOOR_RADIUS;

                        Render2dUtil.drawRect(x2d, y2d, DOOR_RADIUS*2, DOOR_RADIUS*2, mapDivision.doorType.colour);
                    }
                }

                for (int i = 0; i < ROOMS.size(); i++) {
                    Room room = ROOMS.get(i);
                    Rectangle highestXY = new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
                    for (int j = 0; j < room.squares.length; j++) {
                        ChunkPos square = room.squares[j];
                        BlockPos chunkCenter = getChunkCenter(square);

                        int x = pos3dTo2d(chunkCenter.getX()) - ROOM_RADIUS;
                        int y = pos3dTo2d(chunkCenter.getZ()) - ROOM_RADIUS;

                        Render2dUtil.drawRect(x, y, ROOM_RADIUS*2, ROOM_RADIUS*2, room.type.colour);

                        highestXY.x = Math.min(highestXY.x, x);
                        highestXY.y = Math.min(highestXY.y, y);
                    }
                    FontUtil.drawString(room.name, highestXY.x, highestXY.y, 4, Color.WHITE, true);
                }

                drawPlayer(C.p());

                return new int[] {DUNGEON_MAP_DIAMETER, DUNGEON_MAP_DIAMETER};
            },
            e -> ModuleUtil.isEnabled(DungeonMap.class) && AreaUtil.isInDungeons(),
            e -> true
    );

    // infrequently posted event
    private final Event playerUpdateEvent = new PlayerUpdateEvent(this, (params -> {
        if (!AreaUtil.isInDungeons()) {
            if (wasInDungeon) onDisable();
            return false;
        }
        else if (!wasInDungeon) {
            wasInDungeon = true;

            onEnable();
        }

        // once every 20 ticks is enough, its fast as is tho tbf
        if (C.p().age % 20 == 0) {
            getSplits();
            registerRooms();
        }

        return false;
    }));

    private static void registerRooms() {
        // make an array to stop concurrent modification exceptions
        for (int x : UNASSIGNED_CHUNKS.keySet().toArray(Integer[]::new)) {
            HashMap<Integer, Boolean> zChunks = UNASSIGNED_CHUNKS.get(x);

            // the hashmap is modified by assignChunk, causing there to be a loop of an x that is already assigned
            if (zChunks == null) continue;

            for (int z : zChunks.keySet().toArray(Integer[]::new)) {
                ChunkPos chunkPos = new ChunkPos(x, z);
                // dont try and scan a chunk if its already scanned, or cant be scanned
                if (isChunkScanned(chunkPos) || isChunkUnloaded(chunkPos)) continue;

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
        if (isChunkUnloaded(chunkPos)) return null;

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

    private static BlockPos getChunkCenter(ChunkPos chunkPos) {
        return new BlockPos((chunkPos.x * 16) + 7, 80, (chunkPos.z * 16) + 7);
    }

    private static int pos3dTo2d(double pos) {
        double multi = (double) DUNGEON_MAP_DIAMETER / MAX_DUNGEON_DIAMETER;
        return (int) ((pos + (DUNGEON_BLOCKS_OFFSET+MAX_DUNGEON_DIAMETER)) * multi);
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

        Block centreBlock = C.w().getBlockState(chunkCenter).getBlock();
        if (isChunkUnloaded(chunkPos)) return false;

        boolean isStartDoor = C.w().getBlockState(new BlockPos(chunkCenter.getX(), 69, chunkCenter.getZ())).getBlock() == Blocks.INFESTED_CHISELED_STONE_BRICKS;

        if (centreBlock == Blocks.AIR || isStartDoor) {
            boolean door = C.w().getBlockState(new BlockPos(chunkCenter.getX(), 66, chunkCenter.getZ())).getBlock() == Blocks.BEDROCK;
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

            boolean north = C.w().getBlockState(chunkCenter.north()).getBlock() == Blocks.AIR
                    || (C.w().getBlockState(chunkCenter.north()).getBlock() == Blocks.INFESTED_CHISELED_STONE_BRICKS && isStartDoor);

            DoorType doorType = isStartDoor ? DoorType.FREE : DoorType.WALL;
            if (door) {
                Block doorBlock = C.w().getBlockState(new BlockPos(chunkCenter.getX(), 69, chunkCenter.getZ())).getBlock();

                if (doorBlock == Blocks.AIR) doorType = DoorType.FREE;
                else if (doorBlock == Blocks.COAL_BLOCK) doorType = DoorType.WITHER;
                else if (doorBlock == Blocks.RED_TERRACOTTA) doorType = DoorType.BLOOD;
            }

            MAP_DIVISIONS.computeIfAbsent(chunkPos.x, k -> new HashMap<>())
                    .put(chunkPos.z, new MapDivision(north ? Direction.Axis.X : Direction.Axis.Z, doorType));

            assignChunk(chunkPos);
        }

        return true;
    }

    private static void assignChunk(ChunkPos chunkPos) {
        HashMap<Integer, Boolean> zChunks = UNASSIGNED_CHUNKS.get(chunkPos.x);
        if (zChunks == null) return;

        UNASSIGNED_CHUNKS.get(chunkPos.x).remove(chunkPos.z);

        if (UNASSIGNED_CHUNKS.get(chunkPos.x).isEmpty()) {
            UNASSIGNED_CHUNKS.remove(chunkPos.x);
        }
    }

    private static boolean isChunkUnloaded(ChunkPos chunkPos) {
        return C.w().getBlockState(new BlockPos(chunkPos.x * 16, 0, chunkPos.z * 16)).getBlock() == Blocks.VOID_AIR;
    }

    private static void drawPlayer(PlayerEntity player) {
        int playerX = pos3dTo2d(C.p().getX());
        int playerZ = pos3dTo2d(C.p().getZ());

        Matrix3x2fStack matrices = Render2dUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();
        matrices.translate(playerX, playerZ);
        matrices.rotate((float) Math.toRadians(player.getYaw()-180));
        Render2dUtil.drawPlayerHead(player, -4, -4, 8, 8, Color.WHITE);
        matrices.popMatrix();
    }

    @Override
    public void onEnable() {
        for (int x = -MAX_DUNGEON_CHUNKS; x < -DUNGEON_CHUNK_OFFSET; x++) {
            for (int z = -MAX_DUNGEON_CHUNKS; z < -DUNGEON_CHUNK_OFFSET; z++) {
                UNSCANNED_CHUNKS.computeIfAbsent(x, k -> new HashMap<>()).put(z, true);
                UNASSIGNED_CHUNKS.computeIfAbsent(x, k -> new HashMap<>()).put(z, true);
            }
        }
    }

    @Override
    public void onDisable() {
        wasInDungeon = false;
        UNSCANNED_CHUNKS.clear();
        MAP_DIVISIONS.clear();
        ROOMS.clear();
    }
}
