package org.littlesheep.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优化的倒计时管理器 - 使用统一任务调度，提高性能
 * 兼容原CountdownManager接口
 */
public class OptimizedCountdownManager {
    private final JavaPlugin plugin;
    private final LanguageManager lang;
    private final Map<UUID, PlayerCountdown> activeCountdowns = new ConcurrentHashMap<>();
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
    
    // 统一调度任务
    private Integer globalTaskId = null;
    private final Object taskLock = new Object();
    
    // 性能统计
    private volatile long lastUpdateTime = 0;
    private volatile int totalUpdates = 0;
    private volatile int activePlayersCount = 0;

    public OptimizedCountdownManager(JavaPlugin plugin, LanguageManager lang) {
        this.plugin = plugin;
        this.lang = lang;
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
        
        plugin.getLogger().info("优化倒计时管理器已初始化 - 统一任务调度模式");
    }

    /**
     * 开始倒计时
     */
    public void startCountdown(Player player, long endTime) {
        UUID uuid = player.getUniqueId();
        
        // 移除现有倒计时
        stopCountdown(uuid);
        
        // 创建新的倒计时
        PlayerCountdown countdown = new PlayerCountdown(player, endTime);
        activeCountdowns.put(uuid, countdown);
        
        // 启动全局任务（如果还没有）
        startGlobalTaskIfNeeded();
        
        plugin.getLogger().fine(String.format("为玩家 %s 开始倒计时，结束时间: %d", player.getName(), endTime));
    }



    /**
     * 取消玩家的倒计时
     */
    public void cancelCountdown(Player player) {
        if (player != null) {
            stopCountdown(player.getUniqueId());
        }
    }

    /**
     * 停止倒计时（兼容原接口）
     */
    public void stopCountdown(UUID uuid) {
        PlayerCountdown countdown = activeCountdowns.remove(uuid);
        if (countdown != null) {
            countdown.cleanup();
            plugin.getLogger().fine("停止玩家倒计时: " + uuid);
        }
        
        // 如果没有活跃倒计时了，停止全局任务
        if (activeCountdowns.isEmpty()) {
            stopGlobalTask();
        }
    }

    /**
     * 启动全局任务（如果需要）
     */
    private void startGlobalTaskIfNeeded() {
        synchronized (taskLock) {
            if (globalTaskId == null && !activeCountdowns.isEmpty()) {
                globalTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, 
                    this::updateAllCountdowns, 0L, 20L);
                plugin.getLogger().fine("启动全局倒计时任务，ID: " + globalTaskId);
            }
        }
    }

    /**
     * 停止全局任务
     */
    private void stopGlobalTask() {
        synchronized (taskLock) {
            if (globalTaskId != null) {
                Bukkit.getScheduler().cancelTask(globalTaskId);
                plugin.getLogger().fine("停止全局倒计时任务，ID: " + globalTaskId);
                globalTaskId = null;
            }
        }
    }

    /**
     * 更新所有倒计时
     */
    private void updateAllCountdowns() {
        long now = System.currentTimeMillis();
        lastUpdateTime = now;
        totalUpdates++;
        
        Iterator<Map.Entry<UUID, PlayerCountdown>> iterator = activeCountdowns.entrySet().iterator();
        int processedCount = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerCountdown> entry = iterator.next();
            UUID uuid = entry.getKey();
            PlayerCountdown countdown = entry.getValue();
            
            try {
                if (!countdown.update(now)) {
                    // 倒计时结束，移除
                    iterator.remove();
                    countdown.cleanup();
                    handleCountdownExpired(countdown);
                }
                processedCount++;
            } catch (Exception e) {
                plugin.getLogger().warning(String.format(
                    "更新玩家 %s 倒计时时出错: %s", uuid, e.getMessage()));
                // 移除有问题的倒计时
                iterator.remove();
                countdown.cleanup();
            }
        }
        
        activePlayersCount = processedCount;
        
        // 如果没有活跃倒计时了，停止任务
        if (activeCountdowns.isEmpty()) {
            stopGlobalTask();
        }
    }

    /**
     * 处理倒计时过期
     */
    private void handleCountdownExpired(PlayerCountdown countdown) {
        Player player = countdown.getPlayer();
        if (player != null && player.isOnline()) {
            // 禁用飞行
            player.setAllowFlight(false);
            player.setFlying(false);
            
            // 清理插件数据
            if (plugin instanceof org.littlesheep.paytofly) {
                org.littlesheep.paytofly paytoFlyPlugin = (org.littlesheep.paytofly) plugin;
                paytoFlyPlugin.getFlyingPlayers().remove(player.getUniqueId());
                paytoFlyPlugin.getStorage().removePlayerFlightTime(player.getUniqueId());
                
                // 同步取消MHDF-Tools飞行权限
                syncMHDFToolsDisableFlight(player, paytoFlyPlugin);
            }
            
            player.sendMessage(lang.getMessage("flight-expired"));
        }
    }

    /**
     * 清理所有倒计时
     */
    public void cleanup() {
        plugin.getLogger().info("清理倒计时管理器...");
        
        // 停止全局任务
        stopGlobalTask();
        
        // 清理所有倒计时
        for (PlayerCountdown countdown : activeCountdowns.values()) {
            countdown.cleanup();
        }
        activeCountdowns.clear();
        
        plugin.getLogger().info("倒计时管理器清理完成");
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format(
            "倒计时统计: 活跃玩家=%d, 总更新次数=%d, 最后更新=%s, 全局任务=%s",
            activePlayersCount,
            totalUpdates,
            lastUpdateTime > 0 ? new java.util.Date(lastUpdateTime).toString() : "从未",
            globalTaskId != null ? "运行中" : "已停止"
        );
    }

    /**
     * 同步取消MHDF-Tools飞行权限
     */
    private void syncMHDFToolsDisableFlight(Player player, org.littlesheep.paytofly paytoFlyPlugin) {
        if (Bukkit.getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "fly " + player.getName() + " false");
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "lp user " + player.getName() + " permission unset mhdtools.commands.fly.temp");
                
                paytoFlyPlugin.getLogger().fine("已在倒计时结束时禁用玩家 " + player.getName() + " 的MHDF-Tools飞行权限");
            } catch (Exception e) {
                paytoFlyPlugin.getLogger().warning("同步取消MHDF-Tools飞行权限失败: " + e.getMessage());
            }
        }
    }

    /**
     * 玩家倒计时数据类
     */
    private class PlayerCountdown {
        private final Player player;
        private final long endTime;
        private final long startTime;
        private BossBar bossBar;
        private long lastChatReminder = 0;
        private boolean bossBarShown = false;

        public PlayerCountdown(Player player, long endTime) {
            this.player = player;
            this.endTime = endTime;
            this.startTime = System.currentTimeMillis();
            
            // 创建BossBar但不立即显示
            if (bossBarEnabled) {
                this.bossBar = Bukkit.createBossBar(
                    lang.getMessage("bossbar-title", "{time}", formatTime(endTime - startTime)),
                    normalColor,
                    barStyle
                );
            }
        }

        /**
         * 更新倒计时
         * @param now 当前时间
         * @return true if countdown should continue, false if expired
         */
        public boolean update(long now) {
            long remaining = endTime - now;
            
            // 检查是否过期
            if (remaining <= 0) {
                return false;
            }
            
            // 检查玩家是否仍在线
            if (!player.isOnline()) {
                return false;
            }
            
            // 更新BossBar
            updateBossBar(remaining);
            
            // 更新聊天提醒
            updateChatReminder(remaining, now);
            
            return true;
        }

        private void updateBossBar(long remaining) {
            if (bossBar == null) return;
            
            // 只在最后倒计时时显示BossBar
            if (remaining <= showBeforeSeconds * 1000L) {
                if (!bossBarShown) {
                    bossBar.addPlayer(player);
                    bossBarShown = true;
                }
                
                if (showProgress) {
                    double progress = Math.max(0.0, remaining / (double)(showBeforeSeconds * 1000L));
                    bossBar.setProgress(progress);
                }
                
                bossBar.setTitle(lang.getMessage("bossbar-title", "{time}", formatTime(remaining)));
                bossBar.setColor(warningColor);
            }
        }

        private void updateChatReminder(long remaining, long now) {
            if (!chatEnabled) return;
            
            if (remaining <= chatWarningTime * 1000 && 
                now - lastChatReminder >= reminderInterval * 1000) {
                
                player.sendMessage(lang.getMessage("flight-ending-soon", 
                    "{time}", formatTime(remaining)));
                lastChatReminder = now;
            }
        }

        public void cleanup() {
            if (bossBar != null) {
                bossBar.removeAll();
                bossBar = null;
            }
        }

        public Player getPlayer() {
            return player;
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
}