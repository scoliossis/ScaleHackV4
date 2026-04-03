package com.scale.scalehackv4.utils.skyblock.SecretsHelper;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SkyblockItems;
import lombok.AllArgsConstructor;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

@AllArgsConstructor
// maybe add walk/aote node? but pearl is probably enough
public class Node {
    public BlockPos startPos;
    public BlockPos targetPos;
    public Rotation rotation;
    public Type type;
    public boolean firstNode;

    @AllArgsConstructor
    // todo: move these predicates, idk where yet.
    public enum Type {
        ETHERWARP(e -> SkyblockItems.ETHERWARP.switchToItem(), (rotation, targetPos) -> {
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.yaw, rotation.pitch));
            return true;
        }),
        AOTE(e -> SkyblockItems.ETHERWARP.switchToItem(), (rotation, targetPos) -> {
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.yaw, rotation.pitch));
            return true;
        }),
        HYPERION(e -> SkyblockItems.HYPERION.switchToItem(), (rotation, targetPos) -> {
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.yaw, rotation.pitch));
            return true;
        }),
        PEARL(e -> SkyblockItems.PEARL.switchToItem(), (rotation, targetPos) -> {
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.yaw, rotation.pitch));
            return true;
        }),
        SUPERBOOM(e -> SkyblockItems.SUPERBOOM.switchToItem(), (rotation, targetPos) -> {
            Room currentRoom = AreaUtil.getCurrentRoom();
            if (currentRoom == null) return false;

            BlockHitResult blockHitResult = WorldUtil.raytrace(C.p().getEyePos(), 4.5, rotation);
            BlockPos relativeTarget = NodeUtil.getNodePos(targetPos, currentRoom);
            if (blockHitResult.getType() != HitResult.Type.BLOCK || !relativeTarget.equals(blockHitResult.getBlockPos())) return false;

            // handling from net.minecraft.client.MinecraftClient.doItemUse
            ActionResult actionResult = C.im().interactBlock(C.p(), Hand.MAIN_HAND, blockHitResult);
            if (actionResult instanceof ActionResult.Success success) {
                if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
                    C.p().swingHand(Hand.MAIN_HAND);
                    return true;
                }
            }
            return false;
        }),
        DUNGEON_BREAKER(e -> SkyblockItems.DUNGEON_BREAKER.switchToItem(), (rotation,targetPos) -> {
            Room currentRoom = AreaUtil.getCurrentRoom();
            if (currentRoom == null) return false;

            BlockPos relativeTarget = NodeUtil.getNodePos(targetPos, currentRoom);
            BlockState blockState = C.w().getBlockState(relativeTarget);
            VoxelShape voxelShape = blockState.getOutlineShape(C.w(), relativeTarget);
            if (voxelShape.isEmpty()) return false;

            Vec3d targetMiddle = new Vec3d(relativeTarget.getX(), relativeTarget.getY(), relativeTarget.getZ()).add(voxelShape.getBoundingBox().getCenter());
            BlockHitResult blockHitResult = C.w().raycastBlock(
                    C.p().getEyePos(),
                    targetMiddle,
                    relativeTarget,
                    voxelShape,
                    blockState
            );

            if (blockHitResult == null || blockHitResult.getType() == HitResult.Type.MISS || !blockHitResult.getBlockPos().equals(relativeTarget)) return false;

            C.p().swingHand(Hand.MAIN_HAND);
            PacketUtil.sendSequencedPacket(sequence -> {
                // technically should have, but only effects weird blocks
                //blockState.onBlockBreakStart(this.client.world, pos, this.client.player);

                // in theory you cant move while dungeon breaker node, so using client pos is fine for now.
                C.im().breakBlock(relativeTarget);
                return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, relativeTarget, blockHitResult.getSide(), sequence);
            });
            return true;
        }),
        SECRET(e -> DUNGEON_BREAKER.getItem.test(null), (rotation, targetPos) -> true);

        public final Predicate<?> getItem;
        public final BiPredicate<Rotation, BlockPos> attemptRoute;
    }
}
