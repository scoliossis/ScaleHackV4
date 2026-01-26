package com.scale.scalehackv4.screens.clickgui.components.submodules;

import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.SubModuleRenderer;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

import java.awt.*;

public class EnumRenderer extends SubModuleRenderer {
    private static final int ENUM_GAP = 3;

    private static final Rectangle ENUM_RECTANGLE = new Rectangle(SubModuleRenderer.MIDDLE_PADDING, 8, SubModuleRenderer.MIDDLE_WIDTH, 12);

    @Override
    public void render(SubModule subModule, Matrix3x2fStack matrices) {
        Enum<?> moduleValue = (Enum<?>) subModule.get();

        matrices.pushMatrix();

        FontUtil.drawString(subModule.getAnnotation().name(), ENUM_RECTANGLE.x, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, true);

        int fontHeight = FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE);
        float textY = ENUM_RECTANGLE.height/2f - fontHeight/2f;

        Render2dUtil.drawRect(ENUM_RECTANGLE.x, ENUM_RECTANGLE.y, ENUM_RECTANGLE.width, ENUM_RECTANGLE.height, new Color(40,40,40));
        FontUtil.drawString(moduleValue.name(), ENUM_RECTANGLE.x+3, ENUM_RECTANGLE.y+textY, ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

        matrices.translate(ENUM_RECTANGLE.x+SubModuleRenderer.MIDDLE_WIDTH - 10, ENUM_RECTANGLE.y+(ENUM_RECTANGLE.height/2f)-1);
        Render2dUtil.drawPoints(new int[] {0, 4, 2}, new int[] {0, 0, 2}, Color.WHITE, false);

        matrices.popMatrix();

        if (SubModuleRenderer.activeSubModule != subModule) return;

        DrawContext drawContext = RenderUtil.getDrawContext();
        ScreenRect scissor = drawContext.scissorStack.peekLast();

        matrices.pushMatrix();
        matrices.translate(0, ENUM_RECTANGLE.height+ENUM_GAP);

        drawContext.disableScissor();

        for (Enum<?> value : moduleValue.getDeclaringClass().getEnumConstants()) {
            boolean hovered = MouseUtil.isMouseInBounds(ENUM_RECTANGLE, false);

            Render2dUtil.drawRect(ENUM_RECTANGLE.x, ENUM_RECTANGLE.y, ENUM_RECTANGLE.width, ENUM_RECTANGLE.height, hovered ? new Color(20,20,20) : new Color(40,40,40), true);
            FontUtil.drawString(value.name(),  ENUM_RECTANGLE.x+3,  ENUM_RECTANGLE.y+textY, ClickGuiScreen.FONT_SIZE, moduleValue == value ? ClickGuiModule.guiColour : Color.WHITE, false, true);
            matrices.translate(0, ENUM_RECTANGLE.height);
        }

        drawContext.scissorStack.push(scissor);
        matrices.popMatrix();
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        boolean hoveringEnum = MouseUtil.isMouseInBounds(ENUM_RECTANGLE, true);
        if (SubModuleRenderer.activeSubModule != subModule && hoveringEnum) {
            SubModuleRenderer.activeSubModule = subModule;

            return true;
        }
        else if (SubModuleRenderer.activeSubModule == subModule) {
            SubModuleRenderer.activeSubModule = null;

            int rectY = ENUM_RECTANGLE.y+ENUM_RECTANGLE.height+ENUM_GAP;

            if (!MouseUtil.isMouseInBounds(ENUM_RECTANGLE.x, rectY, ENUM_RECTANGLE.width, C.res().getHeight(), RenderUtil.getDrawContext(), false)) return false;

            float projectedY = RenderUtil.projectPos(new Vector2f(0, rectY), Render2dUtil.getMatrices()).y;
            float projectedH = RenderUtil.projectPos(new Vector2f(0, rectY+ENUM_RECTANGLE.height), Render2dUtil.getMatrices()).y-projectedY;

            int enumOrdinal = (int) ((MouseUtil.getMousePos().y - projectedY) / projectedH);
            Enum<?>[] values = ((Enum<?>) subModule.get()).getDeclaringClass().getEnumConstants();

            if (enumOrdinal < 0 || enumOrdinal >= values.length) return false;

            subModule.set(values[enumOrdinal]);

            return true;
        }

        return false;
    }

    @Override
    public boolean handleMouseDown(SubModule subModule) {
        return false;
    }

    @Override
    public int getHeight() {
        return ENUM_RECTANGLE.y + ENUM_RECTANGLE.height+ENUM_GAP;
    }
}
