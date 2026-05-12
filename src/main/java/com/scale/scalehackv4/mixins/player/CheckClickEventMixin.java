package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.utils.C;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class CheckClickEventMixin {
    @Shadow @Final public GameOptions options;

    @Shadow
    protected abstract void handleBlockBreaking(boolean breaking);

    @Unique private boolean cancelledAttack = false;

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0))
    private void handleInputEvents(CallbackInfo ci) {
        if (C.p().isUsingItem()) return;

        // if the event is cancelled, it stops u from clicking
        cancelledAttack = Bus.post(CheckClickEvent.Left.class, new CheckClickEvent.Params());
        while (cancelledAttack && this.options.attackKey.wasPressed());
        if (Bus.post(CheckClickEvent.Right.class, new CheckClickEvent.Params())) while (this.options.useKey.wasPressed());
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;handleBlockBreaking(Z)V"))
    private void blockClickDown(MinecraftClient instance, boolean breaking) {
        if (cancelledAttack) this.handleBlockBreaking(false);
        else this.handleBlockBreaking(breaking);
    }
}
