package com.scale.scalehackv4.bridge;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public interface MouseTargetBridge {
    static MouseTargetBridge from(Object instance) {
        return (MouseTargetBridge) instance;
    }

    HitResult bridge$checkCrosshairTargetRange(HitResult hitResult, Vec3d cameraPos, double range);
    HitResult bridge$getCrosshairTarget(Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float tickProgress);
}
