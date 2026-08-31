package com.scale.scalehackv4.screens.clickgui.components.submodules;

import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.SubModuleRenderer;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

import java.awt.*;
import java.util.Arrays;
import java.util.stream.Collectors;

// todo: SHOOT ME I HATE THIS CODE HOW IS THIS ONLY LIKE A YEAR OLD.
public class EnumRenderer extends SubModuleRenderer {
    private static final int ENUM_GAP = 3;

    private static final int ENUM_Y = 8;
    private static final int ENUM_HEIGHT = 12;

    private static final int MAX_ENUM_VALUES = 6;

    @Override
    public void render(SubModule subModule, Matrix3x2fStack matrices) {
        Enum<?> moduleValue = (Enum<?>) subModule.get();

        DrawContext drawContext = RenderUtil.getDrawContext();
        matrices.pushMatrix();
        int rectWidth = SubModuleRenderer.getMiddleWidth(subModule);

        matrices.translate(SubModuleRenderer.MIDDLE_PADDING, 0);
        FontUtil.drawString(subModule.getAnnotation().name(), 0, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, true);

        int fontHeight = FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE);
        float textY = ENUM_HEIGHT/2f - fontHeight/2f;

        Render2dUtil.drawRect(0, ENUM_Y, rectWidth, ENUM_HEIGHT, new Color(40,40,40));
        if (moduleValue instanceof MultiSelectEnum) {
            drawContext.enableScissor(0, ENUM_Y, rectWidth - 10, C.res().getHeight());

            String enabledEnums = Arrays.stream(moduleValue.getDeclaringClass().getEnumConstants())
                    .filter(e -> MultiSelectUtil.ENABLED_ENUMS.contains(e))
                    .map(e -> fixValueName(e.name()))
                    .collect(Collectors.joining(", "));
            if (enabledEnums.isEmpty()) enabledEnums = "None";
            FontUtil.drawString(enabledEnums, 3, (int) (ENUM_Y+textY), ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

            drawContext.disableScissor();
        }
        else FontUtil.drawString(fixValueName(moduleValue.name()), 3, (int) (ENUM_Y+textY), ClickGuiScreen.FONT_SIZE, Color.WHITE, false);

        matrices.pushMatrix();
        matrices.translate(rectWidth - 10, ENUM_Y+(ENUM_HEIGHT/2f)-1);
        Render2dUtil.drawPoints(new int[] {0, 4, 2}, new int[] {0, 0, 2}, Color.WHITE, false);

        matrices.popMatrix();

        if (SubModuleRenderer.activeSubModule != subModule) {
            matrices.popMatrix();
            return;
        }

        ScreenRect scissor = drawContext.scissorStack.peekLast();

        matrices.translate(0, ENUM_HEIGHT+ENUM_GAP);

        drawContext.disableScissor();

        // we dont wanna show like 37898956798 enum options!
        drawContext.enableScissor(0, ENUM_Y, rectWidth, ENUM_Y + ENUM_HEIGHT * MAX_ENUM_VALUES);

        Enum<?>[] values = moduleValue.getDeclaringClass().getEnumConstants();

        matrices.pushMatrix();
        matrices.translate(0, getScrollOffset(subModule));

        boolean canScroll = values.length > MAX_ENUM_VALUES;
        double possibleScrollRatio = canScroll ? 1 - ((1d / values.length) / (1d / (values.length - MAX_ENUM_VALUES))) : 0;

        for (Enum<?> value : values) {
            boolean hovered = MouseUtil.isMouseInBounds(0, ENUM_Y, rectWidth, ENUM_HEIGHT, false);
            boolean selected = moduleValue instanceof MultiSelectEnum ? MultiSelectUtil.isEnabled(value) : moduleValue == value;

            if (canScroll) {
                // if you are hovering an enum module, we should be good to try scrolling
                if (hovered) {
                    subModule.scrolledAmount -= ClickGuiScreen.lastMouseScroll * possibleScrollRatio * 0.2;
                }
                subModule.scrolledAmount = Math.clamp(subModule.scrolledAmount, 0, 1 - possibleScrollRatio);
            }

            Render2dUtil.drawRect(0, ENUM_Y, rectWidth, ENUM_HEIGHT, hovered ? new Color(20,20,20) : new Color(40,40,40), true);
            FontUtil.drawString(fixValueName(value.name()),  3,  (int)(ENUM_Y+textY), ClickGuiScreen.FONT_SIZE, selected ? ClickGuiModule.guiColour : Color.WHITE, false, true);
            matrices.translate(0, ENUM_HEIGHT);
        }
        matrices.popMatrix();
        if (canScroll) {
            int scrollBarY = ENUM_Y + (int) (subModule.scrolledAmount * (ENUM_HEIGHT * (MAX_ENUM_VALUES - 1)));
            int scrollBarHeight = (int) (ENUM_HEIGHT * MAX_ENUM_VALUES * possibleScrollRatio) + 1;
            Render2dUtil.drawRect(rectWidth-4, ENUM_Y, 4, ENUM_HEIGHT * MAX_ENUM_VALUES, new Color(60, 60, 60), true);
            Render2dUtil.drawRect(rectWidth - 3, scrollBarY+1, 2, scrollBarHeight, new Color(130, 130, 130), true);
        }

        drawContext.disableScissor();
        drawContext.scissorStack.push(scissor);
        matrices.popMatrix();
    }

    private int getScrollOffset(SubModule subModule) {
        int values = ((Enum<?>) subModule.get()).getDeclaringClass().getEnumConstants().length;
        return (int) ((float) -subModule.scrolledAmount * ENUM_HEIGHT * values);
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        int rectWidth = SubModuleRenderer.getMiddleWidth(subModule);

        boolean hoveringEnum = MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING, ENUM_Y, rectWidth, ENUM_HEIGHT, true);
        if (SubModuleRenderer.activeSubModule != subModule && hoveringEnum) {
            SubModuleRenderer.activeSubModule = subModule;

            return true;
        }
        else if (SubModuleRenderer.activeSubModule == subModule) {
            int scroll = getScrollOffset(subModule);
            int rectY = ENUM_Y+ENUM_HEIGHT+ENUM_GAP + scroll;

            float projectedY = RenderUtil.projectPos(new Vector2f(0, rectY), Render2dUtil.getMatrices()).y;
            float projectedH = RenderUtil.projectPos(new Vector2f(0, rectY+ENUM_HEIGHT), Render2dUtil.getMatrices()).y-projectedY;

            int relativeMouseY = (int) (MouseUtil.getMousePos().y - projectedY + scroll - ENUM_GAP);

            if (!MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING, rectY, rectWidth, C.res().getHeight(), RenderUtil.getDrawContext(), false)
                    || relativeMouseY < -2
                    || relativeMouseY >= ENUM_HEIGHT * MAX_ENUM_VALUES) {
                SubModuleRenderer.activeSubModule = null;
                return false;
            }

            int enumOrdinal = (int) ((MouseUtil.getMousePos().y - projectedY) / projectedH);
            Enum<?>[] values = ((Enum<?>) subModule.get()).getDeclaringClass().getEnumConstants();

            if (enumOrdinal < 0 || enumOrdinal >= values.length) {
                SubModuleRenderer.activeSubModule = null;
                return false;
            }

            Enum<?> hoveredValue = values[enumOrdinal];
            if (subModule.get() instanceof MultiSelectEnum) {
                if (MultiSelectUtil.ENABLED_ENUMS.contains(hoveredValue)) MultiSelectUtil.ENABLED_ENUMS.remove(hoveredValue);
                else MultiSelectUtil.ENABLED_ENUMS.add(hoveredValue);
            }
            else {
                subModule.set(hoveredValue);
                SubModuleRenderer.activeSubModule = null;
            }

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
        return ENUM_Y + ENUM_HEIGHT+ENUM_GAP;
    }

    public static String fixValueName(String value) {
        return (value.startsWith("_") ? value.substring(1) : value).replace("_", " ").replace("DOT", ".");
    }
}
