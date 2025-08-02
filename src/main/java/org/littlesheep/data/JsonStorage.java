package org.littlesheep.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 优化的JSON存储实现，使用批量写入和缓存机制
 */
public class JsonStorage implements Storage {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final File backupFile;
    private final Gson gson;
    
    // 数据存储
    private final Map<UUID, Long> data = new ConcurrentHashMap<>();
    
    // 批量写入相关
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PayToFly-JsonStorage");
        t.setDaemon(true);
        return t;
    });
    
    private final AtomicBoolean needsSave = new AtomicBoolean(false);
    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final ReadWriteLock dataLock = new ReentrantReadWriteLock();
    
    // 配置参数
    private final int saveIntervalSeconds;
    private final boolean enableBackup;
    private final int maxBackups;
    
    // 统计信息
    private volatile long lastSaveTime = 0;
    private volatile int saveCount = 0;
    private volatile int writeOperations = 0;

    public JsonStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "flight_data.json");
        this.backupFile = new File(plugin.getDataFolder(), "flight_data.json.bak");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
        
        // 从配置读取参数
        this.saveIntervalSeconds = plugin.getConfig().getInt("storage.json.save-interval", 30);
        this.enableBackup = plugin.getConfig().getBoolean("storage.json.enable-backup", true);
        this.maxBackups = plugin.getConfig().getInt("storage.json.max-backups", 5);
    }

    @Override
    public void init() {
        try {
            // 确保数据目录存在
            if (!dataFile.getParentFile().exists()) {
                if (!dataFile.getParentFile().mkdirs()) {
                    throw new IOException("无法创建数据目录: " + dataFile.getParentFile());
                }
            }
            
            // 加载现有数据
            loadData();
            
            // 启动定期保存任务
            scheduler.scheduleAtFixedRate(this::saveDataIfNeeded, 
                saveIntervalSeconds, saveIntervalSeconds, TimeUnit.SECONDS);
            
            // 添加JVM关闭钩子确保数据保存
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                plugin.getLogger().info("正在关闭JSON存储，保存数据...");
                forceSave();
                scheduler.shutdown();
            }));
            
            plugin.getLogger().info(String.format(
                "JSON存储已初始化 - 数据文件: %s, 自动保存间隔: %d秒, 备份: %s", 
                dataFile.getName(), saveIntervalSeconds, enableBackup ? "启用" : "禁用"));
                
        } catch (Exception e) {
            plugin.getLogger().severe("JSON存储初始化失败: " + e.getMessage());
            throw new RuntimeException("JSON存储初始化失败", e);
        }
    }

    @Override
    public void close() {
        plugin.getLogger().info("正在关闭JSON存储...");
        
        // 停止调度器
        scheduler.shutdown();
        
        try {
            // 等待当前任务完成
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("调度器未能在10秒内正常关闭，强制关闭");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
        
        // 强制保存数据
        forceSave();
        
        plugin.getLogger().info(String.format(
            "JSON存储已关闭 - 总保存次数: %d, 写操作数: %d", 
            saveCount, writeOperations));
    }

    @Override
    public void setPlayerFlightTime(UUID uuid, long endTime) {
        dataLock.writeLock().lock();
        try {
            data.put(uuid, endTime);
            needsSave.set(true);
            writeOperations++;
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    @Override
    public Long getPlayerFlightTime(UUID uuid) {
        dataLock.readLock().lock();
        try {
            return data.get(uuid);
        } finally {
            dataLock.readLock().unlock();
        }
    }

    @Override
    public Map<UUID, Long> getAllPlayerData() {
        dataLock.readLock().lock();
        try {
            return new HashMap<>(data);
        } finally {
            dataLock.readLock().unlock();
        }
    }

    @Override
    public void removePlayerFlightTime(UUID uuid) {
        dataLock.writeLock().lock();
        try {
            if (data.remove(uuid) != null) {
                needsSave.set(true);
                writeOperations++;
            }
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    /**
     * 加载JSON数据
     */
    private void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("数据文件不存在，将创建新文件");
            return;
        }
        
        try {
            // 检查文件是否损坏
            if (!isFileValid(dataFile)) {
                plugin.getLogger().warning("主数据文件损坏，尝试加载备份文件");
                if (backupFile.exists() && isFileValid(backupFile)) {
                    Files.copy(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("已从备份文件恢复数据");
                } else {
                    plugin.getLogger().severe("主文件和备份文件都损坏，将创建新的数据文件");
                    return;
                }
            }
            
            // 加载数据
            try (Reader reader = new BufferedReader(new FileReader(dataFile))) {
                Type type = new TypeToken<Map<UUID, Long>>(){}.getType();
                Map<UUID, Long> loadedData = gson.fromJson(reader, type);
                
                if (loadedData != null) {
                    dataLock.writeLock().lock();
                    try {
                        data.clear();
                        data.putAll(loadedData);
                    } finally {
                        dataLock.writeLock().unlock();
                    }
                    
                    plugin.getLogger().info(String.format("成功加载 %d 条玩家数据", data.size()));
                    
                    // 清理过期数据
                    cleanExpiredData();
                } else {
                    plugin.getLogger().warning("数据文件为空或格式不正确");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("加载数据失败: " + e.getMessage());
            // 尝试加载备份
            if (backupFile.exists()) {
                try {
                    Files.copy(backupFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    loadData(); // 递归重试
                } catch (IOException backupError) {
                    plugin.getLogger().severe("备份恢复也失败: " + backupError.getMessage());
                }
            }
        }
    }

    /**
     * 检查文件是否有效
     */
    private boolean isFileValid(File file) {
        try (Reader reader = new FileReader(file)) {
            gson.fromJson(reader, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清理过期数据
     */
    private void cleanExpiredData() {
        long now = System.currentTimeMillis();
        int removedCount = 0;
        
        dataLock.writeLock().lock();
        try {
            removedCount = data.entrySet().removeIf(entry -> entry.getValue() < now) ? 1 : 0;
            if (removedCount > 0) {
                needsSave.set(true);
            }
        } finally {
            dataLock.writeLock().unlock();
        }
        
        if (removedCount > 0) {
            plugin.getLogger().info(String.format("清理了 %d 条过期数据", removedCount));
        }
    }

    /**
     * 如果需要则保存数据
     */
    private void saveDataIfNeeded() {
        if (needsSave.compareAndSet(true, false)) {
            saveDataAsync();
        }
    }

    /**
     * 异步保存数据
     */
    private void saveDataAsync() {
        if (isSaving.compareAndSet(false, true)) {
            try {
                saveDataToFile();
                lastSaveTime = System.currentTimeMillis();
                saveCount++;
            } catch (Exception e) {
                plugin.getLogger().severe("保存数据失败: " + e.getMessage());
                needsSave.set(true); // 标记需要重新保存
            } finally {
                isSaving.set(false);
            }
        }
    }

    /**
     * 强制同步保存
     */
    public void forceSave() {
        if (needsSave.get() || isSaving.get()) {
            // 等待当前保存完成
            while (isSaving.get()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 强制保存
            try {
                saveDataToFile();
                needsSave.set(false);
                lastSaveTime = System.currentTimeMillis();
                saveCount++;
            } catch (Exception e) {
                plugin.getLogger().severe("强制保存失败: " + e.getMessage());
            }
        }
    }

    /**
     * 保存数据到文件
     */
    private void saveDataToFile() throws IOException {
        // 创建备份
        if (enableBackup && dataFile.exists()) {
            try {
                Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().warning("创建备份失败: " + e.getMessage());
            }
        }
        
        // 使用临时文件避免写入过程中的数据损坏
        File tempFile = new File(dataFile.getParentFile(), dataFile.getName() + ".tmp");
        
        Map<UUID, Long> dataToSave;
        dataLock.readLock().lock();
        try {
            dataToSave = new HashMap<>(data);
        } finally {
            dataLock.readLock().unlock();
        }
        
        // 写入临时文件
        try (Writer writer = new BufferedWriter(new FileWriter(tempFile))) {
            gson.toJson(dataToSave, writer);
        }
        
        // 原子性替换
        Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        // 删除旧的备份文件（保留指定数量）
        if (enableBackup) {
            manageBackupFiles();
        }
    }

    /**
     * 管理备份文件数量
     */
    private void manageBackupFiles() {
        File[] backups = dataFile.getParentFile().listFiles((dir, name) -> 
            name.startsWith(dataFile.getName() + ".bak"));
        
        if (backups != null && backups.length > maxBackups) {
            // 按修改时间排序，删除最老的
            java.util.Arrays.sort(backups, (a, b) -> 
                Long.compare(a.lastModified(), b.lastModified()));
            
            for (int i = 0; i < backups.length - maxBackups; i++) {
                if (!backups[i].delete()) {
                    plugin.getLogger().warning("无法删除旧备份文件: " + backups[i].getName());
                }
            }
        }
    }

    /**
     * 获取存储统计信息
     */
    public String getStatistics() {
        dataLock.readLock().lock();
        try {
            return String.format(
                "JSON存储统计: 数据条数=%d, 保存次数=%d, 写操作=%d, 最后保存=%s, 待保存=%s",
                data.size(),
                saveCount,
                writeOperations,
                lastSaveTime > 0 ? new java.util.Date(lastSaveTime).toString() : "从未",
                needsSave.get() ? "是" : "否"
            );
        } finally {
            dataLock.readLock().unlock();
        }
    }

    /**
     * 手动触发数据清理
     */
    public void cleanupExpiredData() {
        cleanExpiredData();
    }
}