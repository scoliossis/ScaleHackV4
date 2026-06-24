package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.client.Targets;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.MathUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    @RegisterSubModule(name = "Nametag Size", min = 10, max = 50, parent = "Nametags")
    public int nametagSize = 20;

    private final Event renderEsp = new RenderWorldEvent(this, params -> {
        Targets.getPlayers(true).forEach(player -> {
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

            if (nametags) {
                if (C.mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) Render3dUtil.getMatrices().scale(-1, 1, 1);

                MutableText playerName = (MutableText) player.getName();
                double playerHealthRatio = (player.getHealth() + player.getAbsorptionAmount()) / player.getMaxHealth();
                // todo: maybe do the colours smoother ig.
                Formatting healthColor = playerHealthRatio > 1 ? Formatting.YELLOW
                        : playerHealthRatio > 0.7 ? Formatting.GREEN
                          : playerHealthRatio > 0.5 ? Formatting.GOLD
                            : playerHealthRatio > 0.3 ? Formatting.RED
                              : Formatting.DARK_RED;
                String playerHealth = MathUtil.roundTo(player.getHealth() + player.getAbsorptionAmount(), 1) +"";
                String playerArmor = (String.valueOf(player.getArmor()));

                MutableText nametag = playerName.withColor(player.getTeamColorValue())
                        .append(Text.literal(" ").formatted(Formatting.GRAY))
                        .append(Text.literal(playerHealth).formatted(healthColor))
                        .append(Text.literal(" ").formatted(Formatting.GRAY))
                        .append(Text.literal(playerArmor).formatted(Formatting.BLUE));

                Render3dUtil.getMatrices().translate(0, squareHeight + FontUtil.getFontHeight(nametagSize, true) + .1, 0);
                Render3dUtil.getMatrices().multiply(new Quaternionf().rotationZYX(0f, 0f, (float) Math.toRadians(C.p().getPitch())));
                FontUtil.drawString(nametag, FontUtil.getStringWidth(nametag.getString(), nametagSize, true) / 2f, 0, 0, nametagSize, Color.WHITE, true);
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
