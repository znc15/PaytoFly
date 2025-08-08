package org.littlesheep;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.littlesheep.data.Storage;
import org.littlesheep.data.StorageFactory;
import org.littlesheep.economy.EconomyManager;
import org.littlesheep.listeners.PlayerListener;
import org.littlesheep.placeholders.FlightExpansion;
import org.littlesheep.utils.OptimizedCountdownManager;
import org.littlesheep.utils.LanguageManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.littlesheep.listeners.PlayerJoinListener;
import org.littlesheep.gui.EnhancedFlightShopGUI;
import org.littlesheep.utils.VersionManager;
import org.littlesheep.utils.UpdateChecker;
import org.littlesheep.utils.TimeManager;
import org.littlesheep.utils.ConfigChecker;
import org.littlesheep.listeners.WorldChangeListener;
import org.littlesheep.commands.FlyCommandTabCompleter;
import org.littlesheep.commands.CommandHandler;
import org.littlesheep.utils.CustomTimeManager;
import org.littlesheep.utils.ResourceManager;
import org.littlesheep.utils.ExceptionHandler;
import org.littlesheep.effects.FlightEffectManager;
import org.littlesheep.speed.FlightSpeedManager;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.HashMap;
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
    private OptimizedCountdownManager countdownManager;
    private static final int BSTATS_ID = 24712;
    private EnhancedFlightShopGUI shopGUI;
    private VersionManager versionManager;
    private UpdateChecker updateChecker;
    private TimeManager timeManager;
    private CustomTimeManager customTimeManager;
    private FileConfiguration messageConfig;
    private FileConfiguration langConfig;
    private ResourceManager resourceManager;
    private ExceptionHandler exceptionHandler;
    private CommandHandler commandHandler;
    private FlightEffectManager effectManager;
    private FlightSpeedManager speedManager;

    @Override
    public void onEnable() {
        // 初始化资源管理器（必须第一个初始化）
        resourceManager = new ResourceManager(this);
        
        // 初始化异常处理器
        exceptionHandler = new ExceptionHandler(this);
        
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
        
        config = getConfig();
        prefix = config.getString("messages.prefix", "&7[&bPayToFly&7] ").replace("&", "§");
        
        // 初始化经济系统
        economyManager = new EconomyManager(this);
        if (economyManager.isInitialized()) {
            getLogger().info("§a经济系统初始化成功！使用: " + economyManager.getEconomyName());
            econ = economyManager.getCurrentEconomy() != null ? 
                   getServer().getServicesManager().getRegistration(Economy.class) != null ? 
                   getServer().getServicesManager().getRegistration(Economy.class).getProvider() : null : null;
        } else {
            getLogger().severe(lang.getMessage("economy-failed-detail"));
            getLogger().severe(lang.getMessage("economy-failed-hint"));
        }
        
        // 初始化存储系统
        try {
            storage = StorageFactory.createStorage(getConfig().getString("storage.type", "JSON"), this);
            storage.init();
            // 注册存储到资源管理器
            resourceManager.registerCloseable(storage);
            getLogger().info("§a存储系统初始化成功！（类型：" + getConfig().getString("storage.type", "JSON") + "）");
        } catch (Exception e) {
            getLogger().severe(lang.getMessage("storage-failed-detail"));
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化优化的倒计时管理器
        countdownManager = new OptimizedCountdownManager(this, lang);
        // 注册倒计时管理器清理钩子
        resourceManager.registerShutdownHook(() -> {
            if (countdownManager != null) {
                countdownManager.cleanup();
            }
        });
        
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
        }
        
        // 初始化 bStats
        if (getConfig().getBoolean("metrics.enabled", true)) {
            setupMetrics();
        }
        
        // 初始化增强版GUI
        shopGUI = new EnhancedFlightShopGUI(this);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        
        // 初始化并运行更新检查器
        updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates();
        
        this.timeManager = new TimeManager(this);
        
        // 注册世界切换监听器
        getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);
        
        customTimeManager = new CustomTimeManager(this);
        getServer().getPluginManager().registerEvents(customTimeManager, this);
        
        // 初始化命令处理器
        commandHandler = new CommandHandler(this);
        
        // 初始化飞行特效管理器
        effectManager = new FlightEffectManager(this);
        // 注册特效管理器清理钩子
        resourceManager.registerShutdownHook(() -> {
            if (effectManager != null) {
                effectManager.cleanup();
            }
        });
        
        // 初始化飞行速度管理器
        speedManager = new FlightSpeedManager(this);
        // 注册速度管理器清理钩子
        resourceManager.registerShutdownHook(() -> {
            if (speedManager != null) {
                speedManager.cleanup();
            }
        });
        
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
        
        getLogger().info("§6===== PayToFly 插件启动说明 =====");
        getLogger().info("§6• 作者: LittleSheep");
        getLogger().info("§6• 当前版本: v" + getDescription().getVersion());
        getLogger().info("§6• 存储类型: " + getConfig().getString("storage.type", "JSON"));
        getLogger().info("§6• 语言: " + getConfig().getString("language", "zh_CN"));
        getLogger().info("§6===== Ciallo～(∠・ω< )⌒★ =====");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fly")) {
            return commandHandler.handleCommand(sender, args);
        }
        return false;
    }

    @Override
    public void onDisable() {
        getLogger().info("PayToFly插件正在关闭...");
        
        // 使用资源管理器统一清理所有资源
        if (resourceManager != null) {
            resourceManager.close();
        } else {
            // 兜底方案：手动清理关键资源
            if (countdownManager != null) {
                countdownManager.cleanup();
            }
            
            if (storage != null) {
                storage.close();
            }
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

    public OptimizedCountdownManager getCountdownManager() {
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
    


    // 添加获取经济管理器的方法
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public EnhancedFlightShopGUI getFlightShopGUI() {
        return shopGUI;
    }

    public EnhancedFlightShopGUI getEnhancedFlightShopGUI() {
        return shopGUI;
    }

    public FlightEffectManager getEffectManager() {
        return effectManager;
    }

    public FlightSpeedManager getSpeedManager() {
        return speedManager;
    }

    /**
     * 同步MHDF-Tools飞行权限
     * @param player 玩家
     * @param endTime 结束时间
     */
    public void syncMHDFToolsFlight(Player player, long endTime) {
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
    public void syncMHDFToolsDisableFlight(Player player) {
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
