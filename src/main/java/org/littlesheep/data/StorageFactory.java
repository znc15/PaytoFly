package org.littlesheep.data;

import org.bukkit.plugin.java.JavaPlugin;

public class StorageFactory {
    public static Storage createStorage(String type, JavaPlugin plugin) {
        if (type == null || type.trim().isEmpty()) {
            plugin.getLogger().warning("存储类型未指定，使用默认JSON存储");
            return new JsonStorage(plugin);
        }

        switch (type.trim().toUpperCase()) {
            case "JSON":
                return new JsonStorage(plugin);
            case "SQLITE":
                return new SqliteStorage(plugin);
            case "MYSQL":
                return new MySqlStorage(plugin);
            default:
                plugin.getLogger().warning("未知的存储类型: " + type + "，使用默认JSON存储");
                return new JsonStorage(plugin);
        }
    }
} 