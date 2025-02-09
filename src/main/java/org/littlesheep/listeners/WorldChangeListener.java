package org.littlesheep.listeners;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
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
            // 在禁飞世界中，如果配置了禁用无限飞行，则忽略无限飞行权限
            if (disableInfiniteInRestricted || !player.hasPermission("paytofly.infinite")) {
                player.setAllowFlight(false);
                player.setFlying(false);
                if (notifyOnEnter) {
                    player.sendMessage(plugin.getPrefix() + "§c这个世界禁止飞行！");
                }
            }
            return;
        }

        // 检查是否是免费飞行世界
        if (freeWorlds.contains(worldName)) {
            player.setAllowFlight(true);
            return;
        }

        // 其他世界检查权限
        if (infiniteEnabled && player.hasPermission("paytofly.infinite")) {
            player.setAllowFlight(true);
            return;
        }

        // 检查购买状态
        Long endTime = plugin.getStorage().getPlayerFlightTime(player.getUniqueId());
        if (endTime == null || endTime < System.currentTimeMillis()) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }
} 