package org.littlesheep.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FlyCommandTabCompleter implements TabCompleter {
    private final List<String> baseCommands = Arrays.asList("help", "time", "check", "disable", "reload", "test", "bypass", "give");
    private final List<String> adminCommands = Arrays.asList("disable", "reload", "test", "bypass", "give");
    private final List<String> timeFormats = Arrays.asList("5m", "10m", "30m", "1h", "2h", "6h", "12h", "1d", "3d", "7d", "1w", "2w", "4w", "1mo", "3mo", "6mo");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 根据发送者类型和权限调整命令补全
        boolean isConsole = !(sender instanceof Player);
        boolean hasAdminPerm = isConsole || sender.hasPermission("paytofly.admin");

        if (args.length == 1) {
            // 控制台只显示管理员命令
            if (isConsole) {
                completions.addAll(adminCommands);
            } else {
                // 基础命令和时间建议
                completions.addAll(baseCommands);
                if (sender.hasPermission("paytofly.use")) {
                    completions.addAll(timeFormats);                }
            }
            return filterCompletions(completions, args[0]);
        } else if (args.length == 2) {
            // 对于需要玩家名参数的子命令
            if ((args[0].equalsIgnoreCase("disable") || 
                 args[0].equalsIgnoreCase("bypass") || 
                 args[0].equalsIgnoreCase("give")) && hasAdminPerm) {
                return getOnlinePlayerNames(args[1]);
            }
        } else if (args.length == 3) {
            // 对于bypass命令的特殊参数
            if (args[0].equalsIgnoreCase("bypass") && hasAdminPerm) {
                return filterCompletions(Arrays.asList("remove"), args[2]);
            }
            // 对于give命令的时间参数
            if (args[0].equalsIgnoreCase("give") && hasAdminPerm) {
                return filterCompletions(timeFormats, args[2]);
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
} 