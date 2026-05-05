package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.bridge.JumpingCooldownBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.player.SendMovementPacketEvent;
import com.scale.scalehackv4.modules.impl.movement.SpeedModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;

@RegisterListeners
public class MovementUtil {
    private static boolean wasJumpDown = false;

    private final Event resetWasJumpDown = new SendMovementPacketEvent(null, params -> {
        wasJumpDown = C.mc.options.jumpKey.isPressed();
        return false;
    });

    public static boolean willJump() {
        int jumpCooldown = JumpingCooldownBridge.from(C.p()).bridge$getJumpingCooldown();
        if (ModuleUtil.isEnabled(SpeedModule.class) && jumpCooldown > SpeedModule.jumpDelay) jumpCooldown = SpeedModule.jumpDelay;

        return C.mc.options.jumpKey.isPressed() && (jumpCooldown <= 1 || !wasJumpDown) && C.p().isOnGround();
    }
}
