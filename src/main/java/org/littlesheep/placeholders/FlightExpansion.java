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

        // 优先从存储中获取飞行结束时间
        Long endTime = plugin.getStorage().getPlayerFlightTime(player.getUniqueId());
        if (endTime == null) {
            // 如果存储中没有，检查内存中的数据（兼容旧版本）
            endTime = plugin.getFlyingPlayers().get(player.getUniqueId());
        }
        
        // 检查无限权限
        boolean hasInfinitePermission = player.hasPermission("paytofly.infinite");
        
        if (params.equals("remaining")) {
            if (hasInfinitePermission) {
                return plugin.getLang().getMessage("time-format.infinite", "无限");
            }
            if (endTime == null || endTime < System.currentTimeMillis()) {
                return plugin.getLang().getMessage("time-format.expired", "已过期");
            }
            return TimeFormatter.formatTime(endTime - System.currentTimeMillis());
        }

        if (params.equals("status")) {
            boolean hasFlightEnabled = hasInfinitePermission || (endTime != null && endTime > System.currentTimeMillis());
            return hasFlightEnabled ? 
                   plugin.getLang().getMessage("status.enabled", "已启用") : 
                   plugin.getLang().getMessage("status.disabled", "已禁用");
        }
        
        if (params.equals("expiretime")) {
            if (hasInfinitePermission) {
                return plugin.getLang().getMessage("time-format.infinite", "无限");
            }
            if (endTime == null || endTime < System.currentTimeMillis()) {
                return plugin.getLang().getMessage("time-format.expired", "已过期");
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.format(new Date(endTime));
        }
        
        if (params.equals("mode")) {
            // 获取计时模式，这里假设是从配置中获取
            String timeMode = plugin.getConfig().getString("time-mode", "real");
            return timeMode.equalsIgnoreCase("real") ? 
                   plugin.getLang().getMessage("time-mode.real", "真实时间") : 
                   plugin.getLang().getMessage("time-mode.game", "游戏时间");
        }
        
        // ========== 特效相关 placeholder ==========
        if (params.equals("effect")) {
            if (plugin.getEffectManager() != null) {
                var currentEffect = plugin.getEffectManager().getPlayerEffect(player);
                // FlightEffectType没有getDisplayName方法，需要手动映射
                return getEffectDisplayName(currentEffect.getName());
            }
            return "未知";
        }
        
        if (params.equals("effect_name")) {
            if (plugin.getEffectManager() != null) {
                var currentEffect = plugin.getEffectManager().getPlayerEffect(player);
                return currentEffect.getName();
            }
            return "basic";
        }
        
        if (params.equals("effect_level")) {
            if (plugin.getEffectManager() != null) {
                var currentEffect = plugin.getEffectManager().getPlayerEffect(player);
                return String.valueOf(currentEffect.getLevel());
            }
            return "1";
        }
        
        // ========== 速度相关 placeholder ==========
        if (params.equals("speed")) {
            if (plugin.getSpeedManager() != null) {
                var currentSpeed = plugin.getSpeedManager().getPlayerSpeed(player);
                return currentSpeed.getDisplayName();
            }
            return "未知";
        }
        
        if (params.equals("speed_name")) {
            if (plugin.getSpeedManager() != null) {
                var currentSpeed = plugin.getSpeedManager().getPlayerSpeed(player);
                return currentSpeed.getName();
            }
            return "normal";
        }
        
        if (params.equals("speed_level")) {
            if (plugin.getSpeedManager() != null) {
                var currentSpeed = plugin.getSpeedManager().getPlayerSpeed(player);
                return String.valueOf(currentSpeed.getLevel());
            }
            return "2";
        }
        
        if (params.equals("speed_value")) {
            if (plugin.getSpeedManager() != null) {
                var currentSpeed = plugin.getSpeedManager().getPlayerSpeed(player);
                return String.valueOf(currentSpeed.getSpeed());
            }
            return "0.1";
        }
        
        // ========== 统计相关 placeholder ==========
        if (params.equals("flight_active")) {
            return player.isFlying() ? "是" : "否";
        }
        
        if (params.equals("allow_flight")) {
            return player.getAllowFlight() ? "是" : "否";
        }
        
        // ========== 购买状态相关 ==========
        if (params.startsWith("has_effect_")) {
            String effectName = params.substring("has_effect_".length());
            boolean hasEffect = hasEffectAccess(player, effectName);
            return hasEffect ? "是" : "否";
        }
        
        if (params.startsWith("has_speed_")) {
            String speedName = params.substring("has_speed_".length());
            boolean hasSpeed = hasSpeedAccess(player, speedName);
            return hasSpeed ? "是" : "否";
        }

        return null;
    }
    
    /**
     * 检查玩家是否有特效访问权限（永久或时间限制）
     */
    private boolean hasEffectAccess(Player player, String effectName) {
        // 检查权限
        if (player.hasPermission("paytofly.effects." + effectName) || 
            player.hasPermission("paytofly.effects.*")) {
            return true;
        }
        
        // 检查永久购买
        if (plugin.getStorage().getPlayerEffects(player.getUniqueId()).contains(effectName)) {
            return true;
        }
        
        // 检查时间限制购买
        Long effectTime = plugin.getStorage().getPlayerEffectTime(player.getUniqueId(), effectName);
        return effectTime != null && effectTime > System.currentTimeMillis();
    }
    
    /**
     * 检查玩家是否有速度访问权限（永久或时间限制）
     */
    private boolean hasSpeedAccess(Player player, String speedName) {
        // 免费速度
        if ("normal".equals(speedName) || "slow".equals(speedName)) {
            return true;
        }
        
        // 检查权限
        if (player.hasPermission("paytofly.speed." + speedName) || 
            player.hasPermission("paytofly.speed.*")) {
            return true;
        }
        
        // 检查永久购买
        if (plugin.getStorage().getPlayerSpeeds(player.getUniqueId()).contains(speedName)) {
            return true;
        }
        
        // 检查时间限制购买
        Long speedTime = plugin.getStorage().getPlayerSpeedTime(player.getUniqueId(), speedName);
        return speedTime != null && speedTime > System.currentTimeMillis();
    }
    
    /**
     * 获取特效显示名称
     */
    private String getEffectDisplayName(String effectName) {
        // 简单的映射，与GUI类中的方法保持一致
        switch (effectName.toLowerCase()) {
            case "basic": return "基础特效";
            case "rainbow": return "彩虹特效";
            case "star": return "星星特效";
            case "fire": return "火焰特效";
            case "magic": return "魔法特效";
            case "dragon": return "龙息特效";
            default: return effectName;
        }
    }
} 