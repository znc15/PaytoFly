package org.littlesheep.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 经济系统管理器
 */
public class EconomyManager {
    
    private final List<EconomyAdapter> adapters = new ArrayList<>();
    private EconomyAdapter currentAdapter = null;
    private final JavaPlugin plugin;
    
    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initAdapters();
    }
    
    /**
     * 初始化所有可用的经济适配器
     */
    private void initAdapters() {
        // 首先尝试初始化Vault经济
        VaultEconomy vaultEconomy = new VaultEconomy(plugin);
        adapters.add(vaultEconomy);
        
        // 尝试初始化MHDF-Tools经济
        MHDFEconomy mhdfEconomy = new MHDFEconomy();
        adapters.add(mhdfEconomy);
        
        // 可以添加更多的经济适配器...
        
        // 选择第一个可用的经济适配器
        for (EconomyAdapter adapter : adapters) {
            if (adapter.isInitialized()) {
                currentAdapter = adapter;
                plugin.getLogger().info("已选择经济系统: " + adapter.getName());
                break;
            }
        }
        
        if (currentAdapter == null) {
            plugin.getLogger().severe("无法找到可用的经济系统!");
        }
    }
    
    /**
     * 获取当前使用的经济系统
     * 
     * @return 经济系统适配器
     */
    public EconomyAdapter getCurrentEconomy() {
        return currentAdapter;
    }
    
    /**
     * 检查经济系统是否已初始化
     * 
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return currentAdapter != null && currentAdapter.isInitialized();
    }
    
    /**
     * 获取可用的经济系统名称
     * 
     * @return 经济系统名称
     */
    public String getEconomyName() {
        if (currentAdapter == null) return "未找到经济系统";
        return currentAdapter.getName();
    }
    
    /**
     * 获取玩家余额
     * 
     * @param player 玩家
     * @return 余额
     */
    public double getBalance(OfflinePlayer player) {
        if (currentAdapter == null) return 0.0;
        return currentAdapter.getBalance(player);
    }
    
    /**
     * 检查玩家是否有足够的余额
     * 
     * @param player 玩家
     * @param amount 金额
     * @return 是否有足够余额
     */
    public boolean hasBalance(OfflinePlayer player, double amount) {
        if (currentAdapter == null) return false;
        return currentAdapter.getBalance(player) >= amount;
    }
    
    /**
     * 从玩家账户扣除金额
     * 
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (currentAdapter == null) return false;
        return currentAdapter.withdraw(player, amount);
    }
    
    /**
     * 给玩家账户增加金额
     * 
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (currentAdapter == null) return false;
        return currentAdapter.deposit(player, amount);
    }
    
    /**
     * 格式化金额
     * 
     * @param amount 金额
     * @return 格式化后的金额字符串
     */
    public String formatMoney(double amount) {
        if (currentAdapter == null) return String.format("%.2f", amount);
        return currentAdapter.formatMoney(amount);
    }
} 