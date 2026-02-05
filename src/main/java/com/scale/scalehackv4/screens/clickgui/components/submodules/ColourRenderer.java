package com.scale.scalehackv4.screens.clickgui.components.submodules;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.ModuleRenderer;
import com.scale.scalehackv4.screens.clickgui.components.SubModuleRenderer;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.joml.Vector2i;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

// scalehack manages to do this in 168 lines, not sure how, but this code is much cleaner imo
public class ColourRenderer extends SubModuleRenderer {
    private static final int SMALL_BOX_HEIGHT = 4;
    private static final int SMALL_BOX_WIDTH = 8;

    private static final int COLOUR_BOX_GAP_SIZE = 3;
    private static final int COLOUR_BOX_DIAMETER = 90;

    private static final int HSB_SLIDER_WIDTH = 10;
    private static final int OPACITY_SLIDER_HEIGHT = 6;

    private static final int BIG_BOX_WIDTH = (COLOUR_BOX_GAP_SIZE*3)+COLOUR_BOX_DIAMETER+HSB_SLIDER_WIDTH;
    private static final int BIG_BOX_HEIGHT = (COLOUR_BOX_GAP_SIZE*3)+COLOUR_BOX_DIAMETER+OPACITY_SLIDER_HEIGHT;

    private static final Rectangle SHOWCASE_SQUARE = new Rectangle(-SMALL_BOX_WIDTH-1, 0, SMALL_BOX_WIDTH, SMALL_BOX_HEIGHT);

    private static final Rectangle POP_UP_SQUARE = new Rectangle(SHOWCASE_SQUARE.x, SHOWCASE_SQUARE.height + ModuleRenderer.SUBMODULE_Y_GAP, BIG_BOX_WIDTH, BIG_BOX_HEIGHT);

    private static final  Rectangle COLOUR_SQUARE = new Rectangle(POP_UP_SQUARE.x+COLOUR_BOX_GAP_SIZE, POP_UP_SQUARE.y+COLOUR_BOX_GAP_SIZE, COLOUR_BOX_DIAMETER, COLOUR_BOX_DIAMETER);
    private static final  Rectangle HUE_SLIDER_RECT = new Rectangle(COLOUR_SQUARE.x + COLOUR_BOX_DIAMETER + COLOUR_BOX_GAP_SIZE, COLOUR_SQUARE.y, HSB_SLIDER_WIDTH, COLOUR_BOX_DIAMETER);
    private static final  Rectangle OPACITY_SLIDER_RECT = new Rectangle(COLOUR_SQUARE.x, COLOUR_SQUARE.y + COLOUR_SQUARE.height + COLOUR_BOX_GAP_SIZE, COLOUR_BOX_DIAMETER, OPACITY_SLIDER_HEIGHT);

    private static final Color[] HUES = new Color[] {
            new Color(255, 0, 0),
            new Color(255, 0, 255),
            new Color(0, 0, 255),
            new Color(0, 255, 255),
            new Color(0, 255, 0),
            new Color(255, 255, 0),
            new Color(255, 0, 0),
    };

    private static Part selectedPart;
    private enum Part {
        SATURATION_BRIGHTNESS, HUE, OPACITY
    }

    private static float[] hsb = new float[] {-1, -1, -1};

    @Override
    public void render(SubModule subModule, Matrix3x2fStack matrices) {
        Color moduleValue = (Color) subModule.get();
        Color moduleRGB = new Color(moduleValue.getRGB(), false);

        FontUtil.drawString(subModule.getAnnotation().name(), SubModuleRenderer.MIDDLE_PADDING, -2, ClickGuiScreen.FONT_SIZE, Color.WHITE, true);

        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.MIDDLE_PADDING+SubModuleRenderer.getMiddleWidth(subModule)+SubModuleRenderer.getRightPadding(subModule), 0);

        Render2dUtil.drawRect(SHOWCASE_SQUARE.x, SHOWCASE_SQUARE.y, SHOWCASE_SQUARE.width, SHOWCASE_SQUARE.height, moduleValue);

        if (SubModuleRenderer.activeSubModule != subModule) {
            matrices.popMatrix();
            return;
        }

        if (hsb[0] == -1) hsb = Color.RGBtoHSB(moduleRGB.getRed(), moduleRGB.getGreen(), moduleRGB.getBlue(), null);

        DrawContext drawContext = RenderUtil.getDrawContext();
        ScreenRect scissor = drawContext.scissorStack.peekLast();

        drawContext.disableScissor();

        // colour square background
        Render2dUtil.drawRect(POP_UP_SQUARE.x-1, POP_UP_SQUARE.y-1, POP_UP_SQUARE.width+2, POP_UP_SQUARE.height+2, ClickGuiScreen.OUTLINE_COLOUR, true);
        Render2dUtil.drawRect(POP_UP_SQUARE.x, POP_UP_SQUARE.y, POP_UP_SQUARE.width, POP_UP_SQUARE.height, new Color(22, 22, 22), true);

        drawSaturationBrightnessPicker(Color.getHSBColor(hsb[0], 1, 1), moduleRGB);
        drawHueSlider(Color.getHSBColor(hsb[0], hsb[1], 1));
        drawOpacitySlider(moduleValue, moduleRGB);

        drawContext.scissorStack.push(scissor);
        matrices.popMatrix();
    }

    private static void drawSaturationBrightnessPicker(Color hueColour, Color moduleRGB) {
        if (ClickGuiModule.colourBoxType == ClickGuiModule.ColourBoxType.Normal) {
            // saturation/brightness square
            Render2dUtil.drawGradient(COLOUR_SQUARE.x, COLOUR_SQUARE.y, COLOUR_SQUARE.width, COLOUR_SQUARE.height, Color.WHITE, Color.WHITE, hueColour, hueColour, true);
            Render2dUtil.drawGradient(COLOUR_SQUARE.x, COLOUR_SQUARE.y, COLOUR_SQUARE.width, COLOUR_SQUARE.height, new Color(0,0,0,0), Color.BLACK, Color.BLACK, new Color(0,0,0,0), true);

            // show selected
            Vector2i selectLightSaturation = new Vector2i(COLOUR_SQUARE.x + (int) (COLOUR_SQUARE.width * hsb[1]), COLOUR_SQUARE.y + (int) (COLOUR_SQUARE.height * (1-hsb[2])));
            Render2dUtil.drawRect(selectLightSaturation.x-1, selectLightSaturation.y-1, 3, 3, Color.BLACK, true);
            Render2dUtil.drawRect(selectLightSaturation.x, selectLightSaturation.y, 1, 1, moduleRGB, true);
        }
        else {
            Render2dUtil.drawTexturedRect(Identifier.of("scalehackv4", "clickgui/animegirlcolourpicker.png"), COLOUR_SQUARE.x, COLOUR_SQUARE.y, COLOUR_SQUARE.width, COLOUR_SQUARE.height, Color.WHITE, true);

            Vector2i selectLightSaturation = new Vector2i(COLOUR_SQUARE.x + (int) (COLOUR_SQUARE.width * ANIME_GIRL_MOUSE_POS[0]), COLOUR_SQUARE.y + (int) (COLOUR_SQUARE.height * ANIME_GIRL_MOUSE_POS[1]));
            Render2dUtil.drawRect(selectLightSaturation.x-1, selectLightSaturation.y-1, 3, 3, Color.BLACK, true);
            Render2dUtil.drawRect(selectLightSaturation.x, selectLightSaturation.y, 1, 1, moduleRGB, true);
        }
    }

    private static void drawHueSlider(Color hueColour) {
        int hsbIndex = 0;
        if (ClickGuiModule.colourBoxType == ClickGuiModule.ColourBoxType.Normal) {
            // hue slider
            for (int i = 0; i < HUES.length - 1; i++) {
                int h = HUE_SLIDER_RECT.height / (HUES.length - 1);
                int y = HUE_SLIDER_RECT.y + h * i;
                Render2dUtil.drawGradient(HUE_SLIDER_RECT.x, y, HUE_SLIDER_RECT.width, h, HUES[i], HUES[i + 1], HUES[i + 1], HUES[i], true);
            }
        }
        else {
            // evil brightness slider, because the anime girl controls the hue+saturation instead of brightness+saturation
            Render2dUtil.drawGradient(HUE_SLIDER_RECT.x, HUE_SLIDER_RECT.y, HUE_SLIDER_RECT.width, HUE_SLIDER_RECT.height, hueColour, Color.BLACK, Color.BLACK, hueColour, true);
            hsbIndex = 2;
        }

        int selectedHueY = (int) (HUE_SLIDER_RECT.y + ((1-hsb[hsbIndex]) * HUE_SLIDER_RECT.height));
        Render2dUtil.drawRect(HUE_SLIDER_RECT.x, selectedHueY-1, HUE_SLIDER_RECT.width, 3, Color.BLACK, true);
        Render2dUtil.drawRect(HUE_SLIDER_RECT.x, selectedHueY, HUE_SLIDER_RECT.width, 1, Color.WHITE, true);
    }

    private static void drawOpacitySlider(Color moduleValue, Color moduleRGB) {
        Color transparentColour = new Color(moduleValue.getRed(), moduleValue.getGreen(), moduleValue.getBlue(), 0);
        Render2dUtil.drawFakeTransparent(OPACITY_SLIDER_RECT.x, OPACITY_SLIDER_RECT.y, OPACITY_SLIDER_RECT.width, OPACITY_SLIDER_RECT.height, true);
        Render2dUtil.drawGradient(OPACITY_SLIDER_RECT.x, OPACITY_SLIDER_RECT.y, OPACITY_SLIDER_RECT.width, OPACITY_SLIDER_RECT.height, transparentColour, transparentColour, moduleRGB, moduleRGB, true);

        int selectedOpacityX = (int) (OPACITY_SLIDER_RECT.x + ((moduleValue.getAlpha()/255f) * OPACITY_SLIDER_RECT.width));
        Render2dUtil.drawRect(selectedOpacityX-1, OPACITY_SLIDER_RECT.y, 3, OPACITY_SLIDER_RECT.height, Color.BLACK, true);
        Render2dUtil.drawRect(selectedOpacityX, OPACITY_SLIDER_RECT.y, 1, OPACITY_SLIDER_RECT.height, Color.WHITE, true);
    }

    @Override
    public boolean handleClick(SubModule subModule) {
        // handle clicking the kawaii little small square
        boolean isMouseOverModule = MouseUtil.isMouseInBounds(SubModuleRenderer.MIDDLE_PADDING, 1, SubModuleRenderer.getMiddleWidth(subModule)+SubModuleRenderer.getRightPadding(subModule)+SMALL_BOX_WIDTH+2, SMALL_BOX_HEIGHT+2, RenderUtil.getDrawContext());
        if (SubModuleRenderer.activeSubModule != subModule && isMouseOverModule) {
            selectedPart = null;
            ColourRenderer.hsb[0] = -1;
            SubModuleRenderer.activeSubModule = subModule;
            if (ClickGuiModule.colourBoxType == ClickGuiModule.ColourBoxType.Anime_Girl) {
                ANIME_GIRL_MOUSE_POS = guessPictureMousePos(ANIME_GIRL, (Color) subModule.get());
                System.out.println(ANIME_GIRL_MOUSE_POS[0] + ", " + ANIME_GIRL_MOUSE_POS[1]);
            }

            return true;
        }

        if (SubModuleRenderer.activeSubModule != subModule) return false;

        // dumb blehh
        Matrix3x2fStack matrices = RenderUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.MIDDLE_PADDING+SubModuleRenderer.getMiddleWidth(subModule)+SubModuleRenderer.getRightPadding(subModule), 0);

        // we have escaped the box. bad.
        if (!MouseUtil.isMouseInBounds(POP_UP_SQUARE, false)) {
            SubModuleRenderer.activeSubModule = null;
            matrices.popMatrix();
            return false;
        }

        // which part of the box have we interacted with? (please dont be empty space)
        if (MouseUtil.isMouseInBounds(COLOUR_SQUARE, false)) selectedPart = Part.SATURATION_BRIGHTNESS;
        else if (MouseUtil.isMouseInBounds(HUE_SLIDER_RECT, false)) selectedPart = Part.HUE;
        else if (MouseUtil.isMouseInBounds(OPACITY_SLIDER_RECT, false)) selectedPart = Part.OPACITY;
        else selectedPart = null;

        matrices.popMatrix();
        return selectedPart != null;
    }

    @Override
    public boolean handleMouseDown(SubModule subModule) {
        if (SubModuleRenderer.activeSubModule != subModule || selectedPart == null) return false;

        Color moduleValue = (Color) subModule.get();

        Matrix3x2fStack matrices = RenderUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();
        matrices.translate(SubModuleRenderer.MIDDLE_PADDING+SubModuleRenderer.getMiddleWidth(subModule)+SubModuleRenderer.getRightPadding(subModule), 0);

        switch (selectedPart) {
            case OPACITY -> {
                float projectedX = RenderUtil.projectPos(new Vector2f(OPACITY_SLIDER_RECT.x, 0), Render2dUtil.getMatrices()).x;
                float projectedW = RenderUtil.projectPos(new Vector2f(OPACITY_SLIDER_RECT.x+OPACITY_SLIDER_RECT.width, 0), Render2dUtil.getMatrices()).x-projectedX;

                double mousePos = Math.clamp(MouseUtil.getMousePos().x - projectedX, 0, OPACITY_SLIDER_RECT.width);
                double percent = Math.clamp(mousePos / projectedW, 0, 1);

                subModule.set(new Color(moduleValue.getRed(), moduleValue.getGreen(), moduleValue.getBlue(), (int) (percent * 255)));
            }
            case HUE -> {
                float projectedY = RenderUtil.projectPos(new Vector2f(0, HUE_SLIDER_RECT.y), Render2dUtil.getMatrices()).y;
                float projectedH = RenderUtil.projectPos(new Vector2f(0, HUE_SLIDER_RECT.y+HUE_SLIDER_RECT.height), Render2dUtil.getMatrices()).y-projectedY;

                float mousePos = Math.clamp(MouseUtil.getMousePos().y - projectedY, 0, HUE_SLIDER_RECT.height);

                // terrible variable name, its hard to name it, cause its used as both the HUE and DARKNESS depending on mode.
                float v = 1 - Math.clamp(mousePos / projectedH, 0, 1);

                if (ClickGuiModule.colourBoxType == ClickGuiModule.ColourBoxType.Normal) {
                    // skeet draws the hue slider upside down, so 1-percent
                    ColourRenderer.hsb[0] = v;

                    float[] hsbValues = Color.RGBtoHSB(moduleValue.getRed(), moduleValue.getGreen(), moduleValue.getBlue(), null);
                    Color colour = Color.getHSBColor(v, hsbValues[1], hsbValues[2]);

                    subModule.set(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), moduleValue.getAlpha()));
                }
                else {
                    ColourRenderer.hsb[2] = v;

                    Color colour = Color.getHSBColor(ColourRenderer.hsb[0], ColourRenderer.hsb[1], v);

                    subModule.set(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), moduleValue.getAlpha()));
                }
            }
            case SATURATION_BRIGHTNESS -> {
                Vector2f projectedXY = RenderUtil.projectPos(new Vector2f(COLOUR_SQUARE.x, COLOUR_SQUARE.y), Render2dUtil.getMatrices());
                Vector2f projectedWH = RenderUtil.projectPos(new Vector2f(COLOUR_SQUARE.x+COLOUR_SQUARE.width, COLOUR_SQUARE.y+COLOUR_SQUARE.height), Render2dUtil.getMatrices()).sub(projectedXY, new Vector2f());

                float mousePosX = Math.clamp(MouseUtil.getMousePos().x - projectedXY.x, 0, COLOUR_SQUARE.width);
                float mousePosY = Math.clamp(MouseUtil.getMousePos().y - projectedXY.y, 0, COLOUR_SQUARE.height);

                if (ClickGuiModule.colourBoxType == ClickGuiModule.ColourBoxType.Normal) {
                    float hue = ColourRenderer.hsb[0] == -1 ? Color.RGBtoHSB(moduleValue.getRed(), moduleValue.getGreen(), moduleValue.getBlue(), null)[0] : ColourRenderer.hsb[0];
                    ColourRenderer.hsb[1] = Math.clamp(mousePosX / projectedWH.x, 0, 1);

                    // darkness = 0, so the further in the mouse is, the lower the number should be, as darkness is drawn at the bottom
                    ColourRenderer.hsb[2] = 1 - Math.clamp(mousePosY / projectedWH.y, 0, 1);

                    Color newColour = Color.getHSBColor(hue, ColourRenderer.hsb[1], ColourRenderer.hsb[2]);
                    newColour = new Color(newColour.getRed(), newColour.getGreen(), newColour.getBlue(), moduleValue.getAlpha());
                    subModule.set(newColour);
                }
                else {
                    // anime girl is so different wowowow
                    float mousePercentX = mousePosX / COLOUR_SQUARE.width;
                    float mousePercentY = mousePosY / COLOUR_SQUARE.height;

                    float colourX = Math.clamp(mousePercentX * ANIME_GIRL.getWidth(), 0, ANIME_GIRL.getWidth());
                    float colourY = Math.clamp(mousePercentY * ANIME_GIRL.getHeight(), 0, ANIME_GIRL.getHeight());

                    Color pixelColour = new Color(ANIME_GIRL.getRGB((int) colourX, (int) colourY));
                    float[] hsb = Color.RGBtoHSB(pixelColour.getRed(), pixelColour.getGreen(), pixelColour.getBlue(), null);

                    // hacky blehhh
                    ANIME_GIRL_MOUSE_POS[0] = mousePercentX;
                    ANIME_GIRL_MOUSE_POS[1] = mousePercentY;

                    ColourRenderer.hsb[0] = hsb[0];
                    ColourRenderer.hsb[1] = hsb[1];

                    subModule.set(Color.getHSBColor(hsb[0], hsb[1], ColourRenderer.hsb[2]));
                }
            }
        }

        matrices.popMatrix();

        return true;
    }

    // i hate these huge chunks of uncommented code, but like, what do i really comment here.
    private static float[] guessPictureMousePos(BufferedImage image, Color colour) {
        float[] hsb = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);

        float[] closestPos = new float[] {0, 0};
        int closestDistance = Integer.MAX_VALUE;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color pixelColour = new Color(image.getRGB(x, y));
                float[] hsbPixelColour = Color.RGBtoHSB(pixelColour.getRed(), pixelColour.getGreen(), pixelColour.getBlue(), null);
                Color adjustedColour = Color.getHSBColor(hsbPixelColour[0], hsbPixelColour[1], hsb[2]);

                int distance = Math.abs(colour.getRed() - adjustedColour.getRed()) + Math.abs(colour.getGreen() - adjustedColour.getGreen()) + Math.abs(colour.getBlue() - adjustedColour.getBlue());

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPos[0] = x / (float) image.getWidth();
                    closestPos[1] = y / (float) image.getHeight();
                }
            }
        }

        return closestPos;
    }

    @Override
    public void handleMouseRelease(SubModule subModule) {
        if (SubModuleRenderer.activeSubModule == subModule) {
            selectedPart = null;
        }
    }

    private static float[] ANIME_GIRL_MOUSE_POS = new float[] {0, 0};

    private static final BufferedImage ANIME_GIRL = getAnimeGirl();

    private static BufferedImage getAnimeGirl() {
        try {
            return ImageIO.read(Main.class.getResourceAsStream("/assets/scalehackv4/clickgui/animegirlcolourpicker.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}