package com.crovex.practice.ffa;

import com.crovex.practice.CrovexPractice;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

public class DynamicFfaCommand extends Command {
    private final CrovexPractice plugin;
    private final String ffaArenaName;

    public DynamicFfaCommand(CrovexPractice plugin, String name, String ffaArenaName) {
        super(name);
        this.plugin = plugin;
        this.ffaArenaName = ffaArenaName;
        this.setDescription("Join the " + ffaArenaName + " FFA arena.");
        this.setUsage("/" + name);
        this.setAliases(Collections.emptyList());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getRawMessage("general.only-players"));
            return true;
        }
        Player player = (Player) sender;
        plugin.getFfaManager().joinFfa(player, ffaArenaName);
        return true;
    }
}
