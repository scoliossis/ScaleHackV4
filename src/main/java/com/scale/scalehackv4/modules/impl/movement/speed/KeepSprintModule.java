package com.scale.scalehackv4.modules.impl.movement.speed;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.movement.SpeedModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.PlayerUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import lombok.AllArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;

import java.util.function.Predicate;

public class KeepSprintModule extends SpeedModule {
    @RegisterSubModule(name = "Keep Sprint", parent = "Modify Speed")
    public static KeepSprint keepSprint = KeepSprint.None;

    public static boolean lastAttackKeepsprinted = false;

    public static boolean shouldKeepsprint() {
        return ModuleUtil.isEnabled(SpeedModule.class)
                && keepSprint != KeepSprint.None
                && !C.p().horizontalCollision
                && C.p().forwardSpeed > 0
                && C.p().isSprinting()
                && C.p().hurtTime < 5;
    }

    private final Event onRotationEvent = new RotationEvent(ModuleUtil.getModule(SpeedModule.class), params -> {
        // hypixel keepsprint only works at 7 cps :pensive:
        Predicate<Entity> attackedLastTick = e -> WorldUtil.tick - PlayerUtil.lastAttackTick <= 1;
        if (lastAttackKeepsprinted && PlayerUtil.getThisTickAttackEntity(attackedLastTick) != null)
            PlayerUtil.unsetAttack();

        return false;
    }, 3001);

    @AllArgsConstructor
    public enum KeepSprint {
        None(()->{},()->{}),
        Vanilla(()->{},()->{}),
        // thanks badaiim <3
        Hypixel(
                () -> {
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(C.p(), ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    PacketUtil.sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, true)));
                },
                () -> {
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(C.p(), ClientCommandC2SPacket.Mode.START_SPRINTING));
                    PacketUtil.sendPacket(new PlayerInputC2SPacket(new PlayerInput(
                            KeyboardInputEvent.lastInput.forwardKey,
                            KeyboardInputEvent.lastInput.backKey,
                            KeyboardInputEvent.lastInput.leftKey,
                            KeyboardInputEvent.lastInput.rightKey,
                            KeyboardInputEvent.lastInput.jumpKey,
                            KeyboardInputEvent.lastInput.sneakKey,
                            true
                    )));

                    // ambient only swings once extra, but i find swinging more gives better hit reg, shrug.
                    C.p().swingHand(Hand.MAIN_HAND);
                }
        );

        public final Runnable preAttack;
        public final Runnable postAttack;
    }
}
