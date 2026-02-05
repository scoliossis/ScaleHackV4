package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import lombok.AllArgsConstructor;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

// straight ripped from https://github.com/scoliossis/ScaleHackOpenSrc which isnt great
// todo: yeah rewrite most of this.
public class FontUtil {
    private static Fonts currentFont;

    private static final HashMap<Integer, FontTexture> fontTextures = new HashMap<>();

    private final static Graphics2D DUMMY_GRAPHICS = setAntiAliasing(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());

    @AllArgsConstructor
    private static class FontTexture {
        public TextureSetup texture;
        public HashMap<Character, CharacterInfo> charBounds;
        public int width;
        public int height;
    }

    @AllArgsConstructor
    private static class UnrenderedCharacter {
        public char character;
        public int x;
        public Color colour;
    }

    @AllArgsConstructor
    private static class CharacterInfo {
        public double u;
        public double uw;
        public int width;
    }

    public static void setCurrentFont(Fonts font) {
        fontTextures.clear();
        currentFont = font;
    }

    private static FontTexture getFontTexture(int size) {
        FontTexture fontTexture = fontTextures.get(size);
        if (fontTexture != null) return fontTexture;

        Font resizedFont = currentFont.font.deriveFont((float) size);

        fontTexture = createFontTexture(resizedFont, getFontTextureBounds(resizedFont, LETTERS));
        fontTextures.put(size, fontTexture);

        return fontTexture;
    }

    // list by minecraft in FontRenderer.renderChar, removed all glyphs.
    private static final String LETTERS = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵž !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»πμΩ∞±≥≤÷≈°∙·√²\u0000";
    private static final String COLOUR_CODES = "0123456789abcdef";

    private static final int X_SPACING = 10;
    private static FontTexture createFontTexture(Font font, Rectangle stringBounds) {
        int textureWidth = stringBounds.width + (LETTERS.length() * X_SPACING);

        BufferedImage texture = new BufferedImage(textureWidth, stringBounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = setAntiAliasing(texture.createGraphics());
        graphics.setColor(Color.WHITE);
        graphics.drawRect(0, 0, 1, stringBounds.height);

        graphics.setFont(font);

        HashMap<Character, CharacterInfo> charBounds = new HashMap<>();
        double x = X_SPACING;
        for (char c : LETTERS.toCharArray()) {
            double width = getFontTextureBounds(font, String.valueOf(c)).getWidth();

            double u = x / textureWidth;
            double uw = (x + width) / textureWidth;
            charBounds.put(c, new CharacterInfo(u, uw, (int) width));

            graphics.drawString(String.valueOf(c), (int) x, font.getSize());

            x += width + X_SPACING;
        }

        graphics.dispose();

        return new FontTexture(RenderUtil.getTexture(texture, font.getFontName()+font.getSize()), charBounds, stringBounds.width, stringBounds.height);
    }

    private static Rectangle getFontTextureBounds(Font font, String string) {
        DUMMY_GRAPHICS.setFont(font);
        // getStringBounds takes into account the antialiasing i think.
        return DUMMY_GRAPHICS.getFontMetrics().getStringBounds(string, DUMMY_GRAPHICS).getBounds();
    }

    private static Graphics2D setAntiAliasing(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        return graphics;
    }

    public static float drawStringFade(String string, float startX, float startY, int fontSize, Color[] colour, double fadeSpeed, double fadeSpread, boolean dropShadow) {
        return drawStringFade(string, startX, startY, fontSize, colour, fadeSpeed, fadeSpread, dropShadow, false);
    }

    public static float drawStringFade(String string, float startX, float startY, int fontSize, Color[] colour, double fadeSpeed, double fadeSpread, boolean dropShadow, boolean queue) {
        if (string.isEmpty()) return 0;
        Color[] inputColour = colour;

        float scaleFactor = getScaleFactor();
        int size = (int) (fontSize * scaleFactor);

        FontTexture fontTexture = getFontTexture(size);

        float x = Math.round(startX);
        float y = Math.round(startY);
        int fontHeight = fontTexture.height;

        Matrix3x2f matrices = Render2dUtil.getMatrices();
        Render2dUtil.drawVertexes(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, fontTexture.texture, (vertices) -> {
            matrices.translate(x, y);
            float downScale = 1f / scaleFactor;
            matrices.scale(downScale);

            // todo:
            //ArrayList<UnrenderedCharacter> unrenderedCharacters = new ArrayList<>();

            boolean randomStyle = false, boldStyle = false, strikethroughStyle = false, underlineStyle = false, italicStyle = false;

            int totalWidth = 0;
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);

                if (c == '§') {
                    if (i + 2 > string.length()) continue;
                    String colourCodeString = string.substring(i + 1, i + 2);
                    char colourCode = colourCodeString.charAt(0);
                    if (COLOUR_CODES.contains(colourCodeString)) {
                        Formatting formatting = Formatting.byCode(colourCode);
                        if (formatting == null || formatting.getColorValue() == null) continue;

                        Color colourCodeColour = new Color(formatting.getColorValue());
                        for (int j = 0; j < colour.length; j++)
                            colour[j] = new Color(colourCodeColour.getRed(), colourCodeColour.getGreen(), colourCodeColour.getBlue(), colour[j].getAlpha());
                        randomStyle = boldStyle = strikethroughStyle = underlineStyle = italicStyle = false;
                    } else {
                        switch (colourCode) {
                            case 'k':
                                randomStyle = true;
                                break;
                            case 'l':
                                boldStyle = true;
                                break;
                            case 'm':
                                strikethroughStyle = true;
                                break;
                            case 'n':
                                underlineStyle = true;
                                break;
                            case 'o':
                                italicStyle = true;
                                break;

                            default:
                                for (int j = 0; j < colour.length; j++)
                                    colour[j] = new Color(inputColour[j].getRed(), inputColour[j].getGreen(), inputColour[j].getBlue(), colour[j].getAlpha());
                                randomStyle = boldStyle = strikethroughStyle = underlineStyle = italicStyle = false;
                                break;
                        }
                    }

                    i++;
                    continue;
                }

                if (randomStyle) c = scrambleCharacter(c, size);

                CharacterInfo characterBounds = fontTexture.charBounds.get(c);

                if (characterBounds == null) {
                    // todo:
                    /*
                    int characterWidth = getMinecraftCharWidth(c, size);
                    Color colourFade = RenderUtil.getColorsFade((x + totalWidth) * fadeSpread, colour, fadeSpeed);

                    unrenderedCharacters.add(new UnrenderedCharacter(c, (int) (totalWidth / scaleFactor), colourFade));
                    totalWidth += characterWidth;
                     */
                    continue;
                }

                float u = (float) characterBounds.u;
                float uw = (float) characterBounds.uw;
                int characterWidth = characterBounds.width;

                // todo:
                //Color[] colourFade = RenderUtil.getColorsFade((x + totalWidth) * fadeSpread, characterWidth * fadeSpread, colour, fadeSpeed);
                Color[] colourFade = new Color[]{colour[0], colour[1]};

                if (dropShadow) {
                    // in the 1.8.9 version, there is a fun fact here,
                    // but i removed it cause im not bothered enough to fact check if for this version
                    Color[] shadowColours = new Color[]{
                            new Color(22, 22, 22, colourFade[0].getAlpha()),
                            new Color(22, 22, 22, colourFade[1].getAlpha())
                    };
                    drawCharacter(totalWidth + 1, 1, characterWidth, fontHeight, shadowColours, u, uw, vertices, matrices);
                }
                drawCharacter(totalWidth, 0, characterWidth, fontHeight, colourFade, u, uw, vertices, matrices);
                if (boldStyle) drawCharacter(totalWidth + 1, 0, characterWidth, fontHeight, colourFade, u, uw, vertices, matrices);

                float filledTextureUW = 1f / fontTexture.width;
                if (underlineStyle)
                    drawCharacter(totalWidth, fontHeight * 0.8f, characterWidth, 1, colourFade, 0, filledTextureUW, vertices, matrices);
                if (strikethroughStyle)
                    drawCharacter(totalWidth, fontHeight / 2f - 1, characterWidth, 2, colourFade, 0, filledTextureUW, vertices, matrices);

                // todo: impl italics, sounds like effort and they dont look good anyway.

                totalWidth += characterWidth;
            }

            return true;
        }, queue);

        // todo:
        /*
        for (UnrenderedCharacter unrenderedCharacter : unrenderedCharacters) {
            drawMinecraftString(String.valueOf(unrenderedCharacter.character), x + unrenderedCharacter.x, y, size / scaleFactor, unrenderedCharacter.colour, dropShadow);
        }
         */

        //return totalWidth / scaleFactor;
        return 0;
    }

    private static void drawCharacter(float x, float y, float w, float h, Color[] colours, float u, float uw, VertexConsumer vertexConsumer, Matrix3x2f matrices) {
        vertexConsumer.vertex(matrices, x, y+h).color(colours[0].getRGB()).texture(u, 1);
        vertexConsumer.vertex(matrices, x + w, y+h).color(colours[1].getRGB()).texture(uw, 1);
        vertexConsumer.vertex(matrices, x + w, y).color(colours[1].getRGB()).texture(uw, 0);
        vertexConsumer.vertex(matrices, x, y).color(colours[0].getRGB()).texture(u, 0);
    }

    public static float drawString(String string, float x, float y, int size, Color colour, boolean dropShadow) {
        return drawStringFade(string, x, y, size, new Color[] {colour, colour}, 0, 0, dropShadow);
    }

    public static float drawString(String string, float x, float y, int size, Color colour, boolean dropShadow, boolean queue) {
        return drawStringFade(string, x, y, size, new Color[] {colour, colour}, 0, 0, dropShadow, queue);
    }

    public static Random fontRandom = new Random();
    private static char scrambleCharacter(char c, int size) {
        int characterWidth = getCharWidth(c, size);

        char scrambledCharacter;
        while (getCharWidth((scrambledCharacter = LETTERS.charAt(fontRandom.nextInt(LETTERS.length()))), size) != characterWidth);

        return scrambledCharacter;
    }

    public static void drawCenteredString(String string, float x, float y, int size, Color color, boolean dropShadow) {
        drawString(string, x - getStringWidth(string, size) / 2f, y, size, color, dropShadow);
    }

    public static int getCharWidth(char c, int fontSize) {
        FontTexture fontTexture = getFontTexture(fontSize);
        // todo:
        //if (fontTexture.charBounds.get(c) == null) return C.mc.fontRendererObj.getCharWidth(c);
        if (fontTexture.charBounds.get(c) == null) return 0;
        return fontTexture.charBounds.get(c).width;
    }

    public static int getStringWidth(String string, int fontSize) {
        float scaleFactor = getScaleFactor();
        fontSize = (int) (fontSize * scaleFactor);

        int width = 0;
        FontTexture fontTexture = getFontTexture(fontSize);

        HashMap<Character, CharacterInfo> charBounds = fontTexture.charBounds;

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            CharacterInfo characterRect = charBounds.get(c);
            if (c == '§')  i++;
            // todo:
            //else if (characterRect == null) width += getMinecraftCharWidth(c, fontSize);
            else if (characterRect == null) width += 0;
            else width += characterRect.width;
        }

        return (int) (width / scaleFactor);
    }

    public static int getFontHeight(int fontSize) {
        float scaleFactor = getScaleFactor();
        fontSize = (int) (fontSize * scaleFactor);

        FontTexture fontTexture = getFontTexture(fontSize);
        return (int) (fontTexture.height / scaleFactor);
    }

    private static float getScaleFactor() {
        Matrix3x2f matrix = RenderUtil.getDrawContext().getMatrices();
        return (float) Math.sqrt(matrix.m00() * matrix.m00() + matrix.m10() * matrix.m10()) * C.res().getScaleFactor();
    }

    // todo:
    /*
    private static int getMinecraftCharWidth(char string, double size) {
        return (int) (C.mc.fontRendererObj.getCharWidth(string) * 0.1 * size);
    }

    private static int getMinecraftStringWidth(String string, double size) {
        return (int) (C.mc.fontRendererObj.getStringWidth(string) * 0.1 * size);
    }
     */

    /*
    private static int drawMinecraftString(String string, float x, float y, double size, Color colour, boolean dropShadow) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y + size / 3, 0);
        GL11.glScaled(0.1 * size, 0.1 * size, 1);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GlStateManager.enableAlpha();

        FontRendererBridge fontRendererBridge = FontRendererBridge.from(C.mc.fontRendererObj);
        fontRendererBridge.bridge$resetStyles();
        int i;
        if (dropShadow) {
            i = fontRendererBridge.bridge$renderString(string, 1.0F, 1.0F, colour.getRGB(), true);
            i = Math.max(i, fontRendererBridge.bridge$renderString(string, 0, 0, colour.getRGB(), false));
        } else {
            i = fontRendererBridge.bridge$renderString(string, 0, 0, colour.getRGB(), false);
        }

        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        return i;
    }
     */

    public enum Fonts {
        Verdana("verdana.ttf"),
        Atkinson("atkinson.ttf"),
        Product_Sans("productsans.ttf"),
        Comic_Sans("comicsans.ttf"),
        Segoe_Print("segoeprint.ttf"),
        Jetbrains_Mono("JetbrainsMono.ttf"),
        Love_Days("lovedays.ttf"),
        Thug_Font("thugfont.otf");

        public final Font font;

        Fonts(String name) {
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 0);

            try {
                font =  Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Main.class.getResourceAsStream("/fonts/" + name)));
            } catch (Exception e) {
                System.err.println("Failed to load font: " + e.getMessage());
                e.printStackTrace();
            }

            this.font = font;
        }
    }
}