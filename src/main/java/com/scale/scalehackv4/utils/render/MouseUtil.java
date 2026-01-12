package com.scale.scalehackv4.utils.render;

import com.scale.scalehackv4.utils.C;

public class MouseUtil {
    public static boolean isMouseInBounds(int x, int y, int width, int height) {
        double mX = C.mc.mouse.getScaledX(C.res());
        double mY = C.mc.mouse.getScaledY(C.res());

        return mX >= x && mX <= x + width && mY >= y && mY <= y + height;
    }
}
