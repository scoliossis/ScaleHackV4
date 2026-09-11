package com.scale.scalehackv4.screens.mainmenu;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.alts.AltManagerUtil;
import com.scale.scalehackv4.utils.alts.AuthServer;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Util;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;

// i think this is a big enough part of the main menu that it deserves its own class
public class AltManager {
    private static final int ACCOUNTS_FONT_SIZE = 15;
    private static final int ALT_MANAGER_INDENT_X = 10;
    private static final int ALT_MANAGER_INDENT_Y = 10;
    private static final int GAP_WIDTH = 4;

    private static boolean altsListOpen = false;

    private static final int MAX_ALTS_DISPLAY = 6;

    protected static int scrolledAmount = 0;

    // yes i had to google the multiplication symbol, i sadly dont have it on my keyboard
    private static final String REMOVE_ACCOUNT_INDICATOR = "×";

    private static final int ADD_ACCOUNT_HEIGHT = 15;
    private static final int ADD_ACCOUNT_MINIMUM_INDENT = 10;
    private static final int ADD_ACCOUNT_FONT_SIZE = 10;
    private static final String ADD_ACCOUNT_TEXT = "+ Add Account";

    protected static void render() {
        Matrix3x2fStack matrices = RenderUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();

        int fontHeight = FontUtil.getFontHeight(ACCOUNTS_FONT_SIZE);
        int HEAD_DIAMETER = getHeadDiameter();

        Rectangle bounds = getAltBounds();

        boolean shouldShowHat = C.mc.options.isPlayerModelPartEnabled(PlayerModelPart.HAT);

        int headX = bounds.x + GAP_WIDTH;
        int headY = ALT_MANAGER_INDENT_Y + GAP_WIDTH;
        int textX = headX + HEAD_DIAMETER + GAP_WIDTH;
        int textY = ALT_MANAGER_INDENT_Y + (bounds.height/2) - (fontHeight/2);

        Rectangle removeIndicatorBounds = getRemoveIndicatorBounds();

        List<AltManagerUtil.Alt> alts = getAltsList();

        DrawContext context = RenderUtil.getDrawContext();
        int altsHeight = getAltsHeight();

        int maxScroll = Math.min(0, (MAX_ALTS_DISPLAY-alts.size()))*bounds.height;
        scrolledAmount = Math.clamp(scrolledAmount, altsListOpen ? maxScroll : 0, 0);
        context.enableScissor(bounds.x, bounds.y, bounds.x+bounds.width, bounds.y+altsHeight);

        matrices.translate(0, scrolledAmount);

        for (AltManagerUtil.Alt account : alts) {
            // ive noticed i use 22,22,22 grey a lot, and have for a while, but im not sure where i got this specific number from
            Render2dUtil.drawRect(bounds.x, bounds.y, bounds.width, bounds.height, new Color(22, 22, 22, 100));
            Render2dUtil.drawSkinHead(account.skin, headX, headY, HEAD_DIAMETER, HEAD_DIAMETER, Color.WHITE, shouldShowHat);

            FontUtil.drawString(account.name, textX, textY, ACCOUNTS_FONT_SIZE, Color.WHITE, false);
            if (account != AltManagerUtil.currentAlt) {
                Color removeColor = MouseUtil.isMouseInBounds(removeIndicatorBounds, false)
                        ? Color.WHITE
                        : new Color(150, 150, 150);
                FontUtil.drawString(REMOVE_ACCOUNT_INDICATOR, removeIndicatorBounds.x, removeIndicatorBounds.y, ACCOUNTS_FONT_SIZE, removeColor, false);
            }

            matrices.translate(0, bounds.height);
        }

        context.disableScissor();
        matrices.popMatrix();

        matrices.pushMatrix();
        matrices.translate(0, altsHeight);

        if (altsListOpen && scrolledAmount > maxScroll) {
            Render2dUtil.drawPolygon(
                    new int[] {(bounds.x + bounds.width / 2) - 5, (bounds.x + bounds.width / 2) + 5, (bounds.x + bounds.width / 2)},
                    new int[] {bounds.y, bounds.y, bounds.y+5},
                    new Color(22, 22, 22, 100),
                    false
            );
            //Render2dUtil.drawGradientDown(bounds.x, -bounds.y/2, bounds.width, bounds.height, new Color(22, 22, 22, 0), new Color(22, 22, 22, 100));
        }

        // drawing the add account button
        Render2dUtil.drawRect(bounds.x, bounds.y, bounds.width, ADD_ACCOUNT_HEIGHT, new Color(22, 22, 22, 100));
        // i think this text being centred is cuter
        int addTextX = bounds.x + (bounds.width/2) - (FontUtil.getStringWidth(ADD_ACCOUNT_TEXT, ADD_ACCOUNT_FONT_SIZE)/2);
        int addTextY = bounds.y + (ADD_ACCOUNT_HEIGHT/2) - (FontUtil.getFontHeight(ADD_ACCOUNT_FONT_SIZE)/2);
        FontUtil.drawString(ADD_ACCOUNT_TEXT, addTextX, addTextY, ADD_ACCOUNT_FONT_SIZE, Color.WHITE, false);

        matrices.popMatrix();
    }

    private static int getHeadDiameter() {
        return FontUtil.getFontHeight(ACCOUNTS_FONT_SIZE) + GAP_WIDTH;
    }

    private static Rectangle getAltBounds() {
        int headDiameter = getHeadDiameter();

        int longestNameWidth = 0;
        for (AltManagerUtil.Alt alt : AltManagerUtil.ALTS) {
            longestNameWidth = Math.max(longestNameWidth, FontUtil.getStringWidth(alt.name, ACCOUNTS_FONT_SIZE));
        }

        int width = Math.max(
                // in case u have only 1/2 letter ign alts cause ur baller
                (ADD_ACCOUNT_MINIMUM_INDENT*2) + FontUtil.getStringWidth(ADD_ACCOUNT_TEXT, ADD_ACCOUNT_FONT_SIZE),
                // GAP_WIDTH * 5 because gap left and right of head, 2 gaps between text and remove indicator, and 1 gap right of remove indicator
                longestNameWidth + (GAP_WIDTH * 5) + headDiameter + getRemoveIndicatorWidth()
        );

        return new Rectangle(
                C.res().getScaledWidth() - ALT_MANAGER_INDENT_X - width,
                ALT_MANAGER_INDENT_Y,
                width,
                headDiameter + (GAP_WIDTH *2)
        );
    }

    private static int getRemoveIndicatorWidth() {
        return FontUtil.getStringWidth(REMOVE_ACCOUNT_INDICATOR, ACCOUNTS_FONT_SIZE);
    }

    private static Rectangle getRemoveIndicatorBounds() {
        Rectangle bounds = getAltBounds();
        int removeIndicatorWidth = getRemoveIndicatorWidth();
        int fontHeight = FontUtil.getFontHeight(ACCOUNTS_FONT_SIZE);

        return new Rectangle(
                bounds.x + bounds.width - removeIndicatorWidth - GAP_WIDTH,
                ALT_MANAGER_INDENT_Y + (bounds.height/2) - (fontHeight/2),
                getRemoveIndicatorWidth(),
                fontHeight
        );
    }

    private static List<AltManagerUtil.Alt> getAltsList() {
        if (!altsListOpen) return List.of(AltManagerUtil.currentAlt);
        AltManagerUtil.ALTS.sort(Comparator.comparing(alt -> -alt.lastLogin));
        return AltManagerUtil.ALTS;
    }

    public static boolean isHoveringAlts() {
        Rectangle bounds = getAltBounds();
        return MouseUtil.isMouseInBounds(new Rectangle(
                bounds.x,
                bounds.y,
                bounds.width,
                getAltsHeight() + ADD_ACCOUNT_HEIGHT
        ), false);
    }

    protected static int getAltsHeight() {
        Rectangle bounds = getAltBounds();
        return bounds.height * Math.min(MAX_ALTS_DISPLAY, getAltsList().size());
    }

    protected static boolean handleClick(Click click) {
        if (!isHoveringAlts()) {
            altsListOpen = false;
            return false;
        }
        if (click.button() != 0) return false;

        Rectangle accountBounds = getAltBounds();
        accountBounds.y += AltManager.scrolledAmount;

        Rectangle removeIndicatorBounds = getRemoveIndicatorBounds();
        removeIndicatorBounds.y += AltManager.scrolledAmount;

        List<AltManagerUtil.Alt> altsList = getAltsList();
        for (int i = 0; i < altsList.size(); i++) {
            AltManagerUtil.Alt alt = altsList.get(i);

            if (MouseUtil.isMouseInBounds(accountBounds, false)) {
                if (i == 0) altsListOpen = !altsListOpen;
                else if (MouseUtil.isMouseInBounds(removeIndicatorBounds, false)) {
                    alt.delete();
                }
                else {
                    C.ASYNC.execute(() -> {
                        if (alt.login()) altsListOpen = false;
                    });
                }
                return true;
            }

            accountBounds.y += accountBounds.height;
            removeIndicatorBounds.y += accountBounds.height;
        }

        if (MouseUtil.isMouseInBounds(accountBounds.x, accountBounds.y, accountBounds.width, ADD_ACCOUNT_HEIGHT, false)) {
            try {
                // minecraft doesnt let u set clipboard or open browser normally </3
                C.mc.keyboard.setClipboard(AuthServer.URL);
                Util.getOperatingSystem().open(new URI(AuthServer.URL));
                return true;
            } catch (URISyntaxException e) {
                e.printStackTrace();
                AltManagerUtil.setErrorMessage("Failed to open oAuth url");
            }
        }

        altsListOpen = false;

        return false;
    }
}
