package org.littlesheep.data;

import org.bukkit.plugin.java.JavaPlugin;
import org.littlesheep.cache.FlightDataCache;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 缓存装饰器存储 - 在原有存储基础上添加缓存层
 */
public class CachedStorage implements Storage {
    private final Storage delegate;
    private final FlightDataCache cache;
    private final JavaPlugin plugin;
    private final boolean writeThrough; // 是否写穿透模式

    public CachedStorage(Storage delegate, JavaPlugin plugin) {
        this.delegate = delegate;
        this.plugin = plugin;
        this.cache = new FlightDataCache(plugin);
        this.writeThrough = plugin.getConfig().getBoolean("cache.write-through", true);
        
        plugin.getLogger().info(String.format(
            "缓存存储已启用 - 底层存储: %s, 写模式: %s",
            delegate.getClass().getSimpleName(),
            writeThrough ? "写穿透" : "写回"
        ));
    }

    @Override
    public void init() {
        // 初始化底层存储
        delegate.init();
        
        // 预加载数据到缓存
        if (plugin.getConfig().getBoolean("cache.preload-on-init", true)) {
            preloadCache();
        }
    }

    @Override
    public void close() {
        // 关闭缓存
        cache.shutdown();
        
        // 关闭底层存储
        delegate.close();
        
        plugin.getLogger().info("缓存存储已关闭");
    }

    @Override
    public void setPlayerFlightTime(UUID uuid, long endTime) {
        // 更新缓存
        cache.setFlightTime(uuid, endTime);
        
        if (writeThrough) {
            // 写穿透模式：立即写入底层存储
            delegate.setPlayerFlightTime(uuid, endTime);
        } else {
            // 写回模式：异步写入底层存储
            CompletableFuture.runAsync(() -> {
                try {
                    delegate.setPlayerFlightTime(uuid, endTime);
                } catch (Exception e) {
                    plugin.getLogger().warning(String.format(
                        "异步写入飞行时间失败 (UUID: %s): %s", uuid, e.getMessage()));
                    // 写入失败时从缓存中移除，保证数据一致性
                    cache.removeFlightTime(uuid);
                }
            });
        }
    }

    @Override
    public Long getPlayerFlightTime(UUID uuid) {
        // 首先检查缓存
        Long cachedTime = cache.getFlightTime(uuid);
        if (cachedTime != null) {
            return cachedTime;
        }
        
        // 缓存未命中，从底层存储读取
        Long storageTime = delegate.getPlayerFlightTime(uuid);
        if (storageTime != null) {
            // 将数据加载到缓存
            cache.setFlightTime(uuid, storageTime);
        }
        
        return storageTime;
    }

    @Override
    public void removePlayerFlightTime(UUID uuid) {
        // 从缓存中移除
        cache.removeFlightTime(uuid);
        
        if (writeThrough) {
            // 写穿透模式：立即从底层存储删除
            delegate.removePlayerFlightTime(uuid);
        } else {
            // 写回模式：异步从底层存储删除
            CompletableFuture.runAsync(() -> {
                try {
                    delegate.removePlayerFlightTime(uuid);
                } catch (Exception e) {
                    plugin.getLogger().warning(String.format(
                        "异步删除飞行时间失败 (UUID: %s): %s", uuid, e.getMessage()));
                }
            });
        }
    }

    @Override
    public Map<UUID, Long> getAllPlayerData() {
        // 直接从底层存储获取所有数据
        // 这个操作通常不频繁，不需要缓存
        return delegate.getAllPlayerData();
    }

    /**
     * 预加载数据到缓存
     */
    private void preloadCache() {
        CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                Map<UUID, Long> allData = delegate.getAllPlayerData();
                cache.preloadData(allData);
                
                long loadTime = System.currentTimeMillis() - startTime;
                plugin.getLogger().info(String.format(
                    "缓存预加载完成，耗时 %dms", loadTime));
                    
            } catch (Exception e) {
                plugin.getLogger().warning("缓存预加载失败: " + e.getMessage());
            }
        });
    }

    /**
     * 刷新缓存 - 重新从底层存储加载数据
     */
    public void refreshCache() {
        plugin.getLogger().info("正在刷新缓存...");
        cache.clear();
        preloadCache();
    }

    /**
     * 强制同步缓存到存储
     * 只在写回模式下有意义
     */
    public void flushCache() {
        if (writeThrough) {
            plugin.getLogger().info("写穿透模式下无需刷新缓存");
            return;
        }
        
        plugin.getLogger().info("写回模式下的缓存刷新功能尚未实现");
        // TODO: 实现写回模式下的缓存刷新逻辑
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStatistics() {
        FlightDataCache.CacheStatistics stats = cache.getStatistics();
        return stats.toString();
    }

    /**
     * 重置缓存统计信息
     */
    public void resetCacheStatistics() {
        cache.resetStatistics();
    }

    /**
     * 获取底层存储实例
     */
    public Storage getDelegate() {
        return delegate;
    }

    /**
     * 获取缓存实例
     */
    public FlightDataCache getCache() {
        return cache;
    }

    /**
     * 检查指定玩家数据是否在缓存中
     */
    public boolean isInCache(UUID uuid) {
        return cache.contains(uuid);
    }

    /**
     * 预热指定玩家的缓存
     */
    public void warmupCache(UUID uuid) {
        if (!cache.contains(uuid)) {
            Long flightTime = delegate.getPlayerFlightTime(uuid);
            if (flightTime != null) {
                cache.setFlightTime(uuid, flightTime);
            }
        }
    }

    /**
     * 获取详细的存储统计信息
     */
    public String getDetailedStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 缓存存储统计 ===\n");
        sb.append("底层存储: ").append(delegate.getClass().getSimpleName()).append("\n");
        sb.append("写模式: ").append(writeThrough ? "写穿透" : "写回").append("\n");
        sb.append(getCacheStatistics()).append("\n");
        
        // 如果底层存储支持统计信息，也显示出来
        if (delegate instanceof MySqlStorage) {
            MySqlStorage mysqlStorage = (MySqlStorage) delegate;
            sb.append("MySQL存储: ").append(mysqlStorage.getDatabaseStatistics()).append("\n");
        } else if (delegate instanceof JsonStorage) {
            JsonStorage jsonStorage = (JsonStorage) delegate;
            sb.append("JSON存储: ").append(jsonStorage.getStatistics()).append("\n");
        }
        
        return sb.toString();
    }
}