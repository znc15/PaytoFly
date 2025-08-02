package org.littlesheep.cache;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.UUID;

/**
 * 飞行数据缓存层 - 减少数据库查询，提升性能
 */
public class FlightDataCache {
    private final JavaPlugin plugin;
    
    // 缓存数据
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // 缓存配置
    private final long maxCacheSize;
    private final long ttlMillis; // 生存时间
    private final long cleanupIntervalMillis;
    
    // 清理任务
    private final ScheduledExecutorService cleanupExecutor;
    
    // 统计信息
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicInteger currentSize = new AtomicInteger(0);

    public FlightDataCache(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // 从配置读取缓存参数
        this.maxCacheSize = plugin.getConfig().getLong("cache.max-size", 1000);
        this.ttlMillis = plugin.getConfig().getLong("cache.ttl-minutes", 30) * 60 * 1000;
        this.cleanupIntervalMillis = plugin.getConfig().getLong("cache.cleanup-interval-minutes", 5) * 60 * 1000;
        
        // 启动清理任务
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FlightDataCache-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        this.cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredEntries, 
            cleanupIntervalMillis, 
            cleanupIntervalMillis, 
            TimeUnit.MILLISECONDS
        );
        
        plugin.getLogger().info(String.format(
            "飞行数据缓存已启动 - 最大容量: %d, TTL: %d分钟, 清理间隔: %d分钟",
            maxCacheSize, 
            ttlMillis / 60000, 
            cleanupIntervalMillis / 60000
        ));
    }

    /**
     * 获取缓存中的飞行时间
     * @param uuid 玩家UUID
     * @return 飞行结束时间，如果缓存中没有则返回null
     */
    public Long getFlightTime(UUID uuid) {
        CacheEntry entry = cache.get(uuid);
        
        if (entry == null) {
            misses.incrementAndGet();
            return null;
        }
        
        // 检查是否过期
        if (isExpired(entry)) {
            cache.remove(uuid);
            currentSize.decrementAndGet();
            misses.incrementAndGet();
            return null;
        }
        
        // 更新访问时间
        entry.lastAccessed = System.currentTimeMillis();
        hits.incrementAndGet();
        return entry.flightEndTime;
    }

    /**
     * 设置缓存中的飞行时间
     * @param uuid 玩家UUID
     * @param flightEndTime 飞行结束时间
     */
    public void setFlightTime(UUID uuid, long flightEndTime) {
        // 检查缓存容量
        if (currentSize.get() >= maxCacheSize && !cache.containsKey(uuid)) {
            evictLeastRecentlyUsed();
        }
        
        CacheEntry entry = new CacheEntry(flightEndTime);
        CacheEntry previous = cache.put(uuid, entry);
        
        if (previous == null) {
            currentSize.incrementAndGet();
        }
    }

    /**
     * 移除缓存中的飞行时间
     * @param uuid 玩家UUID
     */
    public void removeFlightTime(UUID uuid) {
        CacheEntry removed = cache.remove(uuid);
        if (removed != null) {
            currentSize.decrementAndGet();
        }
    }

    /**
     * 检查缓存中是否存在指定玩家的数据
     * @param uuid 玩家UUID
     * @return 如果存在且未过期则返回true
     */
    public boolean contains(UUID uuid) {
        return getFlightTime(uuid) != null;
    }

    /**
     * 批量预加载数据到缓存
     * @param data 飞行数据映射
     */
    public void preloadData(Map<UUID, Long> data) {
        long now = System.currentTimeMillis();
        int preloadCount = 0;
        
        for (Map.Entry<UUID, Long> entry : data.entrySet()) {
            // 只缓存未过期的数据
            if (entry.getValue() > now) {
                setFlightTime(entry.getKey(), entry.getValue());
                preloadCount++;
            }
        }
        
        plugin.getLogger().info(String.format("预加载了 %d 条飞行数据到缓存", preloadCount));
    }

    /**
     * 清理过期条目
     */
    private void cleanupExpiredEntries() {
        long cleanupStart = System.currentTimeMillis();
        int cleanedCount = 0;
        
        cache.entrySet().removeIf(entry -> {
            if (isExpired(entry.getValue())) {
                currentSize.decrementAndGet();
                return true;
            }
            return false;
        });
        
        if (cleanedCount > 0) {
            long cleanupTime = System.currentTimeMillis() - cleanupStart;
            plugin.getLogger().fine(String.format(
                "缓存清理完成: 清理了 %d 条过期数据，耗时 %dms", 
                cleanedCount, cleanupTime));
        }
    }

    /**
     * 驱逐最近最少使用的条目
     */
    private void evictLeastRecentlyUsed() {
        UUID lruKey = null;
        long oldestAccess = Long.MAX_VALUE;
        
        for (Map.Entry<UUID, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().lastAccessed < oldestAccess) {
                oldestAccess = entry.getValue().lastAccessed;
                lruKey = entry.getKey();
            }
        }
        
        if (lruKey != null) {
            cache.remove(lruKey);
            currentSize.decrementAndGet();
            evictions.incrementAndGet();
        }
    }

    /**
     * 检查缓存条目是否过期
     */
    private boolean isExpired(CacheEntry entry) {
        long now = System.currentTimeMillis();
        
        // 检查缓存TTL过期
        if (now - entry.createdTime > ttlMillis) {
            return true;
        }
        
        // 检查飞行时间是否已过期
        return entry.flightEndTime <= now;
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.clear();
        currentSize.set(0);
        plugin.getLogger().info("飞行数据缓存已清空");
    }

    /**
     * 关闭缓存
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanupExecutor.shutdownNow();
        }
        
        clear();
        plugin.getLogger().info("飞行数据缓存已关闭");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getStatistics() {
        long totalRequests = hits.get() + misses.get();
        double hitRate = totalRequests > 0 ? (hits.get() * 100.0 / totalRequests) : 0.0;
        
        return new CacheStatistics(
            currentSize.get(),
            maxCacheSize,
            hits.get(),
            misses.get(),
            evictions.get(),
            hitRate
        );
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        hits.set(0);
        misses.set(0);
        evictions.set(0);
        plugin.getLogger().info("缓存统计信息已重置");
    }

    /**
     * 缓存条目类
     */
    private static class CacheEntry {
        final long flightEndTime;
        final long createdTime;
        volatile long lastAccessed;

        CacheEntry(long flightEndTime) {
            this.flightEndTime = flightEndTime;
            this.createdTime = System.currentTimeMillis();
            this.lastAccessed = this.createdTime;
        }
    }

    /**
     * 缓存统计信息类
     */
    public static class CacheStatistics {
        public final long currentSize;
        public final long maxSize;
        public final long hits;
        public final long misses;
        public final long evictions;
        public final double hitRate;

        CacheStatistics(long currentSize, long maxSize, long hits, long misses, long evictions, double hitRate) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }

        @Override
        public String toString() {
            return String.format(
                "缓存统计: 大小=%d/%d, 命中=%d, 未命中=%d, 驱逐=%d, 命中率=%.1f%%",
                currentSize, maxSize, hits, misses, evictions, hitRate
            );
        }
    }
}