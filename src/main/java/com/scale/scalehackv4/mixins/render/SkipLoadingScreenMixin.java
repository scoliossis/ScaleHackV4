package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.modules.impl.render.NoRender;
import net.minecraft.client.world.ClientChunkLoadProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientChunkLoadProgress.class)
public class SkipLoadingScreenMixin {
    // surprisingly, this does not conflict with any other mods i use, therefore its likely got a bad side effect.
    @Inject(method = "isDone", at = @At("HEAD"), cancellable = true)
    private void forceWait(CallbackInfoReturnable<Boolean> cir) {
        if (NoRender.shouldHideLoadingWorldScreen()) {
            cir.setReturnValue(true);
        }
    }
}
