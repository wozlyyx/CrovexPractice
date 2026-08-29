package com.crovex.practice.command;

import com.crovex.practice.CrovexPractice;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DynamicPracticeCommand extends Command {
    private final CrovexPractice plugin;
    private final String internalKey;
    private final PracticeCommand handler;

    public DynamicPracticeCommand(CrovexPractice plugin, String name, String description, List<String> aliases, String internalKey, PracticeCommand handler) {
        super(name);
        this.plugin = plugin;
        this.internalKey = internalKey;
        this.handler = handler;
        this.setDescription(description);
        this.setAliases(aliases);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return handler.handleInternalCommand(sender, internalKey, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return handler.handleInternalTabComplete(sender, internalKey, args);
    }
}
