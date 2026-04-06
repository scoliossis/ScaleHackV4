package com.scale.scalehackv4.modules.impl.client.Hud;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;

import java.awt.*;

public class Watermark extends HudModule {
    @RegisterSubModule(name = "Watermark")
    public static boolean watermark = true;
    // some anticheats dont like the ping packet, including watchdog...
    @RegisterSubModule(name = "Show Ping")
    public static boolean ping = false;


    private static final String CLIENT_NAME = Main.MOD_NAME.split(" ")[0];
    private static final String GAMESENSE_TAG = "Sense";

    private static final int GAMESENSE_FONT_SIZE = 10;
    private static final int TEXT_INDENT_X = 2;

    // long because people who live on mars deserve to use this too <3
    private static long currentPing = 0;

    public final Draggable watermarkDraggable = new Draggable(
            "Watermark",
            () -> {
                String server = C.mc.isInSingleplayer() ? "singleplayer" : C.mc.getCurrentServerEntry() != null ? C.mc.getCurrentServerEntry().address : "unknown";

                String remainingHudText = " | " + C.mc.getCurrentFps() + "fps | " + server + (ping ? " | " + currentPing + "ms" : "");

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

    private static long lastPingTime = 0;

    // todo: better way to get ping
    private final Event receivePing = new PacketEvent.Received(ModuleUtil.getModule(HudModule.class), (params) -> {
        if (!watermark || !ping) return false;

        if (params.packet instanceof StatisticsS2CPacket) {
            currentPing = System.currentTimeMillis() - lastPingTime;
        }

        if (System.currentTimeMillis()-lastPingTime > Math.max(1000, currentPing*2)) {
            lastPingTime = System.currentTimeMillis();
            PacketUtil.sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS));
        }

        return false;
    });

    private final Event resetPing = new WorldJoinEvent(ModuleUtil.getModule(HudModule.class), (params) -> {
        currentPing = 0;
        lastPingTime = System.currentTimeMillis();
        return false;
    });
}
