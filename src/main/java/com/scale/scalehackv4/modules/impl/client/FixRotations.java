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
// todo: ig add option to disable showing rotations, but blehhh
//  allow all rotations to be client side
//  maybe allow no fix rotation on interact
public class FixRotations extends Module {
    @RegisterSubModule(name = "Fix Movement")
    public static boolean fixMovement = true;

    public static boolean shouldFixMovement() {
        return fixMovement && ModuleUtil.isEnabled(FixRotations.class);
    }

    private static boolean shouldShowRotation(LivingEntity entity) {
        return ModuleUtil.isEnabled(FixRotations.class) && RotationEvent.clientRotation != null && entity == C.p();
    }

    public static float getPitch(LivingEntity entity, float tickProgress) {
        return shouldShowRotation(entity)
                ? MathHelper.lerpAngleDegrees(tickProgress, RotationEvent.lastPitch, RotationEvent.currentPitch)
                : entity.getPitch(tickProgress);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
