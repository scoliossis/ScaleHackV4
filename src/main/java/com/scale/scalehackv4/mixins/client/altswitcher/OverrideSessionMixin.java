package com.scale.scalehackv4.mixins.client.altswitcher;

import com.scale.scalehackv4.utils.client.altswitcher.SessionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// code from mushroom client, no exact link because it's not an open source cheat, but there are src leaks, im sure u can find it
@Mixin(MinecraftClient.class)
public class OverrideSessionMixin {
    @Shadow
    @Final
    private Session session;

    @Inject(at = @At("HEAD"), method = "getSession", cancellable = true)
    public void getSession(CallbackInfoReturnable<Session> cir) {
        if (SessionUtil.sessionOverride != null) cir.setReturnValue(SessionUtil.sessionOverride);
    }

    // comment from batman, how fancy!
    // This is the only method that directly references the session field instead of using getSession()
    @Redirect(method = "getGameProfile", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;session:Lnet/minecraft/client/session/Session;", opcode = Opcodes.GETFIELD))
    public Session getProfileProperties(MinecraftClient instance) {
        return SessionUtil.sessionOverride != null ? SessionUtil.sessionOverride : this.session;
    }

}
