package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.TargetUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;

@RegisterModule(
        name = "ESP",
        category = Category.RENDER
)
public class EspModule extends Module {
    @RegisterSubModule(name = "Glow")
    public static boolean glow = false;

    @RegisterSubModule(name = "Square")
    public boolean square = true;
    @RegisterSubModule(name = "Width", parent = "Square", min = 0.5, max = 2)
    public float squareWidth = 1;
    @RegisterSubModule(name = "Height", parent = "Square", min = 1.5, max = 2.5)
    public float squareHeight = 2;

    @RegisterSubModule(name = "Nametags")
    public static boolean nametags = true;
    public int nametagSize = 30;

    private final Event renderEsp = new RenderWorldEvent(this, params -> {
        TargetUtil.getPlayers().forEach(player -> {
            if (player == C.p() && C.mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

            float x = (float) (player.lastX + (player.getX() - player.lastX) * C.mc.getRenderTickCounter().getTickProgress(false));
            float y = (float) (player.lastY + (player.getY() - player.lastY) * C.mc.getRenderTickCounter().getTickProgress(false));
            float z = (float) (player.lastZ + (player.getZ() - player.lastZ) * C.mc.getRenderTickCounter().getTickProgress(false));

            float cornerSize = -squareWidth/4;

            Render3dUtil.getMatrices().push();
            Render3dUtil.getMatrices().translate(x, y, z);
            Render3dUtil.getMatrices().multiply(new Quaternionf().rotationZYX(0f, (float) -Math.toRadians(C.p().getYaw()), 0f));

            if (square) {
                drawCorner(squareWidth / 2, squareHeight, cornerSize, cornerSize, player);
                drawCorner(-squareWidth / 2, squareHeight, -cornerSize, cornerSize, player);
                drawCorner(squareWidth / 2, -squareHeight / 2 + player.getHeight() / 2, cornerSize, -cornerSize, player);
                drawCorner(-squareWidth / 2, -squareHeight / 2 + player.getHeight() / 2, -cornerSize, -cornerSize, player);
            }

            nametagSize = (int) (20);
            if (nametags) {
                if (C.mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) Render3dUtil.getMatrices().scale(-1, 1, 1);

                Text playerName = player.getDisplayName() == null ? player.getName() : player.getDisplayName();
                Render3dUtil.getMatrices().translate(0, squareHeight + FontUtil.getFontHeight(nametagSize, true) + .1, 0);
                Render3dUtil.getMatrices().multiply(new Quaternionf().rotationZYX(0f, 0f, (float) Math.toRadians(C.p().getPitch())));
                FontUtil.drawString(playerName, FontUtil.getStringWidth(playerName.getString(), nametagSize, true) / 2f, 0, 0, nametagSize, Color.WHITE, true);
            }

            Render3dUtil.getMatrices().pop();
        });

        return false;
    });

    private void drawCorner(float x, float y, float width, float height, Entity entity) {
        Render3dUtil.drawLine(
                new Vector3f(x, y, 0),
                new Vector3f(x + width, y, 0),
                new Color(entity.getTeamColorValue()),
                false
        );
        Render3dUtil.drawLine(
                new Vector3f(x, y, 0),
                new Vector3f(x, y + height, 0),
                new Color(entity.getTeamColorValue()),
                false
        );
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
