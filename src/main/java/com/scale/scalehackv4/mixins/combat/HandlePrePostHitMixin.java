package com.scale.scalehackv4.mixins.combat;

import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.modules.impl.movement.speed.KeepSprintModule;
import com.scale.scalehackv4.utils.player.PlayerUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class HandlePrePostHitMixin {
    @Unique private boolean attacked = false;

    @Inject(method = "doAttack", at = @At(value = "HEAD"))
    private void onAttackHead(CallbackInfoReturnable<Boolean> cir) {
        PlayerUtil.lastSwingTick = WorldUtil.tick;
        attacked = false;
    }

    @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"))
    private void onPreAttack(CallbackInfoReturnable<Boolean> cir) {
        PlayerUtil.lastAttackTick = WorldUtil.tick;

        if (!(KeepSprintModule.lastAttackKeepsprinted = KeepSprintModule.shouldKeepsprint())) return;

        attacked = true;
        KeepSprintModule.keepSprint.preAttack.run();
    }

    @Inject(method = "doAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V", shift = At.Shift.AFTER))
    private void onPostAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!attacked || KeyboardInputEvent.lastInput == null) return;

        KeepSprintModule.keepSprint.postAttack.run();
    }

}
