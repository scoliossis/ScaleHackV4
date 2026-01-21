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

    private static final int BIG_BOX_HEIGHT = (COLOUR_BOX_GAP_SIZE*3)+COLOUR_BOX_DIAMETER+OPACITY_SLIDER_HEIGHT;
    private static final int BIG_BOX_WIDTH = (COLOUR_BOX_GAP_SIZE*3)+COLOUR_BOX_DIAMETER+HSL_SLIDER_WIDTH;

    private static final Color[] HSL_COLOURS = new Color[] {
            new Color(255, 0, 0),
            new Color(255, 0, 255),
            new Color(0, 0, 255),
            new Color(0, 255, 255),
            new Color(0, 255, 0),
            new Color(255, 255, 0),
            new Color(255, 0, 0),
    };

    @Override
    public void render(SubModule subModule, Matrix3x2fStack matrices) {
        Color moduleValue = (Color) subModule.get();

        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.MIDDLE_PADDING, 0);

        FontUtil.drawString(subModule.getAnnotation().name(), 0, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, true);

        matrices.translate(SubModuleRenderer.MIDDLE_WIDTH + SubModuleRenderer.RIGHT_PADDING - SMALL_BOX_WIDTH, 0);
        Render2dUtil.drawRect(0, 0, SMALL_BOX_WIDTH, SMALL_BOX_HEIGHT, moduleValue);

        matrices.translate(0, ModuleRenderer.SUBMODULE_Y_GAP);

        if (SubModuleRenderer.activeSubModule == subModule) {
            DrawContext drawContext = RenderUtil.getDrawContext();
            ScreenRect scissor = drawContext.scissorStack.peekLast();

            drawContext.disableScissor();
            Render2dUtil.drawRect(-1, -1, BIG_BOX_WIDTH+2, BIG_BOX_HEIGHT+2, ClickGuiScreen.OUTLINE_COLOUR, true);
            Render2dUtil.drawRect(0, 0, BIG_BOX_WIDTH, BIG_BOX_HEIGHT, new Color(22, 22, 22), true);

            Render2dUtil.drawGradient(COLOUR_BOX_GAP_SIZE, COLOUR_BOX_GAP_SIZE, COLOUR_BOX_DIAMETER, COLOUR_BOX_DIAMETER, Color.WHITE, Color.WHITE, moduleValue, moduleValue, true);
            Render2dUtil.drawGradient(COLOUR_BOX_GAP_SIZE, COLOUR_BOX_GAP_SIZE, COLOUR_BOX_DIAMETER, COLOUR_BOX_DIAMETER, new Color(0,0,0,0), Color.BLACK, Color.BLACK, new Color(0,0,0,0), true);

            for (int i = 0; i < HSL_COLOURS.length-1; i++) {
                int h = COLOUR_BOX_DIAMETER / (HSL_COLOURS.length-1);
                int y = COLOUR_BOX_GAP_SIZE + h*i;
                Render2dUtil.drawGradient((COLOUR_BOX_GAP_SIZE*2)+COLOUR_BOX_DIAMETER, y, HSL_SLIDER_WIDTH, h, HSL_COLOURS[i], HSL_COLOURS[i+1], HSL_COLOURS[i+1], HSL_COLOURS[i], true);
            }

            Color transparentColour = new Color(moduleValue.getRed(), moduleValue.getGreen(), moduleValue.getBlue(), 0);
            Render2dUtil.drawFakeTransparent(COLOUR_BOX_GAP_SIZE, (COLOUR_BOX_GAP_SIZE*2)+COLOUR_BOX_DIAMETER, COLOUR_BOX_DIAMETER, OPACITY_SLIDER_HEIGHT, true);
            Render2dUtil.drawGradient(COLOUR_BOX_GAP_SIZE, (COLOUR_BOX_GAP_SIZE*2)+COLOUR_BOX_DIAMETER, COLOUR_BOX_DIAMETER, OPACITY_SLIDER_HEIGHT, transparentColour, transparentColour, moduleValue, moduleValue, true);

            drawContext.scissorStack.push(scissor);
        }

        matrices.popMatrix();
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        int colourBoxX = SubModuleRenderer.MIDDLE_PADDING + SubModuleRenderer.MIDDLE_WIDTH + SubModuleRenderer.RIGHT_PADDING - 8;
        if (SubModuleRenderer.activeSubModule != subModule && MouseUtil.isMouseInBounds(colourBoxX, 0, 8, 4, RenderUtil.getDrawContext())) {
            SubModuleRenderer.activeSubModule = subModule;

            return true;
        }

        if (SubModuleRenderer.activeSubModule == subModule && !MouseUtil.isMouseInBounds(colourBoxX, ModuleRenderer.SUBMODULE_Y_GAP, 117, 117, RenderUtil.getDrawContext(), false)) {
            SubModuleRenderer.activeSubModule = null;
        }

        return false;
    }

    @Override
    public boolean handleMouseDown(SubModule subModule) {


        return false;
    }
}
