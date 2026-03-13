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

import java.util.concurrent.ConcurrentHashMap;
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

    @RegisterSubModule(name = "Withdraw Delay", min = 100, max = 5000, increment = 100)
    public static long withdrawDelay = 1500;
    @RegisterSubModule(name = "Delay Between", min = 1500, max = 5000, increment = 100)
    public static long delayBetween = 1500;

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

    // not actually a queue, flop
    private final ConcurrentHashMap<SackItems, WithdrawRequest> WITHDRAW_QUEUE = new ConcurrentHashMap<>();
    private record WithdrawRequest(int itemSlot, long requestTime) {}

    private long lastWithdraw = 0;

    private final Event onPlayerUpdate = new PlayerUpdateEvent(this, params -> {
        if (System.currentTimeMillis() - lastWithdraw < delayBetween || !AreaUtil.isInSkyblock()) return false;

        if (!WITHDRAW_QUEUE.isEmpty()) {
            for (SackItems sackItem : WITHDRAW_QUEUE.keySet()) {
                WithdrawRequest request = WITHDRAW_QUEUE.get(sackItem);
                // do we really need to withdraw?
                if (System.currentTimeMillis() - request.requestTime < withdrawDelay) continue;

                ItemStack itemStack = C.p().getInventory().getStack(request.itemSlot);
                if (itemStack != null && itemStack.getCount() < sackItem.maxStack) {
                    // we are READY, withdraw, gogogo
                    C.p().networkHandler.sendChatCommand("gfs " + sackItem.name + " " + (sackItem.maxStack - itemStack.getCount()));
                    lastWithdraw = System.currentTimeMillis();
                    return false;
                }

                WITHDRAW_QUEUE.remove(sackItem);
            }
        }

        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            for (SackItems sackItem : SackItems.values()) {
                if (!sackItem.enabled.test(null)) continue;

                ItemStack itemStack = C.p().getInventory().getStack(i);
                if (!sackItem.predicate.test(itemStack) || itemStack.getCount() >= sackItem.maxStack) continue;

                long requestTime = WITHDRAW_QUEUE.get(sackItem) == null ? System.currentTimeMillis() : WITHDRAW_QUEUE.get(sackItem).requestTime;
                WITHDRAW_QUEUE.put(sackItem, new WithdrawRequest(i, requestTime));
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