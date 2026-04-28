package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

@RegisterModule(
        name = "Velocity",
        category = Category.COMBAT
)
public class VelocityModule extends Module {
    @RegisterSubModule(name = "Knockback Mode")
    public static Mode mode = Mode.Jump_Reset;
    @RegisterSubModule(name = "Horizontal", parent = "Knockback Mode", modeParentString = {"Edit", "Add"}, min = -2, max = 2)
    public static float horizontal = 0;
    @RegisterSubModule(name = "Vertical", parent = "Knockback Mode", modeParentString = {"Edit", "Add"}, min = -2, max = 2)
    public static float vertical = 0;

    private static boolean handledVelocity = true;

    public enum Mode {
        Cancel,
        Edit,
        Add,
        Jump_Reset
    }

    private final Event cancelVelocity = new PacketEvent.Received(this, params -> {
        if (params.packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            if (C.p() == null || velocityPacket.getEntityId() != C.p().getId()) return false;

            handledVelocity = false;

            switch (mode) {
                case Cancel:
                    return true;
                case Add:
                    C.p().setVelocity(
                            velocityPacket.getVelocity().x * horizontal + C.p().getVelocity().x *( 1-horizontal),
                            velocityPacket.getVelocity().y * vertical + C.p().getVelocity().y * (1-vertical),
                            velocityPacket.getVelocity().z * horizontal + C.p().getVelocity().z * (1-horizontal)
                    );
                    return true;
                case Edit:
                    C.p().setVelocity(velocityPacket.getVelocity().x * horizontal, velocityPacket.getVelocity().y * vertical, velocityPacket.getVelocity().z * horizontal);
                    return true;
            }
        }

        return false;
    });

    private final Event handleVelocityOnRotation = new RotationEvent(this, params -> {
        if (handledVelocity) return false;

        return false;
    });

    private final Event handleVelocityKeyInputs = new KeyboardInputEvent(this, params -> {
        if (handledVelocity) return false;
        handledVelocity = true;

        switch (mode) {
            case Jump_Reset:
                if (params.forwardKey && params.sprintKey) params.jumpKey = true;
                break;
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
