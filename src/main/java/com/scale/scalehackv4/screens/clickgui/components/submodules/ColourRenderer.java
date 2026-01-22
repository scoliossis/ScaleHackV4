package com.scale.scalehackv4.screens.clickgui.components.submodules;

import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.ModuleRenderer;
import com.scale.scalehackv4.screens.clickgui.components.SubModuleRenderer;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2fStack;

import java.awt.*;

public class ColourRenderer extends SubModuleRenderer {
    private static final int SMALL_BOX_HEIGHT = 4;
    private static final int SMALL_BOX_WIDTH = 8;

    private static final int COLOUR_BOX_GAP_SIZE = 3;
    private static final int COLOUR_BOX_DIAMETER = 90;

    private static final int HSL_SLIDER_WIDTH = 10;
    private static final int OPACITY_SLIDER_HEIGHT = 6;

    private static final int BIG_BOX_WIDTH = (COLOUR_BOX_GAP_SIZE*3)+COLOUR_BOX_DIAMETER+HSL_SLIDER_WIDTH;
    private static final int BIG_BOX_HEIGHT = (COLOUR_BOX_GAP_SIZE*3)+COLOUR_BOX_DIAMETER+OPACITY_SLIDER_HEIGHT;

    private static final int COLOUR_BOX_X = SubModuleRenderer.MIDDLE_PADDING+SubModuleRenderer.MIDDLE_WIDTH+SubModuleRenderer.RIGHT_PADDING;

    private static final Rectangle SHOWCASE_SQUARE = new Rectangle(COLOUR_BOX_X, 0, SMALL_BOX_WIDTH, SMALL_BOX_HEIGHT);

    private static final Rectangle POP_UP_SQUARE = new Rectangle(SHOWCASE_SQUARE.x, ModuleRenderer.SUBMODULE_Y_GAP, BIG_BOX_WIDTH, BIG_BOX_HEIGHT);

    private static final  Rectangle COLOUR_SQUARE = new Rectangle(POP_UP_SQUARE.x+COLOUR_BOX_GAP_SIZE, POP_UP_SQUARE.y+COLOUR_BOX_GAP_SIZE, COLOUR_BOX_DIAMETER, COLOUR_BOX_DIAMETER);
    private static final  Rectangle SATURATION_SLIDER_RECT = new Rectangle(COLOUR_SQUARE.x + COLOUR_BOX_DIAMETER + COLOUR_BOX_GAP_SIZE, COLOUR_SQUARE.y, HSL_SLIDER_WIDTH, COLOUR_BOX_DIAMETER);
    private static final  Rectangle OPACITY_SLIDER_RECT = new Rectangle(COLOUR_SQUARE.x, COLOUR_SQUARE.y + COLOUR_SQUARE.height + COLOUR_BOX_GAP_SIZE, COLOUR_BOX_DIAMETER, OPACITY_SLIDER_HEIGHT);

    private static final Color[] HSL_COLOURS = new Color[] {
            new Color(255, 0, 0),
            new Color(255, 0, 255),
            new Color(0, 0, 255),
            new Color(0, 255, 255),
            new Color(0, 255, 0),
            new Color(255, 255, 0),
            new Color(255, 0, 0),
    };

    private static Part selectedPart;
    private enum Part {
        LIGHT, SATURATION, OPACITY
    }

    @Override
    public void render(SubModule subModule, Matrix3x2fStack matrices) {
        Color moduleValue = (Color) subModule.get();

        matrices.pushMatrix();

        FontUtil.drawString(subModule.getAnnotation().name(), SubModuleRenderer.MIDDLE_PADDING, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, true);

        Render2dUtil.drawRect(SHOWCASE_SQUARE.x, SHOWCASE_SQUARE.y, SHOWCASE_SQUARE.width, SHOWCASE_SQUARE.height, moduleValue);

        if (SubModuleRenderer.activeSubModule == subModule) {
            DrawContext drawContext = RenderUtil.getDrawContext();
            ScreenRect scissor = drawContext.scissorStack.peekLast();

            drawContext.disableScissor();
            Render2dUtil.drawRect(POP_UP_SQUARE.x-1, POP_UP_SQUARE.y-1, POP_UP_SQUARE.width+2, POP_UP_SQUARE.height+2, ClickGuiScreen.OUTLINE_COLOUR, true);
            Render2dUtil.drawRect(POP_UP_SQUARE.x, POP_UP_SQUARE.y, POP_UP_SQUARE.width, POP_UP_SQUARE.height, new Color(22, 22, 22), true);

            Render2dUtil.drawGradient(COLOUR_SQUARE.x, COLOUR_SQUARE.y, COLOUR_SQUARE.width, COLOUR_SQUARE.height, Color.WHITE, Color.WHITE, moduleValue, moduleValue, true);
            Render2dUtil.drawGradient(COLOUR_SQUARE.x, COLOUR_SQUARE.y, COLOUR_SQUARE.width, COLOUR_SQUARE.height, new Color(0,0,0,0), Color.BLACK, Color.BLACK, new Color(0,0,0,0), true);

            for (int i = 0; i < HSL_COLOURS.length-1; i++) {
                int h = SATURATION_SLIDER_RECT.height / (HSL_COLOURS.length-1);
                int y = SATURATION_SLIDER_RECT.y + h*i;
                Render2dUtil.drawGradient(SATURATION_SLIDER_RECT.x, y, SATURATION_SLIDER_RECT.width, h, HSL_COLOURS[i], HSL_COLOURS[i+1], HSL_COLOURS[i+1], HSL_COLOURS[i], true);
            }

            Color transparentColour = new Color(moduleValue.getRed(), moduleValue.getGreen(), moduleValue.getBlue(), 0);
            Render2dUtil.drawFakeTransparent(OPACITY_SLIDER_RECT.x, OPACITY_SLIDER_RECT.y, OPACITY_SLIDER_RECT.width, OPACITY_SLIDER_RECT.height, true);
            Render2dUtil.drawGradient(OPACITY_SLIDER_RECT.x, OPACITY_SLIDER_RECT.y, OPACITY_SLIDER_RECT.width, OPACITY_SLIDER_RECT.height, transparentColour, transparentColour, moduleValue, moduleValue, true);

            drawContext.scissorStack.push(scissor);
        }

        matrices.popMatrix();
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        boolean isMouseOverModule = MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING, 1, SubModuleRenderer.MIDDLE_WIDTH+SubModuleRenderer.RIGHT_PADDING+SMALL_BOX_WIDTH+2, SMALL_BOX_HEIGHT+2, RenderUtil.getDrawContext());
        if (SubModuleRenderer.activeSubModule != subModule && isMouseOverModule) {
            SubModuleRenderer.activeSubModule = subModule;

            return true;
        }

        if (SubModuleRenderer.activeSubModule != subModule) return false;

        // we have escaped the box. bad.
        if (!MouseUtil.isMouseInBounds(COLOUR_SQUARE, false)) {
            System.err.println("bad");
            SubModuleRenderer.activeSubModule = null;
            return false;
        }

        if (!MouseUtil.isMouseInBounds(COLOUR_SQUARE, false)) {
            System.err.println("light");
            selectedPart = Part.LIGHT;
        }
        else if (MouseUtil.isMouseInBounds(SATURATION_SLIDER_RECT, false)) {
            System.err.println("satu");
            selectedPart = Part.SATURATION;
        }
        else if (MouseUtil.isMouseInBounds(OPACITY_SLIDER_RECT, false)) {
            System.err.println("op");
            selectedPart = Part.OPACITY;
        }

        return false;
    }

    @Override
    public boolean handleMouseDown(SubModule subModule) {
        if (SubModuleRenderer.activeSubModule != subModule || selectedPart == null) return false;

        switch (selectedPart) {
            case OPACITY -> {

            }
        }

        return false;
    }
}
