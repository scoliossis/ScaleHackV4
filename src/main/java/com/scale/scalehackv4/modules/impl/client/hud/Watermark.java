package com.scale.scalehackv4.modules.impl.client.hud;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;

import java.awt.*;

public class Watermark extends HudModule {
    @RegisterSubModule(name = "Watermark")
    public static boolean watermark = true;

    private static final String CLIENT_NAME = Main.MOD_NAME.split(" ")[0];
    private static final String GAMESENSE_TAG = "Sense";

    private static final int GAMESENSE_FONT_SIZE = 10;
    private static final int TEXT_INDENT_X = 2;

    public final Draggable watermarkDraggable = new Draggable(
            "Watermark",
            () -> {
                String server = C.mc.isInSingleplayer() ? "singleplayer" : C.mc.getServer() != null ? C.mc.getServer().getServerIp() : "unknown";
                long ping = C.mc.getCurrentServerEntry() != null ? C.mc.getCurrentServerEntry().ping : 0;

                String remainingHudText = " | " + C.mc.getCurrentFps() + "fps | " + server + " | " + ping + "ms";

                int clientNameWidth = FontUtil.getStringWidth(CLIENT_NAME, GAMESENSE_FONT_SIZE);
                int gamesenseWidth = FontUtil.getStringWidth(GAMESENSE_TAG, GAMESENSE_FONT_SIZE);
                int remainingWidth = FontUtil.getStringWidth(remainingHudText, GAMESENSE_FONT_SIZE);

                int innerBoxWidth = clientNameWidth + gamesenseWidth + remainingWidth + (TEXT_INDENT_X * 2);
                int innerBoxHeight = FontUtil.getFontHeight(GAMESENSE_FONT_SIZE);

                Render2dUtil.drawSkeetRect(
                        0,
                        0,
                        innerBoxWidth,
                        innerBoxHeight,
                        new Color(255, 117, 117)
                );

                FontUtil.drawString(CLIENT_NAME, TEXT_INDENT_X, 0, GAMESENSE_FONT_SIZE, new Color(255, 255, 255), false);
                FontUtil.drawString(GAMESENSE_TAG, TEXT_INDENT_X + clientNameWidth, 0, GAMESENSE_FONT_SIZE, new Color(255, 117, 117), false);
                FontUtil.drawString(remainingHudText, TEXT_INDENT_X + clientNameWidth + gamesenseWidth, 0, GAMESENSE_FONT_SIZE, new Color(255, 255, 255), false);

                return new int[] {innerBoxWidth, innerBoxHeight};
            },
            e -> ModuleUtil.isEnabled(HudModule.class) && watermark,
            e -> true
    );
}
