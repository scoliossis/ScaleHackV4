package com.scale.scalehackv4.utils.render;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.impl.client.HudModule;
import com.scale.scalehackv4.modules.impl.client.OptionsModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterListeners
public class FontUtil {
    private static final Graphics2D DUMMY_GRAPHICS = getDummyGraphics();

    private static long STRING_INDEX = 0;

    private static final HashMap<UniqueText, RenderableTexture> HASHED_STRINGS = new HashMap<>();

    private record UniqueText(Text text, Font font, int size, Color colour) { }
    private record RenderableTexture(Identifier identifier, AbstractTexture closableTexture, int width, int height, long time) { }

    // list by minecraft in FontRenderer.renderChar, removed all glyphs.
    // todo: render minecraft glyphs seperate.
    private static final String LETTERS = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵž !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»πμΩ∞±≥≤÷≈°∙·√²\u0000";

    public static void drawString(String string, int x, int y, int size, Color colour, boolean shadow) {
        drawString(string, x, y, size, colour, shadow, false);
    }
    public static void drawString(String string, int x, int y, int size, Color colour, boolean shadow, boolean queue) {
        drawString(Text.of(string), x, y, size, colour, shadow, queue);
    }

    public static void drawString(Text text, int x, int y, int size, Color colour, boolean shadow) {
        drawString(text, x, y, size, colour, shadow, false);
    }

    public static void drawString(Text text, int x, int y, int size, Color colour, boolean shadow, boolean queue) {
        RenderableTexture renderableTexture = getStringImage(text, OptionsModule.defaultFont.font, size, colour);

        int scaleFactor = C.res().getScaleFactor();

        Matrix3x2fStack matrices = RenderUtil.getDrawContext().getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(1f/scaleFactor);

        if (shadow)
            Render2dUtil.drawTexturedRect(
                    renderableTexture.identifier,
                    scaleFactor,
                    scaleFactor,
                    renderableTexture.width,
                    renderableTexture.height,
                    new Color(0, 0, 0, 128),
                    queue
            );
        Render2dUtil.drawTexturedRect(
                renderableTexture.identifier,
                0,
                0,
                renderableTexture.width,
                renderableTexture.height,
                Color.WHITE,
                queue
        );
        matrices.popMatrix();
    }

    private static RenderableTexture getStringImage(Text text, Font font, int size, Color colour) {
        size *= C.res().getScaleFactor();

        UniqueText uniqueText = new UniqueText(text, font, size, colour);
        RenderableTexture cachedRenderableTexture = HASHED_STRINGS.get(uniqueText);
        if (cachedRenderableTexture != null) {
            HASHED_STRINGS.put(
                    uniqueText,
                    new RenderableTexture(
                            cachedRenderableTexture.identifier,
                            cachedRenderableTexture.closableTexture,
                            cachedRenderableTexture.width,
                            cachedRenderableTexture.height,
                            System.currentTimeMillis()
                    )
            );
            return cachedRenderableTexture;
        }

        List<Text> texts = fixText(text, colour);

        int width = getStyledStringWidth(texts, size) + 20;
        int height = getFontHeight(size);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = getGraphics(image);

        graphics.setColor(colour);
        graphics.setFont(OptionsModule.defaultFont.font.deriveFont((float)size));

        for (Text t : texts) {
            TextColor textColour = t.getStyle().getColor();

            int fontType = getFontType(t.getStyle());

            graphics.setFont(OptionsModule.defaultFont.font.deriveFont((float) size).deriveFont(fontType));
            // todo: if (t.getStyle().isObfuscated())

            int sectionWidth = graphics.getFontMetrics().stringWidth(t.getString());

            if (textColour != null) {
                graphics.setColor(new Color(t.getStyle().getColor().getRgb()));
            }

            graphics.drawString(t.getString(), 0, size);

            if (t.getStyle().isUnderlined())
                graphics.drawLine(0, graphics.getFontMetrics().getDescent(), sectionWidth, graphics.getFontMetrics().getDescent());
            if (t.getStyle().isStrikethrough())
                graphics.drawLine(0, height / 2, sectionWidth, height / 2);

            graphics.translate(sectionWidth, 0);
        }

        Identifier identifier = Identifier.of(Main.MOD_ID, "string/" + STRING_INDEX++);
        AbstractTexture abstractTexture = RenderUtil.createTexture(identifier, image);

        graphics.dispose();

        RenderableTexture renderableTexture = new RenderableTexture(identifier, abstractTexture, width, height, System.currentTimeMillis());
        HASHED_STRINGS.put(uniqueText, renderableTexture);

        System.out.println("new texture, currently storing: " + HASHED_STRINGS.size() + " unique strings");
        return renderableTexture;
    }

    public static int getStringWidth(String string, int size) {
        DUMMY_GRAPHICS.setFont(OptionsModule.defaultFont.font.deriveFont((float)size));

        String fixedString = FORMATTING_CODE_PATTERN.matcher(string).replaceAll("");
        return (int) DUMMY_GRAPHICS.getFontMetrics().getStringBounds(fixedString, DUMMY_GRAPHICS).getWidth();
    }

    public static int getFontHeight(int size) {
        DUMMY_GRAPHICS.setFont(OptionsModule.defaultFont.font.deriveFont((float)size));
        return (int) DUMMY_GRAPHICS.getFontMetrics().getStringBounds("", DUMMY_GRAPHICS).getHeight();
    }

    private static int getStyledStringWidth(List<Text> texts, int size) {
        int width = 0;
        for (Text text : texts) {
            DUMMY_GRAPHICS.setFont(OptionsModule.defaultFont.font.deriveFont((float) size).deriveFont(getFontType(text.getStyle())));

            String fixedString = FORMATTING_CODE_PATTERN.matcher(text.getString()).replaceAll("");
            width += (int) DUMMY_GRAPHICS.getFontMetrics().getStringBounds(fixedString, DUMMY_GRAPHICS).getWidth();
        }
        return width;
    }

    private static int getFontType(Style style) {
        if (style.isBold() && style.isItalic()) return Font.BOLD | Font.ITALIC;
        if (style.isBold()) return Font.BOLD;
        if (style.isItalic()) return Font.ITALIC;
        return Font.PLAIN;
    }

    private static Graphics2D getDummyGraphics() {
        return getGraphics(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
    }
    private static Graphics2D getGraphics(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        return graphics;
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

    // cancer. im sure there is a minecraft function that does this
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

    private static long lastClearTime = 0;
    private static final long STRING_TIMEOUT = 1000;

    public Event clearUnusedStrings = new RenderTickEvent(null, params -> {
        long now = System.currentTimeMillis();
        if (now - lastClearTime < STRING_TIMEOUT) return false;

        C.ASYNC.execute(() -> {
            for (UniqueText text : HASHED_STRINGS.keySet().toArray(UniqueText[]::new)) {
                RenderableTexture renderableTexture = HASHED_STRINGS.get(text);
                if (now - renderableTexture.time() < STRING_TIMEOUT) continue;

                HASHED_STRINGS.remove(text);
                C.mc.execute(renderableTexture.closableTexture::close);
            }

            lastClearTime = now;
        });

        return false;
    });
}
