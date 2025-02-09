package org.littlesheep.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public class ConfigChecker {
    private final JavaPlugin plugin;
    private final Logger logger;
    private static final double CONFIG_VERSION = 1.0;

    public ConfigChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean checkConfig() {
        logger.info("正在检查配置文件...");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            logger.info("未找到配置文件，正在创建默认配置...");
            plugin.saveDefaultConfig();
            return true;
        }

        YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
        double version = currentConfig.getDouble("config-version", 1.0);
        
        if (version != CONFIG_VERSION) {
            logger.info("配置文件版本不匹配，正在更新...");
            try {
                // 备份旧配置
                File backupFile = new File(plugin.getDataFolder(), "config.yml.bak");
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("已将旧配置文件备份为 config.yml.bak");
                
                // 保存新配置
                configFile.delete();
                plugin.saveDefaultConfig();
                logger.info("配置文件已更新到最新版本");
            } catch (IOException e) {
                logger.severe("更新配置文件时出错：" + e.getMessage());
                return false;
            }
        }
        
        checkEssentialConfigs(plugin.getConfig());
        
        return true;
    }

    private void checkEssentialConfigs(FileConfiguration config) {
        // 检查必要的配置项
        String[] essentialPaths = {
            "settings.default-fly",
            "update-checker.enabled",
            "worlds.free-fly",
            "worlds.disabled",
            "worlds.notify-on-enter"
        };

        for (String path : essentialPaths) {
            if (!config.contains(path)) {
                logger.warning("缺少必要的配置项: " + path);
            }
        }
    }
} 