package org.littlesheep.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySqlStorage implements Storage {
    private final JavaPlugin plugin;
    private Connection connection;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String table;

    public MySqlStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.host = config.getString("storage.mysql.host", "localhost");
        this.port = config.getInt("storage.mysql.port", 3306);
        this.database = config.getString("storage.mysql.database", "minecraft");
        this.username = config.getString("storage.mysql.username", "root");
        this.password = config.getString("storage.mysql.password", "password");
        this.table = config.getString("storage.mysql.table", "paytofly_data");
    }

    @Override
    public void init() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database, username, password
            );
            createTable();
        } catch (Exception e) {
            plugin.getLogger().severe("无法连接到MySQL数据库: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + table + " (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "end_time BIGINT)"
            );
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("关闭数据库连接时出错: " + e.getMessage());
        }
    }

    @Override
    public void setPlayerFlightTime(UUID uuid, long endTime) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "REPLACE INTO " + table + " (uuid, end_time) VALUES (?, ?)"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, endTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家数据时出错: " + e.getMessage());
        }
    }

    @Override
    public Long getPlayerFlightTime(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT end_time FROM " + table + " WHERE uuid = ?"
        )) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("end_time");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家数据时出错: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void removePlayerFlightTime(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM " + table + " WHERE uuid = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("删除玩家数据时出错: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Long> getAllPlayerData() {
        Map<UUID, Long> data = new HashMap<>();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long endTime = rs.getLong("end_time");
                data.put(uuid, endTime);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取所有玩家数据时出错: " + e.getMessage());
        }
        return data;
    }
} 