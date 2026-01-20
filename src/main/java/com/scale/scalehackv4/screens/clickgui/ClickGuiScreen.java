package com.scale.scalehackv4.screens.clickgui;

import com.scale.scalehackv4.bridge.MouseBridge;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.ModuleManager;
import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class ClickGuiScreen extends Screen {
    public ClickGuiScreen() {
        super(Text.of("Click GUI"));
    }

    private static MouseInput lastMouseInput = null;

    public static final int WIDTH = 390;
    public static final int HEIGHT = 340;

    public static final int CATEGORY_DIAMETER = 48;
    public static final int HALF_CATEGORY_DIAMETER = CATEGORY_DIAMETER/2;
    public static final int CATEGORY_GAP = 14;
    public static final int HALF_CATEGORY_GAP = CATEGORY_GAP/2;

    
    public static final int MODULE_WIDTH = 150;
    public static final int MODULE_X_GAP = (WIDTH - CATEGORY_DIAMETER - (MODULE_WIDTH*2)) / 3;
    public static final int MODULE_Y_GAP = 15;

    public static final int SUBMODULE_Y_GAP = 10;

    public static final int FONT_SIZE = 6;

    public static Category selectedCategory = Category.CLIENT;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        MouseInput mouseInput = MouseBridge.from(C.mc.mouse).bridge$getActiveButton();
        boolean leftMouseDown = mouseInput != null && mouseInput.button() == 0;
        boolean mousePressed = mouseInput != null && (lastMouseInput == null || lastMouseInput.button() != 0) && leftMouseDown;

        RenderUtil.setDrawContext(context);

        Matrix3x2fStack matrices = context.getMatrices();

        matrices.pushMatrix();
        matrices.translate(
                (float) (ClickGuiModule.guiRenderer.x * context.getScaledWindowWidth()) + 2,
                (float) (ClickGuiModule.guiRenderer.y * context.getScaledWindowHeight()) + 3
        );

        Render2dUtil.drawSkeetRect(0, 0, WIDTH, HEIGHT, null);


        Render2dUtil.drawRect(0, 0, CATEGORY_DIAMETER, HEIGHT, new Color(15, 15, 15));
        Render2dUtil.drawRect(CATEGORY_DIAMETER, 0, 1, HEIGHT, new Color(60, 60, 60));

        int totalHeightCategoryHeight = Category.values().length * (HALF_CATEGORY_DIAMETER+CATEGORY_GAP) - CATEGORY_GAP;
        int extraHeight = HEIGHT - totalHeightCategoryHeight;

        matrices.pushMatrix();
        matrices.translate(0, extraHeight/2f);

        for (Category category : Category.values()) {
            if (mousePressed && MouseUtil.isMouseInBounds(0, -HALF_CATEGORY_GAP, CATEGORY_DIAMETER+1, HALF_CATEGORY_DIAMETER+CATEGORY_GAP, context)) {
                selectedCategory = category;
            }

            boolean selected = category == selectedCategory;

            if (selected) {
                Render2dUtil.drawRect(0, HALF_CATEGORY_DIAMETER+HALF_CATEGORY_GAP, CATEGORY_DIAMETER, 1, new Color(60, 60, 60));

                // box highlighting the selected category
                Render2dUtil.drawRect(0, -HALF_CATEGORY_GAP, CATEGORY_DIAMETER+1, HALF_CATEGORY_DIAMETER+CATEGORY_GAP, new Color(22, 22, 22));

                Render2dUtil.drawRect(0, -HALF_CATEGORY_GAP-1, CATEGORY_DIAMETER, 1, new Color(60, 60, 60));
                Render2dUtil.drawRect(0, HALF_CATEGORY_DIAMETER+HALF_CATEGORY_GAP, CATEGORY_DIAMETER, 1, new Color(60, 60, 60));
            }

            Render2dUtil.drawTexturedRect(category.texture, HALF_CATEGORY_DIAMETER/2, 0, HALF_CATEGORY_DIAMETER, HALF_CATEGORY_DIAMETER, selected ? new Color(180, 180, 180) : new Color(130, 130, 130));

            matrices.translate(0, HALF_CATEGORY_DIAMETER+CATEGORY_GAP);
        }
        matrices.popMatrix();

        matrices.translate(CATEGORY_DIAMETER+MODULE_X_GAP, 0);

        List<Module> modules = ModuleManager.getModulesByCategory(selectedCategory).stream().sorted(Comparator.comparingInt(m -> m.y)).toList();
        for (Module module : modules) {
            matrices.pushMatrix();
            drawModule(module, context, matrices);
            matrices.popMatrix();
        }

        matrices.popMatrix();

        lastMouseInput = mouseInput;
    }

    private static void drawModule(Module module, DrawContext context, Matrix3x2fStack matrices) {
        matrices.translate(module.xSide == 0 ? 0 : MODULE_WIDTH+MODULE_X_GAP, (module.y * MODULE_Y_GAP) + (module.y * module.height) + MODULE_Y_GAP);

        int moduleHeight = module.height;
        String moduleName = module.getAnnotation().name();
        int moduleNameWidth = FontUtil.getStringWidth(moduleName, FONT_SIZE);
        int fontHeight = FontUtil.getFontHeight(FONT_SIZE);

        // outline
        Render2dUtil.drawRect(-1, -1, 6, moduleHeight+2, new Color(60, 60, 60));
        Render2dUtil.drawRect(9+moduleNameWidth, -1, MODULE_WIDTH-8-moduleNameWidth, moduleHeight+2, new Color(60, 60, 60));
        Render2dUtil.drawRect(-1, moduleHeight, MODULE_WIDTH+2, 1, new Color(60, 60, 60));

        // interior crocodile alligator
        Render2dUtil.drawRect(0, 0, MODULE_WIDTH, moduleHeight, new Color(22, 22, 22));

        // module options n shi ig
        context.enableScissor(1, 1, MODULE_WIDTH, moduleHeight);

        matrices.pushMatrix();
        matrices.translate(MODULE_X_GAP, 0);
        for (SubModule subModule : module.getChildren()) {
            matrices.translate(0, SUBMODULE_Y_GAP);

            // draw box.
            matrices.pushMatrix();
            matrices.translate(MODULE_WIDTH/5f - MODULE_X_GAP, 0);

            FontUtil.drawString(subModule.getAnnotation().name(), 0, 0, FONT_SIZE, new Color(255, 255, 255), false);
            matrices.popMatrix();
        }
        matrices.popMatrix();

        context.disableScissor();

        // top and bottom gradient to hide the cutoff smoothly ig, skeet does it
        Render2dUtil.drawGradientDown(0, 0, MODULE_WIDTH, 10, new Color(22, 22, 22), new Color(22, 22, 22, 0));
        Render2dUtil.drawGradientDown(0, moduleHeight-10, MODULE_WIDTH, 10, new Color(22, 22, 22, 0), new Color(22, 22, 22));

        // module name
        FontUtil.drawString("§l"+moduleName, 7, -fontHeight / 2f, FONT_SIZE, new Color(255, 255, 255), false);

        // scroll bar
        // todo: check if the module stuffs doesnt fit first.
        Render2dUtil.drawRect(MODULE_WIDTH-3, 0, 4, moduleHeight, new Color(60, 60, 60));
        Render2dUtil.drawRect(MODULE_WIDTH-2, 1, 2, moduleHeight/2, new Color(130, 130, 130));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}