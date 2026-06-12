package com.scale.scalehackv4.screens.mainmenu;

import com.scale.scalehackv4.modules.impl.client.Hud.Watermark;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.awt.*;

// todo: come up with a swag idea for a custom background (even tho the default one slaps)
public class MainMenuScreen extends Screen {
    public MainMenuScreen(Text title) {
        super(title);
    }

    private record Option(String name, Screen screen) {
        private void setScreen() {
            // quit has screen set to null
            if (screen == null) System.exit(0);
            C.mc.setScreen(screen);
        }
    }

    private final Option[] OPTIONS = new Option[] {
            new Option("Singleplayer", new SelectWorldScreen(this)),
            new Option("Multiplayer", new MultiplayerScreen(this)),
            new Option("Options", new OptionsScreen(this, C.mc.options)),
            new Option("Realms", new RealmsMainScreen(this)),
            new Option("Quit", null)
    };

    private final int OPTIONS_FONT_SIZE = 20;
    private final String HOVER_INDICATOR = "> ";
    private final int X_INDENT = 10;
    private final int Y_INDENT = 10;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        RenderUtil.setDrawContext(context);
        Matrix3x2fStack matrixStack = context.getMatrices();

        matrixStack.pushMatrix();

        for (int i = 0; i < OPTIONS.length; i++) {
            Option option = OPTIONS[i];
            boolean hovered = isOptionHovered(i);

            Color color = hovered ? Color.WHITE : new Color(150, 150, 150);

            FontUtil.drawString(option.name, getOptionX(i), getOptionY(i), OPTIONS_FONT_SIZE, color, false);
            if (hovered) {
                int hoverIndicatorWidth = FontUtil.getStringWidth(HOVER_INDICATOR, OPTIONS_FONT_SIZE);
                FontUtil.drawString(HOVER_INDICATOR, getOptionX(i)-hoverIndicatorWidth, getOptionY(i), OPTIONS_FONT_SIZE, Color.WHITE, false);
            }
        }

        matrixStack.translate(10, 10);
        Watermark.watermarkDraggable.render();

        matrixStack.popMatrix();
    }

    // takes an index in case i wanna change the style in the future
    private int getOptionX(int index) {
        return FontUtil.getStringWidth(HOVER_INDICATOR, OPTIONS_FONT_SIZE) + X_INDENT;
    }

    private int getOptionY(int index) {
        index = OPTIONS.length-index;
        return (C.res().getScaledHeight() - Y_INDENT)
                - (FontUtil.getFontHeight(OPTIONS_FONT_SIZE) * index)
                - (5 * index);
    }

    private boolean isOptionHovered(int index) {
        int stringWidth = FontUtil.getStringWidth(OPTIONS[index].name, OPTIONS_FONT_SIZE);
        int stringHeight = FontUtil.getFontHeight(OPTIONS_FONT_SIZE);
        return MouseUtil.isMouseInBounds(getOptionX(index), getOptionY(index), stringWidth, stringHeight);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;

        for (int i = 0; i < OPTIONS.length; i++) if (isOptionHovered(i)) {
            OPTIONS[i].setScreen();
            return true;
        }

        return false;
    }
}
