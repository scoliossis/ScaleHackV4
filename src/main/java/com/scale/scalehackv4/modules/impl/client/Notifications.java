package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.utils.client.ChatUtil;

@RegisterModule(
        name = "Notifications",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class Notifications extends Module {
    public static void post(SubModule subModule, boolean enabled) {
        String moduleName = subModule.getAnnotation().name();
        moduleName = moduleName.equals(Module.ENABLED_STRING) ? subModule.getParentModule().getAnnotation().name() : moduleName;

        if (enabled) {
            ChatUtil.prefixMessage("&aEnabled &f" + moduleName);
        }
        else {
            ChatUtil.prefixMessage("&cDisabled &f" + moduleName);
        }
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
