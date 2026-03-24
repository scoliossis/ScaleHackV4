package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.utils.C;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@RegisterListeners
public class WorldUtil {
    public static int ticks = 0;

    private final Event countTicks = new RotationEvent(null, params -> {
        ticks++;
        return false;
    });

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
}
