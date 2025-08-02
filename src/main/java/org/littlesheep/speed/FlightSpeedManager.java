package org.littlesheep.speed;

import org.bukkit.entity.Player;
import org.littlesheep.paytofly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 飞行速度管理器 - 管理玩家的飞行速度等级
 * 
 * 功能包括：
 * - 多等级飞行速度控制
 * - 权限检查和验证
 * - 速度等级管理
 * - 平滑速度过渡
 */
public class FlightSpeedManager {
    
    private final paytofly plugin;
    private final Map<Player, FlightSpeedLevel> playerSpeeds;
    
    public FlightSpeedManager(paytofly plugin) {
        this.plugin = plugin;
        this.playerSpeeds = new ConcurrentHashMap<>();
    }
    
    /**
     * 飞行速度等级枚举
     */
    public enum FlightSpeedLevel {
        SLOW("slow", 1, 0.05f, "§7缓慢"),
        NORMAL("normal", 2, 0.1f, "§f普通"),
        FAST("fast", 3, 0.2f, "§a快速"),
        VERY_FAST("very_fast", 4, 0.3f, "§e极速"),
        SUPER_FAST("super_fast", 5, 0.5f, "§6超速"),
        LIGHT_SPEED("light_speed", 6, 0.8f, "§b光速"),
        WARP_SPEED("warp_speed", 7, 1.0f, "§d曲速");
        
        private final String name;
        private final int level;
        private final float speed;
        private final String displayName;
        
        FlightSpeedLevel(String name, int level, float speed, String displayName) {
            this.name = name;
            this.level = level;
            this.speed = speed;
            this.displayName = displayName;
        }
        
        public String getName() { return name; }
        public int getLevel() { return level; }
        public float getSpeed() { return speed; }
        public String getDisplayName() { return displayName; }
        
        public static FlightSpeedLevel fromString(String name) {
            for (FlightSpeedLevel level : values()) {
                if (level.name.equalsIgnoreCase(name)) {
                    return level;
                }
            }
            return NORMAL;
        }
        
        public static FlightSpeedLevel fromLevel(int level) {
            for (FlightSpeedLevel speedLevel : values()) {
                if (speedLevel.level == level) {
                    return speedLevel;
                }
            }
            return NORMAL;
        }
        
        public static FlightSpeedLevel fromSpeed(float speed) {
            FlightSpeedLevel closest = NORMAL;
            float closestDiff = Math.abs(NORMAL.speed - speed);
            
            for (FlightSpeedLevel level : values()) {
                float diff = Math.abs(level.speed - speed);
                if (diff < closestDiff) {
                    closest = level;
                    closestDiff = diff;
                }
            }
            return closest;
        }
    }
    
    /**
     * 设置玩家的飞行速度
     */
    public boolean setFlightSpeed(Player player, FlightSpeedLevel speedLevel) {
        if (!plugin.getConfig().getBoolean("flight-speed.enabled", true)) {
            return false;
        }
        
        // 检查权限
        if (!hasSpeedPermission(player, speedLevel)) {
            player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("speed-no-permission", 
                "{speed}", speedLevel.getDisplayName()));
            return false;
        }
        
        // 检查是否在飞行
        if (!player.isFlying() && !player.getAllowFlight()) {
            player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("speed-not-flying"));
            return false;
        }
        
        // 应用速度
        float targetSpeed = speedLevel.getSpeed();
        
        // 检查配置中的速度限制
        float maxSpeed = (float) plugin.getConfig().getDouble("flight-speed.max-speed", 1.0);
        if (targetSpeed > maxSpeed) {
            targetSpeed = maxSpeed;
        }
        
        // 平滑过渡到新速度
        smoothSpeedTransition(player, targetSpeed);
        
        // 记录玩家速度
        playerSpeeds.put(player, speedLevel);
        
        // 发送成功消息
        player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("speed-changed", 
            "{speed}", speedLevel.getDisplayName(),
            "{value}", String.format("%.1f", targetSpeed)));
        
        plugin.getLogger().info(plugin.getLang().getMessage("speed-set-log", 
            "{player}", player.getName(),
            "{speed}", speedLevel.getName(),
            "{value}", String.valueOf(targetSpeed)));
        
        return true;
    }
    
    /**
     * 平滑速度过渡
     */
    private void smoothSpeedTransition(Player player, float targetSpeed) {
        float currentSpeed = player.getFlySpeed();
        
        // 如果速度差异很小，直接设置
        if (Math.abs(currentSpeed - targetSpeed) < 0.05f) {
            player.setFlySpeed(targetSpeed);
            return;
        }
        
        // 计算过渡步数
        int steps = 5;
        float speedDiff = targetSpeed - currentSpeed;
        float stepSize = speedDiff / steps;
        
        // 异步执行平滑过渡
        new Thread(() -> {
            for (int i = 1; i <= steps; i++) {
                float newSpeed = currentSpeed + (stepSize * i);
                
                // 同步设置速度
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.setFlySpeed(newSpeed);
                    }
                });
                
                try {
                    Thread.sleep(50); // 50ms间隔
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
    
    /**
     * 获取玩家当前的速度等级
     */
    public FlightSpeedLevel getPlayerSpeed(Player player) {
        return playerSpeeds.getOrDefault(player, FlightSpeedLevel.NORMAL);
    }
    
    /**
     * 重置玩家速度到默认值
     */
    public void resetPlayerSpeed(Player player) {
        FlightSpeedLevel defaultSpeed = FlightSpeedLevel.fromString(
            plugin.getConfig().getString("flight-speed.default-speed", "normal"));
        
        setFlightSpeed(player, defaultSpeed);
    }
    
    /**
     * 检查玩家是否有特定速度等级的权限（权限或购买）
     */
    public boolean hasSpeedPermission(Player player, FlightSpeedLevel speedLevel) {
        // 默认免费速度
        if (speedLevel == FlightSpeedLevel.SLOW || speedLevel == FlightSpeedLevel.NORMAL) {
            return true;
        }
        
        // 管理员权限
        if (player.hasPermission("paytofly.speed.*")) {
            return true;
        }
        
        // 特定速度权限
        String permission = "paytofly.speed." + speedLevel.getName();
        if (player.hasPermission(permission)) {
            return true;
        }
        
        // 基于等级的权限检查
        String levelPermission = "paytofly.speed.level." + speedLevel.getLevel();
        if (player.hasPermission(levelPermission)) {
            return true;
        }
        
        // 检查最大允许等级
        for (int level = speedLevel.getLevel(); level >= 1; level--) {
            if (player.hasPermission("paytofly.speed.max." + level)) {
                return level >= speedLevel.getLevel();
            }
        }
        
        // 检查是否已购买
        if (plugin.getConfig().getBoolean("flight-speed.purchase.enabled", true)) {
            return plugin.getStorage().getPlayerSpeeds(player.getUniqueId()).contains(speedLevel.getName());
        }
        
        return false;
    }
    
    /**
     * 购买速度等级
     */
    public boolean purchaseSpeed(Player player, FlightSpeedLevel speedLevel) {
        if (!plugin.getConfig().getBoolean("flight-speed.purchase.enabled", true)) {
            return false;
        }
        
        if (speedLevel == FlightSpeedLevel.SLOW || speedLevel == FlightSpeedLevel.NORMAL) {
            return true; // 免费速度
        }
        
        // 检查是否已拥有
        if (hasSpeedPermission(player, speedLevel)) {
            return false; // 已拥有
        }
        
        // 获取价格
        double price = plugin.getConfig().getDouble("flight-speed.purchase.prices." + speedLevel.getName(), 0.0);
        if (price <= 0) {
            return false; // 价格无效
        }
        
        // 检查余额
        if (plugin.getEconomyManager() != null && plugin.getEconomyManager().isInitialized()) {
            if (!plugin.getEconomyManager().hasBalance(player, price)) {
                return false; // 余额不足
            }
            
            // 扣款
            if (plugin.getEconomyManager().withdraw(player, price)) {
                // 添加购买记录
                plugin.getStorage().addPlayerSpeed(player.getUniqueId(), speedLevel.getName());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取速度价格
     */
    public double getSpeedPrice(FlightSpeedLevel speedLevel) {
        if (speedLevel == FlightSpeedLevel.SLOW || speedLevel == FlightSpeedLevel.NORMAL) {
            return 0.0;
        }
        return plugin.getConfig().getDouble("flight-speed.purchase.prices." + speedLevel.getName(), 0.0);
    }
    
    /**
     * 获取玩家可用的速度等级列表
     */
    public List<FlightSpeedLevel> getAvailableSpeeds(Player player) {
        List<FlightSpeedLevel> available = new ArrayList<>();
        
        for (FlightSpeedLevel level : FlightSpeedLevel.values()) {
            if (hasSpeedPermission(player, level)) {
                available.add(level);
            }
        }
        
        return available;
    }
    
    /**
     * 获取玩家的最大允许速度等级
     */
    public FlightSpeedLevel getMaxAllowedSpeed(Player player) {
        FlightSpeedLevel maxSpeed = FlightSpeedLevel.NORMAL;
        
        for (FlightSpeedLevel level : FlightSpeedLevel.values()) {
            if (hasSpeedPermission(player, level) && level.getLevel() > maxSpeed.getLevel()) {
                maxSpeed = level;
            }
        }
        
        return maxSpeed;
    }
    
    /**
     * 升级玩家速度（如果有权限）
     */
    public boolean upgradeSpeed(Player player) {
        FlightSpeedLevel currentSpeed = getPlayerSpeed(player);
        List<FlightSpeedLevel> availableSpeeds = getAvailableSpeeds(player);
        
        // 找到下一个更高的速度等级
        FlightSpeedLevel nextSpeed = null;
        for (FlightSpeedLevel level : availableSpeeds) {
            if (level.getLevel() > currentSpeed.getLevel()) {
                if (nextSpeed == null || level.getLevel() < nextSpeed.getLevel()) {
                    nextSpeed = level;
                }
            }
        }
        
        if (nextSpeed != null) {
            return setFlightSpeed(player, nextSpeed);
        } else {
            player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("speed-max-reached"));
            return false;
        }
    }
    
    /**
     * 降级玩家速度
     */
    public boolean downgradeSpeed(Player player) {
        FlightSpeedLevel currentSpeed = getPlayerSpeed(player);
        List<FlightSpeedLevel> availableSpeeds = getAvailableSpeeds(player);
        
        // 找到下一个更低的速度等级
        FlightSpeedLevel nextSpeed = null;
        for (FlightSpeedLevel level : availableSpeeds) {
            if (level.getLevel() < currentSpeed.getLevel()) {
                if (nextSpeed == null || level.getLevel() > nextSpeed.getLevel()) {
                    nextSpeed = level;
                }
            }
        }
        
        if (nextSpeed != null) {
            return setFlightSpeed(player, nextSpeed);
        } else {
            player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("speed-min-reached"));
            return false;
        }
    }
    
    /**
     * 自动调整速度（根据飞行时间等因素）
     */
    public void autoAdjustSpeed(Player player) {
        if (!plugin.getConfig().getBoolean("flight-speed.auto-adjust.enabled", false)) {
            return;
        }
        
        // 获取玩家的飞行时间
        Long endTime = plugin.getStorage().getPlayerFlightTime(player.getUniqueId());
        if (endTime == null) {
            return;
        }
        
        long remainingTime = endTime - System.currentTimeMillis();
        long totalTime = remainingTime; // 这里可以添加更复杂的逻辑来计算总时间
        
        // 根据剩余时间百分比调整速度
        double timePercentage = (double) remainingTime / totalTime;
        
        FlightSpeedLevel targetSpeed;
        if (timePercentage > 0.8) {
            targetSpeed = getMaxAllowedSpeed(player);
        } else if (timePercentage > 0.5) {
            targetSpeed = FlightSpeedLevel.FAST;
        } else if (timePercentage > 0.2) {
            targetSpeed = FlightSpeedLevel.NORMAL;
        } else {
            targetSpeed = FlightSpeedLevel.SLOW;
        }
        
        // 检查权限并设置速度
        if (hasSpeedPermission(player, targetSpeed)) {
            setFlightSpeed(player, targetSpeed);
        }
    }
    
    /**
     * 清理玩家数据
     */
    public void removePlayer(Player player) {
        playerSpeeds.remove(player);
    }
    
    /**
     * 清理所有资源
     */
    public void cleanup() {
        playerSpeeds.clear();
        plugin.getLogger().info(plugin.getLang().getMessage("speed-cleanup"));
    }
    
    /**
     * 获取速度等级信息字符串
     */
    public String getSpeedInfo(Player player) {
        FlightSpeedLevel currentSpeed = getPlayerSpeed(player);
        List<FlightSpeedLevel> availableSpeeds = getAvailableSpeeds(player);
        
        StringBuilder info = new StringBuilder();
        info.append(plugin.getLang().getMessage("speed-current", 
            "{speed}", currentSpeed.getDisplayName())).append("\n");
        
        info.append(plugin.getLang().getMessage("speed-available")).append(" ");
        for (int i = 0; i < availableSpeeds.size(); i++) {
            FlightSpeedLevel level = availableSpeeds.get(i);
            info.append(level.getDisplayName());
            if (i < availableSpeeds.size() - 1) {
                info.append("§7, ");
            }
        }
        
        return info.toString();
    }
}