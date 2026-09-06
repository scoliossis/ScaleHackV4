package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.utils.render.Render2dUtil;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(GameRenderer.class)
public abstract class HookProjectionMatrixMixin {
    @Shadow public abstract Matrix4f getBasicProjectionMatrix(float fovDegrees);
    @Shadow protected abstract float getFov(Camera camera, float tickProgress, boolean changingFov);
    @Shadow @Final private Camera camera;

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getFov(Lnet/minecraft/client/render/Camera;FZ)F", ordinal = 0))
    public void renderWorld(RenderTickCounter renderTickCounter, CallbackInfo ci) {
        // i gotta call "getFOV" to get the fov with the dynamic fov changes and to not conflict with https://github.com/Wurst-Imperium/WI-Zoom/blob/1.21.11/src/main/java/net/wimods/zoom/mixin/GameRendererMixin.java
        float fov = this.getFov(this.camera, renderTickCounter.getTickProgress(true), true);
        Render2dUtil.setLastProjectionMatrix(this.getBasicProjectionMatrix(fov));
    }
}
