package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.modules.impl.player.ScaffoldModule;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class FastPlaceMixin {
    @Shadow
    private int itemUseCooldown;

    @Inject(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;itemUseCooldown:I", ordinal = 0, shift = At.Shift.AFTER))
    private void doItemUse(CallbackInfo ci) {
        this.itemUseCooldown = ScaffoldModule.getPlaceDelay();
    }
}
