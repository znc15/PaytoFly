package org.littlesheep.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CountdownManager {
    private final JavaPlugin plugin;
    private final LanguageManager lang;
    private final Map<UUID, BossBar> bossBars;
    private final Map<UUID, Integer> countdownTasks;
    private final FileConfiguration config;
    
    // 配置选项
    private final boolean bossBarEnabled;
    private final boolean chatEnabled;
    private final BarColor normalColor;
    private final BarColor warningColor;
    private final BarStyle barStyle;
    private final boolean showProgress;
    private final int reminderInterval;
    private final int chatWarningTime;
    private final int showBeforeSeconds;

    public CountdownManager(JavaPlugin plugin, LanguageManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.bossBars = new HashMap<>();
        this.countdownTasks = new HashMap<>();
        this.config = plugin.getConfig();
        
        // 加载配置
        this.bossBarEnabled = config.getBoolean("notifications.bossbar.enabled", true);
        this.chatEnabled = config.getBoolean("notifications.chat.enabled", true);
        this.normalColor = BarColor.valueOf(config.getString("notifications.bossbar.colors.normal", "GREEN"));
        this.warningColor = BarColor.valueOf(config.getString("notifications.bossbar.colors.warning", "RED"));
        this.barStyle = BarStyle.valueOf(config.getString("notifications.bossbar.style", "SEGMENTED_20"));
        this.showProgress = config.getBoolean("notifications.bossbar.show-progress", true);
        this.reminderInterval = config.getInt("notifications.chat.reminder-interval", 10);
        this.chatWarningTime = config.getInt("notifications.chat.warning-time", 60);
        this.showBeforeSeconds = config.getInt("notifications.bossbar.show-before", 15);
    }

    public void startCountdown(Player player, long endTime) {
        UUID uuid = player.getUniqueId();
        
        // 清除现有的倒计时
        stopCountdown(uuid);
        
        // 创建 BossBar（但先不显示）
        if (bossBarEnabled) {
            BossBar bossBar = Bukkit.createBossBar(
                lang.getMessage("bossbar-title", "{time}", formatTime(endTime - System.currentTimeMillis())),
                normalColor,
                barStyle
            );
            bossBars.put(uuid, bossBar);
        }

        // 启动倒计时任务
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long remaining = endTime - System.currentTimeMillis();
            
            if (remaining <= 0) {
                stopCountdown(uuid);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(lang.getMessage("flight-expired"));
                return;
            }

            // 更新 BossBar
            BossBar bossBar = bossBars.get(uuid);
            if (bossBar != null) {
                // 只在最后倒计时时显示 BossBar
                if (remaining <= showBeforeSeconds * 1000L) {
                    if (!bossBar.getPlayers().contains(player)) {
                        bossBar.addPlayer(player);
                    }
                    
                    if (showProgress) {
                        double progress = Math.max(0.0, remaining / (double)(showBeforeSeconds * 1000L));
                        bossBar.setProgress(progress);
                    }
                    
                    bossBar.setTitle(lang.getMessage("bossbar-title", "{time}", formatTime(remaining)));
                    bossBar.setColor(warningColor);
                }
            }

            // 聊天框提醒
            if (chatEnabled) {
                if (remaining <= chatWarningTime * 1000 && remaining % (reminderInterval * 1000) <= 50) {
                    player.sendMessage(lang.getMessage("flight-ending-soon", 
                        "{time}", formatTime(remaining)));
                }
            }
        }, 0L, 20L);

        countdownTasks.put(uuid, taskId);
    }

    public void stopCountdown(UUID uuid) {
        // 取消任务
        Integer taskId = countdownTasks.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        // 移除 BossBar
        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /**
     * 取消玩家的倒计时
     * @param player 要取消倒计时的玩家
     */
    public void cancelCountdown(Player player) {
        if (player != null) {
            stopCountdown(player.getUniqueId());
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + hours % 24 + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes % 60 + "m";
        } else if (minutes > 0) {
            return minutes + "m " + seconds % 60 + "s";
        } else {
            return seconds + "s";
        }
    }

    public void cleanup() {
        // 清理所有倒计时
        for (UUID uuid : new HashMap<>(countdownTasks).keySet()) {
            stopCountdown(uuid);
        }
    }
} 