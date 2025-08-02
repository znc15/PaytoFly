package org.littlesheep.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.littlesheep.paytofly;
import org.littlesheep.utils.DataValidator;

/**
 * 命令处理器 - 拆分主类中的命令逻辑
 */
public class CommandHandler {
    private final paytofly plugin;
    private final AdminCommandHandler adminHandler;
    private final PlayerCommandHandler playerHandler;

    public CommandHandler(paytofly plugin) {
        this.plugin = plugin;
        this.adminHandler = new AdminCommandHandler(plugin);
        this.playerHandler = new PlayerCommandHandler(plugin);
    }

    /**
     * 处理命令
     */
    public boolean handleCommand(CommandSender sender, String[] args) {
        // 检查是否为管理员命令
        if (isAdminCommand(args)) {
            return adminHandler.handleAdminCommand(sender, args);
        }

        // 玩家命令必须由玩家执行
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("command-player-only"));
            return true;
        }

        Player player = (Player) sender;
        return playerHandler.handlePlayerCommand(player, args);
    }

    /**
     * 检查是否为管理员命令
     */
    private boolean isAdminCommand(String[] args) {
        if (args.length == 0) {
            return false;
        }
        
        String cmd = args[0].toLowerCase();
        return cmd.equals("disable") || 
               cmd.equals("reload") || 
               cmd.equals("give") || 
               cmd.equals("bypass") ||
               cmd.equals("test") ||
               cmd.equals("stats");
    }

    /**
     * 解析时间参数的通用方法
     */
    public static TimeParseResult parseTimeArgument(String timeArg) {
        DataValidator.ValidationResult<DataValidator.TimeData> validationResult = 
            DataValidator.validateTimeFormat(timeArg);
        
        if (!validationResult.isSuccess()) {
            return TimeParseResult.error(validationResult.getErrorMessage());
        }
        
        DataValidator.TimeData timeData = validationResult.getData();
        return TimeParseResult.success(timeData);
    }

    /**
     * 时间解析结果类
     */
    public static class TimeParseResult {
        private final boolean success;
        private final DataValidator.TimeData timeData;
        private final String errorMessage;

        private TimeParseResult(boolean success, DataValidator.TimeData timeData, String errorMessage) {
            this.success = success;
            this.timeData = timeData;
            this.errorMessage = errorMessage;
        }

        public static TimeParseResult success(DataValidator.TimeData timeData) {
            return new TimeParseResult(true, timeData, null);
        }

        public static TimeParseResult error(String errorMessage) {
            return new TimeParseResult(false, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public DataValidator.TimeData getTimeData() { return timeData; }
        public String getErrorMessage() { return errorMessage; }
    }
}