package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.commands.CommandManager;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.ModuleManager;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ToggleCommand extends Command {
    @Override
    public String name() {
        return "toggle";
    }

    @Override
    public boolean handleCommand(String text) {
        if (text.isEmpty()) return false;

        Module module = ModuleManager.getModule(text, CommandManager.moduleFuzzySearchLimit);
        if (module == null) return false;

        module.setEnabled(!module.isEnabled());

        return true;
    }

    @Override
    public void handleTab(String text, int pos, CompletableFuture<Suggestions> pendingSuggestions) {
        List<Module> modules = ModuleManager.getModules();

        if (text.isEmpty()) {
            for (Module module : modules) {
                CommandManager.addCommand(module.getAnnotation().name(), StringRange.at(pos), pendingSuggestions);
            }

            return;
        }

        StringRange range = StringRange.between(pos, pos+text.length());

        for (ExtractedResult module : ModuleManager.getModules(text, CommandManager.moduleFuzzySearchLimit)) {
            CommandManager.addCommand(modules.get(module.getIndex()).getAnnotation().name(), range, pendingSuggestions);
        }
    }
}
