package com.scale.scalehackv4.mixins.client.altswitcher;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.alts.SessionUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class TokenLoginButtonMixin extends Screen {
    protected TokenLoginButtonMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    protected void init(CallbackInfo ci) {
        this.addDrawableChild(
                ButtonWidget.builder(Text.of("Token Login"), (buttonWidget) -> {
                    Session session = SessionUtil.queryPlayerProfile(this.client.keyboard.getClipboard());
                    if (session == null) return;

                    ((OverrideSessionMixin)C.mc).setSession(session);
                }).dimensions(10, 10, 100, 20).build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        context.drawText(this.textRenderer, Text.of(C.mc.getSession().getUsername()), 112, 15, 0xFFFFFFFF, true);
    }
}
