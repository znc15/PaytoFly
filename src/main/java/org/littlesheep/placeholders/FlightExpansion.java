package org.littlesheep.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.littlesheep.paytofly;
import org.littlesheep.utils.TimeFormatter;

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

        if (params.equals("time_left")) {
            Long endTime = plugin.getFlyingPlayers().get(player.getUniqueId());
            if (endTime == null || endTime < System.currentTimeMillis()) {
                return plugin.getLang().getMessage("time-format.expired");
            }
            return TimeFormatter.formatTime(endTime - System.currentTimeMillis());
        }

        if (params.equals("has_flight")) {
            Long endTime = plugin.getFlyingPlayers().get(player.getUniqueId());
            return (endTime != null && endTime > System.currentTimeMillis()) ? "true" : "false";
        }

        return null;
    }
} 