package com.scale.scalehackv4.mixins.render.freelook;

import com.scale.scalehackv4.modules.impl.render.Freelook;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class FakePlayerSetterMixin {
    @Unique private static Vec3d lastRenderPos = null;
    @Unique private static Vec3d lastRealPos = null;

    @Unique private static Rotation lastRenderRotation = null;
    @Unique private static Rotation lastRealRotation = null;

    @Inject(method = "tick", at = @At(value = "HEAD"))
    public void tickHEAD(CallbackInfo ci) {
        if (Freelook.realPos != null) {
            if (lastRealPos != null) {
                C.p().lastX = lastRealPos.x;
                C.p().lastY = lastRealPos.y;
                C.p().lastZ = lastRealPos.z;
            }
            C.p().setPos(Freelook.realPos.x, Freelook.realPos.y, Freelook.realPos.z);
            lastRealPos = Freelook.realPos;
        }

        if (Freelook.realRotation != null) {
            Freelook.renderRotation = new Rotation(C.p().getYaw(), C.p().getPitch());

            if (lastRealRotation != null) {
                C.p().lastYaw = lastRealRotation.yaw;
                C.p().lastPitch = lastRealRotation.pitch;
            }
            C.p().setYaw(Freelook.realRotation.yaw);
            C.p().setPitch(Freelook.realRotation.pitch);
            lastRealRotation = Freelook.realRotation;
        }
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void tickTAIL(CallbackInfo ci) {
        if (Freelook.renderPos != null) {
            if (lastRenderPos != null) {
                C.p().lastX = C.p().lastRenderX = lastRenderPos.x;
                C.p().lastY = C.p().lastRenderY = lastRenderPos.y;
                C.p().lastZ = C.p().lastRenderZ = lastRenderPos.z;
            }
            C.p().setPos(Freelook.renderPos.x, Freelook.renderPos.y, Freelook.renderPos.z);
            lastRenderPos = Freelook.renderPos;
        }
        else lastRenderPos = null;

        if (Freelook.renderRotation != null) {
            if (lastRenderRotation != null) {
                C.p().lastYaw = C.p().lastRenderYaw = lastRenderRotation.yaw;
                C.p().lastPitch = lastRenderRotation.pitch;
            }

            C.p().renderYaw = Freelook.renderRotation.yaw;
            C.p().setYaw(Freelook.renderRotation.yaw);
            C.p().setPitch(Freelook.renderRotation.pitch);
            lastRenderRotation = Freelook.renderRotation;
        }
        else lastRenderRotation = null;
    }
}