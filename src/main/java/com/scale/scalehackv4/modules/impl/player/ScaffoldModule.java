package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.ModuleUtil;

@RegisterModule(
        name = "Scaffold",
        category = Category.PLAYER
)
public class ScaffoldModule extends Module {
    @RegisterSubModule(name = "Fast Place", hasKeybind = true)
    public static boolean fastPlace = true;

    // in theory, the "fast place" option isnt needed, only place delay.
    @RegisterSubModule(name = "Place Delay", parent = "Fast Place", min = 1, max = 3)
    public static int placeDelay = 1;

    public static int getPlaceDelay() {
        return ModuleUtil.isEnabled(ScaffoldModule.class) && fastPlace ? placeDelay : 4;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
