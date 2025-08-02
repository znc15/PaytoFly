package org.littlesheep;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.littlesheep.data.Storage;
import org.littlesheep.data.StorageFactory;
import org.littlesheep.economy.EconomyAdapter;
import org.littlesheep.economy.EconomyManager;
import org.littlesheep.listeners.PlayerListener;
import org.littlesheep.placeholders.FlightExpansion;
import org.littlesheep.utils.CountdownManager;
import org.littlesheep.utils.LanguageManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.littlesheep.listeners.PlayerJoinListener;
import org.littlesheep.gui.FlightShopGUI;
import org.littlesheep.listeners.GUIListener;
import org.littlesheep.utils.VersionManager;
import org.littlesheep.utils.UpdateChecker;
import org.littlesheep.utils.TimeManager;
import org.littlesheep.utils.ConfigChecker;
import org.littlesheep.listeners.WorldChangeListener;
import org.littlesheep.commands.FlyCommandTabCompleter;
import org.littlesheep.utils.CustomTimeManager;
import org.bukkit.ChatColor;
import me.clip.placeholderapi.PlaceholderAPI;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class paytofly extends JavaPlugin {
    private Economy econ;
    private EconomyManager economyManager;
    private Map<UUID, Long> flyingPlayers = new HashMap<>();
    private FileConfiguration config;
    private String prefix;
    private Storage storage;
    private LanguageManager lang;
    private CountdownManager countdownManager;
    private static final int BSTATS_ID = 24712;
    private FlightShopGUI shopGUI;
    private VersionManager versionManager;
    private UpdateChecker updateChecker;
    private TimeManager timeManager;
    private CustomTimeManager customTimeManager;
    private FileConfiguration messageConfig;
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        // 初始化配置检查器
        ConfigChecker configChecker = new ConfigChecker(this);
        
        // 检查配置文件
        if (!configChecker.checkConfig()) {
            getLogger().severe(lang.getMessage("config-failed"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化版本管理器
        versionManager = new VersionManager(this);
        
        // 初始化语言管理器
        lang = new LanguageManager(this, getConfig().getString("language", "zh_CN"));
        
        getLogger().info(lang.getMessage("plugin-loading"));
        
        config = getConfig();
        prefix = config.getString("messages.prefix", "&7[&bPayToFly&7] ").replace("&", "§");
        
        // 初始化经济系统
        getLogger().info(lang.getMessage("economy-init-detail"));
        economyManager = new EconomyManager(this);
        if (economyManager.isInitialized()) {
            getLogger().info(lang.getMessage("economy-success-detail", "{name}", economyManager.getEconomyName()));
            econ = economyManager.getCurrentEconomy() != null ? 
                   getServer().getServicesManager().getRegistration(Economy.class) != null ? 
                   getServer().getServicesManager().getRegistration(Economy.class).getProvider() : null : null;
        } else {
            getLogger().severe(lang.getMessage("economy-failed-detail"));
            getLogger().severe(lang.getMessage("economy-failed-hint"));
        }
        
        // 初始化存储系统
        getLogger().info(lang.getMessage("storage-init-detail"));
        try {
            storage = StorageFactory.createStorage(getConfig().getString("storage.type", "JSON"), this);
            storage.init();
            getLogger().info(lang.getMessage("storage-success-detail", 
                "{type}", getConfig().getString("storage.type", "JSON")));
        } catch (Exception e) {
            getLogger().severe(lang.getMessage("storage-failed-detail"));
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化倒计时管理器
        countdownManager = new CountdownManager(this, lang);
        
        // 从存储加载飞行数据
        flyingPlayers = storage.getAllPlayerData();
        for (Map.Entry<UUID, Long> entry : flyingPlayers.entrySet()) {
            Player player = getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                long endTime = entry.getValue();
                if (endTime > System.currentTimeMillis()) {
                    countdownManager.startCountdown(player, endTime);
                }
            }
        }
        getLogger().info(lang.getMessage("data-loaded-detail", "{count}", String.valueOf(flyingPlayers.size())));
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this, storage), this);
        
        // 注册命令和补全器
        getCommand("fly").setExecutor(this);
        getCommand("fly").setTabCompleter(new FlyCommandTabCompleter());
        
        // 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        // 注册 PAPI 扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FlightExpansion(this).register();
            getLogger().info(lang.getMessage("papi-hooked"));
        }
        
        // 初始化 bStats
        if (getConfig().getBoolean("metrics.enabled", true)) {
            setupMetrics();
            getLogger().info(lang.getMessage("metrics-enabled"));
        } else {
            getLogger().info(lang.getMessage("metrics-disabled"));
        }
        
        // 初始化GUI
        shopGUI = new FlightShopGUI(this);
        getServer().getPluginManager().registerEvents(new GUIListener(shopGUI), this);
        
        // 初始化并运行更新检查器
        updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates();
        
        this.timeManager = new TimeManager(this);
        
        // 注册世界切换监听器
        getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);
        
        customTimeManager = new CustomTimeManager(this);
        getServer().getPluginManager().registerEvents(customTimeManager, this);
        
        getLogger().info(lang.getMessage("plugin-enabled-detail"));
        getLogger().info(lang.getMessage("plugin-splash"));
        
        loadMessageConfig();
        
        // 显示插件启动艺术字和说明
        getLogger().info("\n" +
                "  _____              _________    ______  _          \n" +
                " |  __ \\            |__   __|   |  ____|| |         \n" +
                " | |__) |__ _  _   _    | | ___  | |__   | | _   _   \n" +
                " |  ___// _` || | | |   | |/ _ \\|  __|  | || | | |  \n" +
                " | |   | (_| || |_| |   | |  (_) | |     | || |_| |  \n" +
                " |_|    \\__,_| \\__,   |_|\\___/|_|     |_|\\__, |  \n" +
                "                __/ |                        __/  |   \n" +
                "               |___/                        |___ /    v");
        
        getLogger().info(lang.getMessage("plugin-info-header"));
        getLogger().info(lang.getMessage("plugin-info-author"));
        getLogger().info(lang.getMessage("plugin-info-version", "{version}", getDescription().getVersion()));
        getLogger().info(lang.getMessage("plugin-info-storage", "{type}", getConfig().getString("storage.type", "JSON")));
        getLogger().info(lang.getMessage("plugin-info-language", "{language}", getConfig().getString("language", "zh_CN")));
        getLogger().info(lang.getMessage("plugin-info-footer"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && !isAdminCommand(args)) {
            sender.sendMessage(prefix + lang.getMessage("command-player-only"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("fly")) {
            // 控制台执行管理命令
            if (!(sender instanceof Player)) {
                return handleConsoleCommand(sender, args);
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                shopGUI.openGUI(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("help")) {
                sendHelpMessage(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("time") || args[0].equalsIgnoreCase("check")) {
                // 首先检查是否有无限飞行权限
                if (player.hasPermission("paytofly.infinite")) {
                    player.sendMessage(prefix + lang.getMessage("time-check-infinite"));
                    return true;
                }
                
                Long endTime = storage.getPlayerFlightTime(player.getUniqueId());
                if (endTime == null) {
                    player.sendMessage(prefix + lang.getMessage("time-check-no-permission"));
                    return true;
                }

                long now = System.currentTimeMillis();
                if (endTime > now) {
                    String expireTime = formatTime(endTime);
                    String remaining = formatDuration(endTime - now);
                    player.sendMessage(prefix + lang.getMessage("time-check-remaining",
                        "{time}", expireTime,
                        "{remaining}", remaining));
                } else {
                    player.sendMessage(prefix + lang.getMessage("time-check-expired"));
                    storage.removePlayerFlightTime(player.getUniqueId());
                }
                return true;
            }

            if (!player.hasPermission("paytofly.use")) {
                player.sendMessage(prefix + lang.getMessage("no-permission"));
                return true;
            }

            // 管理员命令
            if (args[0].equalsIgnoreCase("disable") && player.hasPermission("paytofly.admin")) {
                if (args.length < 2) {
                    player.sendMessage(prefix + lang.getMessage("cmd-disable-usage"));
                    return true;
                }
                
                Player target = getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(prefix + lang.getMessage("player-not-found", "{player}", args[1]));
                    return true;
                }
                
                // 检查目标玩家是否有飞行权限
                Long endTime = storage.getPlayerFlightTime(target.getUniqueId());
                boolean hadFlight = false;
                
                if (endTime != null && endTime > System.currentTimeMillis()) {
                    hadFlight = true;
                }
                
                // 禁用飞行
                target.setAllowFlight(false);
                target.setFlying(false);
                storage.removePlayerFlightTime(target.getUniqueId());
                flyingPlayers.remove(target.getUniqueId());
                
                // 同步取消MHDF-Tools飞行权限
                syncMHDFToolsDisableFlight(target);
                
                // 取消倒计时
                countdownManager.cancelCountdown(target);
                
                // 发送消息
                String disableMessage = lang.getMessage("flight-disabled-by-admin");
                target.sendMessage(prefix + disableMessage);
                
                if (hadFlight) {
                    player.sendMessage(prefix + lang.getMessage("admin-disabled-flight", "{player}", target.getName()));
                } else {
                    player.sendMessage(prefix + lang.getMessage("cmd-player-had-no-permission", "{player}", target.getName()));
                }
                
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("paytofly.admin")) {
                    player.sendMessage(prefix + lang.getMessage("no-permission"));
                    return true;
                }
                // 重载配置文件
                reloadConfig();
                config = getConfig();
                
                // 重载语言文件
                lang = new LanguageManager(this, getConfig().getString("language", "zh_CN"));
                
                // 重新初始化GUI
                shopGUI.reloadConfig();
                
                player.sendMessage(prefix + lang.getMessage("config-reloaded"));
                return true;
            }

            if (args[0].equalsIgnoreCase("test")) {
                if (!player.hasPermission("paytofly.admin")) {
                    player.sendMessage(prefix + lang.getMessage("no-permission"));
                    return true;
                }
                
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                    player.sendMessage(prefix + lang.getMessage("papi-not-found"));
                    return true;
                }
                
                player.sendMessage(prefix + lang.getMessage("papi-test-title"));
                player.sendMessage(prefix + lang.getMessage("papi-test-remaining", 
                    "{value}", PlaceholderAPI.setPlaceholders(player, "%paytofly_remaining%")));
                player.sendMessage(prefix + lang.getMessage("papi-test-status", 
                    "{value}", PlaceholderAPI.setPlaceholders(player, "%paytofly_status%")));
                player.sendMessage(prefix + lang.getMessage("papi-test-expiretime", 
                    "{value}", PlaceholderAPI.setPlaceholders(player, "%paytofly_expiretime%")));
                player.sendMessage(prefix + lang.getMessage("papi-test-mode", 
                    "{value}", PlaceholderAPI.setPlaceholders(player, "%paytofly_mode%")));
                
                return true;
            }

            if (args[0].equalsIgnoreCase("bypass")) {
                if (!player.hasPermission("paytofly.admin")) {
                    player.sendMessage(prefix + lang.getMessage("no-permission"));
                    return true;
                }
                
                if (args.length < 2) {
                    player.sendMessage(prefix + lang.getMessage("bypass.usage"));
                    return true;
                }
                
                Player target = getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(prefix + lang.getMessage("player-not-found", "{player}", args[1]));
                    return true;
                }
                
                if (args.length >= 3 && args[2].equalsIgnoreCase("remove")) {
                    // 移除绕过权限
                    getServer().dispatchCommand(getServer().getConsoleSender(), 
                        "lp user " + target.getName() + " permission unset paytofly.bypass");
                    player.sendMessage(prefix + lang.getMessage("bypass.remove-success", "{player}", target.getName()));
                    target.sendMessage(prefix + lang.getMessage("bypass.target-remove"));
                } else {
                    // 添加绕过权限
                    getServer().dispatchCommand(getServer().getConsoleSender(), 
                        "lp user " + target.getName() + " permission set paytofly.bypass true");
                    player.sendMessage(prefix + lang.getMessage("bypass.add-success", "{player}", target.getName()));
                    target.sendMessage(prefix + lang.getMessage("bypass.target-add"));
                }
                return true;
            }

            // 添加给予飞行权限命令
            if (args[0].equalsIgnoreCase("give")) {
                if (!player.hasPermission("paytofly.admin")) {
                    player.sendMessage(prefix + lang.getMessage("no-permission"));
                    return true;
                }
                
                if (args.length < 3) {
                    player.sendMessage(prefix + lang.getMessage("cmd-give-usage"));
                    return true;
                }
                
                Player target = getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(prefix + lang.getMessage("player-not-found", "{player}", args[1]));
                    return true;
                }
                
                // 解析时间格式
                String timeArg = args[2];
                if (!timeArg.matches("\\d+[mhdw]") && !timeArg.matches("\\d+mo")) {
                    player.sendMessage(prefix + lang.getMessage("invalid-time-format"));
                    return true;
                }

                int amount;
                String unit;
                
                // 检查是否是月份格式
                if (timeArg.endsWith("mo")) {
                    amount = Integer.parseInt(timeArg.substring(0, timeArg.length() - 2));
                    unit = "mo";
                } else {
                    amount = Integer.parseInt(timeArg.substring(0, timeArg.length() - 1));
                    unit = timeArg.substring(timeArg.length() - 1);
                }
                
                long durationMillis;
                String unitName;

                switch (unit) {
                    case "m":
                        durationMillis = amount * 60 * 1000L;
                        unitName = lang.getMessage("time-format.minute");
                        break;
                    case "h":
                        durationMillis = amount * 60 * 60 * 1000L;
                        unitName = lang.getMessage("time-format.hour");
                        break;
                    case "d":
                        durationMillis = amount * 24 * 60 * 60 * 1000L;
                        unitName = lang.getMessage("time-format.day");
                        break;
                    case "w":
                        durationMillis = amount * 7 * 24 * 60 * 60 * 1000L;
                        unitName = lang.getMessage("time-format.week");
                        break;
                    case "mo":
                        durationMillis = amount * 30L * 24 * 60 * 60 * 1000L;
                        unitName = lang.getMessage("time-format.month");
                        break;
                    default:
                        player.sendMessage(prefix + lang.getMessage("invalid-time-format"));
                        return true;
                }

                // 检查是否已经在飞行，如果是，则增加时间
                long endTime;
                if (flyingPlayers.containsKey(target.getUniqueId())) {
                    long existingEndTime = flyingPlayers.get(target.getUniqueId());
                    if (existingEndTime > System.currentTimeMillis()) {
                        endTime = existingEndTime + durationMillis;
                        player.sendMessage(prefix + lang.getMessage("cmd-add-time", 
                            "{player}", target.getName(),
                            "{amount}", String.valueOf(amount),
                            "{unit}", unitName));
                    } else {
                        endTime = System.currentTimeMillis() + durationMillis;
                        player.sendMessage(prefix + lang.getMessage("cmd-give-time", 
                            "{player}", target.getName(),
                            "{amount}", String.valueOf(amount),
                            "{unit}", unitName));
                    }
                } else {
                    endTime = System.currentTimeMillis() + durationMillis;
                    player.sendMessage(prefix + lang.getMessage("cmd-give-time", 
                        "{player}", target.getName(),
                        "{amount}", String.valueOf(amount),
                        "{unit}", unitName));
                }
                
                // 设置飞行权限
                target.setAllowFlight(true);
                target.setFlying(true);
                
                flyingPlayers.put(target.getUniqueId(), endTime);
                storage.setPlayerFlightTime(target.getUniqueId(), endTime);
                
                // 同步MHDF-Tools飞行权限
                syncMHDFToolsFlight(target, endTime);
                
                // 启动倒计时
                countdownManager.startCountdown(target, endTime);
                
                // 通知玩家
                target.sendMessage(prefix + lang.getMessage("cmd-given-time", 
                    "{amount}", String.valueOf(amount),
                    "{unit}", unitName));
                
                return true;
            }

            // 解析时间格式
            String timeArg = args[0];
            if (!timeArg.matches("\\d+[mhdw]") && !timeArg.matches("\\d+mo")) {
                player.sendMessage(prefix + lang.getMessage("invalid-time-format"));
                return true;
            }

            int amount;
            String unit;
            
            // 检查是否是月份格式
            if (timeArg.endsWith("mo")) {
                amount = Integer.parseInt(timeArg.substring(0, timeArg.length() - 2));
                unit = "mo";
            } else {
                amount = Integer.parseInt(timeArg.substring(0, timeArg.length() - 1));
                unit = timeArg.substring(timeArg.length() - 1);
            }
            
            long durationMillis;
            double costPerUnit;
            String unitName;
            int minLimit;
            int maxLimit;

            switch (unit) {
                case "m":
                    durationMillis = amount * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.minute");
                    unitName = lang.getMessage("time-format.minute");
                    minLimit = config.getInt("time-limits.minute.min", 5);
                    maxLimit = config.getInt("time-limits.minute.max", 60);
                    break;
                case "h":
                    durationMillis = amount * 60 * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.hour");
                    unitName = lang.getMessage("time-format.hour");
                    minLimit = config.getInt("time-limits.hour.min", 1);
                    maxLimit = config.getInt("time-limits.hour.max", 24);
                    break;
                case "d":
                    durationMillis = amount * 24 * 60 * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.day");
                    unitName = lang.getMessage("time-format.day");
                    minLimit = config.getInt("time-limits.day.min", 1);
                    maxLimit = config.getInt("time-limits.day.max", 7);
                    break;
                case "w":
                    durationMillis = amount * 7 * 24 * 60 * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.week");
                    unitName = lang.getMessage("time-format.week");
                    minLimit = config.getInt("time-limits.week.min", 1);
                    maxLimit = config.getInt("time-limits.week.max", 4);
                    break;
                case "mo":
                    durationMillis = amount * 30L * 24 * 60 * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.month");
                    unitName = lang.getMessage("time-format.month");
                    minLimit = config.getInt("time-limits.month.min", 1);
                    maxLimit = config.getInt("time-limits.month.max", 12);
                    break;
                default:
                    player.sendMessage(prefix + lang.getMessage("invalid-time-format"));
                    return true;
            }

            // 检查时间限制
            if (amount < minLimit) {
                player.sendMessage(prefix + lang.getMessage("time-too-small", 
                    "{min}", String.valueOf(minLimit),
                    "{unit}", unitName));
                return true;
            }
            if (amount > maxLimit) {
                player.sendMessage(prefix + lang.getMessage("time-too-large", 
                    "{max}", String.valueOf(maxLimit),
                    "{unit}", unitName));
                return true;
            }

            // 检查经济系统
            if (econ == null) {
                if (!economyManager.isInitialized()) {
                    player.sendMessage(prefix + lang.getMessage("economy-not-initialized"));
                    getLogger().severe(lang.getMessage("economy-not-initialized"));
                    return true;
                }
            }

            // 计算总价
            double totalCost = costPerUnit * amount;

            // 显示购买详情
            player.sendMessage(prefix + lang.getMessage("purchase-details",
                "{duration}", amount + unitName,
                "{amount}", String.format("%.2f", totalCost)));

            // 检查余额并处理购买
            if (economyManager.getBalance(player) >= totalCost) {
                // 检查是否已经在飞行
                if (flyingPlayers.containsKey(player.getUniqueId())) {
                    long remainingTime = flyingPlayers.get(player.getUniqueId()) - System.currentTimeMillis();
                    if (remainingTime > 0) {
                        player.sendMessage(prefix + lang.getMessage("already-flying",
                            "{time}", formatTime(remainingTime)));
                        return true;
                    }
                }

                economyManager.withdraw(player, totalCost);
                player.setAllowFlight(true);
                player.setFlying(true);
                
                long endTime = System.currentTimeMillis() + durationMillis;
                flyingPlayers.put(player.getUniqueId(), endTime);
                storage.setPlayerFlightTime(player.getUniqueId(), endTime);
                
                // 同步MHDF-Tools飞行权限
                syncMHDFToolsFlight(player, endTime);
                
                // 启动倒计时
                countdownManager.startCountdown(player, endTime);

                player.sendMessage(prefix + lang.getMessage("purchase-success",
                    "{amount}", String.format("%.2f", totalCost),
                    "{duration}", amount + unitName));
            } else {
                player.sendMessage(prefix + lang.getMessage("insufficient-money",
                    "{amount}", String.format("%.2f", totalCost)));
            }
            return true;
        }
        return false;
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + lang.getMessage("time-day") + hours % 24 + lang.getMessage("time-hour");
        } else if (hours > 0) {
            return hours + lang.getMessage("time-hour") + minutes % 60 + lang.getMessage("time-minute");
        } else if (minutes > 0) {
            return minutes + lang.getMessage("time-minute");
        } else {
            return seconds + lang.getMessage("time-second");
        }
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + lang.getMessage("time-day") + hours % 24 + lang.getMessage("time-hour") + minutes % 60 + lang.getMessage("time-minute");
        } else if (hours > 0) {
            return hours + lang.getMessage("time-hour") + minutes % 60 + lang.getMessage("time-minute");
        } else if (minutes > 0) {
            return minutes + lang.getMessage("time-minute");
        } else {
            return seconds + lang.getMessage("time-second");
        }
    }

    private boolean setupEconomy() {
        // 此方法已被EconomyManager替代，但为了兼容性保留
        return economyManager != null && economyManager.isInitialized();
    }

    private void sendHelpMessage(Player player) {
        String helpTitle = lang.getMessage("help-title");
        String helpFooter = lang.getMessage("help-footer");
        List<String> helpCommands = lang.getStringList("help-commands");

        player.sendMessage(helpTitle);
        for (String line : helpCommands) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }

        player.sendMessage(helpFooter);
    }

    @Override
    public void onDisable() {
        if (countdownManager != null) {
            countdownManager.cleanup();
        }
        
        if (storage != null) {
            storage.close();
        }
        
        getLogger().info("PayToFly插件已禁用！");
    }

    // Getter methods for other classes to access
    public Map<UUID, Long> getFlyingPlayers() {
        return flyingPlayers;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLang(String path) {
        String message = langConfig.getString(path);
        if (message == null) {
            getLogger().warning(lang.getMessage("lang-key-missing", "{key}", path));
            message = path;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public CountdownManager getCountdownManager() {
        return countdownManager;
    }

    public Storage getStorage() {
        return storage;
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public CustomTimeManager getCustomTimeManager() {
        return customTimeManager;
    }

    public Economy getEconomy() {
        return econ;
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, BSTATS_ID);

        // 添加自定义图表
        // 存储类型统计
        metrics.addCustomChart(new SimplePie("storage_type", () -> 
            getConfig().getString("storage-type", "JSON")));

        // 语言统计
        metrics.addCustomChart(new SimplePie("language", () ->
            getConfig().getString("language", "zh_CN")));

        // 通知方式统计
        metrics.addCustomChart(new SimplePie("notification_method", () -> {
            boolean bossBar = getConfig().getBoolean("notifications.bossbar.enabled", true);
            boolean chat = getConfig().getBoolean("notifications.chat.enabled", true);
            if (bossBar && chat) return "Both";
            if (bossBar) return "BossBar Only";
            if (chat) return "Chat Only";
            return "None";
        }));

        // 活跃飞行玩家数量
        metrics.addCustomChart(new SingleLineChart("active_flyers", () -> {
            int count = 0;
            long now = System.currentTimeMillis();
            for (Long endTime : flyingPlayers.values()) {
                if (endTime > now) count++;
            }
            return count;
        }));
    }

    public void loadMessageConfig() {
        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String language = getConfig().getString("language", "zh_CN");
        File langFile = new File(langFolder, language + ".yml");
        
        if (!langFile.exists()) {
            try {
                saveResource("lang/" + language + ".yml", false);
            } catch (IllegalArgumentException e) {
                getLogger().warning(lang.getMessage("lang-not-found", "{file}", language + ".yml"));
                getLogger().warning(lang.getMessage("lang-default"));
                saveResource("lang/zh_CN.yml", false);
                langFile = new File(langFolder, "zh_CN.yml");
            }
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }
    
    public FileConfiguration getMessageConfig() {
        return messageConfig;
    }

    public LanguageManager getLang() {
        return this.lang;
    }
    
    /**
     * 检查是否为管理员命令
     * @param args 命令参数
     * @return 如果是管理员命令则返回true
     */
    private boolean isAdminCommand(String[] args) {
        if (args.length == 0) {
            return false;
        }
        
        String cmd = args[0].toLowerCase();
        return cmd.equals("disable") || cmd.equals("reload") || cmd.equals("give") || cmd.equals("bypass");
    }
    
    /**
     * 处理控制台执行的命令
     * @param sender 命令发送者
     * @param args 命令参数
     * @return 命令执行结果
     */
    private boolean handleConsoleCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(prefix + lang.getMessage("console-player-command"));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage(prefix + lang.getMessage("cmd-give-usage"));
                return true;
            }
            
            Player target = getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(prefix + lang.getMessage("player-not-found", "{player}", args[1]));
                return true;
            }
            
            // 解析时间格式
            String timeArg = args[2];
            if (!timeArg.matches("\\d+[mhdw]") && !timeArg.matches("\\d+mo")) {
                sender.sendMessage(prefix + lang.getMessage("invalid-time-format"));
                return true;
            }

            int amount;
            String unit;
            
            // 检查是否是月份格式
            if (timeArg.endsWith("mo")) {
                amount = Integer.parseInt(timeArg.substring(0, timeArg.length() - 2));
                unit = "mo";
            } else {
                amount = Integer.parseInt(timeArg.substring(0, timeArg.length() - 1));
                unit = timeArg.substring(timeArg.length() - 1);
            }
            
            long durationMillis;
            String unitName;

            switch (unit) {
                case "m":
                    durationMillis = amount * 60 * 1000L;
                    unitName = lang.getMessage("time-format.minute");
                    break;
                case "h":
                    durationMillis = amount * 60 * 60 * 1000L;
                    unitName = lang.getMessage("time-format.hour");
                    break;
                case "d":
                    durationMillis = amount * 24 * 60 * 60 * 1000L;
                    unitName = lang.getMessage("time-format.day");
                    break;
                case "w":
                    durationMillis = amount * 7 * 24 * 60 * 60 * 1000L;
                    unitName = lang.getMessage("time-format.week");
                    break;
                case "mo":
                    durationMillis = amount * 30L * 24 * 60 * 60 * 1000L;
                    unitName = lang.getMessage("time-format.month");
                    break;
                default:
                    sender.sendMessage(prefix + lang.getMessage("invalid-time-format"));
                    return true;
            }

            // 检查是否已经在飞行，如果是，则增加时间
            long endTime;
            if (flyingPlayers.containsKey(target.getUniqueId())) {
                long existingEndTime = flyingPlayers.get(target.getUniqueId());
                if (existingEndTime > System.currentTimeMillis()) {
                    endTime = existingEndTime + durationMillis;
                    sender.sendMessage(prefix + lang.getMessage("cmd-add-time", 
                        "{player}", target.getName(),
                        "{amount}", String.valueOf(amount),
                        "{unit}", unitName));
                } else {
                    endTime = System.currentTimeMillis() + durationMillis;
                    sender.sendMessage(prefix + lang.getMessage("cmd-give-time", 
                        "{player}", target.getName(),
                        "{amount}", String.valueOf(amount),
                        "{unit}", unitName));
                }
            } else {
                endTime = System.currentTimeMillis() + durationMillis;
                sender.sendMessage(prefix + lang.getMessage("cmd-give-time", 
                    "{player}", target.getName(),
                    "{amount}", String.valueOf(amount),
                    "{unit}", unitName));
            }
            
            // 设置飞行权限
            target.setAllowFlight(true);
            target.setFlying(true);
            
            flyingPlayers.put(target.getUniqueId(), endTime);
            storage.setPlayerFlightTime(target.getUniqueId(), endTime);
            
            // 同步MHDF-Tools飞行权限
            syncMHDFToolsFlight(target, endTime);
            
            // 启动倒计时
            countdownManager.startCountdown(target, endTime);
            
            // 通知玩家
            target.sendMessage(prefix + lang.getMessage("cmd-given-time", 
                "{amount}", String.valueOf(amount),
                "{unit}", unitName));
            
            return true;
        } else if (args[0].equalsIgnoreCase("disable")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + lang.getMessage("cmd-disable-usage"));
                return true;
            }
            
            Player target = getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(prefix + lang.getMessage("player-not-found", "{player}", args[1]));
                return true;
            }
            
            // 检查目标玩家是否有飞行权限
            Long endTime = storage.getPlayerFlightTime(target.getUniqueId());
            boolean hadFlight = false;
            
            if (endTime != null && endTime > System.currentTimeMillis()) {
                hadFlight = true;
            }
            
            // 禁用飞行
            target.setAllowFlight(false);
            target.setFlying(false);
            storage.removePlayerFlightTime(target.getUniqueId());
            flyingPlayers.remove(target.getUniqueId());
            
            // 同步取消MHDF-Tools飞行权限
            syncMHDFToolsDisableFlight(target);
            
            // 取消倒计时
            countdownManager.cancelCountdown(target);
            
            // 发送消息
            String disableMessage = lang.getMessage("flight-disabled-by-admin");
            target.sendMessage(prefix + disableMessage);
            
            if (hadFlight) {
                sender.sendMessage(prefix + lang.getMessage("admin-disabled-flight", "{player}", target.getName()));
            } else {
                sender.sendMessage(prefix + lang.getMessage("cmd-player-had-no-permission", "{player}", target.getName()));
            }
            
            return true;
        } else if (args[0].equalsIgnoreCase("reload")) {
            // 重载配置文件
            reloadConfig();
            config = getConfig();
            
            // 重载语言文件
            lang = new LanguageManager(this, getConfig().getString("language", "zh_CN"));
            
            // 重新初始化GUI
            shopGUI.reloadConfig();
            
            sender.sendMessage(prefix + lang.getMessage("config-reloaded"));
            return true;
        } else if (args[0].equalsIgnoreCase("bypass")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + lang.getMessage("bypass.usage"));
                return true;
            }
            
            Player target = getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(prefix + lang.getMessage("player-not-found", "{player}", args[1]));
                return true;
            }
            
            if (args.length >= 3 && args[2].equalsIgnoreCase("remove")) {
                // 移除绕过权限
                getServer().dispatchCommand(getServer().getConsoleSender(), 
                    "lp user " + target.getName() + " permission unset paytofly.bypass");
                sender.sendMessage(prefix + lang.getMessage("bypass.remove-success", "{player}", target.getName()));
                target.sendMessage(prefix + lang.getMessage("bypass.target-remove"));
            } else {
                // 添加绕过权限
                getServer().dispatchCommand(getServer().getConsoleSender(), 
                    "lp user " + target.getName() + " permission set paytofly.bypass true");
                sender.sendMessage(prefix + lang.getMessage("bypass.add-success", "{player}", target.getName()));
                target.sendMessage(prefix + lang.getMessage("bypass.target-add"));
            }
            return true;
        }
        
        sender.sendMessage(prefix + lang.getMessage("console-unknown-command"));
        return true;
    }

    // 添加获取经济管理器的方法
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    /**
     * 同步MHDF-Tools飞行权限
     * @param player 玩家
     * @param endTime 结束时间
     */
    private void syncMHDFToolsFlight(Player player, long endTime) {
        if (getServer().getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                // 只使用fly命令设置飞行权限，不使用flytime命令
                String command = "fly " + player.getName() + " true";
                getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
                getLogger().info(lang.getMessage("mhdf-sync-success", "{player}", player.getName()));
            } catch (Exception e) {
                getLogger().warning(lang.getMessage("mhdf-sync-failed", "{error}", e.getMessage()));
            }
        }
    }

    /**
     * 同步取消MHDF-Tools飞行权限
     * @param player 玩家
     */
    private void syncMHDFToolsDisableFlight(Player player) {
        if (getServer().getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                // 使用MHDF-Tools的fly命令取消飞行权限
                getServer().dispatchCommand(Bukkit.getConsoleSender(), 
                        "fly " + player.getName() + " false");
                
                // 确保完全移除权限 - 尝试清除可能存在的任何临时权限
                getServer().dispatchCommand(Bukkit.getConsoleSender(), 
                        "lp user " + player.getName() + " permission unset mhdtools.commands.fly.temp");
                
                getLogger().info(lang.getMessage("mhdf-disable-success", "{player}", player.getName()));
            } catch (Exception e) {
                getLogger().warning(lang.getMessage("mhdf-disable-failed", "{error}", e.getMessage()));
            }
        }
    }
}
