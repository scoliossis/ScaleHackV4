package com.scale.scalehackv4.mixins.render.Freelook;

import com.scale.scalehackv4.modules.impl.render.Freelook;
import com.scale.scalehackv4.utils.C;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public abstract class RealPlayerRendererMixin {
    @Shadow
    @Final
    private EntityRenderManager entityRenderManager;

    @Shadow
    protected abstract EntityRenderState getAndUpdateRenderState(Entity entity, float tickProgress);

    /* // allows u to add an EXTRA entity, wow.
    @Inject(method = "pushEntityRenders", at = @At("TAIL"))
    private void pushEntityRenders(MatrixStack matrices, WorldRenderState renderStates, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        if (Freelook.realPos == null) return;

        PlayerEntityRenderState playerEntityRenderState = renderStates.entityRenderStates.stream()
                .filter(state -> state.entityType == EntityType.PLAYER && ((PlayerEntityRenderState)state).id == C.p().getId())
                .map(state -> (PlayerEntityRenderState) state)
                .findFirst().orElse(null);

        if (playerEntityRenderState == null) return;

        Vec3d vec3d = Freelook.realPos.subtract(renderStates.cameraRenderState.pos);

        this.entityRenderManager.render(playerEntityRenderState, renderStates.cameraRenderState, vec3d.x, vec3d.y, vec3d.z, matrices, queue);
    }
     */

    // decided just moving the entity made more sense than doubling them
    @Redirect(method = "fillEntityRenderStates", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;isThirdPerson()Z"))
    private boolean fillEntityRenderStates(Camera instance) {
        return Freelook.isPositionLocked() || instance.isThirdPerson();
    }

    @Redirect(method = "fillEntityRenderStates", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;"))
    private EntityRenderState fillEntityRenderStates(WorldRenderer instance, Entity entity, float tickProgress) {
        EntityRenderState entityRenderState = this.getAndUpdateRenderState(entity, tickProgress);

        if (entityRenderState.entityType != EntityType.PLAYER || ((PlayerEntityRenderState)entityRenderState).id != C.p().getId())
            return entityRenderState;

        if (Freelook.realPos != null) {
            Vec3d lerpedPos = MathHelper.lerp(tickProgress, Freelook.lastRealPos == null ? Freelook.realPos : Freelook.lastRealPos, Freelook.realPos);
            entityRenderState.x = lerpedPos.x;
            entityRenderState.y = lerpedPos.y;
            entityRenderState.z = lerpedPos.z;
        }
        if (Freelook.realRotation != null) {
            ((PlayerEntityRenderState) entityRenderState).pitch = Freelook.realRotation.pitch;
        }

        return entityRenderState;
    }

    // stops the culling of chunks, done weirdly to stop conflict with sodium "net.caffeinemc.mods.sodium.mixin.core.render.world.LevelRendererMixin"
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSpectator()Z"))
    private boolean updateCamera(ClientPlayerEntity instance) {
        return instance.isSpectator() || Freelook.isPositionLocked();
    }
}