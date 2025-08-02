package org.littlesheep.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.littlesheep.paytofly;

import java.util.List;

public class WorldChangeListener implements Listener {
    private final paytofly plugin;

    public WorldChangeListener(paytofly plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();

        // 获取配置
        List<String> freeWorlds = plugin.getConfig().getStringList("worlds.free-fly");
        List<String> disabledWorlds = plugin.getConfig().getStringList("worlds.disabled");
        boolean notifyOnEnter = plugin.getConfig().getBoolean("worlds.notify-on-enter", true);
        boolean infiniteEnabled = plugin.getConfig().getBoolean("settings.infinite-flight", true);
        boolean disableInfiniteInRestricted = plugin.getConfig().getBoolean("settings.disable-infinite-in-restricted", true);

        // 检查是否是禁飞世界
        if (disabledWorlds.contains(worldName)) {
            if (disableInfiniteInRestricted || !player.hasPermission("paytofly.infinite")) {
                player.setAllowFlight(false);
                player.setFlying(false);
                
                // 同步取消MHDF-Tools飞行权限
                syncMHDFToolsDisableFlight(player);
                
                if (notifyOnEnter) {
                    player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("world-flight-disabled"));
                }
            }
            return;
        }

        // 检查是否是免费飞行世界
        if (freeWorlds.contains(worldName)) {
            player.setAllowFlight(true);
            
            // 同步MHDF-Tools飞行权限 (免费世界无限时长)
            syncMHDFToolsEnableFlight(player);
            
            if (notifyOnEnter) {
                player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("world-free-flight"));
            }
            return;
        }

        // 检查无限飞行权限
        if (infiniteEnabled && player.hasPermission("paytofly.infinite")) {
            player.setAllowFlight(true);
            
            // 同步MHDF-Tools飞行权限 (无限权限)
            syncMHDFToolsEnableFlight(player);
            
            return;
        }

        // 检查购买状态
        Long endTime = plugin.getStorage().getPlayerFlightTime(player.getUniqueId());
        if (endTime != null && endTime > System.currentTimeMillis()) {
            player.setAllowFlight(true);
            
            // 同步MHDF-Tools飞行权限
            syncMHDFToolsFlight(player, endTime);
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
            
            // 同步取消MHDF-Tools飞行权限
            syncMHDFToolsDisableFlight(player);
        }
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
            } catch (Exception e) {
                plugin.getLogger().warning("同步MHDF-Tools飞行权限失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 同步启用MHDF-Tools无限飞行权限
     * @param player 玩家
     */
    private void syncMHDFToolsEnableFlight(Player player) {
        if (Bukkit.getPluginManager().getPlugin("MHDF-Tools") != null) {
            try {
                // 使用MHDF-Tools的fly命令启用无限飞行权限
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "fly " + player.getName() + " true");
            } catch (Exception e) {
                plugin.getLogger().warning("同步启用MHDF-Tools飞行权限失败: " + e.getMessage());
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
            } catch (Exception e) {
                plugin.getLogger().warning("同步取消MHDF-Tools飞行权限失败: " + e.getMessage());
            }
        }
    }
    
    // 添加死亡事件监听
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        
        // 延迟一秒检查飞行状态，避免死亡后立即取消飞行
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 检查玩家飞行权限
            Long endTime = plugin.getStorage().getPlayerFlightTime(player.getUniqueId());
            if (endTime != null && endTime > System.currentTimeMillis()) {
                player.setAllowFlight(true);
                
                // 同步MHDF-Tools飞行权限
                syncMHDFToolsFlight(player, endTime);
            }
        }, 20L); // 20 ticks = 1 second
    }
    
    // 添加传送事件监听
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        final World fromWorld = event.getFrom().getWorld();
        final World toWorld = event.getTo().getWorld();
        
        // 如果是跨世界传送，WorldChangeListener已经处理，不需要重复处理
        if (fromWorld != null && toWorld != null && !fromWorld.equals(toWorld)) {
            return;
        }
        
        // 延迟几tick检查飞行状态，确保传送完成
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 如果玩家已经离线，跳过处理
            if (!player.isOnline()) {
                return;
            }
            
            checkAndRestoreFlightStatus(player);
        }, 3L); // 3 ticks = 0.15 seconds
    }
    
    /**
     * 检查并恢复玩家的飞行状态
     * @param player 玩家
     */
    private void checkAndRestoreFlightStatus(Player player) {
        World world = player.getWorld();
        String worldName = world.getName();
        
        // 获取配置
        List<String> freeWorlds = plugin.getConfig().getStringList("worlds.free-fly");
        List<String> disabledWorlds = plugin.getConfig().getStringList("worlds.disabled");
        boolean infiniteEnabled = plugin.getConfig().getBoolean("settings.infinite-flight", true);
        boolean disableInfiniteInRestricted = plugin.getConfig().getBoolean("settings.disable-infinite-in-restricted", true);
        
        // 检查是否是禁飞世界
        if (disabledWorlds.contains(worldName)) {
            if (disableInfiniteInRestricted || !player.hasPermission("paytofly.infinite")) {
                player.setAllowFlight(false);
                player.setFlying(false);
                syncMHDFToolsDisableFlight(player);
            }
            return;
        }
        
        // 检查是否是免费飞行世界
        if (freeWorlds.contains(worldName)) {
            player.setAllowFlight(true);
            syncMHDFToolsEnableFlight(player);
            return;
        }
        
        // 检查无限飞行权限
        if (infiniteEnabled && player.hasPermission("paytofly.infinite")) {
            player.setAllowFlight(true);
            syncMHDFToolsEnableFlight(player);
            return;
        }
        
        // 检查购买状态
        Long endTime = plugin.getStorage().getPlayerFlightTime(player.getUniqueId());
        if (endTime != null && endTime > System.currentTimeMillis()) {
            player.setAllowFlight(true);
            syncMHDFToolsFlight(player, endTime);
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
            syncMHDFToolsDisableFlight(player);
        }
    }
} 