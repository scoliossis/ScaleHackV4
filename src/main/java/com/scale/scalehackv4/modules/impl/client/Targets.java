package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import lombok.AllArgsConstructor;

@RegisterModule(
        name = "Targets",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class Targets extends Module {
    @RegisterSubModule(name = "Anti Bot")
    public static boolean antibot = true;
    @RegisterSubModule(name = "Player Entry Check")
    public static boolean playerEntry = true;
    @RegisterSubModule(name = "Ping Check")
    public static boolean pingCheck = false;
    @RegisterSubModule(name = "Bot Ping", parent = "Ping Check")
    public static Bot_Mode botPingMode = Bot_Mode.Old_Hypixel;

    @AllArgsConstructor
    public enum Bot_Mode {
        // .ordinal -1, mb. keeping for later ig idk
        Negative(-1),
        Old_Hypixel(0),
        Cracked(1);

        public final int ping;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        this.setEnabled(true);
    }
}
