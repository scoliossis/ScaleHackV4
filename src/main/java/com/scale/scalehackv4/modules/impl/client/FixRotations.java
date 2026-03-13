package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

@RegisterModule(
        name = "Fix Rotations",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class FixRotations extends Module {
    @RegisterSubModule(name = "Fix Movement")
    public static boolean fixMovement = true;

    @RegisterSubModule(name = "Show Rotation")
    public static boolean showRotation = true;

    public static boolean shouldFixMovement() {
        return fixMovement && ModuleUtil.isEnabled(FixRotations.class);
    }

    private static boolean shouldShowRotation(LivingEntity entity) {
        return showRotation && ModuleUtil.isEnabled(FixRotations.class) && RotationEvent.lastRotation != null && entity == C.p();
    }

    public static float getYaw(LivingEntity entity, float tickProgress) {
        return shouldShowRotation(entity)
                ? MathHelper.lerpAngleDegrees(tickProgress, RotationEvent.lastRotation.yaw, RotationEvent.currentRotation.yaw)
                : MathHelper.lerpAngleDegrees(tickProgress, entity.lastBodyYaw, entity.bodyYaw);
    }

    public static float getHeadYaw(LivingEntity entity, float tickProgress) {
        return (shouldShowRotation(entity)
                ? MathHelper.lerpAngleDegrees(tickProgress, RotationEvent.lastHeadYaw, RotationEvent.headYaw)
                : MathHelper.lerpAngleDegrees(tickProgress, entity.lastHeadYaw, entity.headYaw)
        ) - getYaw(entity, tickProgress);
    }

    public static float getPitch(LivingEntity entity, float tickProgress) {
        return shouldShowRotation(entity)
                ? MathHelper.lerpAngleDegrees(tickProgress, RotationEvent.lastRotation.pitch, RotationEvent.currentRotation.pitch)
                : entity.getPitch(tickProgress);
    }

    // LivingEntity.turnHead()
    public static void updateHeadYaw() {
        RotationEvent.lastHeadYaw = RotationEvent.headYaw;

        float bodyYaw = getYaw(C.p(), 1);
        float diff = MathHelper.wrapDegrees(RotationEvent.headYaw - getYaw(C.p(), 1));

        bodyYaw += diff * 0.3F;

        diff = MathHelper.wrapDegrees(RotationEvent.headYaw - bodyYaw);
        diff = MathHelper.clamp(diff, -75F, 75F);

        RotationEvent.headYaw = RotationEvent.headYaw - diff;

        if (diff > 50f) {
            RotationEvent.headYaw += diff * 0.2F;
        }
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
