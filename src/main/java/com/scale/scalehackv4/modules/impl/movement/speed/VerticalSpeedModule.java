package com.scale.scalehackv4.modules.impl.movement.speed;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.movement.SpeedModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;

public class VerticalSpeedModule extends SpeedModule {
    @RegisterSubModule(name = "Vertical Modifier", parent = "Modify Vertical Speed")
    public static VerticalSpeedModifier verticalSpeedModifier = VerticalSpeedModifier.None;
    @RegisterSubModule(name = "Vertical Speed", max = 10, parent = "Vertical Modifier", modeParentString = "Fly")
    public static float verticalSpeed = 1;

    public enum VerticalSpeedModifier {
        None {
            @Override public void handlePlayerUpdate(PlayerUpdateEvent.Params params) {}
        },
        Fly {
            @Override
            public void handlePlayerUpdate(PlayerUpdateEvent.Params params) {
                C.p().setVelocity(
                        C.p().getVelocity().x,
                        verticalSpeed * (C.mc.options.jumpKey.isPressed() ? 1 : 0) + verticalSpeed * (C.mc.options.sneakKey.isPressed() ? -1 : 0),
                        C.p().getVelocity().z
                );
            }
        };

        public abstract void handlePlayerUpdate(PlayerUpdateEvent.Params params);
    }

    private final Event playerUpdateEvent = new PlayerUpdateEvent(ModuleUtil.getModule(SpeedModule.class), params -> {
        verticalSpeedModifier.handlePlayerUpdate(params);

        return false;
    });
}
