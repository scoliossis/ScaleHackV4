package com.scale.scalehackv4;

import com.scale.scalehackv4.modules.ModuleManager;
import net.fabricmc.api.ModInitializer;

public class Main implements ModInitializer {
    @Override
    public void onInitialize() {
        ModuleManager.init();
    }
}
