package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.utils.player.PlayerUtil;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public abstract class KeybindPressedMixin {
    @Shadow
    public abstract String getId();

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    public void isPressed(CallbackInfoReturnable<Boolean> cir) {
        Boolean override = PlayerUtil.getOverriddenKeybinds().get(this.hashCode());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    public void wasPressed(CallbackInfoReturnable<Boolean> cir) {
        Boolean override = PlayerUtil.getOverriddenKeybinds().get(this.hashCode());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
