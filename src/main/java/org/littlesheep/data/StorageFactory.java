package org.littlesheep.data;

import org.bukkit.plugin.java.JavaPlugin;

public class StorageFactory {
    public static Storage createStorage(String type, JavaPlugin plugin) {
        if (type == null || type.trim().isEmpty()) {
            plugin.getLogger().warning("存储类型未指定，使用默认JSON存储");
            return createWithCache(new JsonStorage(plugin), plugin);
        }

        Storage baseStorage;
        switch (type.trim().toUpperCase()) {
            case "JSON":
                baseStorage = new JsonStorage(plugin);
                break;
            case "SQLITE":
                baseStorage = new SqliteStorage(plugin);
                break;
            case "MYSQL":
                baseStorage = new MySqlStorage(plugin);
                break;
            default:
                plugin.getLogger().warning("未知的存储类型: " + type + "，使用默认JSON存储");
                baseStorage = new JsonStorage(plugin);
                break;
        }

        return createWithCache(baseStorage, plugin);
    }

    /**
     * 根据配置决定是否启用缓存
     */
    private static Storage createWithCache(Storage baseStorage, JavaPlugin plugin) {
        boolean cacheEnabled = plugin.getConfig().getBoolean("cache.enabled", true);
        
        if (cacheEnabled) {
            plugin.getLogger().info("缓存已启用，创建缓存装饰器存储");
            return new CachedStorage(baseStorage, plugin);
        } else {
            plugin.getLogger().info("缓存已禁用，使用原始存储");
            return baseStorage;
        }
    }
} 