package com.scale.scalehackv4.mixins.combat;

import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.modules.impl.combat.KeepSprintModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.PacketUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class HypixelKeepSprintMixin {
    @Unique private boolean attacked = false;

    @Inject(method = "doAttack", at = @At(value = "HEAD"))
    private void onAttackHead(CallbackInfoReturnable<Boolean> cir) {
        attacked = false;
    }

    @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"))
    private void onPreAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!KeepSprintModule.shouldKeepsprint() || !KeepSprintModule.hypixel) return;

        attacked = true;
        PacketUtil.sendPacket(new ClientCommandC2SPacket(C.p(), ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        PacketUtil.sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, true)));
    }

    @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V", shift = At.Shift.AFTER))
    private void onPostAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!attacked || KeyboardInputEvent.lastInput == null) return;

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
        C.p().swingHand(Hand.MAIN_HAND);
    }

}
