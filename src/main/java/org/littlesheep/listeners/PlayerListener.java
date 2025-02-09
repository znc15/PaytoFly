package org.littlesheep.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.littlesheep.data.Storage;
import org.littlesheep.paytofly;

public class PlayerListener implements Listener {
    private final Storage storage;

    public PlayerListener(paytofly plugin, Storage storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("paytofly.infinite")) {
            Long endTime = storage.getPlayerFlightTime(player.getUniqueId());
            if (endTime == null || endTime < System.currentTimeMillis()) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }
} 