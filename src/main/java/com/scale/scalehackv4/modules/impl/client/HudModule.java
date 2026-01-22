package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;

import java.awt.*;

@RegisterModule(
        name = "HUD",
        enabledByDefault = true,
        category = Category.CLIENT
)
public class HudModule extends Module {
    @RegisterSubModule(
            name = "Array List"
    )
    public static boolean arrayList = true;

    @RegisterSubModule(
            name = "Colour wowow"
    )
    public static Color colour = new Color(255, 0, 0);

    @RegisterSubModule(
            name = "Watermark"
    )
    public static boolean watermark = true;

    @RegisterSubModule(
            name = "smol slider",
            min = 0.001,
            max = 0.1,
            increment = 0.001
    )
    public static double smolslider = 0.01;

    @RegisterSubModule(
            name = "mid slider"
    )
    public static float midslider = 1;

    @RegisterSubModule(
            name = "big slider",
            min = 1000,
            max = 10000
    )
    public static long bigslider = 1010;

    private static final String CLIENT_NAME = Main.MOD_NAME.split(" ")[0];
    private static final String GAMESENSE_TAG = "Sense";

    private static final int GAMESENSE_FONT_SIZE = 10;
    private static final int TEXT_INDENT_X = 2;

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
