package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@RegisterModule(
        name = "Click GUI",
        category = Category.CLIENT,
        defaultKeybind = GLFW.GLFW_KEY_RIGHT_SHIFT
)
public class ClickGuiModule extends Module {
    @RegisterSubModule(
            name = "Menu Colour"
    )
    public static Color guiColour = new Color(0x7B9818);

    @RegisterSubModule(name = "Colour Box Type")
    public static ColourBoxType colourBoxType = ColourBoxType.Normal;

    public enum ColourBoxType {
        Normal,
        Anime_Girl
    }

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
                    ModuleUtil.getModule(ClickGuiModule.class).setEnabled(false);
                    return new int[] {0, 0};
                }

                float scaleFactor = (2f/C.res().getScaleFactor());

                return new int[] {(int) ((ClickGuiScreen.WIDTH + 4) * scaleFactor), (int) (6*scaleFactor)};
            },
            e -> ModuleUtil.getModule(ClickGuiModule.class).isEnabled(),
            e -> true
    );


    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        C.mc.setScreen(null);
    }

    @Override
    public String arrayListExtraInfo() {
        if (colourBoxType == ColourBoxType.Anime_Girl) {
            return "Official Kawaii Anime Girl Edition 2025 bypass sigma sigma boy";
        } else {
            return "Skeet";
        }
    }
}
