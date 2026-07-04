package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.client.Targets;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.EasingUtil;
import com.scale.scalehackv4.utils.client.MathUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2i;

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
    @RegisterSubModule(name = "Width", parent = "Square", min = 0.5, max = 3, increment = 0.1)
    public float squareWidth = 1;
    @RegisterSubModule(name = "Height", parent = "Square", min = 1.5, max = 3, increment = 0.1)
    public float squareHeight = 2;

    @RegisterSubModule(name = "Blur Radius", parent = "Square", max = 10)
    public int blurRadius = 3;

    @RegisterSubModule(name = "Nametags")
    public static boolean nametags = true;
    @RegisterSubModule(name = "Nametag Size", min = 10, max = 50, parent = "Nametags")
    public int nametagSize = 20;

    private int blurOverrideLastTick = -1;

    private final Event renderEsp2d = new RenderTickEvent(this, params -> {
        SimpleOption<Integer> blurOption = C.mc.options.getMenuBackgroundBlurriness();
        if (blurOverrideLastTick != -1) {
            blurOption.setValue(blurOverrideLastTick);
            blurOverrideLastTick = -1;
        }

        Targets.getPlayers(true).forEach(player -> {
            if (player == C.p() && C.mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

            float partialTicks = C.mc.getRenderTickCounter().getTickProgress(false);
            Vec3d playerPos = player.getLerpedPos(partialTicks);

            if (square) drawSquareESP(playerPos, partialTicks, params);
            if (nametags) drawNameTag(player, playerPos, params);
        });

        return false;
    });

    private void drawSquareESP(Vec3d pos, float partialTicks, RenderTickEvent.Params params) {
        Vec3d center = pos.add(0, 0.9, 0);

        Vec3d playerWidth = new Vec3d(squareWidth / 2, 0, 0)
                .rotateX((float) Math.toRadians(-C.p().getPitch(partialTicks)))
                .rotateY((float) Math.toRadians(-C.p().getYaw(partialTicks)));

        Vec3d playerHeight = new Vec3d(0, squareHeight / 2, 0)
                .rotateX((float) Math.toRadians(-C.p().getPitch(partialTicks)))
                .rotateY((float) Math.toRadians(-C.p().getYaw(partialTicks)));

        Vector2i p1 = Render3dUtil.projectPoint(center.subtract(playerWidth).subtract(playerHeight));
        Vector2i p2 = Render3dUtil.projectPoint(center.subtract(playerWidth).add(playerHeight));
        Vector2i p3 = Render3dUtil.projectPoint(center.add(playerWidth).subtract(playerHeight));
        Vector2i p4 = Render3dUtil.projectPoint(center.add(playerWidth).add(playerHeight));

        if (p1 == null || p2 == null || p3 == null || p4 == null) return;

        int x = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
        int width = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x)) - x;

        int y = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
        int height = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y)) - y;

        params.drawContext.enableScissor(x, y, x + width, y + height);
        //Render2dUtil.drawRect(0, 0, 2000, 2000, new Color(0, 0, 0, 100));
        params.drawContext.disableScissor();

        Render2dUtil.drawBlur(x, C.res().getScaledHeight() - y - height, width, height);
        if (blurOverrideLastTick == -1 && C.mc.currentScreen == null) {
            SimpleOption<Integer> blurOption = C.mc.options.getMenuBackgroundBlurriness();
            blurOverrideLastTick = blurOption.getValue();
            blurOption.setValue(blurRadius);
        }
    }

    private void drawNameTag(LivingEntity player, Vec3d pos, RenderTickEvent.Params params) {
        Vector2i playerHeadPos2d = Render3dUtil.projectPoint(pos.add(0, player.getHeight(), 0));
        if (playerHeadPos2d == null) return;

        MutableText playerName = (MutableText) player.getName();
        double playerHealthRatio = (player.getHealth() + player.getAbsorptionAmount()) / player.getMaxHealth();

        double smoothedHealthRatio = EasingUtil.EasingFunctions.Ease_In_Out_Sine.ease(playerHealthRatio);

        int progressInt = (int) (255 * MathHelper.clamp(smoothedHealthRatio, 0, 1));
        Color healthColor =  new Color(255-progressInt, progressInt, 80);

        String playerHealth = MathUtil.roundTo(player.getHealth() + player.getAbsorptionAmount(), 1) +"";
        String playerArmor = (String.valueOf(player.getArmor()));

        MutableText nametag = playerName.withColor(player.getTeamColorValue())
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(Text.literal(playerHealth).withColor(healthColor.getRGB()))
                .append(Text.literal(" ").formatted(Formatting.GRAY))
                .append(Text.literal(playerArmor).formatted(Formatting.BLUE));

        int fontHeight = FontUtil.getFontHeight(nametagSize);
        float fontWidth = FontUtil.getStringWidth(nametag.getString(), nametagSize);

        int x = playerHeadPos2d.x - (int) (fontWidth / 2);
        int y = playerHeadPos2d.y - fontHeight;
        FontUtil.drawString(nametag, x, y, nametagSize, Color.WHITE, true);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}