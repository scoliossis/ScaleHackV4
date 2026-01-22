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
    public static final int LEFT_PADDING = 10;

    public static final int MIDDLE_PADDING = 10;
    public static final int MIDDLE_WIDTH = (ModuleRenderer.MODULE_WIDTH / 7) * 4;

    public static final int RIGHT_PADDING = 30;

    public static SubModule activeSubModule;

    public static void handle(SubModule subModule, Matrix3x2fStack matrices) {
        SubModuleRenderer renderer;

        // todo: make this a map
        if (subModule.getField().getType() == boolean.class) {
            renderer = new BooleanRenderer();
        }
        else if (subModule.getField().getType() == Color.class) {
            renderer = new ColourRenderer();
        }
        else if (subModule.getField().getType().isEnum()) {
            renderer = new EnumRenderer();
        }
        else {
            renderer = new SliderRenderer();
        }

        renderer.render(subModule, matrices);
        if (ClickGuiScreen.mousePressed) ClickGuiScreen.mousePressed = !renderer.handleClick(subModule);
        if (ClickGuiScreen.leftMouseDown) ClickGuiScreen.leftMouseDown = !renderer.handleMouseDown(subModule);
    }

    public abstract void render(SubModule subModule, Matrix3x2fStack matrices);

    /// return whether to cancel the clicking for other modules drawn below.
    public abstract boolean handleClick(SubModule subModule);
    public abstract boolean handleMouseDown(SubModule subModule);
}
