package com.scale.scalehackv4.screens.mainmenu;

import com.scale.scalehackv4.modules.impl.client.hud.Watermark;
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
    private final int OPTIONS_X_INDENT = 10;
    private final int OPTIONS_Y_INDENT = 5;

    private final String COPYRIGHT_STRING = "an awesome mod by https://github.com/scoliossis";
    private final int COPYRIGHT_STRING_FONT_SIZE = 10;

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        RenderUtil.setDrawContext(context);

        drawOptions();
        drawCopyright();
        drawWatermark();
        AltManager.render();

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void drawOptions() {
        for (int i = 0; i < OPTIONS.length; i++) {
            Option option = OPTIONS[i];

            boolean hovered = isOptionHovered(i);
            Color color = hovered ? Color.WHITE : new Color(150, 150, 150);

            FontUtil.drawString(option.name, getOptionX(i), getOptionY(i), OPTIONS_FONT_SIZE, color, true);
        }
    }

    // we GOTTA replace the minecraft copyright, scale RUNS this minecraft stuff.
    private void drawCopyright() {
        int copyrightStringWidth = FontUtil.getStringWidth(COPYRIGHT_STRING, COPYRIGHT_STRING_FONT_SIZE);
        int copyrightStringHeight = FontUtil.getFontHeight(COPYRIGHT_STRING_FONT_SIZE);
        int copyrightStringX = C.res().getScaledWidth() - copyrightStringWidth - 5;
        int copyrightStringY = C.res().getScaledHeight() - copyrightStringHeight - 2;
        FontUtil.drawString(COPYRIGHT_STRING, copyrightStringX, copyrightStringY, COPYRIGHT_STRING_FONT_SIZE, Color.WHITE, true);
    }

    // todo: bad, make a logo
    private void drawWatermark() {
        Matrix3x2fStack matrixStack = RenderUtil.getDrawContext().getMatrices();

        matrixStack.pushMatrix();
        matrixStack.translate(10, 10);
        Watermark.watermarkDraggable.render();

        matrixStack.popMatrix();
    }

    // takes an index in case i wanna change the style in the future
    private int getOptionX(int index) {
        return OPTIONS_X_INDENT;
    }

    private int getOptionY(int index) {
        index = OPTIONS.length-index;
        return (C.res().getScaledHeight() - OPTIONS_Y_INDENT)
                - (FontUtil.getFontHeight(OPTIONS_FONT_SIZE) * index)
                - (OPTIONS_Y_INDENT * index);
    }

    private boolean isOptionHovered(int index) {
        int stringWidth = FontUtil.getStringWidth(OPTIONS[index].name, OPTIONS_FONT_SIZE);
        int stringHeight = FontUtil.getFontHeight(OPTIONS_FONT_SIZE);
        return MouseUtil.isMouseInBounds(getOptionX(index), getOptionY(index), stringWidth, stringHeight);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return AltManager.handleClick(click) || handleClickingOptions(click);
    }

    private boolean handleClickingOptions(Click click) {
        if (click.button() == 0) for (int i = 0; i < OPTIONS.length; i++) if (isOptionHovered(i)) {
            OPTIONS[i].setScreen();
            return true;
        }

        return false;
    }
}