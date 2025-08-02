package org.littlesheep.data;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;

/**
 * JSON数据容器 - 用于序列化和反序列化所有玩家数据
 */
public class JsonDataContainer {
    private Map<UUID, Long> flightData;
    private Map<UUID, Set<String>> effectData;
    private Map<UUID, Set<String>> speedData;
    private int version = 1; // 数据格式版本号，用于将来的升级
    
    public JsonDataContainer() {
        this.flightData = new HashMap<>();
        this.effectData = new HashMap<>();
        this.speedData = new HashMap<>();
    }
    
    public JsonDataContainer(Map<UUID, Long> flightData, 
                           Map<UUID, Set<String>> effectData, 
                           Map<UUID, Set<String>> speedData) {
        this.flightData = flightData != null ? new HashMap<>(flightData) : new HashMap<>();
        this.effectData = effectData != null ? new HashMap<>(effectData) : new HashMap<>();
        this.speedData = speedData != null ? new HashMap<>(speedData) : new HashMap<>();
    }
    
    // Getters and Setters
    public Map<UUID, Long> getFlightData() {
        return flightData;
    }
    
    public void setFlightData(Map<UUID, Long> flightData) {
        this.flightData = flightData;
    }
    
    public Map<UUID, Set<String>> getEffectData() {
        return effectData;
    }
    
    public void setEffectData(Map<UUID, Set<String>> effectData) {
        this.effectData = effectData;
    }
    
    public Map<UUID, Set<String>> getSpeedData() {
        return speedData;
    }
    
    public void setSpeedData(Map<UUID, Set<String>> speedData) {
        this.speedData = speedData;
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    /**
     * 检查是否为新格式数据
     */
    public boolean isNewFormat() {
        return version >= 1 && (effectData != null || speedData != null);
    }
    
    /**
     * 检查容器是否为空
     */
    public boolean isEmpty() {
        return (flightData == null || flightData.isEmpty()) &&
               (effectData == null || effectData.isEmpty()) &&
               (speedData == null || speedData.isEmpty());
    }
}