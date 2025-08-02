package org.littlesheep.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 统一异常处理器，提供重试机制、断路器模式和异常统计
 */
public class ExceptionHandler {
    private final JavaPlugin plugin;
    private final AtomicInteger totalRetries = new AtomicInteger(0);
    private final AtomicInteger successfulRetries = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    
    // 默认重试配置
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_BASE_DELAY_MS = 100;
    private static final long DEFAULT_MAX_DELAY_MS = 5000;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    
    public ExceptionHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 执行操作，带重试机制
     */
    public <T> T executeWithRetry(String operationName, Callable<T> operation) {
        return executeWithRetry(operationName, operation, DEFAULT_MAX_RETRIES, 
            DEFAULT_BASE_DELAY_MS, this::isRetryableException);
    }

    /**
     * 执行操作，自定义重试配置
     */
    public <T> T executeWithRetry(String operationName, Callable<T> operation, 
                                 int maxRetries, long baseDelayMs, 
                                 Predicate<Exception> isRetryable) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                T result = operation.call();
                
                if (attempt > 1) {
                    successfulRetries.incrementAndGet();
                    plugin.getLogger().info(String.format(
                        "操作 '%s' 在第 %d 次尝试后成功", operationName, attempt));
                }
                
                return result;
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt > maxRetries) {
                    // 最后一次尝试失败
                    failedOperations.incrementAndGet();
                    plugin.getLogger().severe(String.format(
                        "操作 '%s' 在 %d 次尝试后最终失败: %s", 
                        operationName, maxRetries + 1, e.getMessage()));
                    break;
                }
                
                if (!isRetryable.test(e)) {
                    // 不可重试的异常
                    failedOperations.incrementAndGet();
                    plugin.getLogger().warning(String.format(
                        "操作 '%s' 遇到不可重试异常: %s", operationName, e.getMessage()));
                    break;
                }
                
                // 准备重试
                totalRetries.incrementAndGet();
                long delayMs = calculateRetryDelay(attempt - 1, baseDelayMs);
                
                plugin.getLogger().warning(String.format(
                    "操作 '%s' 第 %d 次尝试失败，将在 %dms 后重试: %s", 
                    operationName, attempt, delayMs, e.getMessage()));
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    plugin.getLogger().warning("重试过程被中断");
                    break;
                }
            }
        }
        
        // 所有尝试都失败了
        throw new OperationFailedException(String.format(
            "操作 '%s' 在 %d 次尝试后失败", operationName, maxRetries + 1), lastException);
    }

    /**
     * 异步执行操作，带重试机制
     */
    public <T> CompletableFuture<T> executeAsyncWithRetry(String operationName, 
                                                         Supplier<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeWithRetry(operationName, operation::get);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 安全执行操作，不抛出异常
     */
    public <T> SafeResult<T> executeSafely(String operationName, Callable<T> operation) {
        try {
            T result = executeWithRetry(operationName, operation);
            return SafeResult.success(result);
        } catch (Exception e) {
            plugin.getLogger().warning(String.format(
                "安全执行操作 '%s' 失败: %s", operationName, e.getMessage()));
            return SafeResult.failure(e);
        }
    }

    /**
     * 计算重试延迟（指数退避算法）
     */
    private long calculateRetryDelay(int attempt, long baseDelayMs) {
        // 指数退避 + 随机抖动
        long delay = (long) (baseDelayMs * Math.pow(DEFAULT_BACKOFF_MULTIPLIER, attempt));
        
        // 添加随机抖动（-25% 到 +25%）
        double jitter = 0.75 + (ThreadLocalRandom.current().nextDouble() * 0.5);
        delay = (long) (delay * jitter);
        
        // 限制最大延迟
        return Math.min(delay, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * 判断异常是否可重试
     */
    private boolean isRetryableException(Exception e) {
        // 网络相关异常通常可重试
        if (e instanceof java.net.SocketException ||
            e instanceof java.net.ConnectException ||
            e instanceof java.net.SocketTimeoutException ||
            e instanceof java.io.IOException) {
            return true;
        }
        
        // SQL异常中的一些情况可重试
        if (e instanceof java.sql.SQLException) {
            java.sql.SQLException sqlEx = (java.sql.SQLException) e;
            String sqlState = sqlEx.getSQLState();
            
            // 连接相关错误
            if (sqlState != null && (
                sqlState.startsWith("08") ||  // 连接异常
                sqlState.startsWith("40") ||  // 事务回滚
                sqlState.equals("HY000"))) { // 一般错误
                return true;
            }
        }
        
        // 检查异常消息中的关键词
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("timeout") ||
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("network") ||
                   lowerMessage.contains("temporary") ||
                   lowerMessage.contains("retry");
        }
        
        return false;
    }

    /**
     * 断路器模式实现
     */
    public static class CircuitBreaker {
        private final int failureThreshold;
        private final long recoveryTimeoutMs;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        
        public enum State { CLOSED, OPEN, HALF_OPEN }
        private volatile State state = State.CLOSED;

        public CircuitBreaker(int failureThreshold, long recoveryTimeoutMs) {
            this.failureThreshold = failureThreshold;
            this.recoveryTimeoutMs = recoveryTimeoutMs;
        }

        public <T> T execute(Callable<T> operation) throws Exception {
            State currentState = getState();
            
            if (currentState == State.OPEN) {
                throw new CircuitBreakerOpenException("断路器已打开，操作被拒绝");
            }
            
            try {
                T result = operation.call();
                onSuccess();
                return result;
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }

        private void onSuccess() {
            failureCount.set(0);
            state = State.CLOSED;
        }

        private void onFailure() {
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            
            if (failureCount.get() >= failureThreshold) {
                state = State.OPEN;
            }
        }

        private State getState() {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime.get() >= recoveryTimeoutMs) {
                    state = State.HALF_OPEN;
                    failureCount.set(0);
                }
            }
            return state;
        }

        public State getCurrentState() {
            return getState();
        }
    }

    /**
     * 安全执行结果类
     */
    public static class SafeResult<T> {
        private final boolean success;
        private final T data;
        private final Exception exception;

        private SafeResult(boolean success, T data, Exception exception) {
            this.success = success;
            this.data = data;
            this.exception = exception;
        }

        public static <T> SafeResult<T> success(T data) {
            return new SafeResult<>(true, data, null);
        }

        public static <T> SafeResult<T> failure(Exception exception) {
            return new SafeResult<>(false, null, exception);
        }

        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public Exception getException() { return exception; }
        
        public T getDataOrDefault(T defaultValue) {
            return success ? data : defaultValue;
        }
        
        public T getDataOrThrow() {
            if (!success) {
                throw new RuntimeException(exception);
            }
            return data;
        }
    }

    /**
     * 操作失败异常
     */
    public static class OperationFailedException extends RuntimeException {
        public OperationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 断路器打开异常
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    /**
     * 获取异常处理统计信息
     */
    public String getStatistics() {
        long currentTime = System.currentTimeMillis();
        long timeSinceReset = currentTime - lastResetTime.get();
        double hoursElapsed = timeSinceReset / (1000.0 * 60.0 * 60.0);
        
        return String.format(
            "异常处理统计: 总重试=%d, 成功重试=%d, 失败操作=%d, 成功率=%.1f%%, 运行时间=%.1f小时",
            totalRetries.get(),
            successfulRetries.get(),
            failedOperations.get(),
            hoursElapsed > 0 ? (successfulRetries.get() * 100.0 / Math.max(1, totalRetries.get())) : 0.0,
            hoursElapsed
        );
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalRetries.set(0);
        successfulRetries.set(0);
        failedOperations.set(0);
        lastResetTime.set(System.currentTimeMillis());
        plugin.getLogger().info("异常处理统计信息已重置");
    }

    /**
     * 创建数据库操作专用的重试配置
     */
    public static RetryConfig forDatabaseOperations() {
        return new RetryConfig()
            .maxRetries(3)
            .baseDelay(200, TimeUnit.MILLISECONDS)
            .maxDelay(5, TimeUnit.SECONDS)
            .exponentialBackoff(2.0)
            .retryIf(e -> e instanceof java.sql.SQLException || 
                         e instanceof java.io.IOException);
    }

    /**
     * 创建网络操作专用的重试配置
     */
    public static RetryConfig forNetworkOperations() {
        return new RetryConfig()
            .maxRetries(5)
            .baseDelay(500, TimeUnit.MILLISECONDS)
            .maxDelay(10, TimeUnit.SECONDS)
            .exponentialBackoff(1.5)
            .retryIf(e -> e instanceof java.net.SocketException ||
                         e instanceof java.net.ConnectException ||
                         e instanceof java.io.IOException);
    }

    /**
     * 重试配置类
     */
    public static class RetryConfig {
        private int maxRetries = 3;
        private long baseDelayMs = 100;
        private long maxDelayMs = 5000;
        private double backoffMultiplier = 2.0;
        private Predicate<Exception> retryPredicate = e -> true;

        public RetryConfig maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public RetryConfig baseDelay(long delay, TimeUnit unit) {
            this.baseDelayMs = unit.toMillis(delay);
            return this;
        }

        public RetryConfig maxDelay(long delay, TimeUnit unit) {
            this.maxDelayMs = unit.toMillis(delay);
            return this;
        }

        public RetryConfig exponentialBackoff(double multiplier) {
            this.backoffMultiplier = multiplier;
            return this;
        }

        public RetryConfig retryIf(Predicate<Exception> predicate) {
            this.retryPredicate = predicate;
            return this;
        }

        // Getters
        public int getMaxRetries() { return maxRetries; }
        public long getBaseDelayMs() { return baseDelayMs; }
        public long getMaxDelayMs() { return maxDelayMs; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public Predicate<Exception> getRetryPredicate() { return retryPredicate; }
    }
}