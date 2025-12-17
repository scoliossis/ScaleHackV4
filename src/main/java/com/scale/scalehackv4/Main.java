package com.scale.scalehackv4;

import com.scale.scalehackv4.modules.ModuleManager;
import net.fabricmc.api.ModInitializer;

public class Main implements ModInitializer {
    public static final String MOD_ID = "scalehackv4";

    public static final String MOD_NAME = "Scale Hack";
    public static final String MOD_VERSION = "4.0";

    @Override
    public void onInitialize() {
        ModuleManager.init();
    }
}
