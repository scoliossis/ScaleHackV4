package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;

import java.awt.*;

@RegisterModule(
        name = "HUD",
        enabledByDefault = true
)
public class HUD extends Module {
    private static final String CLIENT_NAME = Main.MOD_NAME.split(" ")[0];
    private static final String GAMESENSE_TAG = "Sense";

    private static final int GAMESENSE_FONT_SIZE = 10;
    private static final int TEXT_INDENT_X = 2;
    private static final int TEXT_INDENT_Y = 2;

    public final Draggable gamesenseWatermark = new Draggable(
            "Gamesense",
            () -> {
                String server = C.mc.isInSingleplayer() ? "singleplayer" : C.mc.getServer() != null ? C.mc.getServer().getServerIp() : "unknown";
                long ping = C.mc.getCurrentServerEntry() != null ? C.mc.getCurrentServerEntry().ping : 0;

                String remainingHudText = " | " + C.mc.getCurrentFps() + "fps | " + server + " | " + ping + "ms";

                int clientNameWidth = FontUtil.getStringWidth(CLIENT_NAME, GAMESENSE_FONT_SIZE);
                int gamesenseWidth = FontUtil.getStringWidth(GAMESENSE_TAG, GAMESENSE_FONT_SIZE);
                int remainingWidth = FontUtil.getStringWidth(remainingHudText, GAMESENSE_FONT_SIZE);

                int innerBoxWidth = clientNameWidth + gamesenseWidth + remainingWidth + (TEXT_INDENT_X * 2);
                int innerBoxHeight = FontUtil.getFontHeight(GAMESENSE_FONT_SIZE) + (TEXT_INDENT_Y * 2);

                Render2dUtil.drawSkeetRect(
                        0,
                        0,
                        innerBoxWidth,
                        FontUtil.getFontHeight(GAMESENSE_FONT_SIZE) + (TEXT_INDENT_Y * 2),
                        new Color(255, 117, 117)
                );

                FontUtil.drawString(CLIENT_NAME, TEXT_INDENT_X, TEXT_INDENT_Y, GAMESENSE_FONT_SIZE, new Color(255, 255, 255), false);
                FontUtil.drawString(GAMESENSE_TAG, TEXT_INDENT_X + clientNameWidth, TEXT_INDENT_Y, GAMESENSE_FONT_SIZE, new Color(255, 117, 117), false);
                FontUtil.drawString(remainingHudText, TEXT_INDENT_X + clientNameWidth + gamesenseWidth, TEXT_INDENT_Y, GAMESENSE_FONT_SIZE, new Color(255, 255, 255), false);

                return new int[] {innerBoxWidth, innerBoxHeight};
            },
            e -> this.isEnabled(),
            e -> true
    );

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
