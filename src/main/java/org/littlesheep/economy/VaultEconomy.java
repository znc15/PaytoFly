package org.littlesheep.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Vault经济系统适配器
 */
public class VaultEconomy implements EconomyAdapter {
    
    private Economy econ = null;
    private boolean initialized;
    
    public VaultEconomy(JavaPlugin plugin) {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
                this.initialized = false;
                return;
            }
            
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                this.initialized = false;
                return;
            }
            
            econ = rsp.getProvider();
            this.initialized = econ != null;
        } catch (Exception e) {
            plugin.getLogger().warning("初始化Vault经济系统失败: " + e.getMessage());
            this.initialized = false;
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public double getBalance(OfflinePlayer player) {
        if (!initialized) return 0.0;
        return econ.getBalance(player);
    }
    
    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!initialized) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!initialized) return false;
        return econ.depositPlayer(player, amount).transactionSuccess();
    }
    
    @Override
    public String getName() {
        if (!initialized) return "Vault-未初始化";
        return econ.getName();
    }
    
    @Override
    public String formatMoney(double amount) {
        if (!initialized) return String.format("%.2f", amount);
        return econ.format(amount);
    }
} 