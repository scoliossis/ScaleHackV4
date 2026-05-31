package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.MathUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtil {
    public static Rotation getRotation(Vec3d to) {
        return applyGcd(RotationEvent.getLastRotation(), getRotationNoGcd(RotationEvent.getLastRotation(), C.p().getEyePos(), to));
    }

    public static Rotation getRotation(Rotation currentRotation, Vec3d to) {
        return applyGcd(RotationEvent.getLastRotation(), getRotationNoGcd(currentRotation, C.p().getEyePos(), to));
    }

    public static Rotation getRotation(Rotation currentRotation, Vec3d from, Vec3d to) {
        return applyGcd(currentRotation, getRotationNoGcd(currentRotation, from, to));
    }

    public static Rotation getClientRotation() {
        return new Rotation(C.p().getYaw(), C.p().getPitch());
    }

    // https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/RotationUtil.java#L64
    private static Rotation getRotationNoGcd(Rotation currentRotation, Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float pitch = (float) Math.toDegrees(-Math.atan2(diff.y, dist));
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;

        float neededPitchChange = currentRotation.pitch - pitch;
        float neededYawChange = (currentRotation.yaw % 360) - yaw;

        neededYawChange = Math.abs(neededYawChange) > 180
                ? -1 *
                Math.signum(neededYawChange) *
                (360 - Math.abs(neededYawChange))
                : neededYawChange;

        return new Rotation(
                currentRotation.yaw - neededYawChange,
                currentRotation.pitch - neededPitchChange
        );
    }

    // https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/RotationUtil.java#L25
    public static Rotation applyGcd(Rotation currentRotation, Rotation targetRotation) {
        double f = C.mc.options.getMouseSensitivity().getValue() * (double) 0.6F + (double) 0.2F;
        double sens = (f * f * f) * (double) 8.0F;

        Rotation difference = targetRotation.difference(currentRotation);

        float yawDelta = (float) MathUtil.toNearest(difference.yaw / 0.15d, sens) * 0.15F;
        float pitchDelta = (float) MathUtil.toNearest(difference.pitch / 0.15d, sens) * 0.15F;

        return new Rotation(
                currentRotation.yaw - yawDelta,
                MathHelper.clamp(currentRotation.pitch - pitchDelta, -90, 90)
        );
    }

    // never know when u might need this!
    public static float applyWrap360(float currentYaw, float targetYaw) {
        float delta = targetYaw - currentYaw;

        while (delta <= -180) delta += 360;
        while (delta > 180) delta -= 360;

        return currentYaw + delta;
    }

    public static Rotation getLimitedRotation(Rotation currentRotation, Vec3d toVector, float maxTurnAmount) {
        Rotation to = getRotation(currentRotation, toVector);

        Rotation rotationDifference = to.subtract(currentRotation);
        float pitchDelta = MathHelper.clamp(rotationDifference.pitch, -maxTurnAmount, maxTurnAmount);
        float yawDelta = MathHelper.clamp(rotationDifference.yaw, -maxTurnAmount, maxTurnAmount);

        return applyGcd(currentRotation, new Rotation(yawDelta, pitchDelta).add(currentRotation));
    }

    public static Rotation getSmoothRotation(Rotation currentRotation, Vec3d toVector, float smoothing) {
        Rotation to = getRotation(currentRotation, toVector);

        Rotation rotationDifference = to.subtract(currentRotation);
        float pitchDelta = rotationDifference.pitch / smoothing;
        float yawDelta = rotationDifference.yaw / smoothing;

        return applyGcd(currentRotation, new Rotation(yawDelta, pitchDelta).add(currentRotation));
    }
}
