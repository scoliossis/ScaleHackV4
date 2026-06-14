package com.scale.scalehackv4.screens.mainmenu;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.alts.AltManagerUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.entity.player.PlayerModelPart;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

// i think this is a big enough part of the main menu that it deserves its own class
public class AltManager {
    private static final int FONT_SIZE = 15;
    private static final int ALT_MANAGER_INDENT_X = 10;
    private static final int ALT_MANAGER_INDENT_Y = 10;
    private static final int HEAD_INDENT = 4;

    private static boolean altsListOpen = false;

    protected static void render() {
        Matrix3x2fStack matrices = RenderUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();

        int fontHeight = FontUtil.getFontHeight(FONT_SIZE);
        int HEAD_DIAMETER = getHeadDiameter();

        Rectangle bounds = getAltBounds();

        boolean shouldShowHat = C.mc.options.isPlayerModelPartEnabled(PlayerModelPart.HAT);

        int headX = bounds.x + HEAD_INDENT;
        int headY = ALT_MANAGER_INDENT_Y + HEAD_INDENT;
        int textX = headX + HEAD_DIAMETER + HEAD_INDENT;
        int textY = ALT_MANAGER_INDENT_Y + (bounds.height/2) - (fontHeight/2);

        for (AltManagerUtil.Alt account : getAltsList()) {
            String accountName = account.name;
            // ive noticed i use 22,22,22 grey a lot, and have for a while, but im not sure where i got this specific number from
            Render2dUtil.drawRect(bounds.x, bounds.y, bounds.width, bounds.height, new Color(22, 22, 22, 100));
            Render2dUtil.drawSkinHead(account.skin, headX, headY, HEAD_DIAMETER, HEAD_DIAMETER, Color.WHITE, shouldShowHat);

            int stringWidth = FontUtil.getStringWidth(accountName, FONT_SIZE);
            int maxStringWidth = bounds.width - HEAD_INDENT*3 - HEAD_DIAMETER;

            if (stringWidth > maxStringWidth) {
                String etcString = "-";
                int etcStringWidth = FontUtil.getStringWidth(etcString, FONT_SIZE);
                int maxWidth = maxStringWidth - etcStringWidth;

                while (FontUtil.getStringWidth(accountName, FONT_SIZE) > maxWidth) accountName = accountName.substring(0, accountName.length() - 1);
                accountName += etcString;
            }
            FontUtil.drawString(accountName, textX, textY, FONT_SIZE, Color.WHITE, false);

            matrices.translate(0, bounds.height);
        }

        matrices.popMatrix();
    }

    private static int getHeadDiameter() {
        return FontUtil.getFontHeight(FONT_SIZE) + HEAD_INDENT;
    }

    private static Rectangle getAltBounds() {
        int headDiameter = getHeadDiameter();
        int width = FontUtil.getStringWidth(C.mc.getGameProfile().name(), FONT_SIZE) + (HEAD_INDENT*3) + headDiameter;

        return new Rectangle(
                C.res().getScaledWidth() - ALT_MANAGER_INDENT_X - width,
                ALT_MANAGER_INDENT_Y,
                width,
                headDiameter + (HEAD_INDENT*2)
        );
    }

    private static List<AltManagerUtil.Alt> getAltsList() {
        if (!altsListOpen) return List.of(AltManagerUtil.currentAlt);
        AltManagerUtil.ALTS.sort(Comparator.comparing(alt -> -alt.lastLogin));
        return AltManagerUtil.ALTS;
    }

    protected static boolean handleClick(Click click) {
        if (click.button() != 0) return false;

        Rectangle bounds = getAltBounds();

        List<AltManagerUtil.Alt> altsList = getAltsList();
        for (int i = 0; i < altsList.size(); i++) {
            AltManagerUtil.Alt alt = altsList.get(i);
            if (MouseUtil.isMouseInBounds(bounds, false)) {
                if (i == 0) altsListOpen = !altsListOpen;
                else {
                    alt.login();
                    altsListOpen = false;
                }
                return true;
            }

            bounds.y += bounds.height;
        }

        altsListOpen = false;

        return false;
    }
}
