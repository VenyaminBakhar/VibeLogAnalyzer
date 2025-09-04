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
public class H2Repository implements DatabaseRepository {

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
            // Table might not exist yet
            initializeTables();
            return findAllLogPatterns();
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
        String sql = "INSERT INTO log_patterns (log_level, log_template) VALUES (?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, pattern.getLogLevel());
            stmt.setString(2, pattern.getLogTemplate());
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    pattern.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting log pattern", e);
        }
        return pattern;
    }

    private LogPattern updateLogPattern(LogPattern pattern) {
        String sql = "UPDATE log_patterns SET log_level = ?, log_template = ? WHERE id = ?";
        
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
        String sql = "DELETE FROM log_patterns WHERE id = ?";
        
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
            initializeTables();
            return findSettingByKey(key);
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
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, setting.getSettingKey());
            stmt.setString(2, setting.getSettingValue());
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    setting.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting setting", e);
        }
        return setting;
    }

    private AppSetting updateSetting(Long id, String value) {
        String sql = "UPDATE app_settings SET setting_value = ? WHERE id = ?";
        
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

    // Execute custom SQL for log queries (simplified for demo)
    public List<LogEntry> executeLogQuery(String sql) {
        List<LogEntry> logs = new ArrayList<>();
        
        // For demonstration, return sample logs since we don't have real log data
        logs.add(new LogEntry(1L, LocalDateTime.now().minusHours(2), "ERROR", "Database connection failed"));
        logs.add(new LogEntry(2L, LocalDateTime.now().minusHours(1), "INFO", "User logged in successfully"));
        logs.add(new LogEntry(3L, LocalDateTime.now().minusMinutes(30), "WARN", "Memory usage high"));
        
        return logs;
    }

    // Initialize tables if they don't exist
    public void initializeTables() {
        try (Connection conn = dataSource.getConnection()) {
            // Create logs table
            String createLogsTable = """
                CREATE TABLE IF NOT EXISTS logs
                (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  timestamp TIMESTAMP,
                  log_level VARCHAR(10),
                  message TEXT
                )
                """;
            
            // Create log_patterns table
            String createPatternsTable = """
                CREATE TABLE IF NOT EXISTS log_patterns
                (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  log_level VARCHAR(10),
                  log_template TEXT
                )
                """;
            
            // Create app_settings table
            String createSettingsTable = """
                CREATE TABLE IF NOT EXISTS app_settings
                (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  setting_key VARCHAR(255) UNIQUE,
                  setting_value TEXT
                )
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createLogsTable);
                stmt.execute(createPatternsTable);
                stmt.execute(createSettingsTable);
                
                // Insert sample log patterns
                insertSampleData(conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing tables", e);
        }
    }
    
    private void insertSampleData(Connection conn) throws SQLException {
        String checkPatterns = "SELECT COUNT(*) FROM log_patterns";
        try (PreparedStatement stmt = conn.prepareStatement(checkPatterns);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Insert sample patterns
                String insertPattern = "INSERT INTO log_patterns (log_level, log_template) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertPattern)) {
                    insertStmt.setString(1, "INFO");
                    insertStmt.setString(2, "logger.info(\"User {} logged in\", userId);");
                    insertStmt.executeUpdate();
                    
                    insertStmt.setString(1, "ERROR");
                    insertStmt.setString(2, "logger.error(\"Database connection failed: {}\", error);");
                    insertStmt.executeUpdate();
                    
                    insertStmt.setString(1, "WARN");
                    insertStmt.setString(2, "logger.warn(\"Memory usage high: {}%\", percentage);");
                    insertStmt.executeUpdate();
                }
            }
        }
    }
}