package com.scale.scalehackv4.modules.impl.skyblock;

import com.google.gson.internal.LinkedTreeMap;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.bridge.MapDecorationsBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.PacketEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.TargetUtil;
import com.scale.scalehackv4.utils.render.*;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import lombok.AllArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.joml.Vector2i;

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
    private static Vector2f CURRENT_DUNGEON_DIAMETER = new Vector2f(MAX_DUNGEON_DIAMETER, MAX_DUNGEON_DIAMETER);
    private static final int MAX_DUNGEON_CHUNKS = Math.ceilDiv(MAX_DUNGEON_DIAMETER, 16);
    private static final int DUNGEON_BLOCKS_OFFSET = 10;
    private static final int DUNGEON_CHUNK_OFFSET = Math.ceilDiv(DUNGEON_BLOCKS_OFFSET, 16);

    private static final int MAX_DUNGEON_MAP_DIAMETER = 100;
    private static final int ROOM_RADIUS = 7;
    private static final int DOOR_RADIUS = 3;

    private static final int MAP_ITEM_DIAMETER = 128;
    private static final int MAP_ITEM_ROOM_SIZES = 18;
    private static final int MAP_ITEM_DOOR_SIZES = 2;
    private static final int MAP_SQUARE_SIZE = MAP_ITEM_ROOM_SIZES+MAP_ITEM_DOOR_SIZES;

    private static boolean wasInDungeon = false;
    private static boolean finishedScanning = false;

    private static final File DUNGEON_ROOMS_FILE = new File(Main.BASE_FOLDER_PATH + "/dungeons/dungeon_rooms.json");

    // blocks for the hashing algorithm to ignore because they can be in different positions randomly
    private static final List<Block> BLACKLISTED_BLOCKS = List.of(
            Blocks.RED_WOOL, // waterboard
            Blocks.LIME_WOOL, // waterboard
            Blocks.BLUE_WOOL, // waterboard
            Blocks.ORANGE_WOOL, // waterboard
            Blocks.PURPLE_WOOL, // waterboard
            Blocks.PISTON, // waterboard
            Blocks.JUNGLE_PLANKS, // boulder
            Blocks.BIRCH_PLANKS, // boulder
            Blocks.POLISHED_ANDESITE, // ice fill
            Blocks.ANDESITE, // trap
            Blocks.POLISHED_GRANITE, // any room with crushers
            Blocks.MOSSY_COBBLESTONE, // i was scared mossy cobble was scattered, but it isnt, already marked rooms, cant remove now
            Blocks.STONE_BRICKS, // crypts
            Blocks.CRACKED_STONE_BRICKS // crypts and exploding walls
    );

    @AllArgsConstructor
    private enum DoorType {
        WALL(new Color(255, 255, 255)),
        FREE(new Color(169, 104, 5)),
        WITHER(new Color(113, 0, 213)),
        BLOOD(new Color(255, 0, 0)),;

        private final Color colour;
    }
    private record MapDivision(Direction.Axis axis, DoorType doorType) {}

    public static class Room {
        public ChunkPos[] squares;
        public Direction facing = Direction.NORTH;
        public int lowestHashIndex = 0;
        public ArrayList<BlockPos> nodes = new ArrayList<>();
        public State state = State.UNVISITED;

        public String name = "";
        public int secrets = -1;
        public int secretsFound = 0;
        public Type type = Type.MOB;
        public int hash;
        public int[] facingHashes;

        public Room(ChunkPos[] squares) {
            this.squares = squares;
            getHashes();
            getRoom();
        }

        private void getRoom() {
            HashMap<String, LinkedTreeMap<String, Object>> rooms = getRooms();

            LinkedTreeMap<String, Object> room = rooms.get(hash+"");
            if (room == null) return;

            this.name = (String) room.getOrDefault("name", "");
            this.secrets = (int) Double.parseDouble(room.getOrDefault("secrets", -1).toString());
            this.type = Type.valueOf((String) room.getOrDefault("type", "RARE"));
            getFacing(room.getOrDefault("hashes", new int[0]));
            getNodes((String) room.getOrDefault("nodes", ""));
        }

        private void getFacing(Object hashes) {
            int[] northFacingHashes = Arrays.stream(hashes.toString().replaceAll("[\\[\\]\\s]", "").split(",")).mapToInt(Integer::parseInt).toArray();

            // loops to find the offset of the array which matches the current hashes
            for (int offset = 0; offset < 4; offset++) {
                boolean matches = true;
                for (int i = 0; i < 4; i++) {
                    // check that everything is in the same order at this offset
                    if (this.facingHashes[i] != northFacingHashes[(i + offset) % 4]) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    this.facing = offset == 0 ? Direction.NORTH : offset == 1 ? Direction.WEST : offset == 2 ? Direction.SOUTH : Direction.EAST;
                    return;
                }
            }

            ChatUtil.chat("WARNING: Room facing hashes not found: " + this.name + "!");
        }

        private void getNodes(String nodesString) {
            if (nodesString.isEmpty()) return;

            String[] nodes = nodesString.split(",");
            for (int i = 0; i < nodes.length; i+=3) {
                this.nodes.add(new BlockPos(Integer.parseInt(nodes[i]), Integer.parseInt(nodes[i+1]), Integer.parseInt(nodes[i+2])));
            }
        }

        public void saveRoom() {
            createRoom(name, type, secrets);
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
            room.put("hashes", Arrays.toString(this.facingHashes));
            room.put("nodes", nodes.stream().map(e -> e.getX()+","+e.getY()+","+e.getZ()).collect(Collectors.joining(",")));

            if (rooms.containsKey(hash+"")) {
                System.out.println("Overwriting room: " + name);
                ChatUtil.chat("Room overwritten: " + name + "!");
            }
            rooms.put(hash+"", room);

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
            // gets the centre of each chunk in the room
            int[] chunkHashes = new int[squares.length];
            for (int i = 0; i < squares.length; i++) {
                ChunkPos square = squares[i];
                BlockPos middle = getChunkCenter(square);

                chunkHashes[i] = hashPos(middle).hashCode();
                // i assume no room has several equal chunk hashes, *gulp*
                if (chunkHashes[i] < chunkHashes[this.lowestHashIndex]) this.lowestHashIndex = i;
            }

            // combine the hashes into a string and sort the hashes to always be the same no matter the orientation of the room
            int roomHash = Arrays.stream(chunkHashes).sorted().mapToObj(String::valueOf).collect(Collectors.joining()).hashCode();

            this.facingHashes = getFacingHashes();

            // originally the "roomHash" was the only part, but ttt and a 1x1 have the exact same blocks in the same order down the center.
            this.hash = (roomHash + Arrays.stream(this.facingHashes).sorted().mapToObj(String::valueOf).collect(Collectors.joining())).hashCode();
        }

        public int[] getFacingHashes() {
            BlockPos centerBlock = getChunkCenter(this.squares[this.lowestHashIndex]);

            String[] hashes = new String[4];

            // scans the whole chunk
            // always offsets from the same point, meaning the result is only affected by the orientation of the room
            for (int i = 1; i < 7; i++) {
                hashes[0] += hashPos(centerBlock.offset(Direction.NORTH, i));
                hashes[1] += hashPos(centerBlock.offset(Direction.EAST, i));
                hashes[2] += hashPos(centerBlock.offset(Direction.SOUTH, i));
                hashes[3] += hashPos(centerBlock.offset(Direction.WEST, i));
            }

            return new int[] {hashes[0].hashCode(), hashes[1].hashCode(), hashes[2].hashCode(), hashes[3].hashCode()};
        }

        @AllArgsConstructor
        public enum Type {
            MOB(new Color(169, 104, 5)),
            MINI(new Color(169, 104, 5)),
            RARE(new Color(0x0666DC)),
            YELLOW(new Color(0xFFE95F)),
            BLOOD(new Color(0xFF0202)),
            FAIRY(new Color(0xFF00FE)),
            PUZZLE(new Color(0x6B3AFA)),
            SPAWN(new Color(0x257506)),
            TRAP(new Color(0xF88521));

            public final Color colour;
        }

        public enum State {
            UNVISITED,
            FAILED,
            VISITED,
            CLEARED,
            GREEN_FN // niche
        }
    }

    public final Draggable dungeonMap = new Draggable(
            "Dungeon Map",
            () -> {
                Vector2i mapDiameter = getMapDiameter();
                Render2dUtil.drawRect(0, 0, mapDiameter.x+1, mapDiameter.y+1, new Color(22, 22, 22, 170));

                DrawContext dc = RenderUtil.getDrawContext();
                dc.enableScissor(0, 0, mapDiameter.x, mapDiameter.y);

                // push a new matrix just to translate 1 pixel to the right :pray:
                dc.getMatrices().pushMatrix();
                dc.getMatrices().translate(1, 1);

                drawDoors();
                drawRooms();
                drawPlayers();

                dc.getMatrices().popMatrix();
                dc.disableScissor();

                return new int[] {mapDiameter.x+1, mapDiameter.y+1};
            },
            e -> ModuleUtil.isEnabled(DungeonMap.class) && AreaUtil.isInDungeons(),
            e -> true
    );

    private final Event renderRoomNodes = new RenderWorldEvent(this, (params -> {
        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return false;

        for (BlockPos relativeNodePos : currentRoom.nodes) {
            BlockPos nodePos = getNodePos(relativeNodePos, currentRoom);
            Render3dUtil.drawBox(nodePos.getX(), nodePos.getY(), nodePos.getZ(), 1, 1, 1, new Color(135, 148, 255), false);
        }

        return false;
    }));

    // infrequently posted event
    private final Event playerUpdateEvent = new PlayerUpdateEvent(this, params -> {
        if (!AreaUtil.isInDungeons()) {
            if (wasInDungeon) onDisable();
            return false;
        }
        else if (!wasInDungeon) {
            wasInDungeon = true;

            onEnable();
        }

        // once a second is enough, its fast as is tho tbf
        if (C.p().age % 20 == 0) {
            getSplits();
            registerRooms();
            parseMap(); // relatively slow process, maybe move to a separate thread
            setCurrentDungeonDiameter();

            if (!finishedScanning && finishedScanning()) {
                onFinishedScanning();
            }
        }

        return false;
    });

    private final Event updateSecretCount = new PacketEvent.Received(this, event -> {
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

    private static Vector2f getDungeonRect() {
        // yeah there are better ways to do this, eg. scanning player pos on the map, get percent and compare to percent of max
        Vector2f highestValue = new Vector2f(Integer.MIN_VALUE, Integer.MIN_VALUE);
        for (int x : MAP_DIVISIONS.keySet()) {
            for (int z : MAP_DIVISIONS.get(x).keySet()) {
                MapDivision mapDivision = MAP_DIVISIONS.get(x).get(z);
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

    private static void drawDoors() {
        for (int x : MAP_DIVISIONS.keySet().stream().sorted().toList()) {
            for (int z : MAP_DIVISIONS.get(x).keySet().stream().sorted().toList()) {
                MapDivision mapDivision = MAP_DIVISIONS.get(x).get(z);

                if (mapDivision.doorType == DoorType.WALL) continue;

                BlockPos chunkCenter = getChunkCenter(new ChunkPos(x, z));

                int x2d = pos3dTo2d(chunkCenter.getX()) - DOOR_RADIUS;
                int y2d = pos3dTo2d(chunkCenter.getZ()) - DOOR_RADIUS;

                // if the door doesnt connect 2 open rooms, make it greyed out.
                Color doorColour = getOpenedConnections(x, z) == 2 ? mapDivision.doorType.colour : mapDivision.doorType.colour.darker();

                Render2dUtil.drawRect(x2d, y2d, DOOR_RADIUS*2, DOOR_RADIUS*2, doorColour);
            }
        }
    }

    private static ArrayList<Room> getConnectedRooms(int x, int z) {
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

    private static int getOpenedConnections(int x, int z) {
        return getConnectedRooms(x, z).stream().filter(e -> e.state != Room.State.UNVISITED).toList().size();
    }

    private static void drawRooms() {
        for (int i = 0; i < ROOMS.size(); i++) {
            Room room = ROOMS.get(i);
            Rectangle highestXY = new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
            for (int j = 0; j < room.squares.length; j++) {
                ChunkPos square = room.squares[j];
                BlockPos chunkCenter = getChunkCenter(square);

                int x = pos3dTo2d(chunkCenter.getX()) - ROOM_RADIUS;
                int y = pos3dTo2d(chunkCenter.getZ()) - ROOM_RADIUS;
                Color roomColour = room.state == Room.State.UNVISITED ? room.type.colour.darker() : room.type.colour;

                Render2dUtil.drawRect(x, y, ROOM_RADIUS*2, ROOM_RADIUS*2, roomColour);

                highestXY.x = Math.min(highestXY.x, x);
                highestXY.y = Math.min(highestXY.y, y);
            }

            Color secretsColour = room.state == Room.State.CLEARED ? Color.WHITE
                    : room.state == Room.State.GREEN_FN ? Color.GREEN
                    : room.state == Room.State.FAILED ? new Color(255, 0, 0)
                    : new Color(101, 101, 101);

            int secretsFound = room.state == Room.State.GREEN_FN ? room.secrets : room.secretsFound;
            String secretsText = room.secrets == -1 ? "N/A" : room.secrets == 0 ? "0" : secretsFound+ "/" + room.secrets;
            FontUtil.drawString(secretsText, highestXY.x, highestXY.y, 6, secretsColour, true);
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
                if (isChunkScanned(chunkPos) || isChunkUnloaded(chunkPos)) continue;

                Room room = getRoom(chunkPos);
                if (room == null) continue;

                ROOMS.add(room);
                for (ChunkPos square : room.squares) assignChunk(square);
            }
        }
    }

    private static String hashPos(BlockPos pos) {
        String hashString = "";

        String lastKey = "";
        for (int y = 0; y < 255; y++) {
            BlockState state = C.w().getBlockState(new BlockPos(pos.getX(), y, pos.getZ()));
            Block block = state.getBlock();
            // ignore non full blocks as they can sometimes not be loaded cause of render distance
            if (state.isOpaqueFullCube() && !BLACKLISTED_BLOCKS.contains(block)) {
                // since the roofs are different sometimes, we dont include the top layer
                // the last valid block is NEVER added to hashString and is forgotten like a grain of sand in the desert.
                hashString += lastKey;
                lastKey = block.getTranslationKey();
            }
        }

        return hashString;
    }

    private static BlockPos getNodePos(BlockPos node, Room room) {
        BlockPos corner = getRoomDefaultCorner(room);
        BlockPos nodeRotated = rotateNode(node, Direction.NORTH, room.facing);
        return corner.add(nodeRotated.getX(), nodeRotated.getY(), nodeRotated.getZ());
    }

    public static BlockPos getRoomDefaultCorner(Room room) {
        return DungeonMap.getChunkCenter(room.squares[room.lowestHashIndex]);
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

    public static ChunkPos getTopRoomChunk(Room room) {
        return Arrays.stream(room.squares)
                .sorted(Comparator.comparingInt(e -> e.x))
                .sorted(Comparator.comparingInt(e -> e.z))
                .toList().getFirst();
    }

    public static BlockPos getChunkCenter(ChunkPos chunkPos) {
        return new BlockPos((chunkPos.x * 16) + 7, 0, (chunkPos.z * 16) + 7);
    }

    private static int pos3dTo2d(double pos) {
        return pos3dTo2d(pos, MAX_DUNGEON_MAP_DIAMETER, MAX_DUNGEON_DIAMETER);
    }

    private static int pos3dTo2d(double pos, double width, double maxWidth) {
        double multi = width / maxWidth;
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
        BlockPos aboveDoorCenter = chunkCenter.offset(Direction.UP, 82);

        Block centreBlock = C.w().getBlockState(aboveDoorCenter).getBlock();
        if (isChunkUnloaded(chunkPos)) return false;

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

    private static boolean isChunkUnloaded(ChunkPos chunkPos) {
        return C.w().getBlockState(new BlockPos(chunkPos.x * 16, 0, chunkPos.z * 16)).getBlock() == Blocks.VOID_AIR;
    }

    private static HashMap<String, HashMap<PlayerEntity, PlayerIcon>> MAP_PLAYERS = new HashMap<>();
    private record PlayerIcon(int score, Identifier skin) {}

    private static void drawPlayers() {
        MapState mapState = getMapState();
        if (mapState != null) {
            Map<String, MapDecoration> decorations = MapDecorationsBridge.from(mapState).bridge$getDecorations();
            if (decorations == null) return;

            for (String id : decorations.keySet()) {
                MapDecoration decoration = decorations.get(id);

                Vector2f pos = new Vector2f(
                        (decoration.x() + 128) / 2f,
                        (decoration.z() + 128) / 2f
                );

                getPotentialPlayersFromMapDecoration(id, pos);
                drawPlayer(id, decoration, pos);
            }
        }
    }

    // todo: im sure there is a way to get the player 100% correct, but i couldnt find anything in the mapstate class.
    private static void getPotentialPlayersFromMapDecoration(String id, Vector2f pos) {
        for (PlayerEntity player : TargetUtil.getPlayers()) {
            int x = pos3dTo2d(player.getX(), MAP_ITEM_DIAMETER, CURRENT_DUNGEON_DIAMETER.x);
            int z = pos3dTo2d(player.getZ(), MAP_ITEM_DIAMETER, CURRENT_DUNGEON_DIAMETER.y);

            if (Math.abs(x - pos.x) <= 5 && Math.abs(z - pos.y) <= 5) {
                MAP_PLAYERS.computeIfAbsent(id, k -> new HashMap<>());
                PlayerIcon playerIcon = MAP_PLAYERS.get(id).get(player);
                int score = 0;
                Identifier skin;
                if (playerIcon != null) {
                    score = playerIcon.score;
                    skin = playerIcon.skin;
                }
                else skin = Render2dUtil.getPlayerSkin(player);

                MAP_PLAYERS.get(id).put(player, new PlayerIcon(score+1, skin));
            }
        }
    }

    private static PlayerIcon getBestGuessPlayer(String id) {
        PlayerIcon defaultPlayer = new PlayerIcon(0, Render2dUtil.getPlayerSkin(C.p()));
        if (!MAP_PLAYERS.containsKey(id)) return defaultPlayer;

        return MAP_PLAYERS.get(id).entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().score))
                .map(Map.Entry::getValue)
                .orElse(defaultPlayer);
    }

    // todo: on lower floors the heads are slightly offset
    private static void drawPlayer(String id, MapDecoration decoration, Vector2f pos) {
        Matrix3x2fStack matrices = Render2dUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();
        Vector2i mapDiameter = getMapDiameter();
        matrices.translate(
                (pos.x/MAP_ITEM_DIAMETER) * mapDiameter.x,
                (pos.y/MAP_ITEM_DIAMETER) * mapDiameter.y
        );
        matrices.rotate((float) Math.toRadians((decoration.rotation()*22.5) - 180));
        Render2dUtil.drawSkinHead(getBestGuessPlayer(id).skin, -4, -4, 8, 8, Color.WHITE);
        matrices.popMatrix();
    }

    private static Vector2i getMapDiameter() {
        return new Vector2i(
                (int) ((CURRENT_DUNGEON_DIAMETER.x/MAX_DUNGEON_DIAMETER) * MAX_DUNGEON_MAP_DIAMETER),
                (int) ((CURRENT_DUNGEON_DIAMETER.y/MAX_DUNGEON_DIAMETER) * MAX_DUNGEON_MAP_DIAMETER)
        );
    }

    private static Vector2f getMapIndent() {
        return new Vector2f(MAP_ITEM_DIAMETER).sub(getDungeonRect().mul(MAP_SQUARE_SIZE));
    }

    private static void parseMap() {
        MapState mapState = getMapState();
        if (mapState == null) return;

        Vector2f mapIndent = getMapIndent();
        if (!hasCalculatedSize()) return;

        for (Room room : ROOMS) {
            BlockPos chunkCenter = getChunkCenter(getTopRoomChunk(room));

            int roomX = (int) (pos3dTo2d(chunkCenter.getX(), MAP_ITEM_DIAMETER - mapIndent.x, CURRENT_DUNGEON_DIAMETER.x)+mapIndent.x/2);
            int roomY = (int) (pos3dTo2d(chunkCenter.getZ(), MAP_ITEM_DIAMETER - mapIndent.y, CURRENT_DUNGEON_DIAMETER.y)+mapIndent.y/2);

            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    int colourIndex = (roomX+x) + ((roomY+y) * MAP_ITEM_DIAMETER);
                    // not sure if this still happens, issue with first try, probably fixed
                    if (colourIndex < 0 || colourIndex >= mapState.colors.length) {
                        ChatUtil.prefixMessage("Critical error!!! (lie, but maybe bad, report pls <3 the map parsing has failed)");
                        continue;
                    }

                    int colour = mapState.colors[colourIndex];

                    // if the map shows the position as grey/nothing there/black, the room is unvisited
                    if (colour == 85 || colour == 0 || colour == 119) continue;

                    if (colour == 34) room.state = Room.State.CLEARED;
                    else if (colour == 30 && room.type != Room.Type.SPAWN) room.state = Room.State.GREEN_FN;
                    else if (colour == 18 && room.type != Room.Type.BLOOD) room.state = Room.State.FAILED;
                    else if (room.state == Room.State.UNVISITED) room.state = Room.State.VISITED;
                }
            }
        }
    }

    private static MapState getMapState() {
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            if (C.p().getInventory().getStack(i).getItem() == Items.FILLED_MAP) {
                return FilledMapItem.getMapState(C.p().getInventory().getStack(i), C.w());
            }
        }
        return null;
    }

    private static boolean finishedScanning() {
        for (int x : MAP_DIVISIONS.keySet()) {
            for (int z : MAP_DIVISIONS.get(x).keySet()) {
                if (MAP_DIVISIONS.get(x).get(z).doorType == DoorType.WALL) continue;

                if (getConnectedRooms(x, z).size() < 2) return false;
            }
        }
        return true;
    }

    // if no door is missing a room to the bottom right, then the dungeon is fully scanned, otherwise, there is still more to come
    private static boolean hasCalculatedSize() {
        for (int x : MAP_DIVISIONS.keySet()) {
            for (int z : MAP_DIVISIONS.get(x).keySet()) {
                if (MAP_DIVISIONS.get(x).get(z).doorType == DoorType.WALL) continue;

                boolean found = false;
                for (Room room : ROOMS) {
                    for (ChunkPos chunkPos : room.squares) {
                        if (chunkPos.x == x+1 && chunkPos.z == z || chunkPos.x == x && chunkPos.z == z+1) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) return false;
            }
        }
        return true;
    }

    private static void onFinishedScanning() {
        finishedScanning = true;
        ChatUtil.chat("--------------------------");
        ChatUtil.prefixMessage("&a&lFinished scanning dungeon!");
        ChatUtil.chat("&2" + ROOMS.size() + " &arooms found!");
        ChatUtil.chat("&2Puzzles:\n" + ROOMS.stream()
                .filter(e -> e.type == Room.Type.PUZZLE)
                .map(e -> "&5 - " + e.name)
                .collect(Collectors.joining("\n"))
        );
        ChatUtil.chat("--------------------------");
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
        MAP_PLAYERS.clear();
        CURRENT_DUNGEON_DIAMETER = new Vector2f(MAX_DUNGEON_DIAMETER, MAX_DUNGEON_DIAMETER);
        finishedScanning = false;
    }
}