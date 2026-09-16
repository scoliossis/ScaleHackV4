package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.InventoryUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.block.Block;

@RegisterModule(
        name = "Nuker",
        category = Category.PLAYER
)
public class NukerModule extends Module {
    @RegisterSubModule(name = "Auto Tool", hasKeybind = true)
    public static boolean autotool = false;
    @RegisterSubModule(name = "Swap Back Delay", min = 1, max = 10, parent = "Auto Tool")
    public static int swapBackDelay = 1;

    @RegisterSubModule(name = "Break Delay", min = 0, max = 5)
    public static int breakDelay = 1;

    @RegisterSubModule(name = "Fast Break", min = 0.5, max = 2)
    public static float fastBreak = 1;

    private static int previousSlot = -1;
    private static int bestToolSlot = -1;
    public static int lastBlockBreakTick = -1;

    public static int getBreakDelay() {
        return ModuleUtil.isEnabled(NukerModule.class) ? breakDelay : 5;
    }

    public static float getBreakSpeed() {
        return ModuleUtil.isEnabled(NukerModule.class) ? fastBreak : 1;
    }

    public Event onPlayerTick = new PlayerUpdateEvent(this, params -> {
        if (C.p().getInventory().getSelectedSlot() != bestToolSlot) {
            previousSlot = -1;
            lastBlockBreakTick = -1;
        }
        else if (WorldUtil.tick - lastBlockBreakTick == swapBackDelay) switchBack();
        return false;
    });

    public static boolean shouldAutotool() {
        return autotool && C.p().isSneaking();
    }

    public static void switchToTool(Block block) {
        // swapSlot swaps instantly, which is the same as sending swapping a tick earlier
        if (previousSlot == -1) previousSlot = C.p().getInventory().getSelectedSlot();
        bestToolSlot = InventoryUtil.getBestHotbarSlotToBreakBlock(block);
        InventoryUtil.swapSlot(
                bestToolSlot,
                true
        );
    }

    public static void switchBack() {
        InventoryUtil.swapSlot(previousSlot, false);
        previousSlot = -1;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
