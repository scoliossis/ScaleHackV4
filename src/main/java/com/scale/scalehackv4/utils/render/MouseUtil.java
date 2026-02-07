package com.scale.scalehackv4.utils.render;

import com.scale.scalehackv4.utils.C;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

import java.awt.*;

public class MouseUtil {
    public static Vector2f getMousePos() {
        return new Vector2f((float) C.mc.mouse.getScaledX(C.res()), (float) C.mc.mouse.getScaledY(C.res()));
    }

    public static boolean isMouseInBounds(Rectangle rectangle, boolean scissor) {
        return isMouseInBounds(rectangle.x, rectangle.y, rectangle.width, rectangle.height, RenderUtil.getDrawContext(), scissor);
    }

    public static boolean isMouseInBounds(float x, float y, float x2, float y2, Mouse mouse) {
        Vector2f mousePos = getMousePos();
        Rectangle rect = new Rectangle((int) Math.min(x, x2), (int) Math.min(y, y2), (int) Math.max(x, x2), (int) Math.max(y, y2));

        return mousePos.x >= rect.x && mousePos.x <= rect.width && mousePos.y >= rect.y && mousePos.y <= rect.height;
    }

    public static boolean isMouseInBounds(float x, float y, float width, float height) {
        return isMouseInBounds(x, y, x+width, y+height, C.mc.mouse);
    }

    public static boolean isMouseInBounds(float x, float y, float width, float height, DrawContext drawContext) {
        return isMouseInBounds(x, y, width, height, drawContext, true);
    }
    public static boolean isMouseInBounds(float x, float y, float width, float height, boolean scissor) {
        return isMouseInBounds(x, y, width, height, RenderUtil.getDrawContext(), scissor);
    }

    public static boolean isMouseInBounds(float x, float y, float width, float height, DrawContext drawContext, boolean scissor) {
        Matrix3x2fStack matrices = drawContext.getMatrices();

        Vector2f xy = RenderUtil.projectPos(new Vector2f(x, y), matrices);
        Vector2f wh = RenderUtil.projectPos(new Vector2f(x+width, y+height), matrices);

        Vector2f mousePos = getMousePos();
        if (scissor && !drawContext.scissorContains((int) mousePos.x, (int) mousePos.y)) return false;

        return isMouseInBounds(xy.x, xy.y, wh.x, wh.y, C.mc.mouse);
    }

}
