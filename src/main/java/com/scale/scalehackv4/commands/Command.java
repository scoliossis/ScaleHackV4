package com.scale.scalehackv4.commands;

import com.mojang.brigadier.suggestion.Suggestions;

import java.util.concurrent.CompletableFuture;

public abstract class Command {
    public abstract String name();
    /// returns whether the command successfully executed
    public abstract boolean handleCommand(String text);
    public abstract void handleTab(String text, int pos, CompletableFuture<Suggestions> pendingSuggestions);
}