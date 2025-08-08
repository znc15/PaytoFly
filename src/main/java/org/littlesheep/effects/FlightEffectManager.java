package org.littlesheep.effects;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.littlesheep.paytofly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

/**
 * 飞行特效管理器 - 管理玩家飞行时的视觉特效和音效
 * 
 * 功能包括：
 * - 粒子特效（彩虹尾迹、星星、火花等）
 * - 音效播放（启动音效、持续音效）
 * - 特效等级管理
 * - 性能优化（异步处理、资源清理）
 */
public class FlightEffectManager {
    
    private final paytofly plugin;
    private final Map<Player, BukkitTask> activeEffects;
    private final Map<Player, FlightEffectType> playerEffects;
    private final Random random;
    private BukkitTask globalEffectTask;
    
    public FlightEffectManager(paytofly plugin) {
        this.plugin = plugin;
        this.activeEffects = new ConcurrentHashMap<>();
        this.playerEffects = new ConcurrentHashMap<>();
        this.random = new Random();
        this.startGlobalEffectTask();
    }
    
    /**
     * 飞行特效类型枚举
     */
    public enum FlightEffectType {
        NONE("none", 0),
        BASIC("basic", 1),
        RAINBOW("rainbow", 2),
        STAR("star", 3),
        FIRE("fire", 4),
        MAGIC("magic", 5),
        DRAGON("dragon", 6);
        
        private final String name;
        private final int level;
        
        FlightEffectType(String name, int level) {
            this.name = name;
            this.level = level;
        }
        
        public String getName() { return name; }
        public int getLevel() { return level; }
        
        public static FlightEffectType fromString(String name) {
            for (FlightEffectType type : values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return BASIC;
        }
    }
    
    /**
     * 启动玩家的飞行特效
     */
    public void startFlightEffect(Player player, FlightEffectType effectType) {
        if (!plugin.getConfig().getBoolean("flight-effects.enabled", true)) {
            plugin.getLogger().info("飞行特效系统已禁用");
            return;
        }
        
        // 停止已有特效
        stopFlightEffect(player);
        
        // 设置玩家特效类型
        playerEffects.put(player, effectType);
        
        // 播放启动音效
        playStartSound(player, effectType);
        
        plugin.getLogger().info("玩家 " + player.getName() + " 启动特效: " + effectType.getName() + 
            "，当前活跃特效玩家数: " + playerEffects.size());
        
        // 调试信息：检查全局任务状态
        if (globalEffectTask == null || globalEffectTask.isCancelled()) {
            plugin.getLogger().warning("全局特效任务未运行，重新启动...");
            startGlobalEffectTask();
        }
    }
    
    /**
     * 停止玩家的飞行特效
     */
    public void stopFlightEffect(Player player) {
        BukkitTask task = activeEffects.remove(player);
        if (task != null) {
            task.cancel();
        }
        
        FlightEffectType effectType = playerEffects.remove(player);
        if (effectType != null) {
            // 播放停止音效
            playStopSound(player, effectType);
        }
    }
    
    /**
     * 全局特效任务 - 统一处理所有玩家的特效
     */
    private void startGlobalEffectTask() {
        if (globalEffectTask != null) {
            globalEffectTask.cancel();
        }
        
        globalEffectTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Player, FlightEffectType> entry : playerEffects.entrySet()) {
                    Player player = entry.getKey();
                    FlightEffectType effectType = entry.getValue();
                    
                    if (player.isOnline() && player.isFlying()) {
                        createParticleEffect(player, effectType);
                    } else if (!player.isOnline()) {
                        // 清理离线玩家
                        playerEffects.remove(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 2L); // 每2tick执行一次
        
        plugin.getLogger().info("全局特效任务已启动，更新频率: 每2tick");
    }
    
    /**
     * 创建粒子特效
     */
    private void createParticleEffect(Player player, FlightEffectType effectType) {
        if (effectType == FlightEffectType.NONE) {
            return;
        }
        
        Location loc = player.getLocation().clone().subtract(0, 0.5, 0);
        World world = player.getWorld();
        
        // 检查玩家移动距离，静止时减少特效
        if (player.getVelocity().lengthSquared() < 0.1) {
            // 玩家基本静止，减少特效频率
            if (random.nextInt(5) != 0) return;
        }
        
        switch (effectType) {
            case NONE:
                // 无特效，什么都不做
                break;
            case BASIC:
                createBasicEffect(world, loc);
                break;
            case RAINBOW:
                createRainbowEffect(world, loc);
                break;
            case STAR:
                createStarEffect(world, loc);
                break;
            case FIRE:
                createFireEffect(world, loc);
                break;
            case MAGIC:
                createMagicEffect(world, loc);
                break;
            case DRAGON:
                createDragonEffect(world, loc);
                break;
        }
    }
    
    /**
     * 基础特效 - 简单的白色粒子
     */
    private void createBasicEffect(World world, Location loc) {
        world.spawnParticle(Particle.CLOUD, loc, 3, 0.3, 0.1, 0.3, 0.02);
    }
    
    /**
     * 彩虹特效 - 彩色尾迹
     */
    private void createRainbowEffect(World world, Location loc) {
        Color[] colors = {
            Color.RED, Color.ORANGE, Color.YELLOW, 
            Color.GREEN, Color.BLUE, Color.PURPLE
        };
        
        for (int i = 0; i < 3; i++) {
            Color color = colors[random.nextInt(colors.length)];
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.0f);
            world.spawnParticle(Particle.REDSTONE, loc, 1, 0.5, 0.2, 0.5, dustOptions);
        }
    }
    
    /**
     * 星星特效 - 闪烁的星星
     */
    private void createStarEffect(World world, Location loc) {
        world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 2, 0.4, 0.2, 0.4, 0);
        if (random.nextInt(3) == 0) {
            world.spawnParticle(Particle.CRIT_MAGIC, loc, 1, 0.3, 0.1, 0.3, 0);
        }
    }
    
    /**
     * 火焰特效 - 火花和烟雾
     */
    private void createFireEffect(World world, Location loc) {
        world.spawnParticle(Particle.FLAME, loc, 2, 0.3, 0.1, 0.3, 0.02);
        world.spawnParticle(Particle.LAVA, loc, 1, 0.2, 0.1, 0.2, 0);
        if (random.nextInt(4) == 0) {
            world.spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }
    
    /**
     * 魔法特效 - 魔法粒子和符文
     */
    private void createMagicEffect(World world, Location loc) {
        world.spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 5, 0.5, 0.3, 0.5, 1);
        world.spawnParticle(Particle.PORTAL, loc, 3, 0.3, 0.2, 0.3, 0.5);
        if (random.nextInt(5) == 0) {
            world.spawnParticle(Particle.SPELL_WITCH, loc, 2, 0.4, 0.2, 0.4, 0);
        }
    }
    
    /**
     * 龙息特效 - 高级龙息效果
     */
    private void createDragonEffect(World world, Location loc) {
        world.spawnParticle(Particle.DRAGON_BREATH, loc, 4, 0.4, 0.2, 0.4, 0.02);
        world.spawnParticle(Particle.END_ROD, loc, 2, 0.3, 0.1, 0.3, 0.05);
        if (random.nextInt(3) == 0) {
            world.spawnParticle(Particle.PORTAL, loc, 3, 0.5, 0.3, 0.5, 0.8);
        }
    }
    
    /**
     * 播放启动音效
     */
    private void playStartSound(Player player, FlightEffectType effectType) {
        if (!plugin.getConfig().getBoolean("flight-effects.sounds.enabled", true)) {
            return;
        }
        
        Sound sound = getStartSound(effectType);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 0.7f, 1.2f);
        }
    }
    
    /**
     * 播放停止音效
     */
    private void playStopSound(Player player, FlightEffectType effectType) {
        if (!plugin.getConfig().getBoolean("flight-effects.sounds.enabled", true)) {
            return;
        }
        
        Sound sound = getStopSound(effectType);
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 0.5f, 0.8f);
        }
    }
    
    /**
     * 获取启动音效
     */
    private Sound getStartSound(FlightEffectType effectType) {
        switch (effectType) {
            case BASIC:
                return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case RAINBOW:
                return Sound.BLOCK_NOTE_BLOCK_CHIME;
            case STAR:
                return Sound.ENTITY_PLAYER_LEVELUP;
            case FIRE:
                return Sound.ITEM_FIRECHARGE_USE;
            case MAGIC:
                return Sound.ENTITY_ENDER_DRAGON_SHOOT;
            case DRAGON:
                return Sound.ENTITY_ENDER_DRAGON_FLAP;
            default:
                return null;
        }
    }
    
    /**
     * 获取停止音效
     */
    private Sound getStopSound(FlightEffectType effectType) {
        switch (effectType) {
            case BASIC:
                return Sound.BLOCK_NOTE_BLOCK_BASS;
            case RAINBOW:
            case STAR:
                return Sound.BLOCK_NOTE_BLOCK_PLING;
            case FIRE:
                return Sound.BLOCK_FIRE_EXTINGUISH;
            case MAGIC:
            case DRAGON:
                return Sound.ENTITY_ENDERMAN_TELEPORT;
            default:
                return null;
        }
    }
    
    /**
     * 获取玩家的特效类型
     */
    public FlightEffectType getPlayerEffect(Player player) {
        return playerEffects.getOrDefault(player, FlightEffectType.BASIC);
    }
    
    /**
     * 设置玩家的特效类型（不启动特效）
     */
    public void setPlayerEffect(Player player, FlightEffectType effectType) {
        playerEffects.put(player, effectType);
    }

    /**
     * 安全地设置玩家默认特效（仅在没有设置时）
     */
    public void setDefaultPlayerEffect(Player player, FlightEffectType effectType) {
        if (!playerEffects.containsKey(player)) {
            playerEffects.put(player, effectType);
        }
    }
    
    /**
     * 检查玩家是否有特效权限（权限、永久购买或时间限制购买）
     */
    public boolean hasEffectPermission(Player player, FlightEffectType effectType) {
        if (effectType == FlightEffectType.NONE || effectType == FlightEffectType.BASIC) {
            return true;
        }
        
        // 检查权限
        String permission = "paytofly.effects." + effectType.getName();
        if (player.hasPermission(permission) || player.hasPermission("paytofly.effects.*")) {
            return true;
        }
        
        // 检查是否已永久购买
        if (plugin.getConfig().getBoolean("flight-effects.purchase.enabled", true)) {
            if (plugin.getStorage().getPlayerEffects(player.getUniqueId()).contains(effectType.getName())) {
                return true;
            }
            
            // 检查时间限制购买
            Long endTime = plugin.getStorage().getPlayerEffectTime(player.getUniqueId(), effectType.getName());
            if (endTime != null && endTime > System.currentTimeMillis()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 购买特效（永久）
     */
    public boolean purchaseEffect(Player player, FlightEffectType effectType) {
        if (!plugin.getConfig().getBoolean("flight-effects.purchase.enabled", true)) {
            return false;
        }
        
        if (effectType == FlightEffectType.NONE || effectType == FlightEffectType.BASIC) {
            return true; // 基础特效免费
        }
        
        // 检查是否已拥有
        if (hasEffectPermission(player, effectType)) {
            return false; // 已拥有
        }
        
        // 获取价格
        double price = plugin.getConfig().getDouble("flight-effects.purchase.prices." + effectType.getName(), 0.0);
        if (price <= 0) {
            return false; // 价格无效
        }
        
        // 检查余额
        if (plugin.getEconomyManager() != null && plugin.getEconomyManager().isInitialized()) {
            if (!plugin.getEconomyManager().hasBalance(player, price)) {
                return false; // 余额不足
            }
            
            // 扣款
            if (plugin.getEconomyManager().withdraw(player, price)) {
                // 添加购买记录
                plugin.getStorage().addPlayerEffect(player.getUniqueId(), effectType.getName());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 购买特效（按时间）
     */
    public boolean purchaseEffectTime(Player player, FlightEffectType effectType, String timeUnit, int amount) {
        if (!plugin.getConfig().getBoolean("flight-effects.purchase.enabled", true)) {
            return false;
        }
        
        if (effectType == FlightEffectType.NONE || effectType == FlightEffectType.BASIC) {
            return true; // 基础特效免费
        }
        
        // 获取基础价格和时间设置
        double basePrice = plugin.getConfig().getDouble("flight-effects.purchase.prices." + effectType.getName(), 0.0);
        if (basePrice <= 0) {
            return false; // 价格无效
        }
        
        // 计算按时间购买的价格和时长
        long milliseconds = 0;
        double priceMultiplier = 1.0;
        
        switch (timeUnit.toLowerCase()) {
            case "hour":
                milliseconds = amount * 60L * 60L * 1000L;
                priceMultiplier = plugin.getConfig().getDouble("flight-effects.time-purchase.hour-multiplier", 0.1);
                break;
            case "day":
                milliseconds = amount * 24L * 60L * 60L * 1000L;
                priceMultiplier = plugin.getConfig().getDouble("flight-effects.time-purchase.day-multiplier", 2.0);
                break;
            case "week":
                milliseconds = amount * 7L * 24L * 60L * 60L * 1000L;
                priceMultiplier = plugin.getConfig().getDouble("flight-effects.time-purchase.week-multiplier", 12.0);
                break;
            default:
                return false; // 不支持的时间单位
        }
        
        double totalPrice = basePrice * priceMultiplier * amount;
        
        // 检查余额
        if (plugin.getEconomyManager() != null && plugin.getEconomyManager().isInitialized()) {
            if (!plugin.getEconomyManager().hasBalance(player, totalPrice)) {
                return false; // 余额不足
            }
            
            // 扣款
            if (plugin.getEconomyManager().withdraw(player, totalPrice)) {
                // 计算结束时间（如果已有时间限制，则延长）
                Long currentEndTime = plugin.getStorage().getPlayerEffectTime(player.getUniqueId(), effectType.getName());
                long newEndTime = Math.max(currentEndTime != null ? currentEndTime : System.currentTimeMillis(), 
                                         System.currentTimeMillis()) + milliseconds;
                
                // 添加时间限制购买记录
                plugin.getStorage().setPlayerEffectTime(player.getUniqueId(), effectType.getName(), newEndTime);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取特效价格
     */
    public double getEffectPrice(FlightEffectType effectType) {
        if (effectType == FlightEffectType.NONE || effectType == FlightEffectType.BASIC) {
            return 0.0;
        }
        return plugin.getConfig().getDouble("flight-effects.purchase.prices." + effectType.getName(), 0.0);
    }
    
    /**
     * 获取玩家可用的特效列表
     */
    public List<FlightEffectType> getAvailableEffects(Player player) {
        List<FlightEffectType> available = new ArrayList<>();
        
        for (FlightEffectType type : FlightEffectType.values()) {
            if (hasEffectPermission(player, type)) {
                available.add(type);
            }
        }
        
        return available;
    }
    
    /**
     * 清理所有资源
     */
    public void cleanup() {
        // 取消所有任务
        activeEffects.values().forEach(BukkitTask::cancel);
        activeEffects.clear();
        
        if (globalEffectTask != null) {
            globalEffectTask.cancel();
            globalEffectTask = null;
        }
        
        playerEffects.clear();
        
        plugin.getLogger().info(plugin.getLang().getMessage("effects-cleanup"));
    }
}