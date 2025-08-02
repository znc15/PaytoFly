package org.littlesheep.data;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface Storage extends Closeable {
    void init();
    void close();
    void setPlayerFlightTime(UUID uuid, long endTime);
    Long getPlayerFlightTime(UUID uuid);
    Map<UUID, Long> getAllPlayerData();
    void removePlayerFlightTime(UUID uuid);
    
    // 特效购买记录
    void addPlayerEffect(UUID uuid, String effectName);
    void removePlayerEffect(UUID uuid, String effectName);
    Set<String> getPlayerEffects(UUID uuid);
    Map<UUID, Set<String>> getAllPlayerEffects();
    
    // 速度购买记录
    void addPlayerSpeed(UUID uuid, String speedName);
    void removePlayerSpeed(UUID uuid, String speedName);
    Set<String> getPlayerSpeeds(UUID uuid);
    Map<UUID, Set<String>> getAllPlayerSpeeds();
} 