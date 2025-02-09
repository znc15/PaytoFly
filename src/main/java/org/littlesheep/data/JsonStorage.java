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

public class JsonStorage implements Storage {
    private final File dataFile;
    private final Gson gson;
    private Map<UUID, Long> data;

    public JsonStorage(JavaPlugin plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "flight_data.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.data = new HashMap<>();
    }

    @Override
    public void init() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                saveData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            loadData();
        }
    }

    @Override
    public void close() {
        saveData();
    }

    @Override
    public void setPlayerFlightTime(UUID uuid, long endTime) {
        data.put(uuid, endTime);
        saveData();
    }

    @Override
    public Long getPlayerFlightTime(UUID uuid) {
        return data.get(uuid);
    }

    @Override
    public Map<UUID, Long> getAllPlayerData() {
        return new HashMap<>(data);
    }

    @Override
    public void removePlayerFlightTime(UUID uuid) {
        data.remove(uuid);
        saveData();
    }

    private void loadData() {
        try (Reader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<HashMap<UUID, Long>>(){}.getType();
            Map<UUID, Long> loadedData = gson.fromJson(reader, type);
            if (loadedData != null) {
                data = loadedData;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveData() {
        try (Writer writer = new FileWriter(dataFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 