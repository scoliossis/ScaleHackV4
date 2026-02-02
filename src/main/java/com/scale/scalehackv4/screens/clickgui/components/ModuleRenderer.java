package com.scale.scalehackv4.screens.clickgui.components;

import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.ModuleManager;
import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.utils.client.MathUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

@RegisterListeners
public class ModuleRenderer {
    public static final int MODULE_X_GAP = 15;
    public static final int MODULE_Y_GAP = 15;

    public static final int MODULES_SECTION_WIDTH = ClickGuiScreen.WIDTH - CategoryRenderer.CATEGORY_DIAMETER - 1;
    public static final int FIRST_SUBMODULE_Y_GAP = 8;
    public static final int SUBMODULE_Y_GAP = 2;

    private static boolean draggingModule = false;
    private static Module activeModule;

    public static void renderSection(Matrix3x2fStack matrices) {
        matrices.translate(0, 0);

        // sort array as left modules top to bottom, then right modules top to bottom
        List<Module> modules = ModuleManager.getModulesByCategory(CategoryRenderer.selectedCategory).stream()
                .sorted(Comparator.comparingInt(m -> m.y))
                .sorted(Comparator.comparingInt(m -> m.x))
                .toList();

        if (ClickGuiScreen.mouseReleased) {
            activeModule = null;
            draggingModule = false;
        }
        ClickGuiScreen.leftMouseDown &= !handleActiveModule();

        for (Module module : modules) {
            matrices.pushMatrix();
            drawModule(module, matrices);
            matrices.popMatrix();
        }
    }

    private static void drawModule(Module module, Matrix3x2fStack matrices) {
        if (ClickGuiScreen.mousePressed) ClickGuiScreen.mousePressed = !handleClick(module);

        // everyone, get ready, positions people!
        matrices.translate(module.x, module.y);

        String moduleName = module.getAnnotation().name();
        int moduleNameWidth = FontUtil.getStringWidth(moduleName, ClickGuiScreen.FONT_SIZE);
        int fontHeight = FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE);
        int titleX = 5;

        int subModulesHeight = module.getChildren().stream()
                .filter(SubModule::isVisible)
                .mapToInt(e -> SUBMODULE_Y_GAP + SubModuleRenderer.getHeight(e)).sum()
                + FIRST_SUBMODULE_Y_GAP + SUBMODULE_Y_GAP;

        float subModuleTranslation = (float) -(subModulesHeight * module.scrolledAmount);

        // outline
        Render2dUtil.drawRect(-1, -1, titleX-1, module.height+2, ClickGuiScreen.OUTLINE_COLOUR);
        Render2dUtil.drawRect(titleX+2+moduleNameWidth, -1, module.width-titleX-1-moduleNameWidth, module.height+2, ClickGuiScreen.OUTLINE_COLOUR);
        Render2dUtil.drawRect(-1, module.height, module.width+2, 1, ClickGuiScreen.OUTLINE_COLOUR);

        // interior crocodile alligator
        Render2dUtil.drawRect(0, 0, module.width, module.height, new Color(22, 22, 22));

        handleSubmodules(module, matrices, subModuleTranslation);

        // top and bottom gradient to hide the cutoff smoothly ig, skeet does it
        Render2dUtil.drawGradientDown(0, 0, module.width, 10, new Color(22, 22, 22), new Color(22, 22, 22, 0));
        Render2dUtil.drawGradientDown(0, module.height-10, module.width, 10, new Color(22, 22, 22, 0), new Color(22, 22, 22));

        // module name
        FontUtil.drawString("§l"+moduleName, titleX, -fontHeight / 2f, ClickGuiScreen.FONT_SIZE, new Color(255, 255, 255), false);

        handleScrollBar(module.height, subModulesHeight, module);
    }

    private static void handleSubmodules(Module module, Matrix3x2fStack matrices, float subModuleTranslation) {
        RenderUtil.getDrawContext().enableScissor(1, 1, module.width, module.height);

        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.LEFT_PADDING, FIRST_SUBMODULE_Y_GAP + subModuleTranslation);
        for (SubModule subModule : module.getChildren()) {
            if (!subModule.isVisible()) continue;

            matrices.translate(0, SUBMODULE_Y_GAP);
            SubModuleRenderer.handle(subModule, matrices);
        }
        matrices.popMatrix();

        RenderUtil.getDrawContext().disableScissor();
    }

    private static void handleScrollBar(int moduleHeight, int subModulesHeight, Module module) {
        if (moduleHeight >= subModulesHeight) {
            // just in case! im sure its possible to have this return with a large number.
            module.scrolledAmount = 0;
            return;
        }

        float possibleScrollRatio = (float) moduleHeight / subModulesHeight;

        if (MouseUtil.isMouseInBounds(0, 0, module.width, moduleHeight, RenderUtil.getDrawContext())) {
            module.scrolledAmount -= ClickGuiScreen.lastMouseScroll * possibleScrollRatio * 0.1;
        }

        module.scrolledAmount = Math.clamp(module.scrolledAmount, 0, 1 - possibleScrollRatio);

        int scrollBarHeight = (int) (moduleHeight * possibleScrollRatio);
        int scrollBarY = (int) (moduleHeight * module.scrolledAmount);

        // background for scroll bar
        Render2dUtil.drawRect(module.width - 3, 0, 4, moduleHeight, new Color(60, 60, 60));

        Render2dUtil.drawRect(module.width - 2, scrollBarY, 2, scrollBarHeight, new Color(130, 130, 130));
    }

    private static boolean handleClick(Module module) {
        int fontHeight = FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE);
        if (MouseUtil.isMouseInBounds(module.x, module.y - fontHeight/2f, module.width, fontHeight, RenderUtil.getDrawContext())) {
            activeModule = module;
            draggingModule = true;
            return true;
        }
        else if (MouseUtil.isMouseInBounds(module.x + module.width - 3, module.y + module.height - 3, 3, 3, RenderUtil.getDrawContext())) {
            activeModule = module;
            return true;
        }
        return false;
    }

    private static boolean handleActiveModule() {
        if (activeModule == null) return false;

        Vector2f mousePos = MouseUtil.getMousePos();
        Vector2f projectedPos = RenderUtil.projectPos(MouseUtil.getMousePos(), RenderUtil.getDrawContext().getMatrices());

        Vector2f relativeMousePos = new Vector2f(mousePos.x - (projectedPos.x - mousePos.x), mousePos.y - (projectedPos.y - mousePos.y));

        if (draggingModule) handleDraggingModule(relativeMousePos);
        else handleResizingModule(relativeMousePos);

        return true;
    }

    private static void handleDraggingModule(Vector2f relativeMousePos) {
        Vector2i newPos = new Vector2i(
                (int) (MathUtil.toNearest(relativeMousePos.x - activeModule.width / 2f, activeModule.width + MODULE_X_GAP) + MODULE_X_GAP),
                (int) (MathUtil.toNearest(relativeMousePos.y - activeModule.height / 2f, activeModule.height + MODULE_Y_GAP) + MODULE_Y_GAP)
        );

        for (Module module : ModuleManager.getModulesByCategory(CategoryRenderer.selectedCategory)) {
            if (module == activeModule) continue;

            // todo: check if its in the box at all, fix size accordingly
            if (module.x == newPos.x && module.y == newPos.y) {
                module.x = activeModule.x;
                module.y = activeModule.y;
                break;
            }
        }

        activeModule.x = newPos.x;
        activeModule.y = newPos.y;
    }

    private static void handleResizingModule(Vector2f relativeMousePos) {

    }
}
