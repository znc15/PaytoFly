package org.littlesheep.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 统一资源管理器，确保所有资源正确清理，防止内存泄漏
 */
public class ResourceManager implements Closeable {
    private final JavaPlugin plugin;
    private final List<Closeable> closeableResources = new CopyOnWriteArrayList<>();
    private final List<BukkitTask> tasks = new CopyOnWriteArrayList<>();
    private final List<ExecutorService> executors = new CopyOnWriteArrayList<>();
    private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
    
    private volatile boolean closed = false;

    public ResourceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // 添加JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!closed) {
                plugin.getLogger().info("JVM关闭中，清理PayToFly资源...");
                close();
            }
        }));
    }

    /**
     * 注册可关闭资源
     */
    public <T extends Closeable> T registerCloseable(T resource) {
        if (resource != null && !closed) {
            closeableResources.add(resource);
            plugin.getLogger().fine("注册可关闭资源: " + resource.getClass().getSimpleName());
        }
        return resource;
    }

    /**
     * 注册Bukkit任务
     */
    public BukkitTask registerTask(BukkitTask task) {
        if (task != null && !closed) {
            tasks.add(task);
            plugin.getLogger().fine("注册Bukkit任务: " + task.getTaskId());
        }
        return task;
    }

    /**
     * 注册线程池
     */
    public <T extends ExecutorService> T registerExecutor(T executor) {
        if (executor != null && !closed) {
            executors.add(executor);
            plugin.getLogger().fine("注册线程池: " + executor.getClass().getSimpleName());
        }
        return executor;
    }

    /**
     * 注册关闭钩子
     */
    public void registerShutdownHook(Runnable hook) {
        if (hook != null && !closed) {
            shutdownHooks.add(hook);
            plugin.getLogger().fine("注册关闭钩子");
        }
    }

    /**
     * 移除资源（如果提前清理）
     */
    public void unregisterCloseable(Closeable resource) {
        closeableResources.remove(resource);
    }

    public void unregisterTask(BukkitTask task) {
        tasks.remove(task);
    }

    public void unregisterExecutor(ExecutorService executor) {
        executors.remove(executor);
    }

    /**
     * 清理所有资源
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        plugin.getLogger().info("开始清理所有资源...");
        
        int totalResources = shutdownHooks.size() + closeableResources.size() + 
                            tasks.size() + executors.size();
        int cleanedResources = 0;
        
        // 1. 执行关闭钩子
        plugin.getLogger().info("执行关闭钩子...");
        for (Runnable hook : shutdownHooks) {
            try {
                hook.run();
                cleanedResources++;
            } catch (Exception e) {
                plugin.getLogger().warning("执行关闭钩子失败: " + e.getMessage());
            }
        }
        shutdownHooks.clear();
        
        // 2. 取消所有Bukkit任务
        plugin.getLogger().info("取消Bukkit任务...");
        for (BukkitTask task : tasks) {
            try {
                if (!task.isCancelled()) {
                    task.cancel();
                    cleanedResources++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("取消任务失败 (ID: " + task.getTaskId() + "): " + e.getMessage());
            }
        }
        tasks.clear();
        
        // 3. 关闭线程池
        plugin.getLogger().info("关闭线程池...");
        for (ExecutorService executor : executors) {
            try {
                shutdownExecutor(executor);
                cleanedResources++;
            } catch (Exception e) {
                plugin.getLogger().warning("关闭线程池失败: " + e.getMessage());
            }
        }
        executors.clear();
        
        // 4. 关闭其他资源
        plugin.getLogger().info("关闭其他资源...");
        for (Closeable resource : closeableResources) {
            try {
                resource.close();
                cleanedResources++;
            } catch (Exception e) {
                plugin.getLogger().warning("关闭资源失败 (" + 
                    resource.getClass().getSimpleName() + "): " + e.getMessage());
            }
        }
        closeableResources.clear();
        
        plugin.getLogger().info(String.format(
            "资源清理完成: %d/%d 个资源已清理", cleanedResources, totalResources));
    }

    /**
     * 优雅关闭线程池
     */
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        
        try {
            // 等待5秒
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("线程池未在5秒内关闭，强制关闭");
                executor.shutdownNow();
                
                // 再等待2秒
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("线程池强制关闭失败");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    /**
     * 获取资源统计信息
     */
    public String getResourceStatistics() {
        return String.format(
            "ResourceManager状态: 关闭钩子=%d, 可关闭资源=%d, Bukkit任务=%d, 线程池=%d, 已关闭=%s",
            shutdownHooks.size(),
            closeableResources.size(),
            tasks.size(),
            executors.size(),
            closed
        );
    }

    /**
     * 检查是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 强制垃圾回收（仅用于调试）
     */
    public void forceGarbageCollection() {
        plugin.getLogger().info("强制垃圾回收...");
        System.gc();
        
        // 等待一小段时间让GC完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        plugin.getLogger().info(String.format(
            "内存使用情况: 已用=%.2fMB, 总计=%.2fMB, 使用率=%.1f%%",
            usedMemory / 1024.0 / 1024.0,
            totalMemory / 1024.0 / 1024.0,
            (usedMemory * 100.0) / totalMemory
        ));
    }
}