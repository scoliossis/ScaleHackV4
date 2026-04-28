package com.scale.scalehackv4.modules.impl.movement;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import net.minecraft.util.math.Vec3d;

@RegisterModule(
        name = "Speed",
        category = Category.MOVEMENT
)
public class SpeedModule extends Module {
    @RegisterSubModule(name = "Sprint")
    public static boolean sprint = true;
    @RegisterSubModule(name = "Jump Delay", min = 1, max = 10)
    public static int jumpDelay = 1;

    @RegisterSubModule(name = "Speed Override")
    public static boolean speedOverride = true;
    @RegisterSubModule(name = "Horizontal Speed", max = 10)
    public static float horizontalSpeed = 1;

    @RegisterSubModule(name = "Fly")
    public static boolean fly = true;
    @RegisterSubModule(name = "Vertical Speed", max = 10)
    public static float vertialSpeed = 1;

    private final Event overrideSprinting = new KeyboardInputEvent(this, params -> {
        params.sprintKey |= sprint;

        return false;
    });

    private final Event playerUpdateEvent = new PlayerUpdateEvent(this, params -> {
        if (fly) {
            double verticalSpeed = vertialSpeed * (C.mc.options.jumpKey.isPressed() ? 1 : 0) + vertialSpeed * (C.mc.options.sneakKey.isPressed() ? -1 : 0);
            C.p().setVelocity(C.p().getVelocity().x, verticalSpeed, C.p().getVelocity().z);
        }
        if (speedOverride) {
            double yaw = Math.toRadians(C.p().getYaw());
            double forward = (C.mc.options.forwardKey.isPressed() ? horizontalSpeed : 0) + (C.mc.options.backKey.isPressed() ? -horizontalSpeed : 0);
            double sideways = (C.mc.options.leftKey.isPressed() ? horizontalSpeed : 0) + (C.mc.options.rightKey.isPressed() ? -horizontalSpeed : 0);

            C.p().setVelocity(new Vec3d(
                    (Math.sin(-yaw) * forward + Math.cos(-yaw) * sideways),
                    C.p().getVelocity().y,
                    (Math.cos(-yaw) * forward - Math.sin(-yaw) * sideways)
            ));
        }

        return false;
    });

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
