package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.KeyEvent;
import com.scale.scalehackv4.modules.SubModule;
import com.scale.scalehackv4.modules.impl.client.Notifications;
import com.scale.scalehackv4.utils.C;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

import java.util.List;

@RegisterListeners
public class KeyboardUtil {
    public final Event onKeyPressed = new KeyEvent.Pressed(null, (e) -> {
        List<SubModule> bound = ModuleUtil.KEYBINDS.get(e.keyCode);

        if (bound == null || (C.mc.currentScreen != null && !(C.mc.currentScreen instanceof InventoryScreen))) return false;

        for (SubModule subModule : bound) {
            boolean enabled = !(boolean) subModule.get();
            Notifications.post(subModule, enabled);

            subModule.set(enabled);
        }

        return false;
    });
}
