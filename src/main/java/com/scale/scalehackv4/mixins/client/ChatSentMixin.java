package com.scale.scalehackv4.mixins.client;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatSentMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    public void sendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (Bus.post(ChatEvent.Sent.class, new ChatEvent.Params(chatText))) {
            ci.cancel();
        }
    }
}
