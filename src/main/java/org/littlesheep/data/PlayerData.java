package org.littlesheep.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 玩家数据类 - 包含飞行时间、购买的特效和速度
 */
public class PlayerData {
    private Long flightEndTime;
    private Set<String> purchasedEffects;
    private Set<String> purchasedSpeeds;
    private Map<String, Long> effectTimes;
    private Map<String, Long> speedTimes;
    
    public PlayerData() {
        this.flightEndTime = null;
        this.purchasedEffects = new HashSet<>();
        this.purchasedSpeeds = new HashSet<>();
        this.effectTimes = new HashMap<>();
        this.speedTimes = new HashMap<>();
    }
    
    public PlayerData(Long flightEndTime, Set<String> purchasedEffects, Set<String> purchasedSpeeds) {
        this.flightEndTime = flightEndTime;
        this.purchasedEffects = purchasedEffects != null ? new HashSet<>(purchasedEffects) : new HashSet<>();
        this.purchasedSpeeds = purchasedSpeeds != null ? new HashSet<>(purchasedSpeeds) : new HashSet<>();
        this.effectTimes = new HashMap<>();
        this.speedTimes = new HashMap<>();
    }
    
    public PlayerData(Long flightEndTime, Set<String> purchasedEffects, Set<String> purchasedSpeeds, 
                     Map<String, Long> effectTimes, Map<String, Long> speedTimes) {
        this.flightEndTime = flightEndTime;
        this.purchasedEffects = purchasedEffects != null ? new HashSet<>(purchasedEffects) : new HashSet<>();
        this.purchasedSpeeds = purchasedSpeeds != null ? new HashSet<>(purchasedSpeeds) : new HashSet<>();
        this.effectTimes = effectTimes != null ? new HashMap<>(effectTimes) : new HashMap<>();
        this.speedTimes = speedTimes != null ? new HashMap<>(speedTimes) : new HashMap<>();
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
    
    // 时间限制特效相关
    public Map<String, Long> getEffectTimes() {
        return new HashMap<>(effectTimes);
    }
    
    public void setEffectTime(String effectName, Long endTime) {
        this.effectTimes.put(effectName, endTime);
    }
    
    public Long getEffectTime(String effectName) {
        return this.effectTimes.get(effectName);
    }
    
    public void removeEffectTime(String effectName) {
        this.effectTimes.remove(effectName);
    }
    
    public boolean hasEffectTime(String effectName) {
        Long endTime = this.effectTimes.get(effectName);
        return endTime != null && endTime > System.currentTimeMillis();
    }
    
    // 时间限制速度相关
    public Map<String, Long> getSpeedTimes() {
        return new HashMap<>(speedTimes);
    }
    
    public void setSpeedTime(String speedName, Long endTime) {
        this.speedTimes.put(speedName, endTime);
    }
    
    public Long getSpeedTime(String speedName) {
        return this.speedTimes.get(speedName);
    }
    
    public void removeSpeedTime(String speedName) {
        this.speedTimes.remove(speedName);
    }
    
    public boolean hasSpeedTime(String speedName) {
        Long endTime = this.speedTimes.get(speedName);
        return endTime != null && endTime > System.currentTimeMillis();
    }
    
    /**
     * 检查玩家是否有特效权限（永久或时间限制）
     */
    public boolean hasEffectAccess(String effectName) {
        return hasEffect(effectName) || hasEffectTime(effectName);
    }
    
    /**
     * 检查玩家是否有速度权限（永久或时间限制）
     */
    public boolean hasSpeedAccess(String speedName) {
        return hasSpeed(speedName) || hasSpeedTime(speedName);
    }
    
    // 检查是否为空数据
    public boolean isEmpty() {
        return flightEndTime == null && purchasedEffects.isEmpty() && purchasedSpeeds.isEmpty() && 
               effectTimes.isEmpty() && speedTimes.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("PlayerData{flightEndTime=%d, effects=%s, speeds=%s, effectTimes=%s, speedTimes=%s}", 
            flightEndTime, purchasedEffects, purchasedSpeeds, effectTimes, speedTimes);
    }
}