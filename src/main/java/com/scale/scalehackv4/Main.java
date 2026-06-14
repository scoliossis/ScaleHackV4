package com.scale.scalehackv4;

import com.scale.scalehackv4.commands.CommandManager;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.alts.AltManagerUtil;
import com.scale.scalehackv4.utils.alts.DroppedFileListener;
import com.scale.scalehackv4.utils.alts.SessionUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.DraggableRenderer;
import com.scale.scalehackv4.utils.render.FontUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class Main implements ModInitializer {
    public static final String MOD_ID = "scalehackv4";

    public static final String MOD_NAME = "Scale Hack";
    public static final String MOD_VERSION = "4.0";

    public static final String BASE_FOLDER_PATH = MOD_ID + "/";
    public static final String CONFIG_PATH = BASE_FOLDER_PATH + "/configs/";
    public static final String BASE_CONFIG = "default";
    public static final String ALTS_PATH = BASE_FOLDER_PATH + "/alts";
    public static final String OTHER_PATH = BASE_FOLDER_PATH + "/other";

    @Override
    public void onInitialize() {
        FontUtil.setCurrentFont(FontUtil.Fonts.Verdana);

        ModuleUtil.init();
        CommandManager.init();
        DraggableRenderer.loadDraggingPositions();
        // the window isnt here yet. await.
        ClientLifecycleEvents.CLIENT_STARTED.register(DroppedFileListener::init);

        if (SessionUtil.queryPlayerProfile(C.mc.getSession().getAccessToken()) != null)
            AltManagerUtil.saveAlt(AltManagerUtil.Alt.getCurrent());
        AltManagerUtil.loadAlts();
    }
}
