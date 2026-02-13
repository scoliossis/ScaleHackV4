package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.ModuleUtil;

@RegisterModule(
        name = "No Render",
        enabledByDefault = true,
        category = Category.RENDER
)
// todo: nausea, blindness, fire, grass, notifications
public class NoRender extends Module {
    @RegisterSubModule(name = "Potions Overlay")
    public static boolean hidePotionEffectsGUI = true;

    @RegisterSubModule(name = "Loading World")
    public static boolean loadingWorld = true;

    public static boolean hidePotionsGUI() {
        return hidePotionEffectsGUI && ModuleUtil.isEnabled(NoRender.class);
    }

    public static boolean shouldHideLoadingWorldScreen() {
        return loadingWorld && ModuleUtil.isEnabled(NoRender.class);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
