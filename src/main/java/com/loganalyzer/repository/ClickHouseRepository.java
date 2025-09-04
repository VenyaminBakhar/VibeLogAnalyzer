package com.loganalyzer.repository;

import com.loganalyzer.model.AppSetting;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogPattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("production")
public class ClickHouseRepository implements DatabaseRepository {

    @Autowired
    private DataSource dataSource;

    // Log Patterns CRUD
    public List<LogPattern> findAllLogPatterns() {
        List<LogPattern> patterns = new ArrayList<>();
        String sql = "SELECT id, log_level, log_template FROM log_patterns ORDER BY id";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                patterns.add(new LogPattern(
                    rs.getLong("id"),
                    rs.getString("log_level"),
                    rs.getString("log_template")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching log patterns", e);
        }
        return patterns;
    }

    public LogPattern saveLogPattern(LogPattern pattern) {
        if (pattern.getId() == null) {
            return insertLogPattern(pattern);
        } else {
            return updateLogPattern(pattern);
        }
    }

    private LogPattern insertLogPattern(LogPattern pattern) {
        String sql = "INSERT INTO log_patterns (id, log_level, log_template) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection()) {
            // Get next ID
            Long nextId = getNextId("log_patterns");
            pattern.setId(nextId);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, pattern.getId());
                stmt.setString(2, pattern.getLogLevel());
                stmt.setString(3, pattern.getLogTemplate());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting log pattern", e);
        }
        return pattern;
    }

    private LogPattern updateLogPattern(LogPattern pattern) {
        String sql = "ALTER TABLE log_patterns UPDATE log_level = ?, log_template = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern.getLogLevel());
            stmt.setString(2, pattern.getLogTemplate());
            stmt.setLong(3, pattern.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating log pattern", e);
        }
        return pattern;
    }

    public void deleteLogPattern(Long id) {
        String sql = "ALTER TABLE log_patterns DELETE WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting log pattern", e);
        }
    }

    // App Settings
    public Optional<AppSetting> findSettingByKey(String key) {
        String sql = "SELECT id, setting_key, setting_value FROM app_settings WHERE setting_key = ? LIMIT 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new AppSetting(
                        rs.getLong("id"),
                        rs.getString("setting_key"),
                        rs.getString("setting_value")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching setting", e);
        }
        return Optional.empty();
    }

    public AppSetting saveSetting(AppSetting setting) {
        Optional<AppSetting> existing = findSettingByKey(setting.getSettingKey());
        
        if (existing.isPresent()) {
            return updateSetting(existing.get().getId(), setting.getSettingValue());
        } else {
            return insertSetting(setting);
        }
    }

    private AppSetting insertSetting(AppSetting setting) {
        String sql = "INSERT INTO app_settings (id, setting_key, setting_value) VALUES (?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection()) {
            Long nextId = getNextId("app_settings");
            setting.setId(nextId);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, setting.getId());
                stmt.setString(2, setting.getSettingKey());
                stmt.setString(3, setting.getSettingValue());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting setting", e);
        }
        return setting;
    }

    private AppSetting updateSetting(Long id, String value) {
        String sql = "ALTER TABLE app_settings UPDATE setting_value = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.setLong(2, id);
            stmt.executeUpdate();
            
            return new AppSetting(id, null, value);
        } catch (SQLException e) {
            throw new RuntimeException("Error updating setting", e);
        }
    }

    // Execute custom SQL for log queries
    public List<LogEntry> executeLogQuery(String sql) {
        List<LogEntry> logs = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                logs.add(new LogEntry(
                    rs.getLong("id"),
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    rs.getString("log_level"),
                    rs.getString("message")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing log query: " + e.getMessage(), e);
        }
        return logs;
    }

    // Helper method to get next ID
    private Long getNextId(String tableName) {
        String sql = "SELECT max(id) + 1 as next_id FROM " + tableName;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                Long nextId = rs.getLong("next_id");
                return nextId == 0 ? 1L : nextId;
            }
        } catch (SQLException e) {
            // If table is empty or doesn't exist, start with 1
            return 1L;
        }
        return 1L;
    }

    // Initialize tables if they don't exist
    public void initializeTables() {
        try (Connection conn = dataSource.getConnection()) {
            // Create logs table
            String createLogsTable = """
                CREATE TABLE IF NOT EXISTS logs
                (
                  id UInt64,
                  timestamp DateTime,
                  log_level String,
                  message String
                )
                ENGINE = MergeTree
                ORDER BY (timestamp)
                """;
            
            // Create log_patterns table
            String createPatternsTable = """
                CREATE TABLE IF NOT EXISTS log_patterns
                (
                  id UInt64,
                  log_level String,
                  log_template String
                )
                ENGINE = MergeTree
                ORDER BY (id)
                """;
            
            // Create app_settings table
            String createSettingsTable = """
                CREATE TABLE IF NOT EXISTS app_settings
                (
                  id UInt64,
                  setting_key String,
                  setting_value String
                )
                ENGINE = MergeTree
                ORDER BY (id)
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createLogsTable);
                stmt.execute(createPatternsTable);
                stmt.execute(createSettingsTable);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing tables", e);
        }
    }
}