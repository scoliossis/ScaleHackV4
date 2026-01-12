package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import net.minecraft.text.Text;

public class ChatUtil {
    public static void prefixMessage(String message) {
        chat("&c[&f"+ Main.MOD_NAME+"&c] " + message);
    }
    public static void chat(String message) {
        C.mc.inGameHud.getChatHud().addMessage(Text.of(message));
    }
}
