package com.scale.scalehackv4.mixins.render.NoRender;

import com.scale.scalehackv4.modules.impl.render.NoRender;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class PotionEffectsGuiMixin {
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(CallbackInfo ci) {
        if (NoRender.hidePotionsGUI()) {
            ci.cancel();
        }
    }
}
