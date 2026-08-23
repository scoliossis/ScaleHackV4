package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
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
    @Unique private boolean cancelledUseItem = false;

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 14))
    private boolean handleLeftClickCheck(KeyBinding instance) {
        // if the event is cancelled, it stops u from clicking
        cancelledAttack = Bus.post(CheckClickEvent.Left.class, new CheckClickEvent.Params());
        while (cancelledAttack && this.options.attackKey.wasPressed());
        return instance.wasPressed();
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 15))
    private boolean handleRightClickCheck(KeyBinding instance) {
        cancelledUseItem = Bus.post(CheckClickEvent.Right.class, new CheckClickEvent.Params());
        while (cancelledUseItem && this.options.useKey.wasPressed());

        return instance.wasPressed();
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;handleBlockBreaking(Z)V"))
    private void blockClickDown(MinecraftClient instance, boolean breaking) {
        if (cancelledAttack) this.handleBlockBreaking(false);
        else this.handleBlockBreaking(breaking);
    }

    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V"), cancellable = true)
    private void blockInteractDown(CallbackInfo ci) {
        if (cancelledUseItem) ci.cancel();
    }
}
