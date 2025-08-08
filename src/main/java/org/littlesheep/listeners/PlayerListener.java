package org.littlesheep.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.GameMode;
import org.littlesheep.data.Storage;
import org.littlesheep.paytofly;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.Bukkit;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final Storage storage;
    private final paytofly plugin;

    public PlayerListener(paytofly plugin, Storage storage) {
        this.storage = storage;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 初始化玩家的默认特效和速度
        initializePlayerDefaults(player);
        
        // 先检查创造模式飞行权限
        boolean canFlyInCreative = checkCreativeFlight(player);

        // 如果是创造模式,立即发送飞行状态消息
        if (player.getGameMode() == GameMode.CREATIVE) {
            if (canFlyInCreative) {
                player.sendMessage(plugin.getPrefix() + plugin.getLang("messages.creative.flight-enabled"));
            } else {
                player.sendMessage(plugin.getPrefix() + plugin.getLang("messages.creative.flight-disabled"));
            }
        } else {
            // 如果不是创造模式,再检查飞行时间  
            if (player.hasPermission("paytofly.infinite")) {
                // 有无限飞行权限
                player.setAllowFlight(true);
            } else {
                // 检查购买的飞行时间
                Long endTime = storage.getPlayerFlightTime(player.getUniqueId());
                if (endTime != null && endTime > System.currentTimeMillis()) {
                    // 有有效的飞行时间
                    player.setAllowFlight(true);
                    // 启动倒计时
                    plugin.getCountdownManager().startCountdown(player, endTime);
                } else {
                    // 没有飞行权限
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }
    }

    /**
     * 初始化玩家的默认特效和速度设置
     */
    private void initializePlayerDefaults(Player player) {
        // 确保特效管理器已初始化
        if (plugin.getEffectManager() != null) {
            // 从存储中加载玩家已购买的特效
            restorePlayerEffect(player);
        }
        
        // 确保速度管理器已初始化
        if (plugin.getSpeedManager() != null) {
            // 从存储中加载玩家已购买的速度
            restorePlayerSpeed(player);
        }
    }

    /**
     * 恢复玩家的特效设置
     */
    private void restorePlayerEffect(Player player) {
        // 获取玩家永久购买的特效
        var purchasedEffects = storage.getPlayerEffects(player.getUniqueId());
        // 获取玩家时间限制购买的特效
        var effectTimes = storage.getPlayerEffectTimes(player.getUniqueId());
        
        var bestEffect = org.littlesheep.effects.FlightEffectManager.FlightEffectType.BASIC;
        
        // 检查永久购买的特效
        for (String effectName : purchasedEffects) {
            var effectType = org.littlesheep.effects.FlightEffectManager.FlightEffectType.fromString(effectName);
            if (effectType.getLevel() > bestEffect.getLevel()) {
                bestEffect = effectType;
            }
        }
        
        // 检查时间限制购买的特效（只考虑未过期的）
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : effectTimes.entrySet()) {
            if (entry.getValue() > currentTime) {
                var effectType = org.littlesheep.effects.FlightEffectManager.FlightEffectType.fromString(entry.getKey());
                if (effectType.getLevel() > bestEffect.getLevel()) {
                    bestEffect = effectType;
                }
            } else {
                // 清理过期的特效
                storage.removePlayerEffectTime(player.getUniqueId(), entry.getKey());
            }
        }
        
        plugin.getEffectManager().setPlayerEffect(player, bestEffect);
        plugin.getLogger().info("玩家 " + player.getName() + " 恢复特效: " + bestEffect.getName());
    }

    /**
     * 恢复玩家的速度设置
     */
    private void restorePlayerSpeed(Player player) {
        // 获取玩家永久购买的速度
        var purchasedSpeeds = storage.getPlayerSpeeds(player.getUniqueId());
        // 获取玩家时间限制购买的速度
        var speedTimes = storage.getPlayerSpeedTimes(player.getUniqueId());
        
        var bestSpeed = org.littlesheep.speed.FlightSpeedManager.FlightSpeedLevel.NORMAL;
        
        // 检查永久购买的速度
        for (String speedName : purchasedSpeeds) {
            var speedLevel = org.littlesheep.speed.FlightSpeedManager.FlightSpeedLevel.fromString(speedName);
            if (speedLevel.getLevel() > bestSpeed.getLevel()) {
                bestSpeed = speedLevel;
            }
        }
        
        // 检查时间限制购买的速度（只考虑未过期的）
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : speedTimes.entrySet()) {
            if (entry.getValue() > currentTime) {
                var speedLevel = org.littlesheep.speed.FlightSpeedManager.FlightSpeedLevel.fromString(entry.getKey());
                if (speedLevel.getLevel() > bestSpeed.getLevel()) {
                    bestSpeed = speedLevel;
                }
            } else {
                // 清理过期的速度
                storage.removePlayerSpeedTime(player.getUniqueId(), entry.getKey());
            }
        }
        
        plugin.getSpeedManager().setDefaultPlayerSpeed(player, bestSpeed);
        plugin.getLogger().info("玩家 " + player.getName() + " 恢复速度: " + bestSpeed.getDisplayName());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        
        if (event.getNewGameMode() == GameMode.CREATIVE) {
            // 检查创造模式飞行权限
            boolean canFlyInCreative = checkCreativeFlight(player);
            if (canFlyInCreative) {
                player.sendMessage(plugin.getPrefix() + plugin.getLang("messages.creative.flight-enabled"));
            } else {
                player.sendMessage(plugin.getPrefix() + plugin.getLang("messages.creative.flight-disabled"));
            }
        } else {
            // 如果切换到其他模式,检查飞行时间或无限飞行权限
            if (!player.hasPermission("paytofly.infinite")) {
                Long endTime = storage.getPlayerFlightTime(player.getUniqueId());
                if (endTime == null || endTime < System.currentTimeMillis()) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage(plugin.getPrefix() + plugin.getLang("messages.survival.flight-disabled"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        
        // 检查玩家是否有有效的飞行时间
        Long endTime = plugin.getFlyingPlayers().get(playerId);
        if (endTime != null && endTime > System.currentTimeMillis()) {
            // 在玩家重生时恢复飞行状态
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setAllowFlight(true);
                player.setFlying(true);
            }, 1L);
        }
    }

    // 检查创造模式飞行权限的方法,返回布尔值表示是否允许飞行
    private boolean checkCreativeFlight(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            boolean allowCreativeFlight = plugin.getConfig().getBoolean("settings.creative.allow-flight", true);
            String bypassPermission = plugin.getConfig().getString("settings.creative.bypass-permission", "paytofly.creative.bypass");
            
            if (allowCreativeFlight) {
                player.setAllowFlight(true);
                return true;
            } else if (player.hasPermission(bypassPermission)) {
                player.setAllowFlight(true);
                return true;
            } else {
                player.setAllowFlight(false);
                player.setFlying(false);
                return false;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        
        // 忽略创造模式玩家
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        // 检查是否有飞行权限或无限权限
        Long endTime = storage.getPlayerFlightTime(player.getUniqueId());
        boolean hasInfinitePermission = player.hasPermission("paytofly.infinite");
        boolean hasPurchasedTime = endTime != null && endTime > System.currentTimeMillis();
        boolean hasFlightAccess = hasInfinitePermission || hasPurchasedTime;
        
        // 处理飞行状态变化
        if (event.isFlying()) {
            // 玩家尝试开始飞行
            if (hasFlightAccess) {
                // 玩家开始飞行 - 启动特效和设置速度
                if (plugin.getEffectManager() != null) {
                    var effectType = plugin.getEffectManager().getPlayerEffect(player);
                    plugin.getLogger().info("玩家 " + player.getName() + " 开始飞行，特效类型: " + effectType.getName());
                    plugin.getEffectManager().startFlightEffect(player, effectType);
                }
                
                if (plugin.getSpeedManager() != null) {
                    var speedLevel = plugin.getSpeedManager().getPlayerSpeed(player);
                    plugin.getLogger().info("玩家 " + player.getName() + " 设置飞行速度: " + speedLevel.getDisplayName());
                    plugin.getSpeedManager().setFlightSpeed(player, speedLevel);
                }
            } else {
                // 没有飞行权限，取消飞行并提示
                event.setCancelled(true);
                player.setFlying(false);
                player.setAllowFlight(false);
                player.sendMessage(plugin.getPrefix() + plugin.getLang("messages.no-permission"));
            }
        } else {
            // 玩家尝试停止飞行或者尝试激活飞行但当前没有allowFlight权限
            if (!player.getAllowFlight() && hasFlightAccess) {
                // 玩家有权限但allowFlight为false，这通常是双击跳跃尝试激活飞行
                // 取消当前事件，然后启用飞行权限并手动开始飞行
                event.setCancelled(true);
                
                // 延迟1tick设置飞行权限和状态，确保事件处理完成
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    
                    // 启动特效和设置速度
                    if (plugin.getEffectManager() != null) {
                        var effectType = plugin.getEffectManager().getPlayerEffect(player);
                        plugin.getLogger().info("玩家 " + player.getName() + " 激活飞行，特效类型: " + effectType.getName());
                        plugin.getEffectManager().startFlightEffect(player, effectType);
                    }
                    
                    if (plugin.getSpeedManager() != null) {
                        var speedLevel = plugin.getSpeedManager().getPlayerSpeed(player);
                        plugin.getLogger().info("玩家 " + player.getName() + " 设置飞行速度: " + speedLevel.getDisplayName());
                        plugin.getSpeedManager().setFlightSpeed(player, speedLevel);
                    }
                }, 1L);
            } else if (player.isFlying()) {
                // 玩家正常停止飞行 - 停止特效
                if (plugin.getEffectManager() != null) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 停止飞行，停止特效");
                    plugin.getEffectManager().stopFlightEffect(player);
                }
            }
        }
    }
} 