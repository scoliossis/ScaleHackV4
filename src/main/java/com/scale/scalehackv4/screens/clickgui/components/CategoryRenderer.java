package com.scale.scalehackv4.screens.clickgui.components;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import org.joml.Matrix3x2fStack;

import java.awt.*;

public class CategoryRenderer {
    public static Category selectedCategory = Category.CLIENT;

    public static final int CATEGORY_DIAMETER = 48;
    private static final int HALF_CATEGORY_DIAMETER = CATEGORY_DIAMETER/2;
    private static final int CATEGORY_GAP = 14;
    private static final int HALF_CATEGORY_GAP = CATEGORY_GAP/2;

    public static void renderSection(Matrix3x2fStack matrices) {
        Render2dUtil.drawRect(0, 0, CATEGORY_DIAMETER, ClickGuiScreen.HEIGHT, new Color(15, 15, 15));
        Render2dUtil.drawRect(CATEGORY_DIAMETER, 0, 1, ClickGuiScreen.HEIGHT, new Color(60, 60, 60));

        int totalHeightCategoryHeight = Category.values().length * (HALF_CATEGORY_DIAMETER+CATEGORY_GAP) - CATEGORY_GAP;
        int extraHeight = ClickGuiScreen.HEIGHT - totalHeightCategoryHeight;

        matrices.pushMatrix();
        matrices.translate(0, extraHeight/2f);

        for (Category category : Category.values()) {
            if (ClickGuiScreen.mousePressed) selectedCategory = CategoryRenderer.handleClicked(category);

            renderCategory(category, matrices);
        }
        matrices.popMatrix();

        matrices.translate(CATEGORY_DIAMETER, 0);
    }

    private static void renderCategory(Category category, Matrix3x2fStack matrices) {
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

    private static Category handleClicked(Category category) {
        return MouseUtil.isMouseInBounds(0, -HALF_CATEGORY_GAP, CATEGORY_DIAMETER+1, HALF_CATEGORY_DIAMETER+CATEGORY_GAP, RenderUtil.getDrawContext()) ? category : selectedCategory;
    }
}
