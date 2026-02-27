package com.scale.scalehackv4.utils.skyblock.DungeonMap;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.TargetUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.joml.Vector2f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@RegisterListeners
public class MapParser {
    public static final int MAP_ITEM_DIAMETER = 128;
    public static final int MAP_ITEM_ROOM_SIZES = 18;
    public static final int MAP_ITEM_DOOR_SIZES = 2;
    public static final int MAP_SQUARE_SIZE = MAP_ITEM_ROOM_SIZES+MAP_ITEM_DOOR_SIZES;

    public final Event onLoadWorld = new WorldJoinEvent(null, params -> {
        MapParser.MAP_PLAYERS.clear();
        return false;
    });

    public static MapState getMapState() {
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            if (C.p().getInventory().getStack(i).getItem() == Items.FILLED_MAP) {
                return FilledMapItem.getMapState(C.p().getInventory().getStack(i), C.w());
            }
        }
        return null;
    }

    private static Vector2f getMapIndent() {
        return new Vector2f(MAP_ITEM_DIAMETER).sub(DungeonScanner.getDungeonRect().mul(MAP_SQUARE_SIZE));
    }

    public static int pos3dTo2d(double pos, double width, double maxWidth) {
        double multi = width / maxWidth;
        return (int) ((pos + (DungeonScanner.DUNGEON_BLOCKS_OFFSET+DungeonScanner.MAX_DUNGEON_DIAMETER)) * multi);
    }

    protected static void parseMap() {
        MapState mapState = getMapState();
        if (mapState == null) return;

        Vector2f mapIndent = getMapIndent();
        if (!hasCalculatedSize()) return;

        for (Room room : DungeonScanner.ROOMS) {
            BlockPos chunkCenter = DungeonScanner.getChunkCenter(DungeonScanner.getTopRoomChunk(room));

            int roomX = (int) (pos3dTo2d(chunkCenter.getX(), MAP_ITEM_DIAMETER - mapIndent.x, DungeonScanner.CURRENT_DUNGEON_DIAMETER.x)+mapIndent.x/2);
            int roomY = (int) (pos3dTo2d(chunkCenter.getZ(), MAP_ITEM_DIAMETER - mapIndent.y, DungeonScanner.CURRENT_DUNGEON_DIAMETER.y)+mapIndent.y/2);

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

    // if no door is missing a room to the bottom right, then the dungeon is fully scanned, otherwise, there is still more to come
    private static boolean hasCalculatedSize() {
        if (DungeonScanner.CURRENT_DUNGEON_DIAMETER.x == DungeonScanner.MAX_DUNGEON_DIAMETER && DungeonScanner.CURRENT_DUNGEON_DIAMETER.y == DungeonScanner.MAX_DUNGEON_DIAMETER) return true;

        for (int x : DungeonScanner.MAP_DIVISIONS.keySet()) {
            for (int z : DungeonScanner.MAP_DIVISIONS.get(x).keySet()) {
                if (DungeonScanner.MAP_DIVISIONS.get(x).get(z).doorType() == DungeonScanner.DoorType.WALL) continue;

                boolean found = false;
                for (Room room : DungeonScanner.ROOMS) {
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

    public static HashMap<String, HashMap<PlayerEntity, PlayerIcon>> MAP_PLAYERS = new HashMap<>();
    public record PlayerIcon(int score, Identifier skin) {}

    // todo: im sure there is a way to get the player 100% correct, but i couldnt find anything in the mapstate class.
    public static void getPotentialPlayersFromMapDecoration(String id, Vector2f pos) {
        for (PlayerEntity player : TargetUtil.getPlayers()) {
            int x = MapParser.pos3dTo2d(player.getX(), MapParser.MAP_ITEM_DIAMETER, DungeonScanner.CURRENT_DUNGEON_DIAMETER.x);
            int z = MapParser.pos3dTo2d(player.getZ(), MapParser.MAP_ITEM_DIAMETER, DungeonScanner.CURRENT_DUNGEON_DIAMETER.y);

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

    public static MapParser.PlayerIcon getBestGuessPlayer(String id) {
        MapParser.PlayerIcon defaultPlayer = new MapParser.PlayerIcon(0, Render2dUtil.getPlayerSkin(C.p()));
        if (!MapParser.MAP_PLAYERS.containsKey(id)) return defaultPlayer;

        return MapParser.MAP_PLAYERS.get(id).entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().score()))
                .map(Map.Entry::getValue)
                .orElse(defaultPlayer);
    }
}