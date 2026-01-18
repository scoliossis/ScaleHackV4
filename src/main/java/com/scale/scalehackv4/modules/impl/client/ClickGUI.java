package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.ModuleManager;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.Draggable;

@RegisterModule(
        name = "Click GUI"
)
public class ClickGUI extends Module {
    private static final ClickGuiScreen GUI = new ClickGuiScreen();

    private static boolean enabled = false;

    public static final Draggable guiRenderer = new Draggable(
            "Skeet GUI",
            () -> {
                if (enabled) {
                    C.mc.setScreen(GUI);
                    enabled = false;
                }

                if (!(C.mc.currentScreen instanceof ClickGuiScreen)) {
                    ModuleManager.getModule(ClickGUI.class).setEnabled(false);
                    return new int[] {0, 0};
                }

                return new int[] {(int) (ClickGuiScreen.WIDTH + (4 * ClickGuiScreen.BORDER_SCALE)), (int) (5 * ClickGuiScreen.BORDER_SCALE)};
            },
            e -> ModuleManager.getModule(ClickGUI.class).isEnabled(),
            e -> true
    );


    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {

    }
}
