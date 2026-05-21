package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.utils.C;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// ripped 1:1 from https://github.com/scoliossis/ScaleHackOpenSrc/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/BlockTracker.java
// but i added async this time, how swag
@RegisterListeners
public class BlockTracker {
    @Getter
    private static final HashMap<Block, ArrayList<BlockPos>> trackedBlocks = new HashMap<>();
    @Getter
    private static final HashMap<Integer, HashMap<Integer, Integer>> loadedChunks = new HashMap<>();

    // keeps track of how many classes are tracking a block, if chest esp stops tracking, we dont want it to break secret aura
    private static final HashMap<Block, ArrayList<Class<?>>> classesTrackingBlock = new HashMap<>();

    private static final ArrayList<Update> incomingUpdates = new ArrayList<>();
    // You are all the things that are wrong with OOP. It's not the alcohol, or the drugs, or any of the shitty things that happened to you in your career, or when you were a kid. It's you. Okay?
    private record Update(BlockPos pos, List<Block> newBlock, UpdateType updateType) {}
    private enum UpdateType {
        CHUNK_LOAD,
        CHUNK_UNLOAD,
        BLOCK_UPDATE
    }
    private boolean currentlyUpdating = false;

    public final Event onLoadWorld = new WorldJoinEvent(null, params -> {
        for (Block block : trackedBlocks.keySet()) {
            trackedBlocks.get(block).clear();
            incomingUpdates.clear();
        }
        loadedChunks.clear();
        currentlyUpdating = false;

        return false;
    });

    // hope no modules which use the block tracker are time sensitive :pray:
    public final Event onRenderWorld = new PlayerUpdateEvent(null, params -> {
        if (C.w() == null || currentlyUpdating) return false;

        C.ASYNC.execute(() -> {
            Update[] updates = incomingUpdates.toArray(Update[]::new);
            try {
                currentlyUpdating = true;

                for (Update update : updates) {
                    if (update != null) switch (update.updateType) {
                        case CHUNK_LOAD -> loadBlocksFromChunk(update.pos.getX(), update.pos.getZ(), update.newBlock);
                        case CHUNK_UNLOAD ->
                                unloadBlocksFromChunk(update.pos.getX(), update.pos.getZ(), update.newBlock);
                        case BLOCK_UPDATE -> updateBlock(update.pos, update.newBlock.getFirst());
                    }
                    // note to self: removeAll is VERY slow
                    incomingUpdates.remove(update);
                }
            } finally {
                currentlyUpdating = false;
            }
        });

        return false;
    });

    private void loadBlocksFromChunk(int chunkX, int chunkZ, List<Block> blocks) {
        if (trackedBlocks.isEmpty()) return;

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    if (C.w() == null) return;

                    BlockPos blockPos = new BlockPos(chunkX * 16 + x, y, chunkZ * 16 + z);
                    Block block = C.w().getBlockState(blockPos).getBlock();

                    if (blocks != null && !blocks.contains(block)) continue;

                    ArrayList<BlockPos> alreadyTrackedBlocks = trackedBlocks.get(block);
                    if (alreadyTrackedBlocks != null) alreadyTrackedBlocks.add(blockPos);
                }
            }
        }
    }

    private void unloadBlocksFromChunk(int chunkX, int chunkZ, List<Block> blocks) {
        if (trackedBlocks.isEmpty()) return;

        for (ArrayList<BlockPos> positions : trackedBlocks.values()) {
            for (int i = 0; i < positions.size(); i++) {
                BlockPos pos = positions.get(i);
                if (pos.getX() < chunkX * 16 || pos.getX() >= chunkX * 16 + 16 || pos.getZ() < chunkZ * 16 || pos.getZ() >= chunkZ * 16 + 16) continue;
                if (blocks != null && !blocks.contains(getTrackedBlockAtPos(pos))) continue;

                positions.remove(i);
                i--;
            }
        }
    }

    // this can be done in sync, but chunk loading is async, so ig not
    private void updateBlock(BlockPos pos, Block block) {
        Block prevBlock = getTrackedBlockAtPos(pos);

        if (trackedBlocks.get(prevBlock) != null) {
            trackedBlocks.get(prevBlock).remove(pos);
        }

        if (trackedBlocks.get(block) != null) {
            trackedBlocks.get(block).add(pos);
        }
    }

    public static void queueBlock(BlockPos blockPos, Block newBlock) {
        // we need to clone the blockpos, cause sometimes its mutable, thanks minecraft, im actually going to file a complaint.
        // what is wrong with you mojang. why.
        incomingUpdates.add(new Update(new BlockPos(blockPos), List.of(newBlock), UpdateType.BLOCK_UPDATE));
    }

    public static void queueChunk(ChunkPos chunkPos, ArrayList<Block> block, boolean load) {
        incomingUpdates.add(new Update(new BlockPos(chunkPos.x, 0, chunkPos.z), block, load ? UpdateType.CHUNK_LOAD : UpdateType.CHUNK_UNLOAD));
    }

    public static void addLoadedChunk(ChunkPos chunkPos) {
        loadedChunks.putIfAbsent(chunkPos.x, new HashMap<>());
        loadedChunks.get(chunkPos.x).put(chunkPos.z, 0);

        queueChunk(chunkPos, null, true);
    }

    public static void removeLoadedChunk(ChunkPos chunkPos) {
        HashMap<Integer, Integer> chunksAtX = loadedChunks.get(chunkPos.x);
        if (chunksAtX == null) return;

        chunksAtX.remove(chunkPos.z);
        if (chunksAtX.isEmpty()) loadedChunks.remove(chunkPos.x);

        queueChunk(chunkPos, null, false);
    }

    public static void beginTracking(List<Block> blocks, Class<?> clazz) {
        ArrayList<Block> newBlocksToTrack = new ArrayList<>();

        for (Block b : blocks) {
            classesTrackingBlock.computeIfAbsent(b, k -> new ArrayList<>());
            classesTrackingBlock.get(b).add(clazz);

            if (classesTrackingBlock.get(b).size() != 1) continue;

            newBlocksToTrack.add(b);
            trackedBlocks.put(b, new ArrayList<>());
        }

        if (newBlocksToTrack.isEmpty()) return;

        // we gotta update it all :sob:
        for (int chunkX : loadedChunks.keySet()) {
            for (int chunkZ : loadedChunks.get(chunkX).keySet()) {
                queueChunk(new ChunkPos(chunkX, chunkZ), newBlocksToTrack, true);
            }
        }
    }

    public static void stopTracking(List<Block> blocks, Class<?> clazz) {
        ArrayList<Block> blocksToStopTracking = new ArrayList<>();

        for (Block b : blocks) {
            ArrayList<Class<?>> classesTracking = classesTrackingBlock.get(b);
            if (classesTracking == null || !classesTracking.contains(clazz)) continue;

            classesTracking.remove(clazz);

            if (classesTracking.isEmpty()) {
                blocksToStopTracking.add(b);
                classesTrackingBlock.remove(b);
                trackedBlocks.remove(b);
            }
        }

        if (blocksToStopTracking.isEmpty()) return;

        // we are done. its over. dont ever pregnant man emoji react me ever again 🫃
        for (int chunkX : loadedChunks.keySet()) {
            for (int chunkZ : loadedChunks.get(chunkX).keySet()) {
                queueChunk(new ChunkPos(chunkX, chunkZ), blocksToStopTracking, false);
            }
        }
    }

    public static BlockPos[] getBlockPositions(Block block) {
        ArrayList<BlockPos> positions = trackedBlocks.get(block);
        return positions == null ? new BlockPos[]{} : positions.toArray(BlockPos[]::new);
    }

    public static Block getTrackedBlockAtPos(BlockPos pos) {
        for (Block block : trackedBlocks.keySet()) {
            if (trackedBlocks.get(block).contains(pos)) return block;
        }

        return null;
    }
}
