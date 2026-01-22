package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

import java.awt.*;

public class Render2dUtil extends RenderUtil {
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
}
