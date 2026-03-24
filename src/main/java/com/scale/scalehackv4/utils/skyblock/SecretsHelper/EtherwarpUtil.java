package com.scale.scalehackv4.utils.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EntityPose;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

@RegisterListeners
public class EtherwarpUtil {
    public static final double ETHERWARP_EYE_HEIGHT = 1.54;

    public static boolean isEtherwarpItem(ItemStack stack) {
        LoreComponent lore = stack.getComponents().get(DataComponentTypes.LORE);
        if (lore == null) return false;

        for (Text text : lore.lines()) {
            if (text.getString().contains("Ether Transmission")) return true;
        }

        return false;
    }

    public static Vec3d scale(Vec3d v, double scalar) {
        return new Vec3d(v.x * scalar, v.y * scalar, v.z * scalar);
    }

    // list from bloomcore
    private static final List<Block> ETHER_BLOCK_TYPES = List.of(
            Blocks.VOID_AIR,
            Blocks.AIR,
            Blocks.FIRE,
            Blocks.RED_CARPET,
            Blocks.PURPLE_CARPET,
            Blocks.BLUE_CARPET,
            Blocks.GREEN_CARPET,
            Blocks.YELLOW_CARPET,
            Blocks.GRAY_CARPET,
            Blocks.LIGHT_GRAY_CARPET,
            Blocks.MAGENTA_CARPET,
            Blocks.BLACK_CARPET,
            Blocks.BROWN_CARPET,
            Blocks.CYAN_CARPET,
            Blocks.LIME_CARPET,
            Blocks.ORANGE_CARPET,
            Blocks.PINK_CARPET,
            Blocks.WHITE_CARPET,
            Blocks.LIGHT_BLUE_CARPET,
            Blocks.CYAN_CARPET,
            Blocks.SKELETON_SKULL,
            Blocks.SKELETON_WALL_SKULL,
            Blocks.WITHER_SKELETON_SKULL,
            Blocks.WITHER_SKELETON_WALL_SKULL,
            Blocks.PLAYER_HEAD,
            Blocks.PLAYER_WALL_HEAD,
            Blocks.LEVER,
            Blocks.STONE_BUTTON,
            Blocks.BIRCH_BUTTON,
            Blocks.OAK_BUTTON,
            Blocks.SPRUCE_BUTTON,
            Blocks.ACACIA_BUTTON,
            Blocks.DARK_OAK_BUTTON,
            Blocks.TORCH,
            Blocks.TRIPWIRE_HOOK,
            Blocks.TRIPWIRE,
            Blocks.RAIL,
            Blocks.ACTIVATOR_RAIL,
            Blocks.SNOW,
            Blocks.CARROTS,
            Blocks.WHEAT,
            Blocks.POTATOES,
            Blocks.NETHER_WART,
            Blocks.PUMPKIN_STEM,
            Blocks.MELON_STEM,
            Blocks.REDSTONE_TORCH,
            Blocks.REDSTONE_WIRE,
            Blocks.POPPY,
            Blocks.DANDELION,
            Blocks.OAK_SAPLING,
            Blocks.ACACIA_SAPLING,
            Blocks.BIRCH_SAPLING,
            Blocks.DARK_OAK_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.FLOWER_POT,
            Blocks.DEAD_BUSH,
            Blocks.TALL_GRASS,
            Blocks.LADDER,
            Blocks.PEONY,
            Blocks.ROSE_BUSH,
            Blocks.REPEATER,
            Blocks.COMPARATOR,
            Blocks.COBWEB,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.TORCH,
            Blocks.VINE,
            Blocks.BROWN_MUSHROOM,
            Blocks.RED_MUSHROOM,
            Blocks.PISTON_HEAD,
            Blocks.NETHER_PORTAL,
            Blocks.COCOA
    );

    public static boolean isValidEtherwarpBlock(Vec3i pos) {
        BlockPos bp = new BlockPos(pos);

        if(ETHER_BLOCK_TYPES.contains(C.w().getBlockState(bp).getBlock())) return false; // We can't stand on any of these blocks
        if(!ETHER_BLOCK_TYPES.contains(C.w().getBlockState(bp.add(0, 2, 0)).getBlock())) return false; // Check for the block at head height
        return ETHER_BLOCK_TYPES.contains(C.w().getBlockState(bp.add(0, 1, 0)).getBlock()); // Check for the block at feet level
    }

    // thank you 112batman, goat 4eva
    // https://gist.github.com/Nik--/0beb2f5f2b93182e467db86223f6e1d2
    // Also see: https://github.com/cgyurgyik/fast-voxel-traversal-algorithm/blob/master/overview/FastVoxelTraversalOverview.md
    public static Vec3i traverseVoxels(Vec3d start, Vec3d end) {
        int currVoxelX = (int) Math.floor(start.x);
        int currVoxelY = (int) Math.floor(start.y);
        int currVoxelZ = (int) Math.floor(start.z);
        final int lastVoxelX = (int) Math.floor(end.x);
        final int lastVoxelY = (int) Math.floor(end.y);
        final int lastVoxelZ = (int) Math.floor(end.z);
        final double diffX = end.x - start.x;
        final double diffY = end.y - start.y;
        final double diffZ = end.z - start.z;
        final int stepX = (int) Math.signum(diffX);
        final int stepY = (int) Math.signum(diffY);
        final int stepZ = (int) Math.signum(diffZ);

        final double tDeltaX = stepX == 0 ? Double.MAX_VALUE : (stepX / diffX);
        final double tDeltaY = stepY == 0 ? Double.MAX_VALUE : (stepY / diffY);
        final double tDeltaZ = stepZ == 0 ? Double.MAX_VALUE : (stepZ / diffZ);

        double tMaxX = stepX > 0 ? (tDeltaX * frac1(start.x)) : (tDeltaX * frac0(start.x));
        double tMaxY = stepY > 0 ? (tDeltaY * frac1(start.y)) : (tDeltaY * frac0(start.y));
        double tMaxZ = stepZ > 0 ? (tDeltaZ * frac1(start.z)) : (tDeltaZ * frac0(start.z));

        int iterations = Math.abs(lastVoxelX - currVoxelX) + Math.abs(lastVoxelY - currVoxelY) + Math.abs(lastVoxelZ - currVoxelZ);
        while(iterations-- >= 0 && (tMaxX <= 1 || tMaxY <= 1 || tMaxZ <= 1)) {
            if(tMaxX < tMaxY) {
                if(tMaxX < tMaxZ) {
                    currVoxelX += stepX;
                    Vec3i v = new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
                    if(!ETHER_BLOCK_TYPES.contains(C.w().getBlockState(new BlockPos(v)).getBlock())) return v;
                    tMaxX += tDeltaX;
                }else {
                    currVoxelZ += stepZ;
                    Vec3i v = new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
                    if(!ETHER_BLOCK_TYPES.contains(C.w().getBlockState(new BlockPos(v)).getBlock())) return v;
                    tMaxZ += tDeltaZ;
                }
            }else {
                if(tMaxY < tMaxZ) {
                    currVoxelY += stepY;
                    Vec3i v = new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
                    if(!ETHER_BLOCK_TYPES.contains(C.w().getBlockState(new BlockPos(v)).getBlock())) return v;
                    tMaxY += tDeltaY;
                }else {
                    currVoxelZ += stepZ;
                    Vec3i v = new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
                    if(!ETHER_BLOCK_TYPES.contains(C.w().getBlockState(new BlockPos(v)).getBlock())) return v;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return new Vec3i(currVoxelX, currVoxelY, currVoxelZ);
    }

    // i jerk off to this function some times 🤭
    public static Vec3i getEtherwarpBlockSuccess(float pitch, float yaw, Vec3d startPos_, double distance) {
        Vec3d lookVec = scale(WorldUtil.fromPitchYaw(pitch, yaw), distance);
        Vec3d startPos = startPos_.add(0, ETHERWARP_EYE_HEIGHT, 0);
        Vec3d endPos = lookVec.add(startPos);

        Vec3i etherSpot = traverseVoxels(startPos, endPos);
        if(!isValidEtherwarpBlock(etherSpot)) return null;
        return etherSpot;
    }

    public static Vec3d getAotePos(float pitch, float yaw, Vec3d startPos_, double distance) {
        Vec3d lookVec = scale(WorldUtil.fromPitchYaw(pitch, yaw), distance);
        Vec3d startPos = startPos_.add(0, C.p().getEyeHeight(EntityPose.STANDING), 0);
        Vec3d endPos = lookVec.add(startPos);
        Vec3i aoteBlock = traverseVoxels(startPos, endPos);
        // hypixel hates u under these conditions.
        if (Math.abs(aoteBlock.getX() - startPos.x) < 1
                && Math.abs(aoteBlock.getZ() - startPos.z) < 1
                && Math.abs(aoteBlock.getY() - startPos.y) < 1
                && pitch >= 60
        ) {
            // handling nulls is annoying, hypixel FOR NOW sends teleport even if u dont move, but ik thats changing
            return startPos;
        }

        //PREDICTED_POSITIONS.add(new Vec3d(aoteBlock.getX() + 0.5, isValidEtherwarpBlock(aoteBlock) ? aoteBlock.getY()+1 : aoteBlock.getY(), aoteBlock.getZ() + 0.5));
        return new Vec3d(aoteBlock.getX() + 0.5, isValidEtherwarpBlock(aoteBlock) ? aoteBlock.getY()+1 : aoteBlock.getY(), aoteBlock.getZ() + 0.5);
    }

    private static final ArrayList<Vec3d> PREDICTED_POSITIONS = new ArrayList<>();
    private final Event onReceiveTP = new PacketEvent.Received(null, params -> {
        //if (!(params.packet instanceof PlayerPositionLookS2CPacket posPacket) || !C.isInGame() || PREDICTED_POSITIONS.isEmpty()) return false;
        //Vec3d aotePos = PREDICTED_POSITIONS.removeFirst();
        //ChatUtil.prefixMessage("Predicted pos: " + aotePos + " real pos: " + posPacket.change().position());

        return false;
    });

    private static double frac0(double val) {
        return val - Math.floor(val);
    }
    private static double frac1(double val) {
        return 1 - val + Math.floor(val);
    }
}
