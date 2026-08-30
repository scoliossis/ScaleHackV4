package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.client.Targets;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.EasingUtil;
import com.scale.scalehackv4.utils.client.MathUtil;
import com.scale.scalehackv4.utils.player.PlayerUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2i;

import java.awt.*;
import java.util.Comparator;
import java.util.HashMap;

@RegisterModule(
        name = "ESP",
        category = Category.RENDER
)
public class EspModule extends Module {
    @RegisterSubModule(name = "Glow")
    public static boolean glow = false;

    @RegisterSubModule(name = "Blur")
    public boolean blurESP = false;
    @RegisterSubModule(name = "Width", parent = "Square", min = 0.5, max = 3, increment = 0.1)
    public float squareWidth = 1;
    @RegisterSubModule(name = "Height", parent = "Square", min = 1.5, max = 3, increment = 0.1)
    public float squareHeight = 2;

    @RegisterSubModule(name = "Nametags")
    public static boolean nametags = true;
    @RegisterSubModule(name = "Nametag Size", min = 5, max = 20, parent = "Nametags")
    public int nametagSize = 7;

    private final HashMap<LivingEntity, Vector2i> nametagPositions  = new HashMap<>();

    private final Event onJoinWorld = new WorldJoinEvent(this, params -> {
        nametagPositions.clear();
        return false;
    });

    private final Event renderEsp2d = new RenderTickEvent(this, params -> {
        Targets.getPlayers(true)
                .stream()
                .sorted(Comparator.comparingDouble(e -> -e.distanceTo(C.p())))
                .forEach(player -> {
            if (player == C.p() && C.mc.options.getPerspective() == Perspective.FIRST_PERSON) return;

            float partialTicks = C.mc.getRenderTickCounter().getTickProgress(false);
            Vec3d playerPos = PlayerUtil.getPlayerRenderPos(player);

            if (blurESP) drawSquareESP(playerPos, partialTicks, params);
            if (nametags) drawNameTag(player, playerPos.add(0, player.getHeight(), 0), params);
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

        Vector2i p1 = Render2dUtil.projectPoint(center.subtract(playerWidth).subtract(playerHeight));
        Vector2i p2 = Render2dUtil.projectPoint(center.subtract(playerWidth).add(playerHeight));
        Vector2i p3 = Render2dUtil.projectPoint(center.add(playerWidth).subtract(playerHeight));
        Vector2i p4 = Render2dUtil.projectPoint(center.add(playerWidth).add(playerHeight));

        if (p1 == null || p2 == null || p3 == null || p4 == null) return;

        int x = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
        int width = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x)) - x;

        int y = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
        int height = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y)) - y;

        Render2dUtil.drawBlur(x, C.res().getScaledHeight() - y - height, width, height);
    }

    private void drawNameTag(PlayerEntity player, Vec3d pos, RenderTickEvent.Params params) {
        Vector2i playerHeadPos2d = Render2dUtil.projectPoint(pos);
        if (playerHeadPos2d == null) {
            nametagPositions.remove(player);
            return;
        }

        nametagPositions
                .compute(player, (k, v) -> (v == null ? playerHeadPos2d : v)
                .add(playerHeadPos2d.sub((v == null ? playerHeadPos2d : v), new Vector2i()).div(5, new Vector2i())));

        float playerHealth = MathUtil.roundTo(player.getHealth() + player.getAbsorptionAmount(), 1);
        double smoothedHealthRatio = EasingUtil.EasingFunctions.Ease_In_Out_Sine
                .ease((playerHealth / player.getMaxHealth()));

        int progressInt = (int) (255 * MathHelper.clamp(smoothedHealthRatio, 0, 1));
        Color healthColor =  new Color(255-progressInt, progressInt, 80);

        MutableText playerText = ((MutableText)player.getName()).withColor(player.getTeamColorValue())
                .append(" ")
                .append(Text.literal(playerHealth+"").withColor(healthColor.getRGB()));

        int rectPadding = 2;
        int armorGap = rectPadding+2;

        int playerMiddleX = nametagPositions.get(player).x;
        int playerMiddleY = nametagPositions.get(player).y;

        int textWidth = FontUtil.getStringWidth(playerText.getString(), nametagSize);
        int textHeight = FontUtil.getFontHeight(nametagSize);

        int SLOT_PIECES = 4;
        int ARMOR_BASE_SIZE = 16;
        float armorScaling = MathUtil.toNearest(1f/ARMOR_BASE_SIZE * nametagSize, 0.5f);
        int armorSize = (int) (ARMOR_BASE_SIZE * armorScaling);

        int maxRowWidth = Math.max(textWidth, (armorSize * SLOT_PIECES + armorGap * (SLOT_PIECES - 1) + 1));

        int rectHeight = textHeight + armorSize + rectPadding;
        int headDiameter = rectHeight;

        int rectWidth = maxRowWidth + headDiameter + rectPadding;

        int rectX = playerMiddleX - rectWidth/2;
        int rectY = playerMiddleY - rectHeight;

        Matrix3x2fStack matrices = RenderUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();
        matrices.translate(rectX, rectY);

        Render2dUtil.drawSkeetRect(-rectPadding, -rectPadding, rectWidth+rectPadding*2, rectHeight+rectPadding*2, null);

        Render2dUtil.drawPlayerHead(player, 0, 0, headDiameter, headDiameter, Color.WHITE, true);

        matrices.translate(headDiameter + rectPadding, 0);
        FontUtil.drawString(playerText, 0, 0, nametagSize, Color.WHITE, false);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

            int xOffset = slot.getEntitySlotId() * (armorSize + armorGap);

            matrices.pushMatrix();
            matrices.translate(xOffset + 1, textHeight + rectPadding - 1);

            Render2dUtil.drawRect(-1, -1, armorSize+2, armorSize+2, new Color(60, 60, 60));
            Render2dUtil.drawRect(0, 0, armorSize, armorSize, new Color(40, 40, 40));

            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                matrices.scale(armorScaling);
                RenderUtil.getDrawContext().drawItem(stack, 0, 0);
            }
            matrices.popMatrix();
        }

        matrices.popMatrix();
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        nametagPositions.clear();
    }
}