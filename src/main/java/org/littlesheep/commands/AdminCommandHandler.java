package org.littlesheep.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.littlesheep.paytofly;
import org.littlesheep.utils.DataValidator;
import org.littlesheep.gui.EnhancedFlightShopGUI;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * 管理员命令处理器
 */
public class AdminCommandHandler {
    private final paytofly plugin;
    private final String prefix;

    public AdminCommandHandler(paytofly plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getPrefix();
    }

    /**
     * 处理管理员命令
     */
    public boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getFlightShopGUI().openGUI((Player) sender, EnhancedFlightShopGUI.GUIType.MAIN);
            } else {
                sender.sendMessage(prefix + plugin.getLang().getMessage("console-player-command"));
            }
            return true;
        }

        String command = args[0].toLowerCase();
        
        switch (command) {
            case "disable":
                return handleDisableCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender, args);
            case "give":
                return handleGiveCommand(sender, args);
            case "bypass":
                return handleBypassCommand(sender, args);
            case "test":
                return handleTestCommand(sender, args);
            case "stats":
                return handleStatsCommand(sender, args);
            default:
                sender.sendMessage(prefix + plugin.getLang().getMessage("console-unknown-command"));
                return true;
        }
    }

    /**
     * 处理禁用命令
     */
    private boolean handleDisableCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("paytofly.admin")) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("cmd-disable-usage"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("player-not-found", "{player}", args[1]));
            return true;
        }

        // 检查目标玩家是否有飞行权限
        Long endTime = plugin.getStorage().getPlayerFlightTime(target.getUniqueId());
        boolean hadFlight = endTime != null && endTime > System.currentTimeMillis();

        // 禁用飞行
        target.setAllowFlight(false);
        target.setFlying(false);
        plugin.getStorage().removePlayerFlightTime(target.getUniqueId());
        plugin.getFlyingPlayers().remove(target.getUniqueId());

        // 同步取消MHDF-Tools飞行权限
        syncMHDFToolsDisableFlight(target);

        // 取消倒计时
        plugin.getCountdownManager().cancelCountdown(target);

        // 发送消息
        target.sendMessage(prefix + plugin.getLang().getMessage("flight-disabled-by-admin"));

        if (hadFlight) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("admin-disabled-flight", "{player}", target.getName()));
        } else {
            sender.sendMessage(prefix + plugin.getLang().getMessage("cmd-player-had-no-permission", "{player}", target.getName()));
        }

        return true;
    }

    /**
     * 处理重载命令
     */
    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("paytofly.admin")) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("no-permission"));
            return true;
        }

        // 重载配置文件
        plugin.reloadConfig();
        
        // 重载语言文件
        plugin.loadMessageConfig();

        // 重新初始化GUI
        plugin.getFlightShopGUI().reloadConfig();

        sender.sendMessage(prefix + plugin.getLang().getMessage("config-reloaded"));
        return true;
    }

    /**
     * 处理给予命令
     */
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("paytofly.admin")) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("cmd-give-usage"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("player-not-found", "{player}", args[1]));
            return true;
        }

        // 解析时间格式
        CommandHandler.TimeParseResult parseResult = CommandHandler.parseTimeArgument(args[2]);
        if (!parseResult.isSuccess()) {
            sender.sendMessage(prefix + "§c" + parseResult.getErrorMessage());
            return true;
        }

        DataValidator.TimeData timeData = parseResult.getTimeData();
        int amount = timeData.amount;
        String unit = timeData.unit;
        long durationMillis = timeData.milliseconds;
        String unitName = getUnitName(unit);

        // 使用安全的时间计算
        long existingEndTime = plugin.getFlyingPlayers().getOrDefault(target.getUniqueId(), System.currentTimeMillis());
        DataValidator.ValidationResult<Long> timeResult = 
            DataValidator.addTimeToEndTime(existingEndTime, durationMillis);

        if (!timeResult.isSuccess()) {
            sender.sendMessage(prefix + "§c" + timeResult.getErrorMessage());
            return true;
        }

        long endTime = timeResult.getData();

        // 设置飞行权限
        target.setAllowFlight(true);
        target.setFlying(true);

        plugin.getFlyingPlayers().put(target.getUniqueId(), endTime);
        plugin.getStorage().setPlayerFlightTime(target.getUniqueId(), endTime);

        // 同步MHDF-Tools飞行权限
        syncMHDFToolsFlight(target, endTime);

        // 启动倒计时
        plugin.getCountdownManager().startCountdown(target, endTime);

        // 发送消息
        if (existingEndTime > System.currentTimeMillis()) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("cmd-add-time", 
                "{player}", target.getName(),
                "{amount}", String.valueOf(amount),
                "{unit}", unitName));
        } else {
            sender.sendMessage(prefix + plugin.getLang().getMessage("cmd-give-time", 
                "{player}", target.getName(),
                "{amount}", String.valueOf(amount),
                "{unit}", unitName));
        }

        target.sendMessage(prefix + plugin.getLang().getMessage("cmd-given-time", 
            "{amount}", String.valueOf(amount),
            "{unit}", unitName));

        return true;
    }

    /**
     * 处理绕过命令
     */
    private boolean handleBypassCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("paytofly.admin")) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("bypass.usage"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("player-not-found", "{player}", args[1]));
            return true;
        }

        if (args.length >= 3 && args[2].equalsIgnoreCase("remove")) {
            // 移除绕过权限
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "lp user " + target.getName() + " permission unset paytofly.bypass");
            sender.sendMessage(prefix + plugin.getLang().getMessage("bypass.remove-success", "{player}", target.getName()));
            target.sendMessage(prefix + plugin.getLang().getMessage("bypass.target-remove"));
        } else {
            // 添加绕过权限
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "lp user " + target.getName() + " permission set paytofly.bypass true");
            sender.sendMessage(prefix + plugin.getLang().getMessage("bypass.add-success", "{player}", target.getName()));
            target.sendMessage(prefix + plugin.getLang().getMessage("bypass.target-add"));
        }

        return true;
    }

    /**
     * 处理测试命令
     */
    private boolean handleTestCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("command-player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("paytofly.admin")) {
            player.sendMessage(prefix + plugin.getLang().getMessage("no-permission"));
            return true;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            player.sendMessage(prefix + plugin.getLang().getMessage("papi-not-found"));
            return true;
        }

        player.sendMessage(prefix + plugin.getLang().getMessage("papi-test-title"));
        player.sendMessage(prefix + plugin.getLang().getMessage("papi-test-remaining", 
            "{value}", PlaceholderAPI.setPlaceholders(player, "%paytofly_remaining%")));
        player.sendMessage(prefix + plugin.getLang().getMessage("papi-test-status", 
            "{value}", PlaceholderAPI.setPlaceholders(player, "%paytofly_status%")));
        player.sendMessage(prefix + plugin.getLang().getMessage("papi-test-expiretime", 
            "{value}", PlaceholderAPI.setPlaceholders(player, "%paytofly_expiretime%")));
        player.sendMessage(prefix + plugin.getLang().getMessage("papi-test-mode", 
            "{value}", PlaceholderAPI.setPlaceholders(player, "%paytofly_mode%")));

        return true;
    }

    /**
     * 处理统计命令
     */
    private boolean handleStatsCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("paytofly.admin")) {
            sender.sendMessage(prefix + plugin.getLang().getMessage("no-permission"));
            return true;
        }

        sender.sendMessage(prefix + "§6===== PayToFly 系统统计 =====");

        // 异常处理统计
        if (plugin.getExceptionHandler() != null) {
            sender.sendMessage(prefix + "§e" + plugin.getExceptionHandler().getStatistics());
        }

        // 倒计时统计
        if (plugin.getCountdownManager() != null) {
            sender.sendMessage(prefix + "§e" + plugin.getCountdownManager().getStatistics());
        }

        // 存储统计
        if (plugin.getStorage() instanceof org.littlesheep.data.CachedStorage) {
            org.littlesheep.data.CachedStorage cachedStorage = (org.littlesheep.data.CachedStorage) plugin.getStorage();
            sender.sendMessage(prefix + "§e" + cachedStorage.getCacheStatistics());
            
            // 显示底层存储统计
            if (cachedStorage.getDelegate() instanceof org.littlesheep.data.MySqlStorage) {
                org.littlesheep.data.MySqlStorage mysqlStorage = (org.littlesheep.data.MySqlStorage) cachedStorage.getDelegate();
                sender.sendMessage(prefix + "§e" + mysqlStorage.getDatabaseStatistics());
            } else if (cachedStorage.getDelegate() instanceof org.littlesheep.data.JsonStorage) {
                org.littlesheep.data.JsonStorage jsonStorage = (org.littlesheep.data.JsonStorage) cachedStorage.getDelegate();
                sender.sendMessage(prefix + "§e" + jsonStorage.getStatistics());
            }
        } else if (plugin.getStorage() instanceof org.littlesheep.data.MySqlStorage) {
            org.littlesheep.data.MySqlStorage mysqlStorage = (org.littlesheep.data.MySqlStorage) plugin.getStorage();
            sender.sendMessage(prefix + "§e" + mysqlStorage.getDatabaseStatistics());
        } else if (plugin.getStorage() instanceof org.littlesheep.data.JsonStorage) {
            org.littlesheep.data.JsonStorage jsonStorage = (org.littlesheep.data.JsonStorage) plugin.getStorage();
            sender.sendMessage(prefix + "§e" + jsonStorage.getStatistics());
        }

        // 资源管理器统计
        if (plugin.getResourceManager() != null) {
            sender.sendMessage(prefix + "§e" + plugin.getResourceManager().getResourceStatistics());
        }

        sender.sendMessage(prefix + "§6========================");
        return true;
    }

    /**
     * 获取时间单位名称
     */
    private String getUnitName(String unit) {
        switch (unit) {
            case "m": return plugin.getLang().getMessage("time-format.minute");
            case "h": return plugin.getLang().getMessage("time-format.hour");
            case "d": return plugin.getLang().getMessage("time-format.day");
            case "w": return plugin.getLang().getMessage("time-format.week");
            case "mo": return plugin.getLang().getMessage("time-format.month");
            default: return unit;
        }
    }

    /**
     * 同步MHDF-Tools飞行权限
     */
    private void syncMHDFToolsFlight(Player player, long endTime) {
        if (Bukkit.getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                String command = "fly " + player.getName() + " true";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                plugin.getLogger().info(plugin.getLang().getMessage("mhdf-sync-success", "{player}", player.getName()));
            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getLang().getMessage("mhdf-sync-failed", "{error}", e.getMessage()));
            }
        }
    }

    /**
     * 同步取消MHDF-Tools飞行权限
     */
    private void syncMHDFToolsDisableFlight(Player player) {
        if (Bukkit.getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "fly " + player.getName() + " false");
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "lp user " + player.getName() + " permission unset mhdtools.commands.fly.temp");
                
                plugin.getLogger().info(plugin.getLang().getMessage("mhdf-disable-success", "{player}", player.getName()));
            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getLang().getMessage("mhdf-disable-failed", "{error}", e.getMessage()));
            }
        }
    }
}