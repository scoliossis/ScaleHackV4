package com.scale.scalehackv4.mixins.render.freelook;

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
public abstract class FreemoveMixin extends AbstractClientPlayerEntity {
    public FreemoveMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow public abstract void tickMovement();

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHEAD(CallbackInfo ci) {
        if (!Freelook.isPositionLocked()) return;

        float up = (C.mc.options.jumpKey.isPressed() ? 1 : 0) + (C.mc.options.sneakKey.isPressed() ? -1 : 0);
        float forward = (C.mc.options.forwardKey.isPressed() ? 1 : 0) + (C.mc.options.backKey.isPressed() ? -1 : 0);
        float strafe = (C.mc.options.leftKey.isPressed() ? 1 : 0) + (C.mc.options.rightKey.isPressed() ? -1 : 0);

        double x = forward * Math.sin(Math.toRadians(-this.getYaw())) + strafe * Math.cos(Math.toRadians(-this.getYaw()));
        double z = forward * Math.cos(Math.toRadians(-this.getYaw())) - strafe * Math.sin(Math.toRadians(-this.getYaw()));

        Vec3d pos = Freelook.renderPos == null ? this.getEntityPos() : Freelook.renderPos;
        Freelook.renderPos = pos.add(x, up, z);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tickTAIL(CallbackInfo ci) {
        if (!Freelook.isPositionLocked()) return;

        Freelook.realPos = this.getEntityPos();
    }
}