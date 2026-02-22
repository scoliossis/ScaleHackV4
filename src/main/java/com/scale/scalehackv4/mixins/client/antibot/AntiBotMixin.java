package com.scale.scalehackv4.mixins.client.antibot;

import com.scale.scalehackv4.bridge.antibot.AntiBotBridge;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(SocialInteractionsPlayerListWidget.class)
public abstract class AntiBotMixin implements AntiBotBridge {
    @Shadow
    @Final
    private List<SocialInteractionsPlayerListEntry> players;

    @Shadow
    public abstract void update(Collection<UUID> uuids, double scrollAmount, boolean includeOffline);

    @Override
    public List<SocialInteractionsPlayerListEntry> bridge$getPlayers() {
        return this.players;
    }
}
