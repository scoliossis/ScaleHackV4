package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.utils.C;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.List;

public class TargetUtil {
    public static List<AbstractClientPlayerEntity> getPlayers() {
        return C.w().getPlayers();
    }

    public static boolean isValidTarget(Entity target) {
        return true;
    }
}
