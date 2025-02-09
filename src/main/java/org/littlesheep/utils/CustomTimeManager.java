package org.littlesheep.utils;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.littlesheep.paytofly;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CustomTimeManager implements Listener {
    private final paytofly plugin;
    private final Set<UUID> waitingForInput = new HashSet<>();
    
    public CustomTimeManager(paytofly plugin) {
        this.plugin = plugin;
    }
    
    public void waitForInput(Player player) {
        waitingForInput.add(player.getUniqueId());
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!waitingForInput.contains(player.getUniqueId())) return;
        
        event.setCancelled(true);
        waitingForInput.remove(player.getUniqueId());
        
        try {
            int minutes = Integer.parseInt(event.getMessage());
            int minTime = plugin.getConfig().getInt("time-limits.minute.min");
            int maxTime = plugin.getConfig().getInt("time-limits.minute.max");
            
            if (minutes <= 0) {
                player.sendMessage(plugin.getPrefix() + 
                    plugin.getLang("messages.custom-time.must-positive"));
                return;
            }
            
            if (minutes < minTime || minutes > maxTime) {
                String message = plugin.getLang("messages.time-limit")
                    .replace("{min}", String.valueOf(minTime))
                    .replace("{max}", String.valueOf(maxTime));
                player.sendMessage(plugin.getPrefix() + message);
                return;
            }
            
            double pricePerMinute = plugin.getConfig().getDouble("fly-cost.minute", 10.0);
            double totalPrice = pricePerMinute * minutes;
            
            if (plugin.getEconomy().has(player, totalPrice)) {
                plugin.getEconomy().withdrawPlayer(player, totalPrice);
                plugin.getTimeManager().addTime(player, minutes * 60 * 1000L);
                
                player.sendMessage(plugin.getPrefix() + 
                    plugin.getLang("messages.custom-time.success").replace("{minutes}", String.valueOf(minutes)));
                player.sendMessage(plugin.getPrefix() + 
                    plugin.getLang("messages.custom-time.cost").replace("{amount}", String.valueOf(totalPrice)));
            } else {
                player.sendMessage(plugin.getPrefix() + 
                    plugin.getLang("messages.custom-time.insufficient-funds").replace("{amount}", String.valueOf(totalPrice)));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getPrefix() + plugin.getLang("messages.custom-time.invalid-number"));
        }
    }
} 