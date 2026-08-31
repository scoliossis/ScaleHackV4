package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.render.FontUtil;

@RegisterModule(
        name = "Options",
        category = Category.CLIENT,
        enabledByDefault = true
)
// todo: ig add option to disable showing rotations, but blehhh
//  pitch rotation isnt shown when freelook is enabled
//  maybe allow no fix rotation on interact
public class OptionsModule extends Module {
    @RegisterSubModule(name = "Blur Radius", parent = "Square", max = 10)
    public static int blurRadius = 3;

    @RegisterSubModule(name = "Client Font")
    public static FontUtil.Fonts defaultFont = FontUtil.Fonts.Verdana;

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
        this.setEnabled(true);
    }
}
