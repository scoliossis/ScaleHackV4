package com.scale.scalehackv4.mixins.render.freelook;

import com.scale.scalehackv4.modules.impl.render.Freelook;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.Rotation;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class FakePlayerSetterMixin {
    @Inject(method = "tick", at = @At(value = "HEAD"))
    public void tickHEAD(CallbackInfo ci) {
        if (C.p() == null) return;

        if (Freelook.realPos != null) {
            if (Freelook.lastRealPos != null) {
                C.p().lastX = Freelook.lastRealPos.x;
                C.p().lastY = Freelook.lastRealPos.y;
                C.p().lastZ = Freelook.lastRealPos.z;
            }
            C.p().setPos(Freelook.realPos.x, Freelook.realPos.y, Freelook.realPos.z);
            Freelook.lastRealPos = Freelook.realPos;
        }

        if (Freelook.realRotation != null) {
            Freelook.renderRotation = new Rotation(C.p().getYaw(), C.p().getPitch());

            if (Freelook.lastRealRotation != null) {
                C.p().lastYaw = Freelook.lastRealRotation.yaw;
                C.p().lastPitch = Freelook.lastRealRotation.pitch;
            }
            C.p().setYaw(Freelook.realRotation.yaw);
            C.p().setPitch(Freelook.realRotation.pitch);
            Freelook.lastRealRotation = Freelook.realRotation;
        }
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void tickTAIL(CallbackInfo ci) {
        if (C.p() == null) return;

        if (Freelook.renderPos != null) {
            if (Freelook.lastRenderPos != null) {
                C.p().lastX = C.p().lastRenderX = Freelook.lastRenderPos.x;
                C.p().lastY = C.p().lastRenderY = Freelook.lastRenderPos.y;
                C.p().lastZ = C.p().lastRenderZ = Freelook.lastRenderPos.z;
            }
            C.p().setPos(Freelook.renderPos.x, Freelook.renderPos.y, Freelook.renderPos.z);
            Freelook.lastRenderPos = Freelook.renderPos;
        }
        else Freelook.lastRenderPos = null;

        if (Freelook.renderRotation != null) {
            if (Freelook.lastRenderRotation != null) {
                C.p().lastYaw = C.p().lastRenderYaw = Freelook.lastRenderRotation.yaw;
                C.p().lastPitch = Freelook.lastRenderRotation.pitch;
            }

            C.p().renderYaw = Freelook.renderRotation.yaw;
            C.p().setYaw(Freelook.renderRotation.yaw);
            C.p().setPitch(Freelook.renderRotation.pitch);
            Freelook.lastRenderRotation = Freelook.renderRotation;
        }
        else Freelook.lastRenderRotation = null;
    }
}