package org.littlesheep.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

/**
 * MHDF-Tools经济系统适配器
 */
public class MHDFEconomy implements EconomyAdapter {
    
    private final boolean initialized;
    private final Plugin mhdfPlugin;

    public MHDFEconomy() {
        this.mhdfPlugin = Bukkit.getPluginManager().getPlugin("MHDF-Tools");
        this.initialized = mhdfPlugin != null && mhdfPlugin.isEnabled();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (!initialized) return 0.0;
        
        // 使用服务器命令获取余额
        try {
            String playerName = player.getName();
            if (playerName == null) return 0.0;
            
            // 调用MHDF的查询命令
            double balance = 0.0;
            
            return balance;
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!initialized) return false;
        
        try {
            String playerName = player.getName();
            if (playerName == null) return false;
            
            // 调用MHDF的扣款命令
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                    "mhdf-tools:money take " + playerName + " " + amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!initialized) return false;
        
        try {
            String playerName = player.getName();
            if (playerName == null) return false;
            
            // 调用MHDF的加款命令
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                    "mhdf-tools:money give " + playerName + " " + amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return "MHDF-Tools";
    }

    @Override
    public String formatMoney(double amount) {
        return String.format("%.2f", amount);
    }
} 