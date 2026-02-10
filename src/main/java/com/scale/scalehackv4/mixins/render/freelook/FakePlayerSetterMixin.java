package com.scale.scalehackv4.mixins.render.freelook;

import com.scale.scalehackv4.modules.impl.render.Freelook;
import com.scale.scalehackv4.utils.C;
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

    @Inject(method = "tick", at = @At(value = "HEAD"))
    public void tickHEAD(CallbackInfo ci) {
        if (C.p() == null || Freelook.renderPos == null || Freelook.realPos == null) {
            Freelook.realPos = Freelook.renderPos = lastRenderPos = null;
            return;
        }

        if (lastRealPos != null) {
            C.p().lastX = lastRealPos.x;
            C.p().lastY = lastRealPos.y;
            C.p().lastZ = lastRealPos.z;
        }
        C.p().setPos(Freelook.realPos.x, Freelook.realPos.y, Freelook.realPos.z);
        lastRealPos = Freelook.realPos;
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void tickTAIL(CallbackInfo ci) {
        if (C.p() == null || Freelook.realPos == null || Freelook.renderPos == null) {
            Freelook.realPos = Freelook.renderPos = lastRenderPos = null;
            return;
        }

        if (lastRenderPos != null) {
            C.p().lastX = C.p().lastRenderX = lastRenderPos.x;
            C.p().lastY = C.p().lastRenderY = lastRenderPos.y;
            C.p().lastZ = C.p().lastRenderZ = lastRenderPos.z;
        }

        C.p().setPos(Freelook.renderPos.x, Freelook.renderPos.y, Freelook.renderPos.z);
        lastRenderPos = Freelook.renderPos;
    }
}
