package org.littlesheep.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final Gson gson;
    private Map<UUID, Long> playerData;

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.playerData = new HashMap<>();
        loadData();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            saveData();
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<HashMap<UUID, Long>>(){}.getType();
            playerData = gson.fromJson(reader, type);
            if (playerData == null) {
                playerData = new HashMap<>();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("加载玩家数据时出错: " + e.getMessage());
            playerData = new HashMap<>();
        }
    }

    public void saveData() {
        try {
            if (!dataFile.exists()) {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            }

            try (Writer writer = new FileWriter(dataFile)) {
                gson.toJson(playerData, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("保存玩家数据时出错: " + e.getMessage());
        }
    }

    public void setPlayerFlightTime(UUID uuid, long endTime) {
        playerData.put(uuid, endTime);
        saveData();
    }

    public Long getPlayerFlightTime(UUID uuid) {
        return playerData.get(uuid);
    }

    public void removePlayerFlightTime(UUID uuid) {
        playerData.remove(uuid);
        saveData();
    }

    public Map<UUID, Long> getAllPlayerData() {
        return new HashMap<>(playerData);
    }
} 