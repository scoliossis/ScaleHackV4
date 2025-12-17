package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import net.minecraft.util.Identifier;

import java.awt.*;

@RegisterModule(name = "HUD")
public class HUD extends Module {
    private static final String CLIENT_NAME = Main.MOD_NAME.split(" ")[0];
    private static final String GAMESENSE_TAG = "Sense";

    private static final int GAMESENSE_FONT_SIZE = 10;
    private static final int GAMESENSE_BOX_HEIGHT = 20;

    public final Event onDrawScreen = new RenderTickEvent(this, (e) -> {
        String server = C.mc.isInSingleplayer() ? "singleplayer" : C.mc.getServer() != null ? C.mc.getServer().getServerIp() : "unknown";
        String remainingHudText = " " + Main.MOD_VERSION + " | " + C.mc.getCurrentFps() + " fps | " + server;

        int clientNameWidth = FontUtil.getStringWidth(CLIENT_NAME, GAMESENSE_FONT_SIZE);
        int gamesenseWidth = FontUtil.getStringWidth(GAMESENSE_TAG, GAMESENSE_FONT_SIZE);
        int remainingWidth = FontUtil.getStringWidth(remainingHudText, GAMESENSE_FONT_SIZE);

        int innerBoxWidth = clientNameWidth + gamesenseWidth + remainingWidth + 6;
        int outerBoxWidth = innerBoxWidth + 6;

        Render2dUtil.drawRect(0, 0, outerBoxWidth, GAMESENSE_BOX_HEIGHT, new Color(60, 60, 60));
        Render2dUtil.drawRect(1, 1, outerBoxWidth-2, GAMESENSE_BOX_HEIGHT-2, new Color(40, 40, 40));
        Render2dUtil.drawRect(2, 2, innerBoxWidth+2, GAMESENSE_BOX_HEIGHT-4, new Color(60, 60, 60));
        Render2dUtil.drawRect(3, 3, innerBoxWidth, GAMESENSE_BOX_HEIGHT-6, new Color(22, 22, 22));

        int textY = 1 + (GAMESENSE_BOX_HEIGHT / 2) - (FontUtil.getFontHeight(GAMESENSE_FONT_SIZE) / 2);
        FontUtil.drawString(CLIENT_NAME, 6, textY, GAMESENSE_FONT_SIZE, new Color(255,255,255), false);
        FontUtil.drawString(GAMESENSE_TAG, 6 + clientNameWidth, textY, GAMESENSE_FONT_SIZE, new Color(255, 117, 117), false);
        FontUtil.drawString(remainingHudText, 6 + clientNameWidth + gamesenseWidth, textY, GAMESENSE_FONT_SIZE, new Color(255,255,255), false);

        Render2dUtil.drawRect(3, 3, innerBoxWidth, 1, new Color(255, 117, 117));

        return false;
    });
}
