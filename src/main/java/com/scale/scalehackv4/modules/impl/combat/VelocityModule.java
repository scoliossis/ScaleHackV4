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
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;

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

    @RegisterSubModule(name = "Ignore Explosions")
    public static boolean ignoreExplosions = true;

    private static boolean handledVelocity = true;

    public enum Mode {
        Cancel,
        Edit,
        Add,
        Jump_Reset
    }

    private final Event cancelVelocity = new PacketEvent.Received(this, params -> {
        if (!C.isInGame()) return false;


        Vec3d velocity = params.packet instanceof EntityVelocityUpdateS2CPacket velocityPacket && velocityPacket.getEntityId() == C.p().getId()
                ? velocityPacket.getVelocity()
                : (params.packet instanceof ExplosionS2CPacket explosionS2CPacket && !ignoreExplosions)
                  ? explosionS2CPacket.playerKnockback().orElse(null)
                  : null;

        if (velocity == null) return false;

        handledVelocity = false;

        return switch (mode) {
            case Cancel -> true;
            case Add -> {
                C.p().setVelocity(
                        velocity.x * horizontal + C.p().getVelocity().x * (1 - horizontal),
                        velocity.y * vertical + C.p().getVelocity().y * (1 - vertical),
                        velocity.z * horizontal + C.p().getVelocity().z * (1 - horizontal)
                );
                yield true;
            }
            case Edit -> {
                C.p().setVelocity(velocity.x * horizontal, velocity.y * vertical, velocity.z * horizontal);
                yield true;
            }
            default -> false;
        };
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
