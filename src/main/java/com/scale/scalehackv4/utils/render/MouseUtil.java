package com.scale.scalehackv4.utils.render;

import com.scale.scalehackv4.utils.C;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

public class MouseUtil {
    public static Vector2f getMousePos() {
        return new Vector2f((float) C.mc.mouse.getScaledX(C.res()), (float) C.mc.mouse.getScaledY(C.res()));
    }

    public static boolean isMouseInBounds(int x, int y, int x2, int y2, Mouse mouse) {
        Vector2f mousePos = getMousePos();

        return mousePos.x >= x && mousePos.x <= x2 && mousePos.y >= y && mousePos.y <= y2;
    }

    public static boolean isMouseInBounds(int x, int y, int width, int height) {
        return isMouseInBounds(x, y, x+width, y+height, C.mc.mouse);
    }

    public static boolean isMouseInBounds(int x, int y, int width, int height, DrawContext drawContext) {
        Matrix3x2fStack matrices = drawContext.getMatrices();

        Vector2f xy = matrices.transformPosition(new Vector2f(x, y));
        Vector2f wh = matrices.transformPosition(new Vector2f(x+width, y+height));

        Vector2f mousePos = getMousePos();
        if (!drawContext.scissorContains((int) mousePos.x, (int) mousePos.y)) return false;

        return isMouseInBounds((int) xy.x, (int) xy.y, (int) wh.x, (int) wh.y, C.mc.mouse);
    }
}
