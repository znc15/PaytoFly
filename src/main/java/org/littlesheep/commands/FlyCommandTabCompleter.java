package org.littlesheep.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;

public class FlyCommandTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("time");
            completions.add("help");
            if (sender.hasPermission("paytofly.admin")) {
                completions.add("reload");
            }
            return completions;
        }
        
        return null;
    }
} 