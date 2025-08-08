package org.littlesheep.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
            // 飞行时间表
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_flight_data (" +
                "uuid TEXT PRIMARY KEY, " +
                "end_time INTEGER)"
            );
            
            // 特效购买记录表
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_effects (" +
                "uuid TEXT, " +
                "effect_name TEXT, " +
                "PRIMARY KEY (uuid, effect_name))"
            );
            
            // 速度购买记录表
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_speeds (" +
                "uuid TEXT, " +
                "speed_name TEXT, " +
                "PRIMARY KEY (uuid, speed_name))"
            );
            
            // 时间限制特效购买记录表
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_effect_times (" +
                "uuid TEXT, " +
                "effect_name TEXT, " +
                "end_time INTEGER, " +
                "PRIMARY KEY (uuid, effect_name))"
            );
            
            // 时间限制速度购买记录表
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_speed_times (" +
                "uuid TEXT, " +
                "speed_name TEXT, " +
                "end_time INTEGER, " +
                "PRIMARY KEY (uuid, speed_name))"
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

    // ============= 特效购买记录相关方法 =============
    
    @Override
    public void addPlayerEffect(UUID uuid, String effectName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO player_effects (uuid, effect_name) VALUES (?, ?)"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, effectName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("添加玩家特效时出错: " + e.getMessage());
        }
    }

    @Override
    public void removePlayerEffect(UUID uuid, String effectName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM player_effects WHERE uuid = ? AND effect_name = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, effectName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("删除玩家特效时出错: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getPlayerEffects(UUID uuid) {
        Set<String> effects = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT effect_name FROM player_effects WHERE uuid = ?"
        )) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                effects.add(rs.getString("effect_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家特效时出错: " + e.getMessage());
        }
        return effects;
    }

    @Override
    public Map<UUID, Set<String>> getAllPlayerEffects() {
        Map<UUID, Set<String>> allEffects = new HashMap<>();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM player_effects");
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String effectName = rs.getString("effect_name");
                allEffects.computeIfAbsent(uuid, k -> new HashSet<>()).add(effectName);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取所有玩家特效时出错: " + e.getMessage());
        }
        return allEffects;
    }

    // ============= 速度购买记录相关方法 =============
    
    @Override
    public void addPlayerSpeed(UUID uuid, String speedName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO player_speeds (uuid, speed_name) VALUES (?, ?)"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, speedName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("添加玩家速度时出错: " + e.getMessage());
        }
    }

    @Override
    public void removePlayerSpeed(UUID uuid, String speedName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM player_speeds WHERE uuid = ? AND speed_name = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, speedName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("删除玩家速度时出错: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getPlayerSpeeds(UUID uuid) {
        Set<String> speeds = new HashSet<>();
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT speed_name FROM player_speeds WHERE uuid = ?"
        )) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                speeds.add(rs.getString("speed_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家速度时出错: " + e.getMessage());
        }
        return speeds;
    }

    @Override
    public Map<UUID, Set<String>> getAllPlayerSpeeds() {
        Map<UUID, Set<String>> allSpeeds = new HashMap<>();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM player_speeds");
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String speedName = rs.getString("speed_name");
                allSpeeds.computeIfAbsent(uuid, k -> new HashSet<>()).add(speedName);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取所有玩家速度时出错: " + e.getMessage());
        }
        return allSpeeds;
    }

    // ============= 时间限制特效购买记录相关方法 =============
    
    @Override
    public void setPlayerEffectTime(UUID uuid, String effectName, long endTime) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO player_effect_times (uuid, effect_name, end_time) VALUES (?, ?, ?)"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, effectName);
            stmt.setLong(3, endTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("设置玩家特效时间时出错: " + e.getMessage());
        }
    }

    @Override
    public Long getPlayerEffectTime(UUID uuid, String effectName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT end_time FROM player_effect_times WHERE uuid = ? AND effect_name = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, effectName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("end_time");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家特效时间时出错: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, Long> getPlayerEffectTimes(UUID uuid) {
        Map<String, Long> effectTimes = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT effect_name, end_time FROM player_effect_times WHERE uuid = ?"
        )) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                effectTimes.put(rs.getString("effect_name"), rs.getLong("end_time"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家特效时间列表时出错: " + e.getMessage());
        }
        return effectTimes;
    }

    @Override
    public void removePlayerEffectTime(UUID uuid, String effectName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM player_effect_times WHERE uuid = ? AND effect_name = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, effectName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("删除玩家特效时间时出错: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Map<String, Long>> getAllPlayerEffectTimes() {
        Map<UUID, Map<String, Long>> allEffectTimes = new HashMap<>();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM player_effect_times");
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String effectName = rs.getString("effect_name");
                long endTime = rs.getLong("end_time");
                allEffectTimes.computeIfAbsent(uuid, k -> new HashMap<>()).put(effectName, endTime);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取所有玩家特效时间时出错: " + e.getMessage());
        }
        return allEffectTimes;
    }

    // ============= 时间限制速度购买记录相关方法 =============
    
    @Override
    public void setPlayerSpeedTime(UUID uuid, String speedName, long endTime) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT OR REPLACE INTO player_speed_times (uuid, speed_name, end_time) VALUES (?, ?, ?)"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, speedName);
            stmt.setLong(3, endTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("设置玩家速度时间时出错: " + e.getMessage());
        }
    }

    @Override
    public Long getPlayerSpeedTime(UUID uuid, String speedName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT end_time FROM player_speed_times WHERE uuid = ? AND speed_name = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, speedName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("end_time");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家速度时间时出错: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, Long> getPlayerSpeedTimes(UUID uuid) {
        Map<String, Long> speedTimes = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT speed_name, end_time FROM player_speed_times WHERE uuid = ?"
        )) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                speedTimes.put(rs.getString("speed_name"), rs.getLong("end_time"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取玩家速度时间列表时出错: " + e.getMessage());
        }
        return speedTimes;
    }

    @Override
    public void removePlayerSpeedTime(UUID uuid, String speedName) {
        try (PreparedStatement stmt = connection.prepareStatement(
            "DELETE FROM player_speed_times WHERE uuid = ? AND speed_name = ?"
        )) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, speedName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("删除玩家速度时间时出错: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Map<String, Long>> getAllPlayerSpeedTimes() {
        Map<UUID, Map<String, Long>> allSpeedTimes = new HashMap<>();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM player_speed_times");
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String speedName = rs.getString("speed_name");
                long endTime = rs.getLong("end_time");
                allSpeedTimes.computeIfAbsent(uuid, k -> new HashMap<>()).put(speedName, endTime);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取所有玩家速度时间时出错: " + e.getMessage());
        }
        return allSpeedTimes;
    }
} 