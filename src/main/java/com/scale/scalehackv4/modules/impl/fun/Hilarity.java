package com.scale.scalehackv4.modules.impl.fun;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.KeyEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.ChatUtil;

@RegisterModule(
        name = "Hilarity",
        category = Category.FUN,
        enabledByDefault = true
)
// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/client/Hilarity.java
// FINALLY changed up the words list, go team!
public class Hilarity extends Module {
    @RegisterSubModule(name = "Slider", min = 1, max = 10000)
    public static int slider = 1000;

    // this does NOTHING!
    @RegisterSubModule(name = "Boolean")
    public static boolean bool = false;

    private static final String[] hilarityNames = {
            "&c[OWNER] hypixel", // recognisable names im sure
            "&c[OWNER] Rezzus", // recognisable names im sure

            "&c[&6ዞ&c] Jayavarmen", // only admins i know
            "&c[&6ዞ&c] Minikloon", // only admins i know
            "&c[&6ዞ&c] Plancke", // only admins i know
            "&c[&6ዞ&c] dctr", // only admins i know

            "&c[&6ዞ&c] LegitPhase", // thats my goat
            "&c[&6ዞ&c] Scale", // mushroom gng 4eva
            "&c[&6ዞ&c] Swig", // mushroom gng 4eva
            "&c[&6ዞ&c] 112batman", // mushroom gng 4eva
            "&c[&6ዞ&c] Sunderw3k", // mushroom gng 4eva
            "&c[&6ዞ&c] Sideload", // mushroom gng 4eva
            "&c[&6ዞ&c] Mushroom", // mushroom gng 4eva

            "&c[MINISTER] 2nfg", // free my goat

            "&c[&fYOUTUBE&c] Frozie", // dodgy dms.
            "&c[&fYOUTUBE&c] AverageSweat", // shady addons goat
            "&c[&fYOUTUBE&c] Alan_Wood", // 7sense official gaming
            "&c[&fYOUTUBE&c] ijustcheat", // rip ijustcheat </3
            "&c[&fYOUTUBE&c] WiZARDHAX", // 11/10 videos

            "&7kulovhax", // hypixel god bypass tech
            "&7chazed", // hypixel god bypass tech
    };

    private static final String[] hilarityMessages = {
            "your gay\n&dFrom &b[MVP&2+&b] Elg_AF&7: you're*",
            "your getting banned :)\n&dFrom &b[MVP&2+&b] Elg_AF&7: you're*",
            "Your flagging.\n&dFrom &b[MVP&2+&b] Elg_AF&7: you're*",
            "watchdog exists",
            "any last words?",
            "x3 nuzzles! pounces on you uwu you so warm",
            "you really thought that wasnt detectable?",
            "hey whats " + Main.MOD_NAME + "? ;3",
            "du kannst nichts daher ist dein vatter milch hollen",
            "t.me/escamas1337 !!",
            "https://github.com/scoliossis",
            "Have you checked out mushroom client? Its the FRESHEST blatant client on mushroomer.top. You can save your accounts BLOCK the ban packets.",
            "scalehack is a rat lol, thanks for the account <3 i have full pc access",
            "scalehack rat injected. count your days.",
            "do a backflip or im banning you",
            "free meow client krac: https://www.youtube.com/watch?v=CdryMk0cT6o"
    };

    private static int i = 0;

    // rga does it on a keypressevent, i gotta do the same
    public final Event hilaritying2027bypassprogodbypass = new KeyEvent.Pressed(this, params -> {
        i++;
        if (i < slider) return false;

        ChatUtil.chat("&dFrom " + hilarityNames[(int) Math.floor(Math.random() * hilarityNames.length)] + "&7: " + hilarityMessages[(int) Math.floor(Math.random() * hilarityMessages.length)]);
        i = 0;

        return false;
    });

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
