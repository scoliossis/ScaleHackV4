package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.MouseTargetBridge;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayerEntity.class)
public abstract class MouseTargetBridgeMixin implements MouseTargetBridge {
    @Shadow
    private static HitResult checkCrosshairTargetRange(HitResult hitResult, Vec3d cameraPos, double range) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    private static HitResult getCrosshairTarget(Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float tickProgress) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Override
    public HitResult bridge$checkCrosshairTargetRange(HitResult hitResult, Vec3d cameraPos, double range) {
        return checkCrosshairTargetRange(hitResult, cameraPos, range);
    }

    @Override
    public HitResult bridge$getCrosshairTarget(Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float tickProgress) {
        return getCrosshairTarget(cameraEntity, blockInteractionRange, entityInteractionRange, tickProgress);
    }
}
