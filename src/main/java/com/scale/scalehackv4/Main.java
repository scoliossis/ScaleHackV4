package com.scale.scalehackv4;

import com.scale.scalehackv4.commands.CommandManager;
import com.scale.scalehackv4.modules.ModuleManager;
import com.scale.scalehackv4.utils.render.DraggableRenderer;
import net.fabricmc.api.ModInitializer;

public class Main implements ModInitializer {
    public static final String MOD_ID = "scalehackv4";

    public static final String MOD_NAME = "Scale Hack";
    public static final String MOD_VERSION = "4.0";

    public static final String BASE_FOLDER_PATH = MOD_ID + "/";
    public static final String CONFIG_PATH = BASE_FOLDER_PATH + "/configs/";
    public static final String ALTS_PATH = BASE_FOLDER_PATH + "/alts";
    public static final String OTHER_PATH = BASE_FOLDER_PATH + "/other";

    @Override
    public void onInitialize() {
        ModuleManager.init();
        CommandManager.init();
        DraggableRenderer.loadDraggingPositions();
    }
}
