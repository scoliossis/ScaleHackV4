package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.utils.render.Render2dUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.gizmo.GizmoDrawerImpl;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GizmoDrawerImpl.class)
public class RenderWorldMixin {
    @Inject(method = "draw", at = @At(value = "TAIL"))
    private void renderMain(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CameraRenderState cameraRenderState, Matrix4f posMatrix, CallbackInfo ci) {
        Render2dUtil.setLastPosMatrix(new Matrix4f(posMatrix));
        if (cameraRenderState != null) {
            Render2dUtil.setCameraPos( cameraRenderState.pos);
        }
    }
}
