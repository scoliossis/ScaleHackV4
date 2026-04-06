package com.scale.scalehackv4.mixins.client;

import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.CommandManager;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class TabCompleteMixin{
    @Shadow private @Nullable CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow @Final TextFieldWidget textField;
    @Shadow private ChatInputSuggestor.SuggestionWindow window;

    // todo: make custom tab complete
    @Inject(method = "refresh", at = @At(value = "HEAD"), cancellable = true)
    public void listSuggestions(CallbackInfo ci) {
        if (this.pendingSuggestions == null) return;

        if (CommandManager.handleTab(this.textField, this.pendingSuggestions)) {
            if (this.textField.getText().endsWith(" ")) this.window = null;
            ci.cancel();
        }
    }
}
