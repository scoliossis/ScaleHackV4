package com.scale.scalehackv4.modules.impl.skyblock;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import lombok.AllArgsConstructor;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.function.Predicate;

@RegisterModule(
        name = "Auto GFS",
        category = Category.SKYBLOCK
)
// todo: multi select enums are IMPERATIVE
public class AutoGFS extends Module {
    @RegisterSubModule(name = "Pearls")
    public static boolean pearls = true;
    @RegisterSubModule(name = "Spirit Leaps")
    public static boolean spiritLeaps = true;
    @RegisterSubModule(name = "Superboom TNT")
    public static boolean superboom = true;

    @RegisterSubModule(name = "Delay", min = 1500, max = 5000, increment = 100)
    public static long delay = 1500;

    @AllArgsConstructor
    private enum SackItems {
        PEARLS(e -> pearls, 16, "ENDER_PEARL", e -> e.getItem() == Items.ENDER_PEARL && getCustomName(e).equals("Ender Pearl")),
        SPIRIT_LEAP(e -> spiritLeaps, 16, "SPIRIT_LEAP", e -> e.getItem() == Items.ENDER_PEARL && getCustomName(e).equals("Spirit Leap") && AreaUtil.isInDungeons()),
        SUPERBOOM(e -> superboom, 64, "SUPERBOOM_TNT", e -> e.getItem() == Items.TNT && getCustomName(e).equals("Superboom TNT") && AreaUtil.isInDungeons());

        public final Predicate<?> enabled;
        public final int maxStack;
        public final String name;
        public final Predicate<ItemStack> predicate;
        private static String getCustomName(ItemStack stack) {
            Text name = stack.getName();
            return name != null ? name.getString() : "";

        }
    }

    private long lastGFS = 0;
    private final Event onPlayerUpdate = new PlayerUpdateEvent(this, params -> {
        if (System.currentTimeMillis() - lastGFS < delay || !AreaUtil.isInSkyblock()) return false;

        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            for (SackItems sackItem : SackItems.values()) {
                if (!sackItem.enabled.test(null)) continue;

                ItemStack itemStack = C.p().getInventory().getStack(i);
                if (!sackItem.predicate.test(itemStack)) continue;

                int remaining = sackItem.maxStack - itemStack.getCount();
                if (remaining <= 0) continue;

                // $wag
                C.p().networkHandler.sendChatCommand("gfs " + sackItem.name + " " + remaining);

                // time to wait :pensive:
                lastGFS = System.currentTimeMillis();
                // dont refill multiple times in the tick
                return false;
            }
        }

        return false;
    });


    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}