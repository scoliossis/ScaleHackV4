package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.bridge.MouseTargetBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.utils.C;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@RegisterListeners
public class WorldUtil {
    public static int ticks = 0;

    private final Event countTicks = new RotationEvent(null, params -> {
        ticks++;
        return false;
    }, 0);

    public static boolean isChunkUnloaded(ChunkPos chunkPos) {
        return C.w().getBlockState(new BlockPos(chunkPos.x * 16, 0, chunkPos.z * 16)).getBlock() == Blocks.VOID_AIR;
    }

    public static BlockHitResult raytrace(Vec3d pos, double reach, Rotation rotation) {
        return C.w().raycast(new RaycastContext(
                pos,
                pos.add(fromPitchYaw(rotation.pitch, rotation.yaw).multiply(reach)),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                C.p()
        ));
    }

    // From UnclaimedBloom6's BloomCore (was for etherwarp util, and im like 90% sure this is just a minecraft 1.8.9 built in function)
    public static Vec3d fromPitchYaw(float pitch, float yaw) {
        double f = Math.cos(-yaw * 0.017453292 - Math.PI);
        double f1 = Math.sin(-yaw * 0.017453292 - Math.PI);
        double f2 = -Math.cos(-pitch * 0.017453292);
        double f3 = Math.sin(-pitch * 0.017453292);
        return new Vec3d(f1 * f2, f3, f * f2).normalize();
    }

    // https://github.com/scoliossis/ScaleHackOpenSrc/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/WorldUtil.java#L208
    public static Vec3d getClosestPoint(Box collisionBox) {
        Vec3d eyePos = C.p().getEyePos();

        double posX = eyePos.x;
        double posY = eyePos.y;
        double posZ = eyePos.z;

        if (eyePos.x < collisionBox.minX) posX = collisionBox.minX;
        else if (eyePos.x > collisionBox.maxX) posX = collisionBox.maxX;

        if (eyePos.y < collisionBox.minY) posY = collisionBox.minY;
        else if (eyePos.y > collisionBox.maxY) posY = collisionBox.maxY;

        if (eyePos.z < collisionBox.minZ) posZ = collisionBox.minZ;
        else if (eyePos.z > collisionBox.maxZ) posZ = collisionBox.maxZ;

        return new Vec3d(posX, posY, posZ);
    }

    public static HitResult getMouseOver(Rotation rotation, double entityReach) {
        return getMouseOver(rotation, C.p().getBlockInteractionRange(), entityReach);
    }
    public static HitResult getMouseOver(Rotation rotation, double blockReach, double entityReach) {
        ClientPlayerEntity playerCopy = (ClientPlayerEntity) C.p().getEntity();
        if (playerCopy == null) return null;

        playerCopy.setYaw(rotation.yaw);
        playerCopy.setPitch(rotation.pitch);

        ItemStack itemStack = playerCopy.getActiveOrMainHandStack();
        AttackRangeComponent attackRangeComponent = itemStack.get(DataComponentTypes.ATTACK_RANGE);
        HitResult hitResult = null;
        if (attackRangeComponent != null) {
            hitResult = attackRangeComponent.getHitResult(playerCopy, 1, EntityPredicates.CAN_HIT);
            if (hitResult instanceof BlockHitResult) {
                hitResult = MouseTargetBridge.from(playerCopy).bridge$checkCrosshairTargetRange(hitResult, playerCopy.getCameraPosVec(1), blockReach);
            }
        }

        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            hitResult = MouseTargetBridge.from(playerCopy).bridge$getCrosshairTarget(playerCopy, blockReach, entityReach, 1);
        }

        return hitResult;
    }

    public static BlockPos getClosestVoid(BlockPos startPos, int radius) {
        return BlockPos.findClosest(startPos, radius, 1, (pos) -> {
            BlockPos headPos = pos.up(2);
            return raytrace(headPos.toBottomCenterPos(), headPos.getY(), new Rotation(0, 90)).getType() == HitResult.Type.MISS;
        }).orElse(null);
    }
}
