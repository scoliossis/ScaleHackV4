package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;

@RegisterModule(
        name = "Anti Aim",
        category = Category.PLAYER
)
public class AntiAimModule extends Module {
    public final Event GOD_BYPASS_TECH_20XX = new RotationEvent(this, params -> {
        params.rotation.pitch += (float) ((Math.random() * 180) - 90);
        params.rotation.yaw += (float) ((Math.random() * 360) - 180);

        return false;
    });

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
