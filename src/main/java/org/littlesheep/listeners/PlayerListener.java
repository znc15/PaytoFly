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
import org.bukkit.Bukkit;
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
            if (!player.hasPermission("paytofly.infinite")) {
                Long endTime = storage.getPlayerFlightTime(player.getUniqueId());
                if (endTime == null || endTime < System.currentTimeMillis()) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }
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
} 