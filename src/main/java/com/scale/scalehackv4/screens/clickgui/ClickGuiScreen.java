package com.scale.scalehackv4.screens.clickgui;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.ModuleManager;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.awt.*;

public class ClickGuiScreen extends Screen {
    public ClickGuiScreen() {
        super(Text.of("Click GUI"));
    }

    public static final int WIDTH = 390;
    private static final int HEIGHT = 340;

    private static Category selectedCategory = Category.CLIENT;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        RenderUtil.setDrawContext(context);

        Matrix3x2fStack matrices = context.getMatrices();

        matrices.pushMatrix();
        matrices.translate(
                (float) (ClickGuiModule.guiRenderer.x * context.getScaledWindowWidth()) + 2,
                (float) (ClickGuiModule.guiRenderer.y * context.getScaledWindowHeight()) + 3
        );

        Render2dUtil.drawSkeetRect(0, 0, WIDTH, HEIGHT, null);

        int categoryDiameter = 48;
        int categoryDiameterHalf = categoryDiameter/2;

        int categoryGap = 14;
        int halfGap = categoryGap/2;

        Render2dUtil.drawRect(0, 0, categoryDiameter, HEIGHT, new Color(15, 15, 15));
        Render2dUtil.drawRect(categoryDiameter, 0, 1, HEIGHT, new Color(60, 60, 60));

        // todo: skeet centers them on the y axis
        matrices.translate(0, categoryDiameterHalf/2f);

        matrices.pushMatrix();
        for (Category category : Category.values()) {
            //Render2dUtil.drawRect(0, 0, 50, 1, new Color(60, 60, 60));

            boolean selected = category == selectedCategory;

            if (selected) {
                Render2dUtil.drawRect(0, categoryDiameterHalf+halfGap, categoryDiameter, 1, new Color(60, 60, 60));

                Render2dUtil.drawRect(0, -halfGap, categoryDiameter+1, categoryDiameterHalf+categoryGap, new Color(22, 22, 22));

                Render2dUtil.drawRect(0, -halfGap-1, categoryDiameter, 1, new Color(60, 60, 60));
                Render2dUtil.drawRect(0, categoryDiameterHalf+halfGap, categoryDiameter, 1, new Color(60, 60, 60));
            }

            Render2dUtil.drawTexturedRect(category.texture, categoryDiameterHalf/2, 0, categoryDiameterHalf, categoryDiameterHalf, selected ? new Color(180, 180, 180) : new Color(130, 130, 130));

            matrices.translate(0, categoryDiameterHalf+categoryGap);
        }
        matrices.popMatrix();

        int xGap = 12;
        int yGap = FontUtil.getFontHeight(18) + 10;

        matrices.translate(54+xGap, 0);

        int side = 0;
        int[] stackHeight = new int[] {0, 0};

        for (Module module : ModuleManager.getModulesByCategory(selectedCategory)) {
            int lastStackHeight = stackHeight[side];

            matrices.pushMatrix();
            stackHeight[side] += drawModule(module, context, matrices) + yGap;
            matrices.popMatrix();

            int newSide = stackHeight[0] <= stackHeight[1] ? 0 : 1;

            if (side != newSide) {
                matrices.translate((side == 1 ? -1 : 1) * (150 + xGap), 0);
            }
            matrices.translate(0, stackHeight[newSide]-lastStackHeight);

            side = newSide;
        }

        matrices.popMatrix();
    }

    private static int drawModule(Module module, DrawContext context, Matrix3x2fStack matrices) {
        int moduleHeight = 130;

        // outline
        Render2dUtil.drawRect(-1, -1, 152, moduleHeight+2, new Color(60, 60, 60));

        // interior crocodile alligator
        Render2dUtil.drawRect(0, 0, 150, moduleHeight, new Color(22, 22, 22));
        Render2dUtil.drawGradientDown(0, 0, 150, 10, new Color(22, 22, 22), new Color(22, 22, 22, 0));
        Render2dUtil.drawGradientDown(0, moduleHeight-10, 150, 10, new Color(22, 22, 22, 0), new Color(22, 22, 22));

        // module name
        String moduleName = module.getAnnotation().name();
        int fontHeight = FontUtil.getFontHeight(18);
        Render2dUtil.drawRect(5, -fontHeight/2, FontUtil.getStringWidth(moduleName, 18) + 3, fontHeight, new Color(22, 22, 22));
        FontUtil.drawString(moduleName, 7, -fontHeight / 2, 18, new Color(255, 255, 255), false);

        // module options n shi ig
        context.enableScissor(0, 0, 150, moduleHeight);


        context.disableScissor();

        // scroll bar
        Render2dUtil.drawRect(147, 0, 4, moduleHeight, new Color(60, 60, 60));

        return moduleHeight;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}