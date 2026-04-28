package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.TargetUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import org.joml.Quaternionf;

import java.awt.*;

@RegisterModule(
        name = "ESP",
        category = Category.RENDER
)
public class EspModule extends Module {
    @RegisterSubModule(name = "Glow")
    public static boolean glow = false;

    @RegisterSubModule(name = "Square")
    public static boolean square = true;
    @RegisterSubModule(name = "Width", parent = "Square", min = 0.5, max = 2)
    public static float squareWidth = 1;
    @RegisterSubModule(name = "Height", parent = "Square", min = 1.5, max = 2.5)
    public static float squareHeight = 2;

    private final Event renderEsp = new RenderWorldEvent(this, params -> {
        if (!square) return false;

        TargetUtil.getPlayers().forEach(player -> {
            if (player == C.p()) return;

            float x = (float) (player.lastX + (player.getX() - player.lastX) * C.mc.getRenderTickCounter().getTickProgress(false));
            float y = (float) (player.lastY + (player.getY() - player.lastY) * C.mc.getRenderTickCounter().getTickProgress(false));
            float z = (float) (player.lastZ + (player.getZ() - player.lastZ) * C.mc.getRenderTickCounter().getTickProgress(false));

            Render3dUtil.getMatrices().push();
            Render3dUtil.getMatrices().translate(x, y, z);
            Render3dUtil.getMatrices().multiply(new Quaternionf().rotationZYX(0f, (float) -Math.toRadians(C.p().getYaw()), 0f));
            Render3dUtil.drawSquare(-squareWidth/2, -squareHeight/2 + player.getHeight()/2, 0, squareWidth, squareHeight, 0, new Color(player.getTeamColorValue()), false, true);
            Render3dUtil.getMatrices().pop();
        });

        return false;
    });

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
