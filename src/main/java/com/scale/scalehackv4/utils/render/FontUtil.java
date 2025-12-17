package com.scale.scalehackv4.utils.render;

import com.scale.scalehackv4.utils.C;

import java.awt.*;

public class FontUtil {
    public static void drawString(String text, int x, int y, int fontSize, Color colour, boolean shadow) {
        Render2dUtil.drawContext.drawText(C.mc.textRenderer, text, x, y, colour.getRGB(), shadow);
    }

    public static int getStringWidth(String text, int fontSize) {
        return C.mc.textRenderer.getWidth(text);
    }

    public static int getFontHeight(int fontSize) {
        return C.mc.textRenderer.fontHeight;
    }
}
