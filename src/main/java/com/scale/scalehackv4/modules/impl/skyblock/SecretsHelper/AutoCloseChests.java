package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.resource.language.I18n;

// todo: make chest stealer, maybe scrap this module?
public class AutoCloseChests extends SecretsHelper {
    @RegisterSubModule(name = "Auto Close Chests", hasKeybind = true)
    public static boolean autoCloseChests = true;
    @RegisterSubModule(name = "Title Check", parent = "Auto Close Chests")
    public static boolean titleCheck = true;
    @RegisterSubModule(name = "Dungeons Only", parent = "Auto Close Chests")
    public static boolean dungeonsOnly = true;

    private final Event onRenderTick = new RenderTickEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (
                !autoCloseChests
                || !(C.mc.currentScreen instanceof GenericContainerScreen)
                || (dungeonsOnly && !AreaUtil.isInDungeons())
                || (titleCheck
                        && !C.mc.currentScreen.getTitle().getString().equals(I18n.translate("container.chest"))
                        && !C.mc.currentScreen.getTitle().getString().equals(I18n.translate("container.chestDouble"))
                )
        ) return false;

        C.p().closeHandledScreen();

        return false;
    });
}
