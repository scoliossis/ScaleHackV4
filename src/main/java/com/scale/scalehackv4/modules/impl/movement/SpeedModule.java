package com.scale.scalehackv4.modules.impl.movement;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.ModuleUtil;

@RegisterModule(
        name = "Speed",
        category = Category.MOVEMENT
)
public class SpeedModule extends Module {
    @RegisterSubModule(name = "Sprint")
    public static boolean sprint = true;

    private final Event overrideSprinting = new KeyboardInputEvent(this, params -> {
        if (sprint) params.sprintKey = true;

        return false;
    });
    public static boolean shouldSprint() {
        return sprint && ModuleUtil.isEnabled(SpeedModule.class);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
