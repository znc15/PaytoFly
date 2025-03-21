package org.littlesheep.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.littlesheep.paytofly;
import org.littlesheep.utils.TimeFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FlightExpansion extends PlaceholderExpansion {
    private final paytofly plugin;

    public FlightExpansion(paytofly plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "paytofly";
    }

    @Override
    public String getAuthor() {
        return "LittleSheep";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }

        // 获取玩家飞行结束时间
        Long endTime = plugin.getFlyingPlayers().get(player.getUniqueId());
        
        if (params.equals("remaining")) {
            if (endTime == null || endTime < System.currentTimeMillis()) {
                return plugin.getLang("time-format.expired");
            }
            return TimeFormatter.formatTime(endTime - System.currentTimeMillis());
        }

        if (params.equals("status")) {
            boolean hasFlightEnabled = (endTime != null && endTime > System.currentTimeMillis());
            return hasFlightEnabled ? plugin.getLang("status.enabled") : plugin.getLang("status.disabled");
        }
        
        if (params.equals("expiretime")) {
            if (endTime == null || endTime < System.currentTimeMillis()) {
                return plugin.getLang("time-format.expired");
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.format(new Date(endTime));
        }
        
        if (params.equals("mode")) {
            // 获取计时模式，这里假设是从配置中获取
            String timeMode = plugin.getConfig().getString("time-mode", "real");
            return timeMode.equalsIgnoreCase("real") ? 
                   plugin.getLang("time-mode.real") : 
                   plugin.getLang("time-mode.game");
        }

        return null;
    }
} 