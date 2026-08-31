package com.scale.scalehackv4.screens.clickgui.components;

import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.MathUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RegisterListeners
public class ModuleRenderer {
    public static final int MODULE_X_GAP = 15;
    public static final int MODULE_Y_GAP = 15;

    public static final int MODULES_SECTION_WIDTH = ClickGuiScreen.WIDTH - CategoryRenderer.CATEGORY_DIAMETER - 1;
    public static final int FIRST_SUBMODULE_Y_GAP = 8;
    public static final int SUBMODULE_Y_GAP = 2;

    private static final int MAX_MODULES = 4;
    private static int[][] VALID_GRID = getValidGrid();

    private static boolean draggingModule = false;
    private static Module activeModule;

    public static void renderSection(Matrix3x2fStack matrices) {
        matrices.translate(0, 0);

        // sort array as left modules top to bottom, then right modules top to bottom
        List<Module> modules = ModuleUtil.getModulesByCategory(CategoryRenderer.selectedCategory).stream()
                .sorted(Comparator.comparingInt(m -> m.y))
                .sorted(Comparator.comparingInt(m -> m.x))
                .toList();

        if (ClickGuiScreen.mouseReleased) {
            if (activeModule != null) {
                activeModule = null;
                draggingModule = false;
                ModuleUtil.saveGuiPositions();
            }
        }
        ClickGuiScreen.leftMouseDown &= !handleActiveModule();

        for (Module module : modules) {
            matrices.pushMatrix();
            drawModule(module, matrices);
            matrices.popMatrix();
        }
    }

    private static void drawModule(Module module, Matrix3x2fStack matrices) {
        if (ClickGuiScreen.mousePressed && handleClick(module)) ClickGuiScreen.mousePressed = false;

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
        Color outlineColour = activeModule == module ? ClickGuiModule.guiColour : ClickGuiScreen.OUTLINE_COLOUR;
        // top left to module name
        Render2dUtil.drawRect(-1, -1, titleX-1, module.height+2, outlineColour);
        // right of module name to top right
        Render2dUtil.drawRect(titleX+2+moduleNameWidth, -1, module.width-titleX-1-moduleNameWidth, module.height+2, outlineColour);
        // bottom of outline
        Render2dUtil.drawRect(-1, module.height, module.width+2, 1, outlineColour);

        /*
        // todo:
        int size = 5;
        // arrow in bottom right!
        Render2dUtil.drawPoints(
                new int[]{module.width - size, module.width, module.width},
                new int[]{module.height, module.height-size, module.height},
                outlineColour,
                false
        );
         */

        // interior crocodile alligator
        Render2dUtil.drawRect(0, 0, module.width, module.height, new Color(22, 22, 22));

        handleSubmodules(module, matrices, subModuleTranslation);

        // top and bottom gradient to hide the cutoff smoothly ig, skeet does it
        Render2dUtil.drawGradientDown(0, 0, module.width, 10, new Color(22, 22, 22), new Color(22, 22, 22, 0));
        Render2dUtil.drawGradientDown(0, module.height-10, module.width, 10, new Color(22, 22, 22, 0), new Color(22, 22, 22));

        // module name
        FontUtil.drawString(
                Text.literal(moduleName).formatted(Formatting.BOLD),
                titleX,
                (int) (-fontHeight / 2f),
                ClickGuiScreen.FONT_SIZE,
                new Color(255, 255, 255),
                false
        );

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

    // todo: this code is all horribly named and i hate the code and i dont know why it works.
    private static void handleScrollBar(int moduleHeight, int subModulesHeight, Module module) {
        if (moduleHeight >= subModulesHeight) {
            // just in case! im sure its possible to have this return with a large number.
            module.scrolledAmount = 0;
            return;
        }

        float possibleScrollRatio = (float) moduleHeight / subModulesHeight;
        boolean moduleHovered = MouseUtil.isMouseInBounds(0, 0, module.width, moduleHeight, RenderUtil.getDrawContext());
        if (moduleHovered && SubModuleRenderer.activeSubModule == null) {
            module.scrolledAmount -= ClickGuiScreen.lastMouseScroll * possibleScrollRatio * 0.1;
        }

        module.scrolledAmount = Math.clamp(module.scrolledAmount, 0, 1 - possibleScrollRatio);

        int scrollBarHeight = (int) (moduleHeight * possibleScrollRatio);
        int scrollBarY = (int) (moduleHeight * module.scrolledAmount);

        // background for scroll bar
        Render2dUtil.drawRect(module.width - 3, 0, 3, moduleHeight, new Color(60, 60, 60));

        Render2dUtil.drawRect(module.width - 2, scrollBarY, 2, scrollBarHeight, new Color(130, 130, 130));
    }

    private static Vector2f CLICK_OFFSET = new Vector2f(0, 0);

    private static boolean handleClick(Module module) {
        int fontHeight = FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE);
        if (MouseUtil.isMouseInBounds(module.x, module.y - fontHeight/2f, module.width, fontHeight, RenderUtil.getDrawContext())) {
            Vector2f guiPos = new Vector2f(((float) ClickGuiModule.guiRenderer.x * C.res().getScaledWidth()) + CategoryRenderer.CATEGORY_DIAMETER, (float) ClickGuiModule.guiRenderer.y * C.res().getScaledHeight());
            CLICK_OFFSET = MouseUtil.getMousePos().sub(guiPos).sub(new Vector2f(module.x, module.y + fontHeight/2f));
            activeModule = module;
            draggingModule = true;
            return true;
        }
        else if (MouseUtil.isMouseInBounds(module.x + module.width - 3, module.y + module.height - 3, 5, 5, RenderUtil.getDrawContext())) {
            CLICK_OFFSET = new Vector2f(3, 3);
            activeModule = module;
            return true;
        }
        return false;
    }

    private static boolean handleActiveModule() {
        VALID_GRID = getValidGrid();

        if (activeModule == null) return false;

        Vector2f mousePos = MouseUtil.getMousePos();
        Vector2f projectedPos = RenderUtil.projectPos(MouseUtil.getMousePos(), RenderUtil.getDrawContext().getMatrices());

        Vector2f relativeMousePos = new Vector2f(mousePos.x - (projectedPos.x - mousePos.x), mousePos.y - (projectedPos.y - mousePos.y)).sub(CLICK_OFFSET);

        if (draggingModule) handleDraggingModule(relativeMousePos);
        else handleResizingModule(relativeMousePos);

        return true;
    }

    private static void handleDraggingModule(Vector2f relativeMousePos) {
        Vector2i newPos = new Vector2i(
                (MathUtil.toNearest((int) (relativeMousePos.x), VALID_GRID[0])),
                (MathUtil.toNearest((int) (relativeMousePos.y), VALID_GRID[1]))
        );

        Vector2i fixedPos = new Vector2i(
                Math.min(newPos.x, MODULES_SECTION_WIDTH - MODULE_X_GAP - activeModule.width),
                Math.min(newPos.y, ClickGuiScreen.HEIGHT - MODULE_Y_GAP - activeModule.height)
        );

        for (Module module : ModuleUtil.getModulesByCategory(CategoryRenderer.selectedCategory)) {
            if (module == activeModule) continue;

            if (Math.abs(newPos.x - module.x) < 10 && Math.abs(newPos.y - module.y) < 10) {
                // swap wowowow
                Rectangle temp = new Rectangle(module.x, module.y, module.width, module.height);
                module.x = activeModule.x; module.y = activeModule.y; module.width = activeModule.width; module.height = activeModule.height;
                activeModule.x = temp.x; activeModule.y = temp.y; activeModule.width = temp.width; activeModule.height = temp.height;

                return;
            }

            boolean intersectsX = newPos.x + activeModule.width >= module.x && newPos.x <= module.x + module.width;
            boolean intersectsY = newPos.y + activeModule.height >= module.y && newPos.y <= module.y + module.height;

            if (intersectsX && intersectsY) {
                fixedPos.x = activeModule.x;
                fixedPos.y = activeModule.y;
            }
        }

        activeModule.x = fixedPos.x;
        activeModule.y = fixedPos.y;
    }

    private static void handleResizingModule(Vector2f relativeMousePos) {
        Vector2i newSize = new Vector2i(
                MathUtil.toNearest((int) (relativeMousePos.x - activeModule.x), VALID_GRID[2]),
                MathUtil.toNearest((int) (relativeMousePos.y - activeModule.y), VALID_GRID[3])
        );

        Vector2i fixedSize = new Vector2i(
                Math.min(newSize.x, MODULES_SECTION_WIDTH - MODULE_X_GAP - activeModule.x),
                Math.min(newSize.y, ClickGuiScreen.HEIGHT - MODULE_Y_GAP - activeModule.y)
        );

        for (Module module : ModuleUtil.getModulesByCategory(CategoryRenderer.selectedCategory)) {
            if (module == activeModule) continue;

            boolean intersectsX = activeModule.x <= module.x + module.width && activeModule.x <= module.x && activeModule.x + fixedSize.x >= module.x;
            boolean intersectsY = activeModule.y <= module.y + module.height && activeModule.y <= module.y && activeModule.y + fixedSize.y >= module.y;

            if (intersectsX && intersectsY) {
                fixedSize.x = activeModule.width;
                fixedSize.y = activeModule.height;
                break;
            }
        }

        activeModule.width = fixedSize.x;
        activeModule.height = fixedSize.y;
    }

    // im dont really like this block of code :(
    private static int[][] getValidGrid() {
        // urm actually, this is memory inefficient!!! the size should be 4+3+2+1, not (4*4)+1!!! 7*32 bits wasted, awful coder.
        int[][] grid = new int[4][MAX_MODULES*MAX_MODULES + 1];

        for (int[] ints : grid) {
            // halfed to stop integer overflow
            Arrays.fill(ints, Integer.MAX_VALUE/2);
        }

        grid[0][0] = MODULE_X_GAP;
        grid[1][0] = MODULE_Y_GAP;

        for (int i = 1; i <= MAX_MODULES; i++) {
            int validWidth = (MODULES_SECTION_WIDTH / i) - MODULE_X_GAP - (MODULE_X_GAP / i);
            int validHeight = (ClickGuiScreen.HEIGHT / i) - MODULE_Y_GAP - (MODULE_Y_GAP / i);

            for (int j = 1; j <= i; j++) {
                int index = (j-1) + ((i-1)*MAX_MODULES) + 1;
                if (j < i) {
                    grid[0][index] = (validWidth * j) + ((j+1) * MODULE_X_GAP);
                    grid[1][index] = (validHeight * j) + ((j+1) * MODULE_Y_GAP);
                }
                grid[2][index] = validWidth * j;
                grid[3][index] = validHeight * j;
            }
        }

        return grid;
    }
}