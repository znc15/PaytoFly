package org.littlesheep.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SqliteStorage implements Storage {
    private final JavaPlugin plugin;
    private Connection connection;
    private final String dbFile;

    public SqliteStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), 
            plugin.getConfig().getString("storage.sqlite.file", "database.db")).getAbsolutePath();
    }

    @Override
    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            createTable();
        } catch (Exception e) {
            plugin.getLogger().severe("无法初始化SQLite数据库: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_flight_data (" +
                "uuid TEXT PRIMARY KEY, " +
                "end_time INTEGER)"
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
            "INSERT OR REPLACE INTO player_flight_data (uuid, end_time) VALUES (?, ?)"
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
            "SELECT end_time FROM player_flight_data WHERE uuid = ?"
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
            "DELETE FROM player_flight_data WHERE uuid = ?"
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
            ResultSet rs = stmt.executeQuery("SELECT * FROM player_flight_data");
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