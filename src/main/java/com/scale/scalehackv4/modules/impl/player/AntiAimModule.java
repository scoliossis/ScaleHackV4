package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.player.MovementUtil;

@RegisterModule(
        name = "Anti Aim",
        category = Category.PLAYER
)
// todo: new modes, and maybe actually dodging arrows...
//  allow interact
public class AntiAimModule extends Module {
    @RegisterSubModule(name = "Allow Jump")
    public boolean allowJump = true;

    public final Event GOD_BYPASS_TECH_20XX = new RotationEvent(this, params -> {
        params.rotation.pitch += (float) ((Math.random() * 180) - 90);

        if (!MovementUtil.willJump() || !allowJump) {
            params.rotation.yaw += (float) ((Math.random() * 360) - 180);
        }

        return false;
        // lowest priority, sadly.
    }, 1);

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
