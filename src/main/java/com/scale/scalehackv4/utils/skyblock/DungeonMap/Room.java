package com.scale.scalehackv4.utils.skyblock.DungeonMap;

import com.google.gson.internal.LinkedTreeMap;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import lombok.AllArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Room {
    private static final File DUNGEON_ROOMS_FILE = new File(Main.BASE_FOLDER_PATH + "/dungeons/dungeon_rooms.json");

    // blocks for the hashing algorithm to ignore because they can be in different positions randomly
    private static final java.util.List<Block> BLACKLISTED_BLOCKS = List.of(
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
            BlockPos middle = DungeonScanner.getChunkCenter(square);

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
        BlockPos centerBlock = DungeonScanner.getChunkCenter(this.squares[this.lowestHashIndex]);

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

    private String hashPos(BlockPos pos) {
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
}
