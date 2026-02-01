package com.scale.scalehackv4.screens.clickgui.components;

import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.submodules.BooleanRenderer;
import com.scale.scalehackv4.screens.clickgui.components.submodules.ColourRenderer;
import com.scale.scalehackv4.screens.clickgui.components.submodules.EnumRenderer;
import com.scale.scalehackv4.screens.clickgui.components.submodules.SliderRenderer;
import org.joml.Matrix3x2fStack;

import java.awt.*;

public abstract class SubModuleRenderer {
    public static final int HEIGHT = 10;

    public static final int LEFT_PADDING = 10;
    public static final int MIDDLE_PADDING = 10;

    public static int getMiddleWidth(SubModule module) {
        return (int) (module.getParentModule().width / 1.7f);
    }

    public static int getRightPadding(SubModule module) {
        return module.getParentModule().width - LEFT_PADDING - MIDDLE_PADDING - getMiddleWidth(module) - 4;
    }

    public static SubModule activeSubModule;

    public static void handle(SubModule subModule, Matrix3x2fStack matrices) {
        SubModuleRenderer renderer = getRenderer(subModule);

        renderer.render(subModule, matrices);
        if (ClickGuiScreen.mousePressed) ClickGuiScreen.mousePressed = !renderer.handleClick(subModule);
        if (ClickGuiScreen.leftMouseDown) ClickGuiScreen.leftMouseDown = !renderer.handleMouseDown(subModule);
        if (ClickGuiScreen.mouseReleased) renderer.handleMouseRelease(subModule);

        matrices.translate(0, renderer.getHeight());
    }

    public abstract void render(SubModule subModule, Matrix3x2fStack matrices);

    /// return whether to cancel the clicking for other modules drawn below.
    public abstract boolean handleClick(SubModule subModule);
    public abstract boolean handleMouseDown(SubModule subModule);
    public void handleMouseRelease(SubModule subModule) {}

    public int getHeight() {
        return HEIGHT;
    }

    public static int getHeight(SubModule subModule) {
        return getRenderer(subModule).getHeight();
    }

    private static final SubModuleRenderer BOOLEAN_RENDERER = new BooleanRenderer();
    private static final SubModuleRenderer COLOUR_RENDERER = new ColourRenderer();
    private static final SubModuleRenderer ENUM_RENDERER = new EnumRenderer();
    private static final SubModuleRenderer SLIDER_RENDERER = new SliderRenderer();

    public static SubModuleRenderer getRenderer(SubModule subModule) {
        if (subModule.getField().getType() == boolean.class) return BOOLEAN_RENDERER;
        else if (subModule.getField().getType() == Color.class) return COLOUR_RENDERER;
        else if (subModule.getField().getType().isEnum()) return ENUM_RENDERER;
        else return SLIDER_RENDERER;
    }
}
