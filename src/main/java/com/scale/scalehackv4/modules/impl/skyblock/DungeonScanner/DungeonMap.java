package com.scale.scalehackv4.modules.impl.skyblock.DungeonScanner;

import com.scale.scalehackv4.bridge.MapDecorationsBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.skyblock.FinishedDungeonScanningEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.MapParser;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.awt.*;
import java.util.Map;
import java.util.stream.Collectors;

public class DungeonMap extends DungeonScannerModule {
    @RegisterSubModule(name = "Dungeon Map")
    public static boolean showMap = true;
    @RegisterSubModule(name = "Print Dungeon Info")
    public static boolean printDungeonInfo = true;

    private static final int MAX_DUNGEON_MAP_DIAMETER = 100;
    private static final int ROOM_RADIUS = 7;
    private static final int DOOR_RADIUS = 3;

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
            e -> ModuleUtil.isEnabled(DungeonScannerModule.class) && AreaUtil.isInDungeons() && showMap,
            e -> true
    );

    private final Event onFinishedScanningDungeon = new FinishedDungeonScanningEvent(ModuleUtil.getModule(DungeonScannerModule.class), params -> {
        if (!printDungeonInfo) return false;

        ChatUtil.chat("--------------------------");
        ChatUtil.prefixMessage("&a&lFinished scanning dungeon!");
        ChatUtil.chat("&2" + DungeonScanner.ROOMS.size() + " &arooms found!");
        ChatUtil.chat("&2Puzzles:\n" + DungeonScanner.ROOMS.stream()
                .filter(e -> e.type == Room.Type.PUZZLE)
                .map(e -> "&5 - " + e.name)
                .collect(Collectors.joining("\n"))
        );
        ChatUtil.chat("--------------------------");

        return false;
    });

    private static void drawDoors() {
        for (int x : DungeonScanner.MAP_DIVISIONS.keySet().stream().sorted().toList()) {
            for (int z : DungeonScanner.MAP_DIVISIONS.get(x).keySet().stream().sorted().toList()) {
                DungeonScanner.MapDivision mapDivision = DungeonScanner.MAP_DIVISIONS.get(x).get(z);

                if (mapDivision.doorType() == DungeonScanner.DoorType.WALL) continue;

                BlockPos chunkCenter = DungeonScanner.getChunkCenter(new ChunkPos(x, z));

                int x2d = pos3dTo2d(chunkCenter.getX()) - DOOR_RADIUS;
                int y2d = pos3dTo2d(chunkCenter.getZ()) - DOOR_RADIUS;

                // if the door doesnt connect 2 open rooms, make it greyed out.
                Color doorColour = getOpenedConnections(x, z) == 2 ? mapDivision.doorType().colour : mapDivision.doorType().colour.darker();

                Render2dUtil.drawRect(x2d, y2d, DOOR_RADIUS*2, DOOR_RADIUS*2, doorColour);
            }
        }
    }

    private static int getOpenedConnections(int x, int z) {
        return DungeonScanner.getConnectedRooms(x, z).stream().filter(e -> e.state != Room.State.UNVISITED).toList().size();
    }

    private static void drawRooms() {
        for (int i = 0; i < DungeonScanner.ROOMS.size(); i++) {
            Room room = DungeonScanner.ROOMS.get(i);
            Rectangle highestXY = new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
            for (int j = 0; j < room.squares.length; j++) {
                ChunkPos square = room.squares[j];
                BlockPos chunkCenter = DungeonScanner.getChunkCenter(square);

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
            if (room.nodes.isEmpty() && room.secrets > 0 && (room.type == Room.Type.MOB || room.type == Room.Type.MINI))
                FontUtil.drawString("DO", highestXY.x, highestXY.y+5, 6, Color.cyan, true);
        }
    }

    private static int pos3dTo2d(double pos) {
        return MapParser.pos3dTo2d(pos, MAX_DUNGEON_MAP_DIAMETER, DungeonScanner.MAX_DUNGEON_DIAMETER);
    }

    private static void drawPlayers() {
        MapState mapState = MapParser.getMapState();
        if (mapState != null) {
            Map<String, MapDecoration> decorations = MapDecorationsBridge.from(mapState).bridge$getDecorations();
            if (decorations == null) return;

            for (String id : decorations.keySet()) {
                MapDecoration decoration = decorations.get(id);

                Vector2f pos = new Vector2f(
                        (decoration.x() + 128) / 2f,
                        (decoration.z() + 128) / 2f
                );

                // todo: do this elsewhere
                MapParser.getPotentialPlayersFromMapDecoration(id, pos);
                drawPlayer(id, decoration, pos);
            }
        }
    }

    // todo: on lower floors the heads are slightly offset
    private static void drawPlayer(String id, MapDecoration decoration, Vector2f pos) {
        Matrix3x2fStack matrices = Render2dUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();
        Vector2i mapDiameter = getMapDiameter();
        matrices.translate(
                (pos.x / MapParser.MAP_ITEM_DIAMETER) * mapDiameter.x,
                (pos.y / MapParser.MAP_ITEM_DIAMETER) * mapDiameter.y
        );
        matrices.rotate((float) Math.toRadians((decoration.rotation()*22.5) - 180));
        Render2dUtil.drawSkinHead(MapParser.getBestGuessPlayer(id).skin(), -4, -4, 8, 8, Color.WHITE);
        matrices.popMatrix();
    }

    private static Vector2i getMapDiameter() {
        return new Vector2i(
                (int) ((DungeonScanner.CURRENT_DUNGEON_DIAMETER.x/DungeonScanner.MAX_DUNGEON_DIAMETER) * MAX_DUNGEON_MAP_DIAMETER),
                (int) ((DungeonScanner.CURRENT_DUNGEON_DIAMETER.y/DungeonScanner.MAX_DUNGEON_DIAMETER) * MAX_DUNGEON_MAP_DIAMETER)
        );
    }
}
