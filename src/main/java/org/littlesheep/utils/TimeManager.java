package org.littlesheep.utils;

import org.bukkit.entity.Player;
import org.littlesheep.paytofly;
import java.util.UUID;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.Map;

public class TimeManager {
    private final boolean useRealTime;
    private final HashMap<UUID, Long> playerFlightTimes = new HashMap<>();
    private final paytofly plugin;

    public TimeManager(paytofly plugin) {
        this.useRealTime = plugin.getConfig().getString("time-calculation.mode", "REAL_TIME").equals("REAL_TIME");
        this.plugin = plugin;
    }

    public long getRemainingTime(Player player, long endTime) {
        if (useRealTime) {
            return endTime - System.currentTimeMillis();
        } else {
            // 只在玩家在线时计时
            long totalPlayTime = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50L; // 转换为毫秒
            return endTime - totalPlayTime;
        }
    }

    public long getEndTime(long duration) {
        if (useRealTime) {
            return System.currentTimeMillis() + duration;
        } else {
            return duration; // 对于游戏时间，直接存储持续时间
        }
    }

    public boolean isExpired(Player player, long endTime) {
        return getRemainingTime(player, endTime) <= 0;
    }

    public void addTime(Player player, long milliseconds) {
        UUID uuid = player.getUniqueId();
        Long currentEndTime = playerFlightTimes.get(uuid);
        long newEndTime;
        
        if (currentEndTime == null || currentEndTime < System.currentTimeMillis()) {
            newEndTime = System.currentTimeMillis() + milliseconds;
        } else {
            newEndTime = currentEndTime + milliseconds;
        }
        
        playerFlightTimes.put(uuid, newEndTime);
        saveData();
        
        // 更新玩家飞行状态
        updatePlayerFlight(player);
    }

    private void saveData() {
        // 保存到数据文件
        File dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        
        for (Map.Entry<UUID, Long> entry : playerFlightTimes.entrySet()) {
            data.set(entry.getKey().toString(), entry.getValue());
        }
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("保存玩家数据时发生错误: " + e.getMessage());
        }
    }

    private void updatePlayerFlight(Player player) {
        if (!isExpired(player, playerFlightTimes.get(player.getUniqueId()))) {
            player.setAllowFlight(true);
            player.sendMessage(plugin.getPrefix() + "§a已启用飞行模式！");
        } else {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(plugin.getPrefix() + "§c飞行时间已到期！");
        }
    }
} 