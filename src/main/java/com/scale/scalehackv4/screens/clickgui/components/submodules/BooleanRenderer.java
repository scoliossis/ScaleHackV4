package com.scale.scalehackv4.screens.clickgui.components.submodules;

import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.SubModuleRenderer;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.util.InputUtil;
import org.joml.Matrix3x2fStack;

import java.awt.*;

public class BooleanRenderer extends SubModuleRenderer {
    @Override
    public void render(SubModule subModule, Matrix3x2fStack matrices) {
        // draw toggle box.
        Render2dUtil.drawRect(0, 0, 4, 4, (boolean) subModule.get() ? ClickGuiModule.guiColour : ClickGuiScreen.OUTLINE_COLOUR);

        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.MIDDLE_PADDING, 0);

        FontUtil.drawString(subModule.getAnnotation().name(), 0, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, true);

        // keybind wowowow
        drawKeybind(subModule);

        matrices.popMatrix();
    }

    private static void drawKeybind(SubModule subModule) {
        if (!subModule.getAnnotation().hasKeybind()) return;

        String keybindString = getKeybindString(subModule);
        int keybindWidth = FontUtil.getStringWidth(keybindString, ClickGuiScreen.FONT_SIZE);
        int textX = SubModuleRenderer.MIDDLE_WIDTH + SubModuleRenderer.RIGHT_PADDING - keybindWidth;
        Color keybindColour = (subModule.getKeybind() == -1 && subModule != SubModuleRenderer.activeSubModule) ? ClickGuiScreen.OUTLINE_COLOUR : new Color(150, 150, 150);

        FontUtil.drawString(keybindString, textX, -2, ClickGuiScreen.FONT_SIZE, keybindColour, false);
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        // handle clicking the keybind
        int keybindWidth = FontUtil.getStringWidth(getKeybindString(subModule), ClickGuiScreen.FONT_SIZE);
        Rectangle keybindRect = new Rectangle(SubModuleRenderer.LEFT_PADDING+SubModuleRenderer.MIDDLE_WIDTH + SubModuleRenderer.RIGHT_PADDING - keybindWidth, -2, keybindWidth, FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE));
        if (MouseUtil.isMouseInBounds(keybindRect, true)) {
            SubModuleRenderer.activeSubModule = subModule;
            return true;
        }

        if (SubModuleRenderer.activeSubModule == subModule) SubModuleRenderer.activeSubModule = null;

        // handle clicking the toggle box
        if (MouseUtil.isMouseInBounds(
                0,
                -2,
                SubModuleRenderer.LEFT_PADDING + SubModuleRenderer.MIDDLE_PADDING + SubModuleRenderer.MIDDLE_WIDTH,
                FontUtil.getFontHeight(ClickGuiScreen.FONT_SIZE),
                RenderUtil.getDrawContext())
        ) {
            subModule.set(!(boolean) subModule.get());
            return true;
        }

        return false;
    }

    private static String getKeybindString(SubModule subModule) {
        int keybind = subModule.getKeybind();

        String keybindString = subModule == SubModuleRenderer.activeSubModule
                ? "!"
                : keybind == -1
                    ? "-"
                    : InputUtil.Type.KEYSYM.createFromCode(keybind).getLocalizedText().getString().toUpperCase();
        return "[" + keybindString + "]";
    }

    @Override
    public boolean handleMouseDown(SubModule subModule) {
        return false;
    }
}
