package com.scale.scalehackv4.mixins.render.Freelook;

import com.mojang.authlib.GameProfile;
import com.scale.scalehackv4.modules.impl.render.Freelook;
import com.scale.scalehackv4.utils.C;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class FreeMoveMixin extends AbstractClientPlayerEntity {
    public FreeMoveMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow public abstract void tickMovement();

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHEAD(CallbackInfo ci) {
        if (!Freelook.isPositionLocked()) return;

        double up = (C.mc.options.jumpKey.isPressed() ? Freelook.verticalSpeed : 0) + (C.mc.options.sneakKey.isPressed() ? -Freelook.verticalSpeed : 0);

        double forward = (C.mc.options.forwardKey.isPressed() ? Freelook.horizontalSpeed : 0) + (C.mc.options.backKey.isPressed() ? -Freelook.horizontalSpeed : 0);
        double strafe = (C.mc.options.leftKey.isPressed() ? Freelook.horizontalSpeed : 0) + (C.mc.options.rightKey.isPressed() ? -Freelook.horizontalSpeed : 0);

        float yaw = Freelook.renderRotation == null ? this.getYaw() : Freelook.renderRotation.yaw;

        double x = forward * Math.sin(Math.toRadians(-yaw)) + strafe * Math.cos(Math.toRadians(-yaw));
        double z = forward * Math.cos(Math.toRadians(-yaw)) - strafe * Math.sin(Math.toRadians(-yaw));

        Vec3d pos = Freelook.renderPos == null ? this.getEntityPos() : Freelook.renderPos;
        Freelook.renderPos = pos.add(x, up, z);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tickTAIL(CallbackInfo ci) {
        if (!Freelook.isPositionLocked()) return;

        Freelook.realPos = this.getEntityPos();
    }
}