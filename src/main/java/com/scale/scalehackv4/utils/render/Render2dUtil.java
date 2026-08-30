package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.impl.client.OptionsModule;
import com.scale.scalehackv4.utils.C;
import lombok.Setter;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;

@RegisterListeners
public class Render2dUtil extends RenderUtil {
    @Setter
    private static Matrix4f lastProjectionMatrix = new Matrix4f();
    protected static Matrix4f lastPosMatrix = new Matrix4f();
    protected static Vec3d cameraPos = new Vec3d(0, 0, 0);

    // this was relatively simple, the tricky part was figuring out that you need "C.mc.gameRenderer.getBasicProjectionMatrix(C.mc.options.getFov().getValue())"
    // i hate you mojang
    public static Vector2i projectPoint(Vec3d pos) {
        int[] viewport = new int[]{0, 0, C.res().getScaledWidth(), C.res().getScaledHeight()};
        Vector3f relativePos = pos.subtract(cameraPos).toVector3f();
        // order actually matters for the multiplication!
        Vector3f screenPos = new Matrix4f(lastProjectionMatrix)
                .mul(lastPosMatrix)
                .project(relativePos, viewport, new Vector3f());

        if (screenPos.z < 0 || screenPos.z > 1
                || screenPos.x < 0 || screenPos.x > C.res().getScaledWidth()
                || screenPos.y < 0 || viewport[3] - (int) screenPos.y > C.res().getScaledHeight()) return null;

        return new Vector2i((int) screenPos.x, viewport[3] - (int) screenPos.y);
    }

    public static Matrix3x2f getMatrices() {
        return new Matrix3x2f(drawContext.getMatrices());
    }

    public static void drawSkeetRect(int x, int y, int width, int height, Color colour) {
        drawRect(x-3, y-4, width+6, height+7, new Color(60, 60, 60));
        drawRect(x-2, y-3, width+4, height+5, new Color(40, 40, 40));
        drawRect(x-1, y-2, width+2, height+3, new Color(60, 60, 60));
        drawRect(x, y, width, height, new Color(22, 22, 22));

        if (colour != null) drawRect(x, y-1, width, 1, colour);
        else drawTexturedRect(Identifier.of("scalehackv4", "clickgui/rainbowbar.png"), x, y-1, width, 1);
    }

    public static void drawRect(int x, int y, int width, int height, Color colour) {
        // as far as im aware, theres no way to set the colour before drawing,
        // minecraft themselves use position_colour in net.minecraft.client.gui.DrawContext.fill
        drawGradient(x, y, width, height, colour, colour, colour, colour);
    }

    public static void drawRect(int x, int y, int width, int height, Color colour, boolean queue) {
        drawGradient(x, y, width, height, colour, colour, colour, colour, queue);
    }

    public static void drawGradientAcross(int x, int y, int width, int height, Color colour1, Color colour2) {
        drawGradient(x, y, width, height, colour1, colour1, colour2, colour2);
    }

    public static void drawGradientDown(int x, int y, int width, int height, Color colour1, Color colour2) {
        drawGradient(x, y, width, height, colour1, colour2, colour2, colour1);
    }

    public static void drawGradientDown(int x, int y, int width, int height, Color colour1, Color colour2, boolean queue) {
        drawGradient(x, y, width, height, colour1, colour2, colour2, colour1, queue);
    }

    public static void drawGradient(int x, int y, int width, int height, Color tl, Color bl, Color br, Color tr) {
        drawGradient(x, y, width, height, tl, bl, br, tr, false);
    }

    public static void drawFakeTransparent(int x, int y, int width, int height, boolean queue) {
        Matrix3x2f matrices = getMatrices();

        Color backgroundColor = new Color(180, 180, 180);

        drawVertexes(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, (vertices) -> {
            vertices.vertex(matrices, x+width, y).color(backgroundColor.getRGB());
            vertices.vertex(matrices, x+width, y+height).color(backgroundColor.getRGB());
            vertices.vertex(matrices, x, y+height).color(backgroundColor.getRGB());
            vertices.vertex(matrices, x, y).color(backgroundColor.getRGB());

            for (int y2 = 0; y2 <= height; y2+=2) {
                for (int x2 = y2 % 4 == 0 ? 0 : 2; x2 <= width; x2+=4) {
                    vertices.vertex(matrices, Math.min(x+x2+2, x+width), Math.min(y+y2  , y+height)).color(backgroundColor.darker().getRGB());
                    vertices.vertex(matrices, Math.min(x+x2+2, x+width), Math.min(y+y2+2, y+height)).color(backgroundColor.darker().getRGB());
                    vertices.vertex(matrices, Math.min(x+x2  , x+width), Math.min(y+y2+2, y+height)).color(backgroundColor.darker().getRGB());
                    vertices.vertex(matrices, Math.min(x+x2  , x+width), Math.min(y+y2  , y+height)).color(backgroundColor.darker().getRGB());
                }
            }

            return true;
        }, queue);
    }

    public static void drawGradient(int x, int y, int width, int height, Color tl, Color bl, Color br, Color tr, boolean queue) {
        Matrix3x2f matrices = getMatrices();

        drawVertexes(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, (vertices) -> {
            vertices.vertex(matrices, x, y).color(tl.getRGB());
            vertices.vertex(matrices, x, y+height).color(bl.getRGB());
            vertices.vertex(matrices, x+width, y+height).color(br.getRGB());
            vertices.vertex(matrices, x+width, y).color(tr.getRGB());

            return true;
        }, queue);
    }

    public static void drawTexturedRect(Identifier image, int x, int y, int width, int height) {
        drawTexturedRect(image, x, y, width, height, Color.WHITE);
    }

    public static void drawTexturedRect(Identifier image, int x, int y, int width, int height, Color color) {
        drawTexturedRect(image, x, y, width, height, color, false);
    }

    public static void drawTexturedRect(Identifier image, int x, int y, int width, int height, Color color, boolean queue) {
        Matrix3x2f matrices = getMatrices();

        drawVertexes(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, getTexture(image), (vertices) -> {
            vertices.vertex(matrices, x, y).texture(0, 0).color(color.getRGB());
            vertices.vertex(matrices, x, y+height).texture(0, 1).color(color.getRGB());
            vertices.vertex(matrices, x+width, y+height).texture(1, 1).color(color.getRGB());
            vertices.vertex(matrices, x+width, y).texture(1, 0).color(color.getRGB());

            return true;
        }, queue);
    }

    public static void drawPoints(int[] xCoords, int[] yCoords, Color color, boolean queue) {
        Matrix3x2f matrices = getMatrices();

        drawVertexes(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_FAN, (vertices) -> {
            for (int i = 0; i < xCoords.length; i++) {
                vertices.vertex(matrices, xCoords[i], yCoords[i]).color(color.getRGB());
            }

            return true;
        }, queue);
    }

    public static Identifier getPlayerSkin(PlayerEntity player) {
        if (C.mc.getNetworkHandler() == null) return null;

        PlayerListEntry playerListEntry = C.mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return null;

        return playerListEntry.getSkinTextures().body().id();
    }

    public static void drawPlayerHead(PlayerEntity player, int x, int y, int width, int height, Color tint, boolean showHat) {
        drawSkinHead(getPlayerSkin(player), x, y, width, height, tint, showHat);
    }

    // https://open.spotify.com/track/722tgOgdIbNe3BEyLnejw4
    public static void drawSkinHead(Identifier skin, int x, int y, int width, int height, Color tint, boolean showHat) {
        Matrix3x2f matrices = getMatrices();

        // yes these depend on the skin...
        int TEXTURE_WIDTH = C.mc.getTextureManager().getTexture(skin).getGlTexture().getWidth(0);
        int TEXTURE_HEIGHT = C.mc.getTextureManager().getTexture(skin).getGlTexture().getHeight(0);

        float HEAD_WIDTH = 8f/TEXTURE_WIDTH;
        float HEAD_HEIGHT = 8f/TEXTURE_HEIGHT;

        float HEAD_Y_OFFSET = 8f/TEXTURE_HEIGHT;
        float BOTTOM_HEAD_LAYER_OFFSET = 8f/TEXTURE_WIDTH;
        float TOP_HEAD_LAYER_OFFSET = 40f/TEXTURE_WIDTH;

        drawVertexes(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, getTexture(skin), (vertices) -> {
            vertices.vertex(matrices, x, y).texture(BOTTOM_HEAD_LAYER_OFFSET, HEAD_Y_OFFSET).color(tint.getRGB());
            vertices.vertex(matrices, x, y+height).texture(BOTTOM_HEAD_LAYER_OFFSET, HEAD_Y_OFFSET+HEAD_HEIGHT).color(tint.getRGB());
            vertices.vertex(matrices, x+width, y+height).texture(BOTTOM_HEAD_LAYER_OFFSET+HEAD_WIDTH, HEAD_Y_OFFSET+HEAD_HEIGHT).color(tint.getRGB());
            vertices.vertex(matrices, x+width, y).texture(BOTTOM_HEAD_LAYER_OFFSET+HEAD_WIDTH, HEAD_Y_OFFSET).color(tint.getRGB());

            if (showHat) {
                vertices.vertex(matrices, x, y).texture(TOP_HEAD_LAYER_OFFSET, HEAD_Y_OFFSET).color(tint.getRGB());
                vertices.vertex(matrices, x, y+height).texture(TOP_HEAD_LAYER_OFFSET, HEAD_Y_OFFSET+HEAD_HEIGHT).color(tint.getRGB());
                vertices.vertex(matrices, x+width, y+height).texture(TOP_HEAD_LAYER_OFFSET+HEAD_WIDTH, HEAD_Y_OFFSET+HEAD_HEIGHT).color(tint.getRGB());
                vertices.vertex(matrices, x+width, y).texture(TOP_HEAD_LAYER_OFFSET+HEAD_WIDTH, HEAD_Y_OFFSET).color(tint.getRGB());
            }

            return true;
        }, false);
    }

    public static ArrayList<Rectangle> blurStack = new ArrayList<>();

    private static int blurOverrideLastTick = -1;

    public static void drawBlur(int x, int y, int width, int height) {
        if (C.mc.currentScreen != null) return;

        x *= C.res().getScaleFactor();
        y *= C.res().getScaleFactor();
        width *= C.res().getScaleFactor();
        height *= C.res().getScaleFactor();

        for (int i = 0; i < 5; i++) blurStack.add(new Rectangle(x, y, width, height));
        C.mc.gameRenderer.renderBlur();

        if (blurOverrideLastTick == -1) {
            SimpleOption<Integer> blurOption = C.mc.options.getMenuBackgroundBlurriness();
            blurOverrideLastTick = blurOption.getValue();
            blurOption.setValue(OptionsModule.blurRadius);
        }
    }

    private final static Event handleRenderTickHead = new RenderTickEvent(null, params -> {
        SimpleOption<Integer> blurOption = C.mc.options.getMenuBackgroundBlurriness();
        if (blurOverrideLastTick != -1) {
            blurOption.setValue(blurOverrideLastTick);
            blurOverrideLastTick = -1;
        }

        return false;
    }, 0);

    public static void handleRenderPass(RenderPass renderPass, int firstVertex, int vertexCount, String id) {
        boolean isBlur = id.contains("blur") && !id.endsWith("0") && !Render2dUtil.blurStack.isEmpty();

        if (isBlur) {
            // doesnt matter which way i pop, because all blurs are the same afaik
            Rectangle blur = Render2dUtil.blurStack.removeLast();
            renderPass.enableScissor(blur.x, blur.y, blur.width, blur.height);
        }
        renderPass.draw(firstVertex, vertexCount);

        if (isBlur) {
            renderPass.disableScissor();
        }
    }
}