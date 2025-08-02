package org.littlesheep.data;

import java.util.HashSet;
import java.util.Set;

/**
 * 玩家数据类 - 包含飞行时间、购买的特效和速度
 */
public class PlayerData {
    private Long flightEndTime;
    private Set<String> purchasedEffects;
    private Set<String> purchasedSpeeds;
    
    public PlayerData() {
        this.flightEndTime = null;
        this.purchasedEffects = new HashSet<>();
        this.purchasedSpeeds = new HashSet<>();
    }
    
    public PlayerData(Long flightEndTime, Set<String> purchasedEffects, Set<String> purchasedSpeeds) {
        this.flightEndTime = flightEndTime;
        this.purchasedEffects = purchasedEffects != null ? new HashSet<>(purchasedEffects) : new HashSet<>();
        this.purchasedSpeeds = purchasedSpeeds != null ? new HashSet<>(purchasedSpeeds) : new HashSet<>();
    }
    
    // 飞行时间相关
    public Long getFlightEndTime() {
        return flightEndTime;
    }
    
    public void setFlightEndTime(Long flightEndTime) {
        this.flightEndTime = flightEndTime;
    }
    
    // 特效相关
    public Set<String> getPurchasedEffects() {
        return new HashSet<>(purchasedEffects);
    }
    
    public void addEffect(String effectName) {
        this.purchasedEffects.add(effectName);
    }
    
    public void removeEffect(String effectName) {
        this.purchasedEffects.remove(effectName);
    }
    
    public boolean hasEffect(String effectName) {
        return this.purchasedEffects.contains(effectName);
    }
    
    // 速度相关
    public Set<String> getPurchasedSpeeds() {
        return new HashSet<>(purchasedSpeeds);
    }
    
    public void addSpeed(String speedName) {
        this.purchasedSpeeds.add(speedName);
    }
    
    public void removeSpeed(String speedName) {
        this.purchasedSpeeds.remove(speedName);
    }
    
    public boolean hasSpeed(String speedName) {
        return this.purchasedSpeeds.contains(speedName);
    }
    
    // 检查是否为空数据
    public boolean isEmpty() {
        return flightEndTime == null && purchasedEffects.isEmpty() && purchasedSpeeds.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("PlayerData{flightEndTime=%d, effects=%s, speeds=%s}", 
            flightEndTime, purchasedEffects, purchasedSpeeds);
    }
}