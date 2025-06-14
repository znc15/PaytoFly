package org.littlesheep.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.bukkit.ChatColor;

public class LanguageManager {
    private final JavaPlugin plugin;
    private YamlConfiguration langConfig;
    private final String language;
    private static final double CURRENT_LANG_VERSION = 1.0;
    private final File langFolder;
    private final File langFile;
    private Map<String, String> messages;

    public LanguageManager(JavaPlugin plugin, String language) {
        this.plugin = plugin;
        this.language = language;
        this.langFolder = new File(plugin.getDataFolder(), "lang");
        this.langFile = new File(langFolder, language + ".yml");
        this.messages = new HashMap<>();
        loadLanguage();
    }

    private void loadLanguage() {
        // 确保语言文件夹存在
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        boolean needsUpdate = false;

        // 检查语言文件是否存在
        if (!langFile.exists()) {
            needsUpdate = true;
        } else {
            // 检查版本和完整性
            try {
                YamlConfiguration existingConfig = YamlConfiguration.loadConfiguration(langFile);
                double fileVersion = existingConfig.getDouble("language-version", 1.0);
                
                // 检查是否有缺失的语言键
                InputStream defaultLangStream = plugin.getResource("lang/" + language + ".yml");
                if (defaultLangStream != null) {
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultLangStream));
                    
                    for (String key : defaultConfig.getKeys(true)) {
                        if (!existingConfig.contains(key)) {
                            needsUpdate = true;
                            break;
                        }
                    }
                }

                // 版本检查
                if (fileVersion < CURRENT_LANG_VERSION) {
                    needsUpdate = true;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "检查语言文件时出错", e);
                needsUpdate = true;
            }
        }

        if (needsUpdate) {
            updateLanguageFile();
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void updateLanguageFile() {
        plugin.getLogger().info("正在更新语言文件 " + language + ".yml ...");

        // 创建备份
        if (langFile.exists()) {
            try {
                File backupFile = new File(langFolder, language + ".yml.bak");
                Files.copy(langFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("已将旧语言文件备份为 " + language + ".yml.bak");
            } catch (IOException e) {
                plugin.getLogger().warning("备份语言文件时出错: " + e.getMessage());
            }
        }

        // 强制更新语言文件
        try (InputStream in = plugin.getResource("lang/" + language + ".yml")) {
            if (in != null) {
                Files.copy(in, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("语言文件已更新到最新版本");
            } else {
                // 如果找不到指定语言文件，使用默认中文
                try (InputStream defaultIn = plugin.getResource("lang/zh_CN.yml")) {
                    if (defaultIn != null) {
                        Files.copy(defaultIn, langFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        plugin.getLogger().warning("找不到 " + language + " 语言文件，使用默认中文");
                    } else {
                        plugin.getLogger().severe("无法加载默认语言文件！");
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("更新语言文件时出错: " + e.getMessage());
        }
    }

    public String getMessage(String path) {
        // 首先看是否设置了默认消息
        if (this.messages.containsKey(path)) {
            return ChatColor.translateAlternateColorCodes('&', messages.get(path));
        }
        
        // 尝试从文件中获取
        String message = langConfig.getString("messages." + path);
        if (message == null) {
            return ChatColor.translateAlternateColorCodes('&', "&c缺失的语言键: " + path);
        }
        
        // 缓存并返回
        messages.put(path, message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public String[] getMessageLines(String path) {
        String message = getMessage(path);
        return message.split("\n");
    }

    public void reloadLanguage() {
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public List<String> getStringList(String path) {
        return langConfig.getStringList(path);
    }
} 