package org.littlesheep.utils;

import org.bukkit.entity.Player;
import org.littlesheep.paytofly;

public class TimeManager {
    private final boolean useRealTime;

    public TimeManager(paytofly plugin) {
        this.useRealTime = plugin.getConfig().getString("time-calculation.mode", "REAL_TIME").equals("REAL_TIME");
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
} 