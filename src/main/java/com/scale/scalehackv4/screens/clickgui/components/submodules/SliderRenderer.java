package com.scale.scalehackv4.screens.clickgui.components.submodules;

import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
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
        int rectWidth = SubModuleRenderer.getMiddleWidth(subModule);
        int fontHeight = FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE)-1;
        double value = Double.parseDouble(subModule.get().toString());
        String valueString = "§l" + value;
        int valueWidth = FontUtil.getStringWidth(valueString, ClickGuiScreen.FONT_SIZE);

        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.MIDDLE_PADDING, 0);

        FontUtil.drawString(subModule.getAnnotation().name(), 0, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

        matrices.translate(0, fontHeight);

        FontUtil.drawString("-", -FontUtil.getStringWidth("-", ClickGuiScreen.FONT_SIZE) - 2, -3, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);
        FontUtil.drawString("+", rectWidth + 2, -3, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

        double range = subModule.getAnnotation().max() - subModule.getAnnotation().min();
        double percent = (value - subModule.getAnnotation().min()) / range;

        int sliderWidth = (int) (percent * rectWidth);

        Render2dUtil.drawRect(sliderWidth, 0, rectWidth-sliderWidth, 2, ClickGuiScreen.OUTLINE_COLOUR);
        Render2dUtil.drawRect(0, 0, sliderWidth, 2, ClickGuiModule.guiColour);

        FontUtil.drawString(valueString, Math.clamp(sliderWidth - (valueWidth/2), 0, rectWidth-valueWidth), -1, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

        matrices.popMatrix();
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        int fontHeight = FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE);

        int sliderHeight = (fontHeight/2)+3+(ModuleRenderer.SUBMODULE_Y_GAP/2);
        int sliderY = -sliderHeight/2 + fontHeight + 1;

        if (MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING, sliderY, SubModuleRenderer.getMiddleWidth(subModule)+1, sliderHeight, RenderUtil.getDrawContext())) {
            SubModuleRenderer.activeSubModule = subModule;
            return true;
        }
        else if (SubModuleRenderer.activeSubModule == subModule) {
            SubModuleRenderer.activeSubModule = null;
        }

        int addWidth = FontUtil.getStringWidth("+", ClickGuiScreen.FONT_SIZE);
        int minusWidth = FontUtil.getStringWidth("-", ClickGuiScreen.FONT_SIZE);

        boolean hoveringMinus = MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING-minusWidth - 2, sliderY, minusWidth, sliderHeight, RenderUtil.getDrawContext());
        boolean hoveringAdd = MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING + SubModuleRenderer.getMiddleWidth(subModule) + 2, sliderY, addWidth, sliderHeight, RenderUtil.getDrawContext());

        if (hoveringMinus || hoveringAdd) {
            double increment = (subModule.getField().getType() == long.class || subModule.getField().getType() == int.class)
                    ? Math.ceil(subModule.getAnnotation().increment())
                    : subModule.getAnnotation().increment();

            double value = Double.parseDouble(subModule.get().toString());

            subModule.set(value + (hoveringMinus ? -increment : increment));

            return true;
        }

        return false;
    }

    @Override
    public boolean handleMouseDown(SubModule subModule) {
        if (SubModuleRenderer.activeSubModule == subModule) {
            float projectedX = RenderUtil.projectPos(new Vector2f(SubModuleRenderer.MIDDLE_PADDING, 0), Render2dUtil.getMatrices()).x;
            float projectedW = RenderUtil.projectPos(new Vector2f(SubModuleRenderer.MIDDLE_PADDING+SubModuleRenderer.getMiddleWidth(subModule), 0), Render2dUtil.getMatrices()).x-projectedX;

            double range = subModule.getAnnotation().max() - subModule.getAnnotation().min();
            // percent doesnt need to be clamped cause i handle that in the set method, well done scale.
            double mousePos = MouseUtil.getMousePos().x - projectedX;
            double percent = mousePos / projectedW; // .y is the width, bore

            subModule.set(subModule.getAnnotation().min() + percent * range);

            return true;
        }

        return false;
    }

    @Override
    public void handleMouseRelease(SubModule subModule) {
        if (SubModuleRenderer.activeSubModule == subModule) {
            SubModuleRenderer.activeSubModule = null;
        }
    }

    @Override
    public int getHeight() {
        return HEIGHT + FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE)-1;
    }
}
