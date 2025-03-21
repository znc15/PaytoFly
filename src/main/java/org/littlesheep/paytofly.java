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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class paytofly extends JavaPlugin {
    private Economy econ;
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
            getLogger().severe("配置文件检查失败！插件将被禁用！");
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
        
        getLogger().info("正在初始化经济系统...");
        // 延迟初始化Vault
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!setupEconomy()) {
                getLogger().severe("未找到 Vault 插件，插件将被禁用！");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("经济系统初始化成功！");
        }, 20L);
        
        // 初始化存储系统
        getLogger().info("正在初始化存储系统...");
        try {
            storage = StorageFactory.createStorage(getConfig().getString("storage.type", "JSON"), this);
            storage.init();
            getLogger().info("存储系统初始化成功！（类型：" + 
                (getConfig().getString("storage.type", "JSON")) + "）");
        } catch (Exception e) {
            getLogger().severe("存储系统初始化失败！");
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
        getLogger().info("已加载 " + flyingPlayers.size() + " 条飞行数据");
        
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
        
        getLogger().info("PayToFly插件启动完成！");
        getLogger().info("插件已经是完全体了喵 Ciallo～(∠・ω< )⌒★");
        
        loadMessageConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + lang.getMessage("command-player-only"));
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("fly")) {
            if (args.length == 0) {
                shopGUI.openGUI(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("help")) {
                sendHelpMessage(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("time") || args[0].equalsIgnoreCase("check")) {
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
                    player.sendMessage(prefix + lang.getMessage("command-usage"));
                    return true;
                }
                
                Player target = getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(prefix + lang.getMessage("player-not-found", "{player}", args[1]));
                    return true;
                }
                
                target.setAllowFlight(false);
                target.setFlying(false);
                storage.removePlayerFlightTime(target.getUniqueId());
                flyingPlayers.remove(target.getUniqueId());
                
                String disableMessage = config.getString("messages.flight-disabled-by-admin", "§c管理员已关闭了你的飞行权限！");
                target.sendMessage(prefix + disableMessage);
                
                String adminMessage = config.getString("messages.admin-disabled-flight", "§a已关闭玩家 {player} 的飞行权限")
                    .replace("{player}", target.getName());
                player.sendMessage(prefix + adminMessage);
                
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

            if (args[0].equalsIgnoreCase("bypass")) {
                if (!player.hasPermission("paytofly.admin")) {
                    player.sendMessage(prefix + lang.getMessage("no-permission"));
                    return true;
                }
                
                if (args.length < 2) {
                    player.sendMessage(prefix + "§c用法: /fly bypass <玩家名> [remove]");
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
                    player.sendMessage(prefix + "§a已移除玩家 " + target.getName() + " 的飞行绕过权限");
                    target.sendMessage(prefix + "§c您的飞行绕过权限已被移除");
                } else {
                    // 添加绕过权限
                    getServer().dispatchCommand(getServer().getConsoleSender(), 
                        "lp user " + target.getName() + " permission set paytofly.bypass true");
                    player.sendMessage(prefix + "§a已给予玩家 " + target.getName() + " 飞行绕过权限");
                    target.sendMessage(prefix + "§a您已获得飞行绕过权限");
                }
                return true;
            }

            // 解析时间格式
            String timeArg = args[0].toLowerCase();
            if (!timeArg.matches("\\d+[mhdwM]")) {
                player.sendMessage(prefix + lang.getMessage("invalid-time-format"));
                return true;
            }

            int amount = Integer.parseInt(timeArg.substring(0, timeArg.length() - 1));
            char unit = timeArg.charAt(timeArg.length() - 1);
            
            long durationMillis;
            double costPerUnit;
            String unitName;
            int minLimit;
            int maxLimit;

            switch (unit) {
                case 'm':
                    durationMillis = amount * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.minute");
                    unitName = lang.getMessage("time-format.minute");
                    minLimit = config.getInt("time-limits.minute.min", 5);
                    maxLimit = config.getInt("time-limits.minute.max", 60);
                    break;
                case 'h':
                    durationMillis = amount * 60 * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.hour");
                    unitName = lang.getMessage("time-format.hour");
                    minLimit = config.getInt("time-limits.hour.min", 1);
                    maxLimit = config.getInt("time-limits.hour.max", 24);
                    break;
                case 'd':
                    durationMillis = amount * 24 * 60 * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.day");
                    unitName = lang.getMessage("time-format.day");
                    minLimit = config.getInt("time-limits.day.min", 1);
                    maxLimit = config.getInt("time-limits.day.max", 7);
                    break;
                case 'w':
                    durationMillis = amount * 7 * 24 * 60 * 60 * 1000L;
                    costPerUnit = config.getDouble("fly-cost.week");
                    unitName = lang.getMessage("time-format.week");
                    minLimit = config.getInt("time-limits.week.min", 1);
                    maxLimit = config.getInt("time-limits.week.max", 4);
                    break;
                case 'M':
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

            // 计算总价
            double totalCost = costPerUnit * amount;

            // 显示购买详情
            player.sendMessage(prefix + lang.getMessage("purchase-details",
                "{duration}", amount + unitName,
                "{amount}", String.format("%.2f", totalCost)));

            // 检查余额并处理购买
            if (econ.getBalance(player) >= totalCost) {
                // 检查是否已经在飞行
                if (flyingPlayers.containsKey(player.getUniqueId())) {
                    long remainingTime = flyingPlayers.get(player.getUniqueId()) - System.currentTimeMillis();
                    if (remainingTime > 0) {
                        player.sendMessage(prefix + lang.getMessage("already-flying",
                            "{time}", formatTime(remainingTime)));
                        return true;
                    }
                }

                econ.withdrawPlayer(player, totalCost);
                player.setAllowFlight(true);
                player.setFlying(true);
                
                long endTime = System.currentTimeMillis() + durationMillis;
                flyingPlayers.put(player.getUniqueId(), endTime);
                storage.setPlayerFlightTime(player.getUniqueId(), endTime);
                
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
            return days + "天" + hours % 24 + "小时";
        } else if (hours > 0) {
            return hours + "小时" + minutes % 60 + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟";
        } else {
            return seconds + "秒";
        }
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "天" + hours % 24 + "小时" + minutes % 60 + "分钟";
        } else if (hours > 0) {
            return hours + "小时" + minutes % 60 + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟";
        } else {
            return seconds + "秒";
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(prefix + lang.getMessage("help-title"));
        player.sendMessage(prefix + lang.getMessage("help-commands"));
        player.sendMessage(prefix + lang.getMessage("help-footer"));
        player.sendMessage(prefix + lang.getMessage("help-time-units"));
        player.sendMessage(prefix + lang.getMessage("help-footer"));
        if (player.hasPermission("paytofly.admin")) {
            player.sendMessage(prefix + lang.getMessage("help-reload"));
        }
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
            getLogger().warning("找不到语言键: " + path);
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
                getLogger().warning("无法找到语言文件: " + language + ".yml");
                getLogger().warning("使用默认语言文件...");
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
}
