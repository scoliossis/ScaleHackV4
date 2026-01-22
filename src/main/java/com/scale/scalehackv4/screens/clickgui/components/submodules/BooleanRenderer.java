package com.scale.scalehackv4.screens.clickgui.components.submodules;

import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.SubModuleRenderer;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import org.joml.Matrix3x2fStack;

import java.awt.*;

public class BooleanRenderer extends SubModuleRenderer {
    @Override
    public void render(SubModule subModule, Matrix3x2fStack matrices) {
        // draw box.
        Render2dUtil.drawRect(0, 0, 4, 4, (boolean) subModule.get() ? ClickGuiModule.guiColour : ClickGuiScreen.OUTLINE_COLOUR);

        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.MIDDLE_PADDING, 0);

        FontUtil.drawString(subModule.getAnnotation().name(), 0, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, true);
        matrices.popMatrix();
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        if (MouseUtil.isMouseInBounds(
                0,
                -2,
                SubModuleRenderer.LEFT_PADDING + SubModuleRenderer.MIDDLE_PADDING + SubModuleRenderer.MIDDLE_WIDTH,
                FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE),
                RenderUtil.getDrawContext())
        ) {
            SubModuleRenderer.activeSubModule = subModule;
            subModule.set(!(boolean) subModule.get());
            return true;
        }

        return false;
    }

    @Override
    public boolean handleMouseDown(SubModule subModule) {
        return false;
    }
}
