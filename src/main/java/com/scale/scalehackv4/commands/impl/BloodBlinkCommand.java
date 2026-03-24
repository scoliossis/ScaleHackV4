package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.commands.CommandManager;
import com.scale.scalehackv4.modules.impl.skyblock.DungeonScanner.BloodBlink;

import java.util.concurrent.CompletableFuture;

public class BloodBlinkCommand extends Command {
    @Override
    public String name() {
        return "bb";
    }

    @Override
    public boolean handleCommand(String text) {
        BloodBlink.enableBB();

        return true;
    }

    @Override
    public void handleTab(String text, int pos, CompletableFuture<Suggestions> pendingSuggestions) {
        if (!text.contains(" ")) {
            StringRange range = StringRange.between(pos, pos+text.length());
            CommandManager.addCommand("scan", range, pendingSuggestions);
        }
    }
}
