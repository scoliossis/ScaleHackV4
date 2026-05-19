package com.scale.scalehackv4.modules.impl.combat;


import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;

@RegisterModule(
        name = "Keep Sprint",
        category = Category.COMBAT
)
public class KeepSprintModule extends Module {
    @RegisterSubModule(name = "Hypixel")
    public static boolean hypixel = true;

    public static boolean shouldKeepsprint() {
        return ModuleUtil.isEnabled(KeepSprintModule.class) && C.p().forwardSpeed > 0;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
