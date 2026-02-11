package com.scale.scalehackv4.mixins.render.freelook;

import com.scale.scalehackv4.modules.impl.render.Freelook;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(InGameOverlayRenderer.class)
public class NoClipRenderMixin {
    @Redirect(method = "renderOverlays", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;noClip:Z"))
    private boolean renderNoClip(PlayerEntity player) {
        return player.noClip || Freelook.isPositionLocked();
    }
}
