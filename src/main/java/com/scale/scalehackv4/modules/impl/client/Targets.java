package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import lombok.AllArgsConstructor;

@RegisterModule(
        name = "Targets",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class Targets extends Module {
    @RegisterSubModule(name = "Teams")
    public static boolean teams = true;

    @RegisterSubModule(name = "Anti Bot")
    public static boolean antibot = true;
    @RegisterSubModule(name = "Player Entry Check", parent = "Anti Bot")
    public static boolean playerEntry = true;
    @RegisterSubModule(name = "Ping Check", parent = "Anti Bot")
    public static boolean pingCheck = false;
    @RegisterSubModule(name = "Bot Ping", parent = "Ping Check")
    public static Bot_Ping botPingMode = Bot_Ping.Old_Hypixel;

    @AllArgsConstructor
    public enum Bot_Ping {
        // .ordinal -1, mb. keeping for later ig idk
        Negative(-1),
        Old_Hypixel(0),
        Cracked(1);

        public final int ping;
    }

    @RegisterSubModule(name = "Valid Weapons")
    public static Valid_Weapons validWeapons = Valid_Weapons.Sword;

    public enum Valid_Weapons implements MultiSelectEnum {
        Sword {@Override public boolean enabledByDefault() {return true;}},
        Knockback {@Override public boolean enabledByDefault() {return true;}},
        Axe;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        this.setEnabled(true);
    }
}
