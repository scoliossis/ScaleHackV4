package com.scale.scalehackv4.screens.clickgui;

import com.scale.scalehackv4.bridge.MouseBridge;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.screens.clickgui.components.CategoryRenderer;
import com.scale.scalehackv4.screens.clickgui.components.ModuleRenderer;
import com.scale.scalehackv4.screens.clickgui.components.SubModuleRenderer;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.DraggableRenderer;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.awt.*;

// todo: text options
//  multi select
//  min-max sliders
//  resizing the whole gui
//  show which settings are sub settings
public class ClickGuiScreen extends Screen {
    public ClickGuiScreen() {
        super(Text.of("Click GUI"));
    }

    private static MouseInput lastMouseInput = null;

    public static final int WIDTH = 390;
    public static final int HEIGHT = 340;

    public static final Color OUTLINE_COLOUR = new Color(60, 60, 60);

    public static final int FONT_SIZE = 6;

    public static boolean leftMouseDown, mousePressed, mouseReleased;

    public static double lastMouseScroll = 0;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        MouseInput mouseInput = MouseBridge.from(C.mc.mouse).bridge$getActiveButton();
        leftMouseDown = mouseInput != null && mouseInput.button() == 0;
        mousePressed = mouseInput != null && (lastMouseInput == null || lastMouseInput.button() != 0) && leftMouseDown;
        mouseReleased = mouseInput == null && (lastMouseInput != null && lastMouseInput.button() == 0);

        RenderUtil.setDrawContext(context);

        Matrix3x2fStack matrices = context.getMatrices();

        Rectangle RECTANGLE = new Rectangle((int) ((ClickGuiModule.guiRenderer.x * context.getScaledWindowWidth()) + 2), (int) ((ClickGuiModule.guiRenderer.y * context.getScaledWindowHeight()) + 3), WIDTH, HEIGHT);

        matrices.pushMatrix();
        matrices.translate(RECTANGLE.x, RECTANGLE.y);
        matrices.scale(2f / C.res().getScaleFactor());

        // wow drawing the whole background in one function call, $wag money
        Render2dUtil.drawSkeetRect(0, 0, WIDTH, HEIGHT, null);

        // i distinctly remember sideload saying "wow your gui modularity sucks" so im trying my best to appease him.
        CategoryRenderer.renderSection(matrices);

        ModuleRenderer.renderSection(matrices);

        matrices.popMatrix();

        Render2dUtil.renderQueued();

        lastMouseInput = mouseInput;
        lastMouseScroll = 0;

        if (MouseUtil.isMouseInBounds(RECTANGLE, true)) {
            DraggableRenderer.overrideDrag = true;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        lastMouseScroll = verticalAmount;
        SubModuleRenderer.activeSubModule = null;
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (SubModuleRenderer.activeSubModule != null && SubModuleRenderer.activeSubModule.getAnnotation().hasKeybind()) {
            int key = input.isEscape() ? -1 : input.getKeycode();
            SubModuleRenderer.activeSubModule.setKeybind(key);
            SubModuleRenderer.activeSubModule = null;
            return true;
        }

        return super.keyPressed(input);
    }
}