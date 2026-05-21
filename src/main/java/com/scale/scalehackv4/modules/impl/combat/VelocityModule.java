package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.BlinkUtil;
import com.scale.scalehackv4.utils.player.PlayerUtil;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
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

    @RegisterSubModule(name = "Max Delay Ticks", parent = "Knockback Mode", modeParentString = {"Delay", "Dynamic"}, min = 1, max = 20)
    public static int maxDelayTicks = 13;

    @RegisterSubModule(name = "Force Sprint", parent = "Knockback Mode", modeParentString = {"Jump_Reset", "Dynamic"})
    public static boolean forceSprint = true;

    @RegisterSubModule(name = "Ignore Explosions")
    public static boolean ignoreExplosions = true;
    @RegisterSubModule(name = "Ignore Fall Damage")
    public static boolean ignoreFallDamage = true;

    private static boolean handledVelocity = true;
    private static Mode lastVelocityMode = mode;
    private static Vec3d lastVelocity = Vec3d.ZERO;
    private static int lastUnhandledVelocityTick = 0;

    public enum Mode {
        Cancel,
        Edit,
        Add,
        Jump_Reset,
        // god bypass tech from https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/combat/Velocity.java
        Dynamic,
        Delay
    }

    public Packet<?> lastPacket = null;
    private final Event cancelVelocity = new PacketEvent.Received(this, params -> {
        if (!C.isInGame() || PacketUtil.clientSentPackets.contains(params.packet)) return false;
        boolean isExplosion = params.packet instanceof ExplosionS2CPacket
                || (params.packet instanceof EntityVelocityUpdateS2CPacket && lastPacket instanceof EntityTrackerUpdateS2CPacket);
        boolean isVelocity = !isExplosion
                && params.packet instanceof EntityVelocityUpdateS2CPacket velocityPacket
                && velocityPacket.getEntityId() == C.p().getId()
                && (!ignoreFallDamage || !isFallDamage(velocityPacket.getVelocity()))
                // if a velocity packet is preceded by a PlayerPositionLookS2CPacket then its NOT real.
                && !(lastPacket instanceof PlayerPositionLookS2CPacket);

        lastPacket = params.packet;
        if ((isExplosion && !ignoreExplosions) || !isVelocity) return false;

        Vec3d velocity = params.packet instanceof EntityVelocityUpdateS2CPacket velocityPacket ? velocityPacket.getVelocity()
                : params.packet instanceof ExplosionS2CPacket explosionS2CPacket
                  ? explosionS2CPacket.playerKnockback().orElse(null)
                  : null;

        if (velocity == null) return false;

        if (handledVelocity) {
            lastUnhandledVelocityTick = WorldUtil.ticks;
            handledVelocity = false;
            lastVelocityMode = mode;
        }
        lastVelocity = velocity;

        return switch (mode) {
            case Cancel -> {
                handledVelocity = true;
                yield true;
            }
            case Add -> {
                C.p().setVelocity(
                        velocity.x * horizontal + C.p().getVelocity().x * (1 - horizontal),
                        velocity.y * vertical + C.p().getVelocity().y * (1 - vertical),
                        velocity.z * horizontal + C.p().getVelocity().z * (1 - horizontal)
                );
                handledVelocity = true;
                yield true;
            }
            case Edit -> {
                C.p().setVelocity(velocity.x * horizontal, velocity.y * vertical, velocity.z * horizontal);
                handledVelocity = true;
                yield true;
            }
            case Dynamic, Delay -> {
                if (!BlinkUtil.isBlinking(false, true, this.getClass())) {
                    BlinkUtil.pushBlink(false, true, this.getClass(), params.packet);
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    });

    private boolean isFallDamage(Vec3d velocity) {
        return velocity.y <= 0;
    }

    private final Event handleVelocityKeyInputs = new KeyboardInputEvent(this, params -> {
        if (handledVelocity) return false;

        switch (lastVelocityMode) {
            case Dynamic:
                if (!C.p().isOnGround()) {
                    handledVelocity |= !BlinkUtil.isBlinking(false, true, this.getClass());
                    return false;
                }
                BlinkUtil.popBlink(false, true, this.getClass());
            case Jump_Reset:
                handledVelocity = true;
                if (forceSprint) params.forwardKey = params.sprintKey = params.jumpKey = true;
                else if (C.p().isSprinting()) params.jumpKey = true;
        }

        return false;
    });

    private final Event handleVelocityRotation = new RotationEvent(this, params -> {
        if (handledVelocity) return false;

        switch (lastVelocityMode) {
            case Dynamic, Jump_Reset:
                if (!C.p().isOnGround() || !forceSprint) return false;
                params.rotation.yaw = RotationUtil.applyWrap360(params.rotation.yaw, getYawFromVelocity(lastVelocity));

                return true;
        }
        return false;
    });

    private final Event resetBlink = new ClientTickEvent(null, params -> {
        if (handledVelocity || !C.isInGame()) return false;

        boolean stopBlinking = WorldUtil.ticks - lastUnhandledVelocityTick > maxDelayTicks;
        switch (lastVelocityMode) {
            case Delay:
                handledVelocity |= stopBlinking;
                BlinkUtil.popBlink(false, stopBlinking, this.getClass());
                break;
            case Dynamic:
                if (C.p().isOnGround()) {
                    stopBlinking = true;

                    // stops you blocking ur sword, so u can sprint, im sure the 1 tick of not blocking wont matter.
                    if (C.p().getMainHandStack().getUseAction() == UseAction.BLOCK || !C.p().isUsingItem())
                        PlayerUtil.oneTickKeybind(C.mc.options.useKey, false);
                }

                BlinkUtil.popBlink(false, stopBlinking, this.getClass());
                break;
        }

        return false;
    });

    // https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/combat/Velocity.java#L203
    private static float getYawFromVelocity(Vec3d velocity) {
        float yaw = (float) (Math.toDegrees(Math.atan2(velocity.x + velocity.z, velocity.x - velocity.z)) + 45);
        return yaw > 180 ? yaw - 360 : yaw;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
