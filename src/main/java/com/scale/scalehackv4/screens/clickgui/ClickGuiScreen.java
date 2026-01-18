package com.scale.scalehackv4.screens.clickgui;

import com.scale.scalehackv4.modules.impl.client.ClickGUI;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
    public ClickGuiScreen() {
        super(Text.of("Click GUI"));
    }

    public static final int WIDTH = 500;
    private static final int HEIGHT = 350;

    public static final float BORDER_SCALE = 1.5f;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        RenderUtil.setDrawContext(context);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) ClickGUI.guiRenderer.x * context.getScaledWindowWidth(), (float) ClickGUI.guiRenderer.y * context.getScaledWindowHeight());

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(BORDER_SCALE);
        Render2dUtil.drawSkeetRect((int) (4*BORDER_SCALE), (int) (5*BORDER_SCALE), (int) (WIDTH/BORDER_SCALE), (int) (HEIGHT/BORDER_SCALE), null);
        context.getMatrices().popMatrix();

        context.getMatrices().popMatrix();
    }
}
