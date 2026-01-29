package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.KeyEvent;
import com.scale.scalehackv4.modules.ModuleManager;
import com.scale.scalehackv4.modules.SubModule;

import java.util.List;

@RegisterListeners
public class KeyboardUtil {
    public final Event onKeyPressed = new KeyEvent.Pressed(null, (e) -> {
        List<SubModule> bound = ModuleManager.KEYBINDS.get(e.keyCode);

        if (bound == null) return false;

        for (SubModule subModule : bound) {
            subModule.set(!(boolean) subModule.get());
        }

        return false;
    });
}
