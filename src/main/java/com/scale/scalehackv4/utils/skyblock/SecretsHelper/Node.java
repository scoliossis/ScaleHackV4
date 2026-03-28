package com.scale.scalehackv4.utils.skyblock.SecretsHelper;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SkyblockItems;
import lombok.AllArgsConstructor;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

@AllArgsConstructor
// maybe add walk/aote node? but pearl is probably enough
public class Node {
    public BlockPos startPos;
    public BlockPos targetPos;
    public Rotation rotation;
    public Type type;

    @AllArgsConstructor
    // todo: move these predicates, idk where yet.
    public enum Type {
        ETHERWARP(e -> SkyblockItems.ETHERWARP.switchToItem(), (rotation, targetPos) -> {
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.yaw, rotation.pitch));
            return true;
        }, 3),
        PEARL(e -> SkyblockItems.PEARL.switchToItem(), (rotation, targetPos) -> {
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, rotation.yaw, rotation.pitch));
            return true;
        }, 2),
        DUNGEON_BREAKER(e -> SkyblockItems.DUNGEON_BREAKER.switchToItem(), (rotation,targetPos) -> {
            Room currentRoom = AreaUtil.getCurrentRoom();
            if (currentRoom == null) return false;

            BlockPos relativeTarget = NodeUtil.getNodePos(targetPos, currentRoom);
            BlockHitResult blockHitResult = WorldUtil.raytrace(C.p().getEyePos(), 4.5, rotation);

            if (blockHitResult.getType() == HitResult.Type.MISS || !blockHitResult.getBlockPos().equals(relativeTarget)) return false;

            C.p().swingHand(Hand.MAIN_HAND);
            PacketUtil.sendSequencedPacket(sequence -> {
                // technically should have, but only effects weird blocks
                //blockState.onBlockBreakStart(this.client.world, pos, this.client.player);

                // in theory you cant move while dungeon breaker node, so using client pos is fine for now.
                C.im().breakBlock(relativeTarget);
                return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, relativeTarget, blockHitResult.getSide(), sequence);
            });
            return true;
        }, 1),
        SECRET(e -> DUNGEON_BREAKER.getItem.test(null), (rotation, targetPos) -> true, 0);

        public final Predicate<?> getItem;
        public final BiPredicate<Rotation, BlockPos> attemptRoute;
        public final int priority;
    }
}
