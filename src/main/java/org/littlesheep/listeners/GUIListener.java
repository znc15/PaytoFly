package org.littlesheep.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.littlesheep.gui.FlightShopGUI;

public class GUIListener implements Listener {
    private final FlightShopGUI gui;

    public GUIListener(FlightShopGUI gui) {
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        gui.handleClick(event);
    }
} 