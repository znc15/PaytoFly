package org.littlesheep.economy;

import org.bukkit.OfflinePlayer;

/**
 * 经济系统适配器接口
 */
public interface EconomyAdapter {
    
    /**
     * 检查经济系统是否已初始化
     * 
     * @return 是否已初始化
     */
    boolean isInitialized();
    
    /**
     * 获取玩家余额
     * 
     * @param player 玩家
     * @return 余额
     */
    double getBalance(OfflinePlayer player);
    
    /**
     * 从玩家账户扣除金额
     * 
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    boolean withdraw(OfflinePlayer player, double amount);
    
    /**
     * 给玩家账户增加金额
     * 
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    boolean deposit(OfflinePlayer player, double amount);
    
    /**
     * 获取经济系统名称
     * 
     * @return 经济系统名称
     */
    String getName();
    
    /**
     * 格式化金额
     * 
     * @param amount 金额
     * @return 格式化后的金额字符串
     */
    String formatMoney(double amount);
} 