package org.littlesheep.utils;

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
                statusChanged = true;
            }
        }

        return statusChanged;
    }

    private String formatTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(TIME_FORMATTER);
    }
} 