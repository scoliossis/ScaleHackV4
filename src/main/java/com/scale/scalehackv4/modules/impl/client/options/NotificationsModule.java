package com.scale.scalehackv4.modules.impl.client.options;

import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.utils.client.ChatUtil;

public class NotificationsModule {
    // todo: make modes
    @RegisterSubModule(name = "Notifications")
    public static boolean notifications = true;

    public static void post(SubModule subModule, boolean enabled) {
        if (!notifications) return;

        String moduleName = subModule.getAnnotation().name();
        moduleName = moduleName.equals(Module.ENABLED_STRING)
                ? subModule.getParentModule().getAnnotation().name()
                : subModule.getParentModule().getAnnotation().name() + " | " + moduleName;

        if (enabled) {
            ChatUtil.prefixMessage("&aEnabled &f" + moduleName);
        }
        else {
            ChatUtil.prefixMessage("&cDisabled &f" + moduleName);
        }
    }
}
