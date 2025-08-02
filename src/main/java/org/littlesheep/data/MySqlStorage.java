package org.littlesheep.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.littlesheep.utils.ExceptionHandler;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MySQL存储实现，使用连接池和重试机制
 */
public class MySqlStorage implements Storage {
    private final JavaPlugin plugin;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String table;
    
    // 连接池参数
    private final int maxConnections;
    private final int connectionTimeout;
    private final int maxRetries;
    private final ExceptionHandler exceptionHandler;
    
    // 简单连接池实现
    private final Map<Connection, Boolean> connectionPool = new ConcurrentHashMap<>();
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final Object poolLock = new Object();

    public MySqlStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        
        // 数据库配置
        this.host = config.getString("storage.mysql.host", "localhost");
        this.port = config.getInt("storage.mysql.port", 3306);
        this.database = config.getString("storage.mysql.database", "minecraft");
        this.username = config.getString("storage.mysql.username", "root");
        this.password = config.getString("storage.mysql.password", "password");
        this.table = config.getString("storage.mysql.table", "paytofly_data");
        
        // 连接池配置
        this.maxConnections = config.getInt("storage.mysql.pool.max-connections", 5);
        this.connectionTimeout = config.getInt("storage.mysql.pool.connection-timeout", 30000);
        this.maxRetries = config.getInt("storage.mysql.pool.max-retries", 3);
        
        // 获取异常处理器
        if (plugin instanceof org.littlesheep.paytofly) {
            this.exceptionHandler = ((org.littlesheep.paytofly) plugin).getExceptionHandler();
        } else {
            this.exceptionHandler = new ExceptionHandler(plugin);
        }
    }

    @Override
    public void init() {
        try {
            // 使用新的MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 预创建一个连接来测试连接性
            Connection testConnection = createNewConnection();
            if (testConnection != null) {
                createTable(testConnection);
                returnConnection(testConnection);
                plugin.getLogger().info("MySQL数据库连接成功，连接池已初始化");
            } else {
                throw new SQLException("无法创建测试连接");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("无法连接到MySQL数据库: " + e.getMessage());
            throw new RuntimeException("MySQL初始化失败", e);
        }
    }

    /**
     * 创建新的数据库连接
     */
    private Connection createNewConnection() throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                host, port, database);
        
        Connection connection = DriverManager.getConnection(url, username, password);
        connection.setAutoCommit(true);
        
        // 设置连接超时
        try (Statement stmt = connection.createStatement()) {
            stmt.setQueryTimeout(30);
        }
        
        return connection;
    }

    /**
     * 从连接池获取连接
     */
    private Connection getConnection() throws SQLException {
        synchronized (poolLock) {
            // 查找可用连接
            for (Map.Entry<Connection, Boolean> entry : connectionPool.entrySet()) {
                if (!entry.getValue()) { // 连接空闲
                    Connection conn = entry.getKey();
                    if (isConnectionValid(conn)) {
                        connectionPool.put(conn, true); // 标记为使用中
                        return conn;
                    } else {
                        // 移除无效连接
                        connectionPool.remove(conn);
                        try {
                            conn.close();
                        } catch (SQLException ignored) {}
                    }
                }
            }
            
            // 如果没有可用连接且未达到最大连接数，创建新连接
            if (activeConnections.get() < maxConnections) {
                Connection newConnection = createNewConnection();
                connectionPool.put(newConnection, true);
                activeConnections.incrementAndGet();
                return newConnection;
            }
            
            // 等待可用连接（简单实现）
            try {
                poolLock.wait(connectionTimeout);
                return getConnection(); // 递归重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("获取连接被中断", e);
            }
        }
    }

    /**
     * 归还连接到池
     */
    private void returnConnection(Connection connection) {
        if (connection != null) {
            synchronized (poolLock) {
                connectionPool.put(connection, false); // 标记为空闲
                poolLock.notifyAll();
            }
        }
    }

    /**
     * 检查连接是否有效
     */
    private boolean isConnectionValid(Connection connection) {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 创建表结构
     */
    private void createTable(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // 飞行时间表
            String createFlightTableSQL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "end_time BIGINT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                table
            );
            stmt.executeUpdate(createFlightTableSQL);
            
            // 特效购买记录表
            String createEffectsTableSQL = String.format(
                "CREATE TABLE IF NOT EXISTS %s_effects (" +
                "uuid VARCHAR(36), " +
                "effect_name VARCHAR(50), " +
                "purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (uuid, effect_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                table
            );
            stmt.executeUpdate(createEffectsTableSQL);
            
            // 速度购买记录表
            String createSpeedsTableSQL = String.format(
                "CREATE TABLE IF NOT EXISTS %s_speeds (" +
                "uuid VARCHAR(36), " +
                "speed_name VARCHAR(50), " +
                "purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (uuid, speed_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
                table
            );
            stmt.executeUpdate(createSpeedsTableSQL);
        }
    }

    /**
     * 执行数据库操作（使用统一异常处理器）
     */
    private <T> T executeWithRetry(String operationName, DatabaseOperation<T> operation) {
        return exceptionHandler.executeWithRetry(operationName, () -> {
            Connection connection = null;
            try {
                connection = getConnection();
                T result = operation.execute(connection);
                return result;
            } catch (SQLException e) {
                // 如果是连接问题，移除无效连接
                if (connection != null && !isConnectionValid(connection)) {
                    synchronized (poolLock) {
                        connectionPool.remove(connection);
                        activeConnections.decrementAndGet();
                    }
                    try {
                        connection.close();
                    } catch (SQLException ignored) {}
                    connection = null;
                }
                throw e;
            } finally {
                if (connection != null) {
                    returnConnection(connection);
                }
            }
        }, maxRetries, 200, this::isDatabaseRetryableException);
    }

    /**
     * 判断数据库异常是否可重试
     */
    private boolean isDatabaseRetryableException(Exception e) {
        if (!(e instanceof SQLException)) {
            return false;
        }
        
        SQLException sqlEx = (SQLException) e;
        String sqlState = sqlEx.getSQLState();
        
        // 连接相关错误可重试
        if (sqlState != null && (
            sqlState.startsWith("08") ||  // 连接异常
            sqlState.startsWith("40") ||  // 事务回滚
            sqlState.equals("HY000"))) { // 一般错误
            return true;
        }
        
        // 检查错误消息
        String message = sqlEx.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("timeout") ||
                   lowerMessage.contains("connection") ||
                   lowerMessage.contains("network") ||
                   lowerMessage.contains("communications link failure") ||
                   lowerMessage.contains("broken pipe");
        }
        
        return false;
    }

    @Override
    public void close() {
        synchronized (poolLock) {
            for (Connection connection : connectionPool.keySet()) {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    plugin.getLogger().warning("关闭数据库连接时出错: " + e.getMessage());
                }
            }
            connectionPool.clear();
            activeConnections.set(0);
        }
        plugin.getLogger().info("MySQL连接池已关闭");
    }

    @Override
    public void setPlayerFlightTime(UUID uuid, long endTime) {
        try {
            executeWithRetry("setPlayerFlightTime", connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                    "REPLACE INTO " + table + " (uuid, end_time) VALUES (?, ?)"
                )) {
                    stmt.setString(1, uuid.toString());
                    stmt.setLong(2, endTime);
                    stmt.executeUpdate();
                    return null;
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("保存玩家飞行时间失败: " + e.getMessage());
        }
    }

    @Override
    public Long getPlayerFlightTime(UUID uuid) {
        try {
            return executeWithRetry("getPlayerFlightTime", connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT end_time FROM " + table + " WHERE uuid = ?"
                )) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("end_time");
                        }
                        return null;
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("获取玩家飞行时间失败: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void removePlayerFlightTime(UUID uuid) {
        try {
            executeWithRetry("removePlayerFlightTime", connection -> {
                try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM " + table + " WHERE uuid = ?"
                )) {
                    stmt.setString(1, uuid.toString());
                    stmt.executeUpdate();
                    return null;
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("删除玩家飞行时间失败: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Long> getAllPlayerData() {
        try {
            return executeWithRetry("getAllPlayerData", connection -> {
                Map<UUID, Long> data = new HashMap<>();
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT uuid, end_time FROM " + table)) {
                    
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            long endTime = rs.getLong("end_time");
                            data.put(uuid, endTime);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("跳过无效的UUID: " + rs.getString("uuid"));
                        }
                    }
                }
                return data;
            });
        } catch (Exception e) {
            plugin.getLogger().severe("获取所有玩家数据失败: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 数据库操作函数式接口
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T execute(Connection connection) throws SQLException;
    }

    /**
     * 获取连接池状态信息
     */
    public String getPoolStatus() {
        synchronized (poolLock) {
            long activeCount = connectionPool.values().stream().mapToLong(inUse -> inUse ? 1 : 0).sum();
            return String.format("连接池状态: %d/%d 活跃, %d 总连接", 
                activeCount, maxConnections, connectionPool.size());
        }
    }

    /**
     * 获取数据库操作统计信息
     */
    public String getDatabaseStatistics() {
        return String.format("MySQL存储统计: %s | %s", 
            getPoolStatus(), 
            exceptionHandler != null ? exceptionHandler.getStatistics() : "异常处理器未初始化");
    }

    // ============= 特效购买记录相关方法 =============
    
    @Override
    public void addPlayerEffect(UUID uuid, String effectName) {
        executeWithRetry("添加玩家特效", connection -> {
            String sql = String.format("INSERT INTO %s_effects (uuid, effect_name) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE purchased_at = CURRENT_TIMESTAMP", table);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, effectName);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void removePlayerEffect(UUID uuid, String effectName) {
        executeWithRetry("删除玩家特效", connection -> {
            String sql = String.format("DELETE FROM %s_effects WHERE uuid = ? AND effect_name = ?", table);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, effectName);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Set<String> getPlayerEffects(UUID uuid) {
        return executeWithRetry("获取玩家特效", connection -> {
            Set<String> effects = new HashSet<>();
            String sql = String.format("SELECT effect_name FROM %s_effects WHERE uuid = ?", table);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        effects.add(rs.getString("effect_name"));
                    }
                }
            }
            return effects;
        });
    }

    @Override
    public Map<UUID, Set<String>> getAllPlayerEffects() {
        return executeWithRetry("获取所有玩家特效", connection -> {
            Map<UUID, Set<String>> allEffects = new HashMap<>();
            String sql = String.format("SELECT uuid, effect_name FROM %s_effects", table);
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String effectName = rs.getString("effect_name");
                    allEffects.computeIfAbsent(uuid, k -> new HashSet<>()).add(effectName);
                }
            }
            return allEffects;
        });
    }

    // ============= 速度购买记录相关方法 =============
    
    @Override
    public void addPlayerSpeed(UUID uuid, String speedName) {
        executeWithRetry("添加玩家速度", connection -> {
            String sql = String.format("INSERT INTO %s_speeds (uuid, speed_name) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE purchased_at = CURRENT_TIMESTAMP", table);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, speedName);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void removePlayerSpeed(UUID uuid, String speedName) {
        executeWithRetry("删除玩家速度", connection -> {
            String sql = String.format("DELETE FROM %s_speeds WHERE uuid = ? AND speed_name = ?", table);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, speedName);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public Set<String> getPlayerSpeeds(UUID uuid) {
        return executeWithRetry("获取玩家速度", connection -> {
            Set<String> speeds = new HashSet<>();
            String sql = String.format("SELECT speed_name FROM %s_speeds WHERE uuid = ?", table);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        speeds.add(rs.getString("speed_name"));
                    }
                }
            }
            return speeds;
        });
    }

    @Override
    public Map<UUID, Set<String>> getAllPlayerSpeeds() {
        return executeWithRetry("获取所有玩家速度", connection -> {
            Map<UUID, Set<String>> allSpeeds = new HashMap<>();
            String sql = String.format("SELECT uuid, speed_name FROM %s_speeds", table);
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String speedName = rs.getString("speed_name");
                    allSpeeds.computeIfAbsent(uuid, k -> new HashSet<>()).add(speedName);
                }
            }
            return allSpeeds;
        });
    }
}