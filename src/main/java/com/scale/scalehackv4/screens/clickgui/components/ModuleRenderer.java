package com.scale.scalehackv4.screens.clickgui.components;

import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.ModuleManager;
import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class ModuleRenderer {
    public static final int MODULE_WIDTH = 150;
    public static final int MODULE_X_GAP = (ClickGuiScreen.WIDTH - CategoryRenderer.CATEGORY_DIAMETER - (MODULE_WIDTH*2)) / 3;
    public static final int MODULE_Y_GAP = 15;

    public static final int FIRST_SUBMODULE_Y_GAP = 8;
    public static final int SUBMODULE_Y_GAP = 2;

    public static void renderSection(Matrix3x2fStack matrices) {
        matrices.translate(MODULE_X_GAP, 0);

        // sort array as left modules top to bottom, then right modules top to bottom
        List<Module> modules = ModuleManager.getModulesByCategory(CategoryRenderer.selectedCategory).stream()
                .sorted(Comparator.comparingInt(m -> m.y))
                .sorted(Comparator.comparingInt(m -> m.xSide))
                .toList();

        for (Module module : modules) {
            matrices.pushMatrix();
            drawModule(module, RenderUtil.getDrawContext(), matrices);
            matrices.popMatrix();
        }
    }

    private static void drawModule(Module module, DrawContext context, Matrix3x2fStack matrices) {
        // everyone, get ready, positions people!
        matrices.translate(module.xSide == 0 ? 0 : MODULE_WIDTH+MODULE_X_GAP, (module.y * MODULE_Y_GAP) + (module.y * module.height) + MODULE_Y_GAP);

        int moduleHeight = module.height;
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
        Render2dUtil.drawRect(-1, -1, titleX-1, moduleHeight+2, ClickGuiScreen.OUTLINE_COLOUR);
        Render2dUtil.drawRect(titleX+2+moduleNameWidth, -1, MODULE_WIDTH-titleX-1-moduleNameWidth, moduleHeight+2, ClickGuiScreen.OUTLINE_COLOUR);
        Render2dUtil.drawRect(-1, moduleHeight, MODULE_WIDTH+2, 1, ClickGuiScreen.OUTLINE_COLOUR);

        // interior crocodile alligator
        Render2dUtil.drawRect(0, 0, MODULE_WIDTH, moduleHeight, new Color(22, 22, 22));

        // module options n shi ig
        context.enableScissor(1, 1, MODULE_WIDTH, moduleHeight);

        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.LEFT_PADDING, FIRST_SUBMODULE_Y_GAP + subModuleTranslation);

        for (SubModule subModule : module.getChildren()) {
            if (!subModule.isVisible()) continue;

            matrices.translate(0, SUBMODULE_Y_GAP);
            SubModuleRenderer.handle(subModule, matrices);
        }
        matrices.popMatrix();

        context.disableScissor();

        // top and bottom gradient to hide the cutoff smoothly ig, skeet does it
        Render2dUtil.drawGradientDown(0, 0, MODULE_WIDTH, 10, new Color(22, 22, 22), new Color(22, 22, 22, 0));
        Render2dUtil.drawGradientDown(0, moduleHeight-10, MODULE_WIDTH, 10, new Color(22, 22, 22, 0), new Color(22, 22, 22));

        // module name
        FontUtil.drawString("§l"+moduleName, titleX, -fontHeight / 2f, ClickGuiScreen.FONT_SIZE, new Color(255, 255, 255), false);

        handleScrollBar(moduleHeight, subModulesHeight, module);
    }

    private static void handleScrollBar(int moduleHeight, int subModulesHeight, Module module) {
        if (moduleHeight >= subModulesHeight) {
            // just in case! im sure its possible to have this return with a large number.
            module.scrolledAmount = 0;
            return;
        }

        float possibleScrollRatio = (float) moduleHeight / subModulesHeight;

        if (MouseUtil.isMouseInBounds(0, 0, MODULE_WIDTH, moduleHeight, RenderUtil.getDrawContext())) {
            module.scrolledAmount -= ClickGuiScreen.lastMouseScroll * possibleScrollRatio * 0.1;
        }

        module.scrolledAmount = Math.clamp(module.scrolledAmount, 0, 1 - possibleScrollRatio);

        int scrollBarHeight = (int) (moduleHeight * possibleScrollRatio);
        int scrollBarY = (int) (moduleHeight * module.scrolledAmount);

        // background for scroll bar
        Render2dUtil.drawRect(MODULE_WIDTH - 3, 0, 4, moduleHeight, new Color(60, 60, 60));

        Render2dUtil.drawRect(MODULE_WIDTH - 2, scrollBarY, 2, scrollBarHeight, new Color(130, 130, 130));
    }
}
