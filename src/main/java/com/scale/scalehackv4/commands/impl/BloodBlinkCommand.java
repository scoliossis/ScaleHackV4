package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.modules.impl.skyblock.DungeonScanner.BloodBlink;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;

import java.util.concurrent.CompletableFuture;

public class BloodBlinkCommand extends Command {
    @Override
    public String name() {
        return "bb";
    }

    @Override
    public boolean handleCommand(String text) {
        BloodBlink.enableBB(true, true, AreaUtil.isDungeonStarted() || DungeonScanner.finishedScanning());

        return true;
    }

    @Override
    public void handleTab(String text, int pos, CompletableFuture<Suggestions> pendingSuggestions) {

    }
}
