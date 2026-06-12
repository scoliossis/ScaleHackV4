package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.screens.mainmenu.MainMenuScreen;
import com.scale.scalehackv4.utils.C;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MainMenuMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        // no point initialising the old ui when the official new scale ui is here.
        ci.cancel();

        C.mc.setScreen(new MainMenuScreen(Text.of("official scalehake main menu boom chika wowow")));
    }
}
