package org.littlesheep.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.littlesheep.paytofly;

import java.util.UUID;

public class PlayerJoinListener implements Listener {
    private final paytofly plugin;

    public PlayerJoinListener(paytofly plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // 延迟1tick执行，确保玩家完全加入
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 检查是否有无限飞行权限
            if (player.hasPermission("paytofly.infinite")) {
                player.setAllowFlight(true);
                return;
            }

            // 获取玩家的飞行到期时间
            Long endTime = plugin.getStorage().getPlayerFlightTime(uuid);
            
            if (endTime != null) {
                long remaining = plugin.getTimeManager().getRemainingTime(player, endTime);
                
                if (remaining > 0) {
                    // 飞行时间未过期
                    player.setAllowFlight(true);
                    player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("flight-restored", 
                        "{time}", formatDuration(remaining),
                        "{remaining}", formatDuration(remaining)));
                    
                    // 启动倒计时（如果接近结束）
                    plugin.getCountdownManager().startCountdown(player, endTime);
                } else {
                    // 飞行时间已过期
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage(plugin.getPrefix() + plugin.getLang("flight-expired"));
                    
                    // 从存储中移除过期数据
                    plugin.getStorage().removePlayerFlightTime(uuid);
                }
            }
        }, 1L);

        // 检查是否为管理员且配置允许提示更新
        if (player.hasPermission("paytofly.admin") && 
            plugin.getConfig().getBoolean("settings.admin-update-notice", true)) {
            
            // 使用异步任务检查更新
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String latestVersion = plugin.getUpdateChecker().getLatestVersion();
                String currentVersion = plugin.getDescription().getVersion();
                
                if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                    player.sendMessage(plugin.getPrefix() + "§e发现新版本！");
                    player.sendMessage(plugin.getPrefix() + "§e当前版本: §f" + currentVersion);
                    player.sendMessage(plugin.getPrefix() + "§e最新版本: §f" + latestVersion);
                    player.sendMessage(plugin.getPrefix() + "§e下载地址: §fhttps://github.com/znc15/paytofly/releases");
                }
            });
        }

        // 检查更新通知 (只有管理员能看见)
        if (player.hasPermission("paytofly.admin") && plugin.getUpdateChecker() != null) {
            String currentVersion = plugin.getDescription().getVersion();
            String latestVersion = plugin.getUpdateChecker().getLatestVersion();
            if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("update-found"));
                player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("update-current", "{version}", currentVersion));
                player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("update-latest", "{latest}", latestVersion));
                player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("update-download"));
            }
        }
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("天");
        if (hours > 0) result.append(hours).append("小时");
        if (minutes > 0) result.append(minutes).append("分钟");
        if (seconds > 0) result.append(seconds).append("秒");
        
        return result.length() > 0 ? result.toString() : "0秒";
    }
} 