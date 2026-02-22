package com.scale.scalehackv4.modules.impl.skyblock.Dungeons;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.awt.*;

public class StarredMobESP extends DungeonScannerModule {
    @RegisterSubModule(name = "Starred Mob ESP")
    public static boolean starredMobESP = true;
    @RegisterSubModule(name = "ESP Colour", parent = "Starred Mob ESP")
    public static Color espColour = new Color(243, 62, 255, 100);

    private final Event drawStarredMobs = new RenderWorldEvent(ModuleUtil.getModule(DungeonScannerModule.class), params -> {
        if (!starredMobESP || !AreaUtil.isInDungeons()) return false;

        float partialTicks = C.mc.getRenderTickCounter().getTickProgress(true);

        for (Entity entity : C.w().getEntities()) {
            boolean isShadowAssassin = entity.getName().getString().contains("Shadow Assassin");
            if (!entity.getName().getString().contains("✯") && !isShadowAssassin) continue;

            float width = 0.8f;
            float height = 2.2f; // wrong height for fels but idc tbh
            Vec3d pos = new Vec3d(entity.lastX, entity.lastY, entity.lastZ).lerp(entity.getEntityPos(), partialTicks).subtract(0, isShadowAssassin ? 0 : height, 0);

            Render3dUtil.getMatrices().push();
            Render3dUtil.getMatrices().translate(pos.x, pos.y, pos.z);
            float yaw = (float) Math.toRadians(180 - MathHelper.lerp(partialTicks, entity.lastYaw, entity.getYaw()));
            Render3dUtil.getMatrices().multiply(new Quaternionf().rotationZYX(0f, yaw, 0f));
            Render3dUtil.drawBox(-width/2, 0, -width/2, width, height, width, espColour, false);
            Render3dUtil.getMatrices().pop();
        }

        return false;
    });
}
