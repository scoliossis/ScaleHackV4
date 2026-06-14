package com.scale.scalehackv4.modules.impl.client.hud;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.client.HudModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends HudModule {
    @RegisterSubModule(name = "Watermark")
    public static boolean watermark = true;
    @RegisterSubModule(name = "Visible Options", parent = "Watermark")
    public static WatermarkFeatures options = WatermarkFeatures.FPS;

    public enum WatermarkFeatures implements MultiSelectEnum {
        FPS {
            @Override
            public String getString() {
                return C.mc.getCurrentFps() + "fps";
            }
            @Override public boolean enabledByDefault() {return true;}
        },
        Server_IP {
            @Override
            public String getString() {
                return !C.isInGame() ? "Main Menu"
                        : C.mc.isInSingleplayer()
                          ? "singleplayer" : C.mc.getCurrentServerEntry() != null ? C.mc.getCurrentServerEntry().address : "unknown";
            }
            @Override public boolean enabledByDefault() {return true;}
        },
        Time {
            @Override
            public String getString() {
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            }
            @Override public boolean enabledByDefault() {return true;}
        };

        public MutableText getText() {
            MutableText defaultText = Text.literal(" | ").withColor(Color.WHITE.getRGB());
            if (MultiSelectUtil.isEnabled(this)) return defaultText.append(Text.literal(getString()));
            return Text.literal("");
        }
        public abstract String getString();
    }


    private static final String CLIENT_NAME = Main.MOD_NAME.split(" ")[0];
    private static final String GAMESENSE_TAG = "Sense";

    private static final int GAMESENSE_FONT_SIZE = 10;
    private static final int TEXT_INDENT_X = 2;

    public static final Draggable watermarkDraggable = new Draggable(
            "Watermark",
            () -> {
                MutableText hudText = Text.literal(CLIENT_NAME).withColor(Color.WHITE.getRGB()).append(Text.literal(GAMESENSE_TAG).withColor(new Color(255, 117, 117).getRGB()));
                for (WatermarkFeatures feature : WatermarkFeatures.values()) {
                    hudText.append(feature.getText());
                }

                int innerBoxWidth = FontUtil.getStringWidth(hudText.getString(), GAMESENSE_FONT_SIZE) + (TEXT_INDENT_X * 2);
                int innerBoxHeight = FontUtil.getFontHeight(GAMESENSE_FONT_SIZE);

                Render2dUtil.drawSkeetRect(0, 0, innerBoxWidth, innerBoxHeight, new Color(255, 117, 117));
                FontUtil.drawString(hudText, TEXT_INDENT_X, 0, GAMESENSE_FONT_SIZE, new Color(255, 255, 255), false);
                return new int[] {innerBoxWidth, innerBoxHeight};
            },
            e -> ModuleUtil.isEnabled(HudModule.class) && watermark,
            e -> true
    );
}
