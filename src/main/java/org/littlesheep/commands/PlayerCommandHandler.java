package org.littlesheep.commands;

import org.bukkit.entity.Player;
import org.littlesheep.paytofly;
import org.littlesheep.utils.DataValidator;
import org.bukkit.ChatColor;
import org.littlesheep.gui.EnhancedFlightShopGUI;

import java.util.List;

/**
 * 玩家命令处理器
 */
public class PlayerCommandHandler {
    private final paytofly plugin;
    private final String prefix;

    public PlayerCommandHandler(paytofly plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getPrefix();
    }

    /**
     * 处理玩家命令
     */
    public boolean handlePlayerCommand(Player player, String[] args) {
        if (args.length == 0) {
            // 打开增强版GUI主界面
            plugin.getFlightShopGUI().openGUI(player, EnhancedFlightShopGUI.GUIType.MAIN);
            return true;
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "help":
                return handleHelpCommand(player);
            case "time":
            case "check":
                return handleTimeCheckCommand(player);
            case "effect":
            case "effects":
                return handleEffectCommand(player, args);
            case "speed":
                return handleSpeedCommand(player, args);
            case "shop":
            case "gui":
                return handleShopCommand(player, args);
            default:
                return handlePurchaseCommand(player, args);
        }
    }

    /**
     * 处理帮助命令
     */
    private boolean handleHelpCommand(Player player) {
        sendHelpMessage(player);
        return true;
    }

    /**
     * 处理时间查看命令
     */
    private boolean handleTimeCheckCommand(Player player) {
        // 优先检查购买的飞行时间，即使有无限权限也显示购买时间
        Long endTime = plugin.getStorage().getPlayerFlightTime(player.getUniqueId());
        
        if (endTime != null) {
            long now = System.currentTimeMillis();
            if (endTime > now) {
                String expireTime = formatTime(endTime);
                String remaining = formatDuration(endTime - now);
                
                // 如果有无限权限，在消息中说明这是购买的时间
                if (player.hasPermission("paytofly.infinite")) {
                    player.sendMessage(prefix + plugin.getLang().getMessage("time-check-purchased-with-infinite",
                        "{time}", expireTime,
                        "{remaining}", remaining));
                } else {
                    player.sendMessage(prefix + plugin.getLang().getMessage("time-check-remaining",
                        "{time}", expireTime,
                        "{remaining}", remaining));
                }
            } else {
                // 购买时间已过期
                player.sendMessage(prefix + plugin.getLang().getMessage("time-check-expired"));
                plugin.getStorage().removePlayerFlightTime(player.getUniqueId());
                
                // 过期后检查是否有无限权限
                if (player.hasPermission("paytofly.infinite")) {
                    player.sendMessage(prefix + plugin.getLang().getMessage("time-check-infinite"));
                }
            }
            return true;
        }

        // 没有购买时间，检查是否有无限权限
        if (player.hasPermission("paytofly.infinite")) {
            player.sendMessage(prefix + plugin.getLang().getMessage("time-check-infinite"));
            return true;
        }

        // 既没有购买时间也没有无限权限
        player.sendMessage(prefix + plugin.getLang().getMessage("time-check-no-permission"));
        return true;
    }

    /**
     * 处理购买命令
     */
    private boolean handlePurchaseCommand(Player player, String[] args) {
        if (!player.hasPermission("paytofly.use")) {
            player.sendMessage(prefix + plugin.getLang().getMessage("no-permission"));
            return true;
        }

        // 解析时间格式
        String timeArg = args[0];
        CommandHandler.TimeParseResult parseResult = CommandHandler.parseTimeArgument(timeArg);
        
        if (!parseResult.isSuccess()) {
            player.sendMessage(prefix + "§c" + parseResult.getErrorMessage());
            return true;
        }

        DataValidator.TimeData timeData = parseResult.getTimeData();
        int amount = timeData.amount;
        String unit = timeData.unit;
        long durationMillis = timeData.milliseconds;

        // 获取价格和限制配置
        PriceConfig priceConfig = getPriceConfig(unit, amount);
        if (priceConfig == null) {
            player.sendMessage(prefix + plugin.getLang().getMessage("invalid-time-format"));
            return true;
        }

        // 检查时间限制
        if (amount < priceConfig.minLimit) {
            player.sendMessage(prefix + plugin.getLang().getMessage("time-too-small", 
                "{min}", String.valueOf(priceConfig.minLimit),
                "{unit}", priceConfig.unitName));
            return true;
        }
        if (amount > priceConfig.maxLimit) {
            player.sendMessage(prefix + plugin.getLang().getMessage("time-too-large", 
                "{max}", String.valueOf(priceConfig.maxLimit),
                "{unit}", priceConfig.unitName));
            return true;
        }

        // 检查经济系统
        if (!plugin.getEconomyManager().isInitialized()) {
            player.sendMessage(prefix + plugin.getLang().getMessage("economy-not-initialized"));
            return true;
        }

        // 计算总价
        double totalCost = priceConfig.costPerUnit * amount;

        // 显示购买详情
        player.sendMessage(prefix + plugin.getLang().getMessage("purchase-details",
            "{duration}", amount + priceConfig.unitName,
            "{amount}", String.format("%.2f", totalCost)));

        // 检查余额并处理购买
        if (plugin.getEconomyManager().getBalance(player) >= totalCost) {
            return processPurchase(player, amount, priceConfig.unitName, durationMillis, totalCost);
        } else {
            player.sendMessage(prefix + plugin.getLang().getMessage("insufficient-money",
                "{amount}", String.format("%.2f", totalCost)));
            return true;
        }
    }

    /**
     * 处理购买逻辑
     */
    private boolean processPurchase(Player player, int amount, String unitName, long durationMillis, double totalCost) {
        // 检查是否已经在飞行
        if (plugin.getFlyingPlayers().containsKey(player.getUniqueId())) {
            long remainingTime = plugin.getFlyingPlayers().get(player.getUniqueId()) - System.currentTimeMillis();
            if (remainingTime > 0) {
                player.sendMessage(prefix + plugin.getLang().getMessage("already-flying",
                    "{time}", formatTime(remainingTime)));
                return true;
            }
        }

        // 扣款
        plugin.getEconomyManager().withdraw(player, totalCost);
        
        // 设置飞行
        player.setAllowFlight(true);
        player.setFlying(true);

        long endTime = System.currentTimeMillis() + durationMillis;
        plugin.getFlyingPlayers().put(player.getUniqueId(), endTime);
        plugin.getStorage().setPlayerFlightTime(player.getUniqueId(), endTime);

        // 同步MHDF-Tools飞行权限
        syncMHDFToolsFlight(player, endTime);

        // 启动倒计时
        plugin.getCountdownManager().startCountdown(player, endTime);

        player.sendMessage(prefix + plugin.getLang().getMessage("purchase-success",
            "{amount}", String.format("%.2f", totalCost),
            "{duration}", amount + unitName));

        return true;
    }

    /**
     * 获取价格配置
     */
    private PriceConfig getPriceConfig(String unit, int amount) {
        double costPerUnit;
        String unitName;
        int minLimit;
        int maxLimit;

        switch (unit) {
            case "m":
                costPerUnit = plugin.getConfig().getDouble("fly-cost.minute");
                unitName = plugin.getLang().getMessage("time-format.minute");
                minLimit = plugin.getConfig().getInt("time-limits.minute.min", 5);
                maxLimit = plugin.getConfig().getInt("time-limits.minute.max", 60);
                break;
            case "h":
                costPerUnit = plugin.getConfig().getDouble("fly-cost.hour");
                unitName = plugin.getLang().getMessage("time-format.hour");
                minLimit = plugin.getConfig().getInt("time-limits.hour.min", 1);
                maxLimit = plugin.getConfig().getInt("time-limits.hour.max", 24);
                break;
            case "d":
                costPerUnit = plugin.getConfig().getDouble("fly-cost.day");
                unitName = plugin.getLang().getMessage("time-format.day");
                minLimit = plugin.getConfig().getInt("time-limits.day.min", 1);
                maxLimit = plugin.getConfig().getInt("time-limits.day.max", 7);
                break;
            case "w":
                costPerUnit = plugin.getConfig().getDouble("fly-cost.week");
                unitName = plugin.getLang().getMessage("time-format.week");
                minLimit = plugin.getConfig().getInt("time-limits.week.min", 1);
                maxLimit = plugin.getConfig().getInt("time-limits.week.max", 4);
                break;
            case "mo":
                costPerUnit = plugin.getConfig().getDouble("fly-cost.month");
                unitName = plugin.getLang().getMessage("time-format.month");
                minLimit = plugin.getConfig().getInt("time-limits.month.min", 1);
                maxLimit = plugin.getConfig().getInt("time-limits.month.max", 12);
                break;
            default:
                return null;
        }

        return new PriceConfig(costPerUnit, unitName, minLimit, maxLimit);
    }

    /**
     * 发送帮助信息
     */
    private void sendHelpMessage(Player player) {
        String helpTitle = plugin.getLang().getMessage("help-title");
        String helpFooter = plugin.getLang().getMessage("help-footer");
        List<String> helpCommands = plugin.getLang().getStringList("help-commands");

        player.sendMessage(helpTitle);
        for (String line : helpCommands) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
        player.sendMessage(helpFooter);
    }

    /**
     * 格式化时间戳
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + plugin.getLang().getMessage("time-day") + hours % 24 + plugin.getLang().getMessage("time-hour");
        } else if (hours > 0) {
            return hours + plugin.getLang().getMessage("time-hour") + minutes % 60 + plugin.getLang().getMessage("time-minute");
        } else if (minutes > 0) {
            return minutes + plugin.getLang().getMessage("time-minute");
        } else {
            return seconds + plugin.getLang().getMessage("time-second");
        }
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + plugin.getLang().getMessage("time-day") + hours % 24 + plugin.getLang().getMessage("time-hour") + minutes % 60 + plugin.getLang().getMessage("time-minute");
        } else if (hours > 0) {
            return hours + plugin.getLang().getMessage("time-hour") + minutes % 60 + plugin.getLang().getMessage("time-minute");
        } else if (minutes > 0) {
            return minutes + plugin.getLang().getMessage("time-minute");
        } else {
            return seconds + plugin.getLang().getMessage("time-second");
        }
    }

    /**
     * 同步MHDF-Tools飞行权限
     */
    private void syncMHDFToolsFlight(Player player, long endTime) {
        if (plugin.getServer().getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                String command = "fly " + player.getName() + " true";
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                plugin.getLogger().info(plugin.getLang().getMessage("mhdf-sync-success", "{player}", player.getName()));
            } catch (Exception e) {
                plugin.getLogger().warning(plugin.getLang().getMessage("mhdf-sync-failed", "{error}", e.getMessage()));
            }
        }
    }

    /**
     * 处理特效命令
     */
    private boolean handleEffectCommand(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("flight-effects.enabled", true)) {
            player.sendMessage(prefix + plugin.getLang().getMessage("effects-disabled"));
            return true;
        }

        if (args.length == 1) {
            // 显示特效信息
            showEffectInfo(player);
            return true;
        }

        String action = args[1].toLowerCase();
        
        switch (action) {
            case "list":
                showAvailableEffects(player);
                break;
            case "set":
                if (args.length < 3) {
                    player.sendMessage(prefix + plugin.getLang().getMessage("effect-usage-set"));
                    return true;
                }
                setPlayerEffect(player, args[2]);
                break;
            case "buy":
            case "purchase":
                if (args.length < 3) {
                    player.sendMessage(prefix + plugin.getLang().getMessage("effect-usage-buy"));
                    return true;
                }
                purchasePlayerEffect(player, args[2]);
                break;
            case "off":
            case "disable":
                disablePlayerEffect(player);
                break;
            case "info":
                showEffectInfo(player);
                break;
            default:
                player.sendMessage(prefix + plugin.getLang().getMessage("effect-usage"));
                break;
        }
        
        return true;
    }

    /**
     * 处理速度命令
     */
    private boolean handleSpeedCommand(Player player, String[] args) {
        if (!plugin.getConfig().getBoolean("flight-speed.enabled", true)) {
            player.sendMessage(prefix + plugin.getLang().getMessage("speed-disabled"));
            return true;
        }

        if (args.length == 1) {
            // 显示速度信息
            showSpeedInfo(player);
            return true;
        }

        String action = args[1].toLowerCase();
        
        switch (action) {
            case "list":
                showAvailableSpeeds(player);
                break;
            case "set":
                if (args.length < 3) {
                    player.sendMessage(prefix + plugin.getLang().getMessage("speed-usage-set"));
                    return true;
                }
                setPlayerSpeed(player, args[2]);
                break;
            case "buy":
            case "purchase":
                if (args.length < 3) {
                    player.sendMessage(prefix + plugin.getLang().getMessage("speed-usage-buy"));
                    return true;
                }
                purchasePlayerSpeed(player, args[2]);
                break;
            case "up":
            case "upgrade":
                upgradePlayerSpeed(player);
                break;
            case "down":
            case "downgrade":
                downgradePlayerSpeed(player);
                break;
            case "reset":
                resetPlayerSpeed(player);
                break;
            case "info":
                showSpeedInfo(player);
                break;
            default:
                player.sendMessage(prefix + plugin.getLang().getMessage("speed-usage"));
                break;
        }
        
        return true;
    }

    /**
     * 显示特效信息
     */
    private void showEffectInfo(Player player) {
        var currentEffect = plugin.getEffectManager().getPlayerEffect(player);
        var availableEffects = plugin.getEffectManager().getAvailableEffects(player);
        
        player.sendMessage(prefix + plugin.getLang().getMessage("effect-current", 
            "{effect}", currentEffect.getName()));
        
        StringBuilder effects = new StringBuilder();
        for (int i = 0; i < availableEffects.size(); i++) {
            effects.append(availableEffects.get(i).getName());
            if (i < availableEffects.size() - 1) {
                effects.append(", ");
            }
        }
        
        player.sendMessage(prefix + plugin.getLang().getMessage("effect-available", 
            "{effects}", effects.toString()));
    }

    /**
     * 显示可用特效列表
     */
    private void showAvailableEffects(Player player) {
        var availableEffects = plugin.getEffectManager().getAvailableEffects(player);
        
        player.sendMessage(prefix + plugin.getLang().getMessage("effect-list-header"));
        for (var effect : availableEffects) {
            player.sendMessage("§7- §b" + effect.getName() + " §7(等级 " + effect.getLevel() + ")");
        }
    }

    /**
     * 设置玩家特效
     */
    private void setPlayerEffect(Player player, String effectName) {
        var effectType = org.littlesheep.effects.FlightEffectManager.FlightEffectType.fromString(effectName);
        
        if (!plugin.getEffectManager().hasEffectPermission(player, effectType)) {
            player.sendMessage(prefix + plugin.getLang().getMessage("effect-no-permission"));
            return;
        }
        
        if (player.isFlying()) {
            plugin.getEffectManager().startFlightEffect(player, effectType);
        } else {
            plugin.getEffectManager().setPlayerEffect(player, effectType);
            player.sendMessage(prefix + plugin.getLang().getMessage("effect-set-inactive", 
                "{effect}", effectType.getName()));
        }
    }

    /**
     * 禁用玩家特效
     */
    private void disablePlayerEffect(Player player) {
        plugin.getEffectManager().stopFlightEffect(player);
        plugin.getEffectManager().setPlayerEffect(player, 
            org.littlesheep.effects.FlightEffectManager.FlightEffectType.NONE);
        player.sendMessage(prefix + plugin.getLang().getMessage("effect-disabled"));
    }

    /**
     * 显示速度信息
     */
    private void showSpeedInfo(Player player) {
        String speedInfo = plugin.getSpeedManager().getSpeedInfo(player);
        player.sendMessage(prefix + speedInfo);
    }

    /**
     * 显示可用速度列表
     */
    private void showAvailableSpeeds(Player player) {
        var availableSpeeds = plugin.getSpeedManager().getAvailableSpeeds(player);
        
        player.sendMessage(prefix + plugin.getLang().getMessage("speed-list-header"));
        for (var speed : availableSpeeds) {
            String status = speed == plugin.getSpeedManager().getPlayerSpeed(player) ? "§a当前" : "§7可用";
            player.sendMessage("§7- " + speed.getDisplayName() + " §7(" + status + "§7)");
        }
    }

    /**
     * 设置玩家速度
     */
    private void setPlayerSpeed(Player player, String speedName) {
        var speedLevel = org.littlesheep.speed.FlightSpeedManager.FlightSpeedLevel.fromString(speedName);
        plugin.getSpeedManager().setFlightSpeed(player, speedLevel);
    }

    /**
     * 升级玩家速度
     */
    private void upgradePlayerSpeed(Player player) {
        plugin.getSpeedManager().upgradeSpeed(player);
    }

    /**
     * 降级玩家速度
     */
    private void downgradePlayerSpeed(Player player) {
        plugin.getSpeedManager().downgradeSpeed(player);
    }

    /**
     * 重置玩家速度
     */
    private void resetPlayerSpeed(Player player) {
        plugin.getSpeedManager().resetPlayerSpeed(player);
    }

    /**
     * 购买特效
     */
    private void purchasePlayerEffect(Player player, String effectName) {
        var effectType = org.littlesheep.effects.FlightEffectManager.FlightEffectType.fromString(effectName);
        
        // 检查是否已拥有
        if (plugin.getEffectManager().hasEffectPermission(player, effectType)) {
            player.sendMessage(prefix + plugin.getLang().getMessage("effect-already-owned"));
            // 如果已拥有，直接切换到这个特效
            setPlayerEffect(player, effectName);
            return;
        }
        
        // 获取价格
        double price = plugin.getEffectManager().getEffectPrice(effectType);
        if (price <= 0) {
            player.sendMessage(prefix + plugin.getLang().getMessage("effect-not-for-sale"));
            return;
        }
        
        // 尝试购买
        if (plugin.getEffectManager().purchaseEffect(player, effectType)) {
            player.sendMessage(prefix + plugin.getLang().getMessage("effect-purchase-success", 
                "{effect}", effectType.getName(),
                "{price}", String.format("%.2f", price)));
            
            // 购买成功后自动激活特效
            setPlayerEffect(player, effectName);
            player.sendMessage(prefix + "&a特效已自动激活！");
        } else {
            player.sendMessage(prefix + plugin.getLang().getMessage("effect-purchase-failed"));
        }
    }

    /**
     * 购买速度
     */
    private void purchasePlayerSpeed(Player player, String speedName) {
        var speedLevel = org.littlesheep.speed.FlightSpeedManager.FlightSpeedLevel.fromString(speedName);
        
        // 检查是否已拥有
        if (plugin.getSpeedManager().hasSpeedPermission(player, speedLevel)) {
            player.sendMessage(prefix + plugin.getLang().getMessage("speed-already-owned"));
            // 如果已拥有，直接切换到这个速度
            setPlayerSpeed(player, speedName);
            return;
        }
        
        // 获取价格
        double price = plugin.getSpeedManager().getSpeedPrice(speedLevel);
        if (price <= 0) {
            player.sendMessage(prefix + plugin.getLang().getMessage("speed-not-for-sale"));
            return;
        }
        
        // 尝试购买
        if (plugin.getSpeedManager().purchaseSpeed(player, speedLevel)) {
            player.sendMessage(prefix + plugin.getLang().getMessage("speed-purchase-success", 
                "{speed}", speedLevel.getDisplayName(),
                "{price}", String.format("%.2f", price)));
            
            // 购买成功后自动激活速度
            setPlayerSpeed(player, speedName);
            player.sendMessage(prefix + "&a速度已自动激活！");
        } else {
            player.sendMessage(prefix + plugin.getLang().getMessage("speed-purchase-failed"));
        }
    }

    /**
     * 处理商店/GUI命令
     */
    private boolean handleShopCommand(Player player, String[] args) {
        if (args.length == 1) {
            // 打开主商店
            plugin.getFlightShopGUI().openGUI(player, EnhancedFlightShopGUI.GUIType.MAIN);
            return true;
        } else if (args.length == 2) {
            String shopType = args[1].toLowerCase();
            try {
                switch (shopType) {
                    case "main":
                    case "time":
                        plugin.getFlightShopGUI().openGUI(player, EnhancedFlightShopGUI.GUIType.MAIN);
                        break;
                    case "effect":
                    case "effects":
                        plugin.getFlightShopGUI().openGUI(player, EnhancedFlightShopGUI.GUIType.EFFECTS);
                        break;
                    case "speed":
                    case "speeds":
                        plugin.getFlightShopGUI().openGUI(player, EnhancedFlightShopGUI.GUIType.SPEEDS);
                        break;
                    default:
                        player.sendMessage(prefix + "&c未知的商店类型！可用类型: main, effects, speeds");
                        break;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("打开GUI时出错: " + e.getMessage());
                player.sendMessage(prefix + "&c打开商店时出错，请联系管理员");
                e.printStackTrace();
            }
            return true;
        } else {
            player.sendMessage(prefix + "&c用法: /fly shop [main|effects|speeds]");
            return true;
        }
    }

    /**
     * 价格配置类
     */
    private static class PriceConfig {
        final double costPerUnit;
        final String unitName;
        final int minLimit;
        final int maxLimit;

        PriceConfig(double costPerUnit, String unitName, int minLimit, int maxLimit) {
            this.costPerUnit = costPerUnit;
            this.unitName = unitName;
            this.minLimit = minLimit;
            this.maxLimit = maxLimit;
        }
    }
}