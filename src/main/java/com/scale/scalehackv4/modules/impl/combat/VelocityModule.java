package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.*;
import lombok.AllArgsConstructor;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

@RegisterModule(
        name = "Velocity",
        category = Category.COMBAT
)
public class VelocityModule extends Module {
    @RegisterSubModule(name = "Knockback Mode")
    public static Mode mode = Mode.Blink;
    @RegisterSubModule(name = "Horizontal", parent = "Knockback Mode", modeParentString = {"Edit", "Add"}, min = -2, max = 2)
    public static float horizontal = 0;
    @RegisterSubModule(name = "Vertical", parent = "Knockback Mode", modeParentString = {"Edit", "Add"}, min = -2, max = 2)
    public static float vertical = 0;

    @RegisterSubModule(name = "Max Delay Ticks", parent = "Knockback Mode", modeParentString = {"Blink"}, min = 1, max = 60)
    public static int maxDelayTicks = 13;

    @RegisterSubModule(name = "Force Sprint", parent = "Knockback Mode", modeParentString = {"Jump_Reset", "Blink"})
    public static boolean forceSprint = true;

    @RegisterSubModule(name = "Unblink Conditions", parent = "Knockback Mode", modeParentString = {"Blink"})
    public static UnblinkConditions unblinkConditions = UnblinkConditions.On_Ground;

    @AllArgsConstructor
    public enum UnblinkConditions implements MultiSelectEnum {
        On_Ground(x -> C.p().isOnGround()){
            @Override public boolean enabledByDefault() {return true;}
        },
        Attack(x -> PlayerUtil.lastAttackTick == WorldUtil.tick){
            @Override public boolean enabledByDefault() {return true;}
        };

        public final Predicate<?> ready;

        public static boolean isReady(UnblinkConditions unblinkConditions) {
            return unblinkConditions.ready.test(null) || !MultiSelectUtil.isEnabled(unblinkConditions);
        }

        public static boolean isReady() {
            if (currentVelocityMode != Mode.Blink || WorldUtil.tick - lastVelocityTick > maxDelayTicks) return true;
            if (noConditions()) return false;

            for (UnblinkConditions unblinkConditions : values()) {
                if (!isReady(unblinkConditions)) return false;
            }

            return true;
        }

        // if you have no unblink conditions, you just want to delay kb.
        public static boolean noConditions() {
            for (UnblinkConditions unblinkCondition : values()) {
                if (MultiSelectUtil.isEnabled(unblinkCondition)) return false;
            }

            return true;
        }
    }

    @RegisterSubModule(name = "Ignore")
    public static InvalidVelocity ignore = InvalidVelocity.Teleports;

    @AllArgsConstructor
    public enum InvalidVelocity implements MultiSelectEnum {
        Teleports(velocity -> PacketUtil.lastPacket instanceof PlayerPositionLookS2CPacket)
                {@Override public boolean enabledByDefault() {return true;}},
        // todo: dumb.
        Explosions(velocity ->
                Math.hypot(velocity.getVelocity().x, velocity.getVelocity().z) > 1
                        || velocity.getVelocity().y > 1
                        || velocity.getVelocity().y < 0.2
        )
                {@Override public boolean enabledByDefault() {return true;}},
        Irrelevant(velocity -> velocity.getVelocity().horizontalLength() < 0.1)
                {@Override public boolean enabledByDefault() {return true;}};

        public final Predicate<EntityVelocityUpdateS2CPacket> isInvalid;

        public static boolean isValid(EntityVelocityUpdateS2CPacket velocity) {
            for (InvalidVelocity ignore : values()) {
                if (MultiSelectUtil.isEnabled(ignore) && ignore.isInvalid.test(velocity)) return false;
            }

            return true;
        }
    }

    private static Mode currentVelocityMode = null;

    private static Vec3d lastVelocity = Vec3d.ZERO;
    private static int lastVelocityTick = 0;

    private static boolean shouldJump = false;

    public enum Mode {
        Edit,
        Add,
        Jump_Reset,
        Blink
    }

    private final Event cancelVelocity = new PacketEvent.Received(this, params -> {
        if (!C.isInGame()
                || PacketUtil.clientSentPackets.contains(params.packet)
                || !(params.packet instanceof EntityVelocityUpdateS2CPacket velocityPacket)
                || velocityPacket.getEntityId() != C.p().getId()
                || !InvalidVelocity.isValid(velocityPacket)) return false;

        lastVelocity = velocityPacket.getVelocity();

        if (currentVelocityMode != null || Bus.isEventCancelled(PacketEvent.Received.class)) return false;

        lastVelocityTick = WorldUtil.tick;

        return switch (mode) {
            case Add -> {
                C.p().setVelocity(
                        lastVelocity.x * horizontal + C.p().getVelocity().x * (1 - horizontal),
                        lastVelocity.y * vertical + C.p().getVelocity().y * (1 - vertical),
                        lastVelocity.z * horizontal + C.p().getVelocity().z * (1 - horizontal)
                );
                yield true;
            }
            case Edit -> {
                C.p().setVelocity(lastVelocity.x * horizontal, lastVelocity.y * vertical, lastVelocity.z * horizontal);
                yield true;
            }
            case Jump_Reset -> {
                currentVelocityMode = Mode.Jump_Reset;
                yield false;
            }
            case Blink -> {
                currentVelocityMode = Mode.Blink;
                BlinkUtil.pushBlink(false, true, this.getClass(), params.packet);
                yield true;
            }
        };
    });

    private final Event handleVelocityRotation = new RotationEvent(this, params -> {
        shouldJump = false;
        MovementUtil.forceNoJump |= MultiSelectUtil.isEnabled(UnblinkConditions.On_Ground) && BlinkUtil.isBlinking(false, true, this.getClass());

        if (currentVelocityMode == null
                || (currentVelocityMode != Mode.Jump_Reset && (currentVelocityMode != Mode.Blink)
                || !UnblinkConditions.isReady())
        ) return false;

        currentVelocityMode = null;
        shouldJump = shouldJump();
        BlinkUtil.popBlink(false, true, this.getClass());

        if (shouldJump) params.rotation.yaw = RotationUtil.applyWrap360(params.rotation.yaw, getYawFromVelocity(lastVelocity));

        return shouldJump;
    }, 0);

    private final Event handleKeyboardInputs = new KeyboardInputEvent(this, params -> {
        params.backKey = !shouldJump && params.backKey;
        params.leftKey = !shouldJump && params.leftKey;
        params.rightKey = !shouldJump && params.rightKey;
        params.sneakKey = !shouldJump && params.sneakKey;

        params.forwardKey = shouldJump || params.forwardKey;
        params.sprintKey = shouldJump || params.sprintKey;
        params.jumpKey = shouldJump || !MultiSelectUtil.isEnabled(UnblinkConditions.On_Ground) || (!BlinkUtil.isBlinking(false, true, this.getClass()) && params.jumpKey);

        // stops you blocking ur sword, so u can sprint, im sure the 1 tick of not blocking wont matter.
        if (C.p().getMainHandStack().getUseAction() == UseAction.BLOCK || !C.p().isUsingItem())
            PlayerUtil.oneTickKeybind(C.mc.options.useKey, false);

        return shouldJump;
    });

    private static boolean shouldJump() {
        return C.p().isOnGround() && (forceSprint || C.p().isSprinting());
    }

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
        if (BlinkUtil.isBlinking(false, true, this.getClass())) {
            BlinkUtil.popBlink(false, true, this.getClass());
        }
        currentVelocityMode = null;
    }
}