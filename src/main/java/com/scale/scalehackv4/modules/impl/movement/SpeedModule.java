package com.scale.scalehackv4.modules.impl.movement;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;

@RegisterModule(
        name = "Speed",
        category = Category.MOVEMENT
)
public class SpeedModule extends Module {
    @RegisterSubModule(name = "Sprint")
    public static boolean sprint = true;
    @RegisterSubModule(name = "Jump Delay", min = 1, max = 10)
    public static int jumpDelay = 1;

    private final Event overrideSprinting = new KeyboardInputEvent(this, params -> {
        params.sprintKey |= sprint;

        return false;
    });

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
