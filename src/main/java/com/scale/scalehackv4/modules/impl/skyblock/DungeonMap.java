package com.scale.scalehackv4.modules.impl.skyblock;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

@RegisterModule(
        name = "Dungeon Map",
        category = Category.SKYBLOCK
)
public class DungeonMap extends Module {
    private static final HashMap<Integer, HashMap<Integer, Boolean>> UNSCANNED_CHUNKS = new HashMap<>();
    private static final ArrayList<MapDivision> MAP_DIVISIONS = new ArrayList<>();

    private static final int MAX_DUNGEON_DIAMETER = 190;
    private static final int MAX_DUNGEON_CHUNKS = Math.floorDiv(MAX_DUNGEON_DIAMETER, 16);
    private static final int DUNGEON_BLOCKS_OFFSET = 10;
    private static final int DUNGEON_CHUNK_OFFSET = Math.ceilDiv(DUNGEON_BLOCKS_OFFSET, 16);

    private record MapDivision(ChunkPos chunkPos, Direction.Axis axis) {}

    private static boolean wasInDungeon = false;

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

        getSplits();

        return false;
    }));

    private final Event debugScanning3D = new RenderWorldEvent(this, (params -> {
        for (MapDivision chunkPos : MAP_DIVISIONS) {
            BlockPos chunkCenter = getChunkCenter(chunkPos.chunkPos());
            Render3dUtil.drawBox(chunkCenter.getX(), chunkCenter.getY(), chunkCenter.getZ(), 1, 1, 1, new Color(223, 0, 255, 100), false);
        }

        return false;
    }));

    private static final int DUNGEON_MAP_DIAMETER = 100;

    public final Draggable dungeonMap = new Draggable(
            "Dungeon Map",
            () -> {
                Render2dUtil.drawRect(0, 0, DUNGEON_MAP_DIAMETER, DUNGEON_MAP_DIAMETER, new Color(169, 104, 5, 255));
                for (MapDivision mapDivision : MAP_DIVISIONS) {
                    BlockPos chunkCenter = getChunkCenter(mapDivision.chunkPos());

                    int width = mapDivision.axis == Direction.Axis.X ? 1 : 17;
                    int height = mapDivision.axis == Direction.Axis.Z ? 1 : 17;

                    int x = pos3dTo2d(chunkCenter.getX()) - width / 2;
                    int y = pos3dTo2d(chunkCenter.getZ()) - height / 2;

                    Render2dUtil.drawRect(x, y, width, height, new Color(255, 255, 255, 255));
                }

                int playerX = pos3dTo2d(C.p().getX());
                int playerZ = pos3dTo2d(C.p().getZ());

                Render2dUtil.drawRect(playerX-3, playerZ-3, 6, 6, new Color(120, 245, 120, 255));

                return new int[] {DUNGEON_MAP_DIAMETER, DUNGEON_MAP_DIAMETER};
            },
            e -> ModuleUtil.isEnabled(DungeonMap.class) && AreaUtil.isInDungeons(),
            e -> true
    );

    private static BlockPos getChunkCenter(ChunkPos chunkPos) {
        return new BlockPos((chunkPos.x * 16) + 7, 80, (chunkPos.z * 16) + 7);
    }

    private static int pos3dTo2d(double pos) {
        double multi = (double) DUNGEON_MAP_DIAMETER /MAX_DUNGEON_DIAMETER;
        return (int) ((pos + (DUNGEON_BLOCKS_OFFSET+DUNGEON_MAP_DIAMETER)) * multi);
    }

    private static void getSplits() {
        int[] xValues = UNSCANNED_CHUNKS.keySet().stream().mapToInt(Integer::intValue).toArray();

        for (int x : xValues) {
            int[] zValues = UNSCANNED_CHUNKS.get(x).keySet().stream().mapToInt(Integer::intValue).toArray();

            for (int z : zValues) {
                if (canCheckSplit(new ChunkPos(x, z))) {
                    UNSCANNED_CHUNKS.get(x).remove(z);
                    if (UNSCANNED_CHUNKS.get(x).isEmpty()) UNSCANNED_CHUNKS.remove(x);
                }
            }
        }
    }

    private static boolean canCheckSplit(ChunkPos chunkPos) {
        BlockPos chunkCenter = getChunkCenter(chunkPos);

        Block centreBlock = C.w().getBlockState(chunkCenter).getBlock();
        if (centreBlock == Blocks.VOID_AIR) return false;

        if (C.w().getBlockState(chunkCenter).getBlock() == Blocks.AIR) {
            boolean north = C.w().getBlockState(chunkCenter.north()).getBlock() == Blocks.AIR;
            boolean south = C.w().getBlockState(chunkCenter.south()).getBlock() == Blocks.AIR;
            boolean east = C.w().getBlockState(chunkCenter.east()).getBlock() == Blocks.AIR;
            boolean west = C.w().getBlockState(chunkCenter.west()).getBlock() == Blocks.AIR;

            if ((north && south && !east && !west) || (!north && !south && east && west)) {
                MAP_DIVISIONS.add(new MapDivision(chunkPos, north ? Direction.Axis.X : Direction.Axis.Z));
            }
        }

        return true;
    }

    @Override
    public void onEnable() {
        // dungeons spawn with -200 as the lowest x/z and -10 as the highest
        for (int x = -MAX_DUNGEON_CHUNKS; x < -DUNGEON_CHUNK_OFFSET; x++) {
            for (int z = -MAX_DUNGEON_CHUNKS; z < -DUNGEON_CHUNK_OFFSET; z++) {
                UNSCANNED_CHUNKS.computeIfAbsent(x, k -> new HashMap<>()).put(z, true);
            }
        }
    }

    @Override
    public void onDisable() {
        System.out.println("Dungeon map reset");
        wasInDungeon = false;
        UNSCANNED_CHUNKS.clear();
        MAP_DIVISIONS.clear();
    }
}
