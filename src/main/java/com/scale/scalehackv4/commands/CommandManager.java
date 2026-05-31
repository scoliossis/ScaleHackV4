package com.scale.scalehackv4.commands;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import lombok.Getter;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// based of the command manager from https://github.com/scoliossis/ScaleHackV3
@RegisterListeners
public class CommandManager {
    public static char prefix = '.';
    public static int moduleFuzzySearchLimit = 20;

    private final Event handleCommandSent = new ChatEvent.Sent(null, (params -> {
        if (params.message.isEmpty() || params.message.charAt(0) != CommandManager.prefix) return false;

        C.mc.inGameHud.getChatHud().addToMessageHistory(params.message);

        int space = params.message.indexOf(' ');
        Command command = getCommand(params.message.substring(1, space == -1 ? params.message.length()-1 : space));

        if (command == null || !command.handleCommand(params.message.substring(space+1))) {
            help(null);
        }

        return true;
    }));

    @Getter private static final ArrayList<Command> commands = new ArrayList<>();

    public static void init() {
        for (Class<?> clazz : C.REFLECTIONS.getSubTypesOf(Command.class)) {
            try {
                System.out.println("Registering command: " + clazz.getSimpleName());
                commands.add((Command) clazz.getDeclaredConstructor().newInstance());
            } catch (Exception e) { }
        }
    }

    public static List<ExtractedResult> getPossibleCommands(String input) {
        List<String> commandNames = commands.stream().map(Command::name).collect(Collectors.toList());

        // i love fuzzysearch
        List<ExtractedResult> results = FuzzySearch.extractAll(input.toLowerCase(), commandNames, 40);
        results.sort(Comparator.comparingInt(ExtractedResult::getScore).reversed());
        return results;
    }

    public static Command getCommand(String command) {
        List<ExtractedResult> results = getPossibleCommands(command);

        if (results.isEmpty()) return null;
        return commands.get(results.getFirst().getIndex());
    }

    public static boolean handleTab(TextFieldWidget textField, CompletableFuture<Suggestions> pendingSuggestions) {
        String text = textField.getText().substring(0, textField.getCursor());

        if (!text.startsWith(String.valueOf(prefix))) return false;

        try {
            pendingSuggestions.get(100, TimeUnit.MILLISECONDS).getList().clear();
        } catch (Exception e) {
            //e.printStackTrace();
            return false;
        }


        if (text.length() == 1) {
            for (Command command : commands) {
                addCommand(command.name(), StringRange.at(1), pendingSuggestions);
            }

            return true;
        }

        int space = text.indexOf(' ');

        if (space == -1) {
            for (ExtractedResult result : getPossibleCommands(text)) {
                addCommand(commands.get(result.getIndex()).name(), StringRange.between(1, textField.getCursor()), pendingSuggestions);
            }

            return true;
        }

        String commandName = text.substring(1, space);

        Command command = getCommand(commandName);
        if (command == null) return true;

        command.handleTab(text.substring(space+1), space+1, pendingSuggestions);

        return true;
    }

    public static void addCommand(String command, StringRange range, CompletableFuture<Suggestions> pendingSuggestions) {
        try {
            pendingSuggestions.get(100, TimeUnit.MILLISECONDS).getList().add(new Suggestion(range, command));
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    // todo: impl help command
    private static void help(Command command) {
        ChatUtil.prefixMessage("help commmand please <3");
    }
}
