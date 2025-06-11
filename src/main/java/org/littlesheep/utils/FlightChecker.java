package org.littlesheep.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.littlesheep.paytofly;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public class FlightChecker {
    private final paytofly plugin;
    private final LanguageManager lang;
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FlightChecker(paytofly plugin, LanguageManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void checkAllPlayers() {
        plugin.getLogger().info(lang.getMessage("checking-players"));
        int checked = 0;
        int updated = 0;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (updatePlayerFlightStatus(player)) {
                updated++;
            }
            checked++;
        }

        plugin.getLogger().info(lang.getMessage("check-complete", 
            "{checked}", String.valueOf(checked),
            "{updated}", String.valueOf(updated)));
    }

    public boolean updatePlayerFlightStatus(Player player) {
        if (player.hasPermission("paytofly.infinite")) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Map<UUID, Long> flyingPlayers = plugin.getFlyingPlayers();
        Long endTime = flyingPlayers.get(uuid);

        boolean statusChanged = false;
        if (endTime == null || endTime < System.currentTimeMillis()) {
            // 飞行时间已过期
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                String expireTime = endTime != null ? formatTime(endTime) : "未知";
                player.sendMessage(plugin.getPrefix() + lang.getMessage("flight-expired", 
                    "{time}", expireTime));
                flyingPlayers.remove(uuid);
                
                // 同步取消MHDF-Tools飞行权限
                syncMHDFToolsDisableFlight(player);
                
                statusChanged = true;
            }
        } else {
            // 飞行时间有效
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                String expireTime = formatTime(endTime);
                player.sendMessage(plugin.getPrefix() + lang.getMessage("flight-restored", 
                    "{time}", expireTime,
                    "{remaining}", TimeFormatter.formatTime(endTime - System.currentTimeMillis())));
                plugin.getCountdownManager().startCountdown(player, endTime);
                
                // 同步MHDF-Tools飞行权限
                syncMHDFToolsFlight(player, endTime);
                
                statusChanged = true;
            }
        }

        return statusChanged;
    }

    /**
     * 同步MHDF-Tools飞行权限
     * @param player 玩家
     * @param endTime 结束时间
     */
    private void syncMHDFToolsFlight(Player player, long endTime) {
        if (Bukkit.getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                // 只使用fly命令设置飞行权限，不使用flytime命令
                String command = "fly " + player.getName() + " true";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                plugin.getLogger().info("已同步玩家 " + player.getName() + " 的飞行权限到MHDF-Tools");
            } catch (Exception e) {
                plugin.getLogger().warning("同步MHDF-Tools飞行权限失败: " + e.getMessage());
            }
        }
    }

    /**
     * 同步取消MHDF-Tools飞行权限
     * @param player 玩家
     */
    private void syncMHDFToolsDisableFlight(Player player) {
        if (Bukkit.getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                // 使用MHDF-Tools的fly命令取消飞行权限
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "fly " + player.getName() + " false");
                
                // 确保完全移除权限 - 尝试清除可能存在的任何临时权限
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "lp user " + player.getName() + " permission unset mhdtools.commands.fly.temp");
                
                plugin.getLogger().info("已禁用玩家 " + player.getName() + " 的MHDF-Tools飞行权限");
            } catch (Exception e) {
                plugin.getLogger().warning("同步取消MHDF-Tools飞行权限失败: " + e.getMessage());
            }
        }
    }

    private String formatTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(TIME_FORMATTER);
    }
} 