package com.scale.scalehackv4.screens.clickgui.components.submodules;

import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.ModuleRenderer;
import com.scale.scalehackv4.screens.clickgui.components.SubModuleRenderer;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

import java.awt.*;

public class SliderRenderer extends SubModuleRenderer {
    @Override
    public void render(SubModule subModule, Matrix3x2fStack matrices) {
        int fontHeight = FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE)-1;
        double value = Double.parseDouble(subModule.get().toString());
        String valueString = "§l" + value;
        int valueWidth = FontUtil.getStringWidth(valueString, ClickGuiScreen.FONT_SIZE);

        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.MIDDLE_PADDING, 0);

        FontUtil.drawString(subModule.getAnnotation().name(), 0, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

        matrices.translate(0, fontHeight);

        FontUtil.drawString("-", -FontUtil.getStringWidth("-", ClickGuiScreen.FONT_SIZE) - 2, -3, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);
        FontUtil.drawString("+", SubModuleRenderer.MIDDLE_WIDTH + 2, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

        double range = subModule.getAnnotation().max() - subModule.getAnnotation().min();
        double percent = (value - subModule.getAnnotation().min()) / range;

        int sliderWidth = (int) (percent * SubModuleRenderer.MIDDLE_WIDTH);

        Render2dUtil.drawRect(sliderWidth, 0, SubModuleRenderer.MIDDLE_WIDTH-sliderWidth, 3, ClickGuiScreen.OUTLINE_COLOUR);
        Render2dUtil.drawRect(0, 0, sliderWidth, 3, ClickGuiScreen.HIGHLIGHT_COLOUR);

        FontUtil.drawString(valueString, Math.max(0, sliderWidth - valueWidth), -1, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

        matrices.popMatrix();

        matrices.translate(0, fontHeight);
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        int sliderHeight = (FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE)/2)+3+(ModuleRenderer.SUBMODULE_Y_GAP/2);
        int sliderY = -sliderHeight/2 + 2;

        int addWidth = FontUtil.getStringWidth("+", ClickGuiScreen.FONT_SIZE);
        int minusWidth = FontUtil.getStringWidth("-", ClickGuiScreen.FONT_SIZE);

        boolean hoveringMinus = MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING-minusWidth - 2, sliderY, minusWidth, sliderHeight, RenderUtil.getDrawContext());
        boolean hoveringAdd = MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING + SubModuleRenderer.MIDDLE_WIDTH + 2, sliderY, addWidth, sliderHeight, RenderUtil.getDrawContext());

        if (hoveringMinus || hoveringAdd) {
            double increment = (subModule.getField().getType() == long.class || subModule.getField().getType() == int.class)
                    ? Math.ceil(subModule.getAnnotation().increment())
                    : subModule.getAnnotation().increment();

            double value = Double.parseDouble(subModule.get().toString());

            subModule.set(value + (hoveringMinus ? -increment : increment));

            return true;
        }

        if (MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING, sliderY, SubModuleRenderer.MIDDLE_WIDTH+1, sliderHeight, RenderUtil.getDrawContext())) {
            SubModuleRenderer.activeSubModule = subModule;
            return true;
        }
        else if (SubModuleRenderer.activeSubModule == subModule) {
            SubModuleRenderer.activeSubModule = null;
        }

        return false;
    }

    @Override
    public boolean handleMouseDown(SubModule subModule) {
        if (SubModuleRenderer.activeSubModule == subModule) {
            double range = subModule.getAnnotation().max() - subModule.getAnnotation().min();
            double mousePos = Math.clamp(MouseUtil.getMousePos().x - RenderUtil.projectPos(new Vector2f(SubModuleRenderer.MIDDLE_PADDING, 0), Render2dUtil.getMatrices()).x, 0, SubModuleRenderer.MIDDLE_WIDTH);
            double percent = mousePos / SubModuleRenderer.MIDDLE_WIDTH;

            subModule.set(subModule.getAnnotation().min() + percent * range);

            return true;
        }

        return false;
    }
}
