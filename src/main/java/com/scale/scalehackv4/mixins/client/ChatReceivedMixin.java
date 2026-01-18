package com.scale.scalehackv4.mixins.client;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatReceivedMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    public void addMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (Bus.post(ChatEvent.Received.class, new ChatEvent.Params(message.getString()))) {
            ci.cancel();
        }
    }
}
