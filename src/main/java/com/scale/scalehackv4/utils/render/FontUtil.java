package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import lombok.AllArgsConstructor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// straight ripped from https://github.com/scoliossis/ScaleHackV3 which isnt great
// todo: yeah rewrite this.
public class FontUtil {
    private static Fonts currentFont;

    private static final HashMap<Integer, FontTexture> fontTextures = new HashMap<>();

    private final static Graphics2D DUMMY_GRAPHICS = setAntiAliasing(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());

    @AllArgsConstructor
    private static class FontTexture {
        public TextureSetup texture;
        public Identifier identifier;
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

        BufferedImage texture = new BufferedImage(textureWidth, Math.max(1, stringBounds.height), BufferedImage.TYPE_INT_ARGB);
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

        String textureName = "font/" + font.getFontName().toLowerCase().replace(" ", "_") + "_" + font.getSize();
        Identifier identifier = Identifier.of(Main.MOD_ID, textureName);

        AbstractTexture abstractTexture = RenderUtil.createTexture(identifier, texture);

        return new FontTexture(RenderUtil.getTexture(abstractTexture), identifier, charBounds, stringBounds.width, stringBounds.height);
    }

    private static Rectangle getFontTextureBounds(Font font, String string) {
        DUMMY_GRAPHICS.setFont(font);
        // getStringBounds takes into account the antialiasing i think. whereas getStringWidth DOESNT!
        return DUMMY_GRAPHICS.getFontMetrics().getStringBounds(string, DUMMY_GRAPHICS).getBounds();
    }

    private static Graphics2D setAntiAliasing(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        return graphics;
    }

    public static void drawStringFade(Text text, float startX, float startY, int fontSize, Color colour, double fadeSpeed, double fadeSpread, boolean dropShadow) {
        drawStringFade(text, startX, startY, 0, fontSize, colour, fadeSpeed, fadeSpread, dropShadow, false, false);
    }

    public static void drawStringFade(Text text, float startX, float startY, float startZ, int fontSize, Color colour, double fadeSpeed, double fadeSpread, boolean dropShadow, boolean queue, boolean world) {
        if (text == ScreenTexts.EMPTY) return;

        float upscale = getFontUpscale(world);
        int scaledFont = getFontSize(fontSize, world);

        FontTexture fontTexture = getFontTexture(scaledFont);

        Object matrices = world ? Render3dUtil.getMatrices().peek() : Render2dUtil.getMatrices();
        if (matrices == null) return;

        Predicate<VertexConsumer> drawTextRunnable = getDrawTextRunnable(text, colour, fadeSpeed, fadeSpread, dropShadow, matrices, fontTexture, scaledFont, upscale);

        if (world) {
            ((MatrixStack.Entry) matrices).translate(startX, startY, startZ);
            // flip it upside down!
            ((MatrixStack.Entry) matrices).scale(-upscale, -upscale, -upscale);
            Render3dUtil.drawTexturedQuads(drawTextRunnable, fontTexture.identifier, false);
        } else {
            ((Matrix3x2f) matrices).translate(Math.round(startX), Math.round(startY));
            ((Matrix3x2f) matrices).scale(upscale);
            RenderUtil.drawVertexes(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, fontTexture.texture, drawTextRunnable, queue);
        }

        // drawing minecraft characters
        float width = 0;
        for (Text drawableString : fixText(text, colour)) {
            Style textStyle = drawableString.getStyle();
            if (textStyle.getColor() != null) colour = new Color(textStyle.getColor().getRgb());
            for (char c : drawableString.getString().toCharArray()) {
                if (fontTexture.charBounds.get(c) == null) {
                    drawMinecraftString(String.valueOf(c), startX + width, startY, startZ, fontSize, colour, dropShadow, world);
                }
                width += getCharWidth(c, fontSize);
            }
        }
    }

    private static Predicate<VertexConsumer> getDrawTextRunnable(Text text, Color finalColour, double fadeSpeed, double fadeSpread, boolean dropShadow, Object matrices, FontTexture fontTexture, int size, float scaleFactor) {
        int fontHeight = fontTexture.height;

        return (vertices) -> {
            Color colour = finalColour;
            float totalWidth = 0;
            boolean randomStyle, boldStyle, strikethroughStyle, underlineStyle, italicStyle;

            for (Text drawableString : fixText(text, colour)) {
                Style textStyle = drawableString.getStyle();
                randomStyle = textStyle.isObfuscated();
                underlineStyle = textStyle.isUnderlined();
                strikethroughStyle = textStyle.isStrikethrough();
                boldStyle = textStyle.isBold();
                italicStyle = textStyle.isItalic();

                if (textStyle.getColor() != null) {
                    colour = new Color(textStyle.getColor().getRgb());
                }

                for (int i = 0; i < drawableString.getString().length(); i++) {
                    char c = drawableString.getString().charAt(i);

                    if (randomStyle) c = scrambleCharacter(c, size);

                    CharacterInfo characterBounds = fontTexture.charBounds.get(c);

                    if (characterBounds == null) {
                        float characterWidth = getMinecraftCharWidth(c, size);
                        // todo:
                        //Color colourFade = RenderUtil.getColorsFade((x + totalWidth[0]) * fadeSpread, colour, fadeSpeed);

                        totalWidth += characterWidth;
                        continue;
                    }

                    float u = (float) characterBounds.u;
                    float uw = (float) characterBounds.uw;
                    int characterWidth = characterBounds.width;

                    if (dropShadow) {
                        // in the 1.8.9 version, there is a fun fact here,
                        // but i removed it cause im not bothered enough to fact check if for this version
                        drawCharacter(totalWidth + 1, 1, characterWidth, fontHeight, new Color(22, 22, 22, colour.getAlpha()), u, uw, vertices, matrices);
                    }
                    drawCharacter(totalWidth, 0, characterWidth, fontHeight, colour, u, uw, vertices, matrices);
                    if (boldStyle)
                        drawCharacter(totalWidth + 1, 0, characterWidth, fontHeight, colour, u, uw, vertices, matrices);

                    float filledTextureUW = 1f / fontTexture.width;
                    if (underlineStyle)
                        drawCharacter(totalWidth, fontHeight * 0.8f, characterWidth, 1, colour, 0, filledTextureUW, vertices, matrices);
                    if (strikethroughStyle)
                        drawCharacter(totalWidth, fontHeight / 2f - 1, characterWidth, 2, colour, 0, filledTextureUW, vertices, matrices);

                    // todo: impl italics, sounds like effort and they dont look good anyway.

                    totalWidth += characterWidth;
                }
            }

            return true;
        };
    }

    private static void drawCharacter(float x, float y, float w, float h, Color colour, float u, float uw, VertexConsumer vertexConsumer, Object matrices) {
        if (matrices instanceof MatrixStack.Entry matrixStackEntry) {
            vertexConsumer.vertex(matrixStackEntry, x, y+h, 0).color(colour.getRGB()).texture(u, 1);
            vertexConsumer.vertex(matrixStackEntry, x + w, y+h, 0).color(colour.getRGB()).texture(uw, 1);
            vertexConsumer.vertex(matrixStackEntry, x + w, y, 0).color(colour.getRGB()).texture(uw, 0);
            vertexConsumer.vertex(matrixStackEntry, x, y, 0).color(colour.getRGB()).texture(u, 0);
        }
        else if (matrices instanceof Matrix3x2f matrix3x2f) {
            vertexConsumer.vertex(matrix3x2f, x, y+h).color(colour.getRGB()).texture(u, 1);
            vertexConsumer.vertex(matrix3x2f, x + w, y+h).color(colour.getRGB()).texture(uw, 1);
            vertexConsumer.vertex(matrix3x2f, x + w, y).color(colour.getRGB()).texture(uw, 0);
            vertexConsumer.vertex(matrix3x2f, x, y).color(colour.getRGB()).texture(u, 0);
        }
    }

    public static void drawString(String string, float x, float y, int size, Color colour, boolean dropShadow) {
        drawStringFade(Text.of(string), x, y, size, colour, 0, 0, dropShadow);
    }

    public static void drawString(Text text, float x, float y, int size, Color colour, boolean dropShadow) {
        drawStringFade(text, x, y, size, colour, 0, 0, dropShadow);
    }

    public static void drawString(String string, float x, float y, int size, Color colour, boolean dropShadow, boolean queue) {
        drawStringFade(Text.of(string), x, y, 0, size, colour, 0, 0, dropShadow, queue, false);
    }

    public static void drawString(String string, float x, float y, float z, int size, Color colour, boolean dropShadow) {
        drawStringFade(Text.of(string), x, y, z, size, colour, 0, 0, dropShadow, false, true);
    }
    public static void drawString(Text text, float x, float y, float z, int size, Color colour, boolean dropShadow) {
        drawStringFade(text, x, y, z, size, colour, 0, 0, dropShadow, false, true);
    }

    public static Random fontRandom = new Random();
    private static char scrambleCharacter(char c, int size) {
        int characterWidth = (int) getCharWidth(c, size);

        char scrambledCharacter;
        while ((int)getCharWidth((scrambledCharacter = LETTERS.charAt(fontRandom.nextInt(LETTERS.length()))), size) != characterWidth);

        return scrambledCharacter;
    }

    public static void drawCenteredString(String string, float x, float y, int size, Color color, boolean dropShadow) {
        drawString(string, x - getStringWidth(string, size) / 2f, y, size, color, dropShadow);
    }

    public static float getCharWidth(char c, int fontSize) {
        FontTexture fontTexture = getFontTexture(fontSize);
        if (fontTexture.charBounds.get(c) == null) return getMinecraftCharWidth(c, fontSize);
        return fontTexture.charBounds.get(c).width;
    }

    public static int getStringWidth(String string, int fontSize) {
        return (int) getStringWidth(string, fontSize, false);
    }
    public static float getStringWidth(String string, int fontSize, boolean world) {
        fontSize = getFontSize(fontSize, world);

        float width = 0;
        FontTexture fontTexture = getFontTexture(fontSize);

        HashMap<Character, CharacterInfo> charBounds = fontTexture.charBounds;

        string = string.replaceAll(String.valueOf(FORMATTING_CODE_PATTERN), "");
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            CharacterInfo characterRect = charBounds.get(c);
            if (characterRect == null) width += getMinecraftCharWidth(c, fontSize);
            else width += characterRect.width;
        }

        return width * getFontUpscale(world);
    }

    public static int getFontHeight(int fontSize) {
        return (int) getFontHeight(fontSize, false);
    }

    public static float getFontHeight(int fontSize, boolean world) {
        FontTexture fontTexture = getFontTexture(getFontSize(fontSize, world));
        return fontTexture.height * getFontUpscale(world);
    }

    private static int getFontSize(int fontSize, boolean world) {
        if (world) return fontSize;

        Matrix3x2f matrix = RenderUtil.getDrawContext().getMatrices();
        return (int) (fontSize * (float) Math.sqrt(matrix.m00() * matrix.m00() + matrix.m10() * matrix.m10()) * C.res().getScaleFactor());
    }

    private static float getFontUpscale(boolean world) {
        if (!world) return 1f/getFontSize(1, false);

        return 1/100f;
    }

    private static float getMinecraftCharWidth(char string, double size) {
        return (float) (C.mc.textRenderer.getWidth(String.valueOf(string)) * 0.1f * size);
    }

    private static int getMinecraftStringWidth(String string, double size) {
        return (int) (C.mc.textRenderer.getWidth(string) * 0.1 * size);
    }

    private static float drawMinecraftString(String string, float x, float y, float z, double size, Color colour, boolean dropShadow, boolean world) {
        y += (float) (size / 3f);

        if (world) {
            MatrixStack matrices3d = Render3dUtil.getMatrices();
            matrices3d.push();
            matrices3d.translate(x, y, z);
            matrices3d.scale(0.1f * (float) size, 0.1f * (float) size, 0.1f * (float) size);
            C.mc.textRenderer.draw(
                    string,
                    0,
                    0,
                    colour.getRGB(),
                    dropShadow,
                    Render3dUtil.getMatrices().peek().getPositionMatrix(),
                    Render3dUtil.getVertexConsumerProvider(),
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    0
            );
            matrices3d.pop();
        }
        else {
            Matrix3x2fStack matrices2d = RenderUtil.drawContext.getMatrices();
            matrices2d.pushMatrix();
            matrices2d.translate(x, y);
            matrices2d.scale(0.1f * (float) size);
            RenderUtil.drawContext.drawText(C.mc.textRenderer, string, 0, 0, colour.getRGB(), dropShadow);
            matrices2d.popMatrix();
        }
        return (float) (C.mc.textRenderer.getWidth(string) * size * 0.1f);
    }

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
                font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(Main.class.getResourceAsStream("/fonts/" + name)));
            } catch (Exception e) {
                System.err.println("Failed to load font: " + e.getMessage());
                e.printStackTrace();
            }

            this.font = font;
        }
    }

    // from net.minecraft.util.Formatting.FORMATTING_CODE_PATTERN
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    // cancer. im sure there is a minecraft
    private static List<Text> fixText(Text text, Color colour) {
        List<Text> texts = new ArrayList<>();

        for (Text segment : text.getWithStyle(Style.EMPTY)) {
            String raw = segment.getString();
            Style baseStyle = segment.getStyle();
            Matcher matcher = FORMATTING_CODE_PATTERN.matcher(raw);

            int lastEnd = 0;
            Formatting lastFormatting = null;

            while (matcher.find()) {
                String part = raw.substring(lastEnd, matcher.start());
                MutableText t = Text.literal(part).setStyle(baseStyle);
                if (lastFormatting != null) t.formatted(lastFormatting);
                if (lastEnd == 0) t.withColor(colour.getRGB());
                texts.add(t);

                lastFormatting = Formatting.byCode(matcher.group().charAt(1));
                lastEnd = matcher.end();
            }

            if (lastEnd < raw.length()) {
                MutableText t = Text.literal(raw.substring(lastEnd)).setStyle(baseStyle);
                if (lastFormatting != null) t.formatted(lastFormatting);
                texts.add(t);
            }
        }

        return texts;
    }
}