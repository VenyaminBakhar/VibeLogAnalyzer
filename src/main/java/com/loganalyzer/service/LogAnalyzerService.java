package com.loganalyzer.service;

import com.loganalyzer.dto.QueryResponse;
import com.loganalyzer.model.AppSetting;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogPattern;
import com.loganalyzer.repository.AppSettingRepository;
import com.loganalyzer.repository.LogEntryRepository;
import com.loganalyzer.repository.LogPatternRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LogAnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(LogAnalyzerService.class);
    private static final String DEEPSEEK_API_KEY = "deepseek_api_key";

    @Autowired
    private LogPatternRepository logPatternRepository;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @Autowired
    private DeepSeekService deepSeekService;

    // Two-step log analysis process
    public QueryResponse processQuery(String userQuery) {
        try {
            // Get API key
            String apiKey = getDeepSeekApiKey();
            
            // Get log patterns
            List<LogPattern> patterns = logPatternRepository.findAll();
            
            // Step 1: Generate SQL query using DeepSeek
            logger.info("Step 1: Generating SQL query for user request: {}", userQuery);
            String sqlQuery = deepSeekService.generateSqlQuery(userQuery, patterns, apiKey);
            logger.info("Generated SQL: {}", sqlQuery);
            
            // For now, we'll use simple text search in logs instead of complex SQL
            // TODO: Implement more sophisticated query execution if needed
            List<LogEntry> relevantLogs = logEntryRepository.findAllByOrderByTimestampDesc();
            if (relevantLogs.size() > 100) {
                relevantLogs = relevantLogs.subList(0, 100); // Limit to latest 100 logs
            }
            logger.info("Found {} relevant logs", relevantLogs.size());
            
            // Step 2: Analyze the logs using DeepSeek
            logger.info("Step 2: Analyzing logs with DeepSeek");
            String analysis = deepSeekService.analyzeLogs(userQuery, relevantLogs, apiKey);
            
            return new QueryResponse(analysis, relevantLogs);
            
        } catch (Exception e) {
            logger.error("Error processing query", e);
            throw new RuntimeException("Failed to process query: " + e.getMessage());
        }
    }

    // Log Patterns CRUD
    public List<LogPattern> getAllLogPatterns() {
        return logPatternRepository.findAll();
    }

    public LogPattern saveLogPattern(LogPattern pattern) {
        return logPatternRepository.save(pattern);
    }

    public void deleteLogPattern(Long id) {
        logPatternRepository.deleteById(id);
    }

    // API Key management
    public String getDeepSeekApiKey() {
        Optional<AppSetting> setting = appSettingRepository.findBySettingKey(DEEPSEEK_API_KEY);
        if (setting.isEmpty()) {
            throw new RuntimeException("DeepSeek API key not configured. Please set it in Settings.");
        }
        return setting.get().getSettingValue();
    }

    public String getMaskedApiKey() {
        try {
            String apiKey = getDeepSeekApiKey();
            if (apiKey.length() > 8) {
                return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
            }
            return "****";
        } catch (Exception e) {
            return "Not configured";
        }
    }

    public void saveDeepSeekApiKey(String apiKey) {
        // Check if setting already exists
        Optional<AppSetting> existingSetting = appSettingRepository.findBySettingKey(DEEPSEEK_API_KEY);
        if (existingSetting.isPresent()) {
            AppSetting setting = existingSetting.get();
            setting.setSettingValue(apiKey);
            appSettingRepository.save(setting);
        } else {
            AppSetting setting = new AppSetting(null, DEEPSEEK_API_KEY, apiKey);
            appSettingRepository.save(setting);
        }
    }

    public void initializeDatabase() {
        // JPA will auto-create tables, but we can add sample data here
        initializeSampleData();
    }

    private void initializeSampleData() {
        // Add sample log patterns if none exist
        if (logPatternRepository.count() == 0) {
            logPatternRepository.save(new LogPattern(null, "INFO", "User {user_id} logged in from {ip_address}"));
            logPatternRepository.save(new LogPattern(null, "ERROR", "Database connection failed: {error_message}"));
            logPatternRepository.save(new LogPattern(null, "WARN", "High memory usage detected: {memory_percent}%"));
        }

        // Add sample log entries if none exist
        if (logEntryRepository.count() == 0) {
            LogEntry entry1 = new LogEntry();
            entry1.setTimestamp(java.time.LocalDateTime.now().minusHours(2));
            entry1.setLogLevel("INFO");
            entry1.setMessage("User john_doe logged in from 192.168.1.100");
            logEntryRepository.save(entry1);

            LogEntry entry2 = new LogEntry();
            entry2.setTimestamp(java.time.LocalDateTime.now().minusHours(1));
            entry2.setLogLevel("ERROR");
            entry2.setMessage("Database connection failed: Connection timeout");
            logEntryRepository.save(entry2);

            LogEntry entry3 = new LogEntry();
            entry3.setTimestamp(java.time.LocalDateTime.now().minusMinutes(30));
            entry3.setLogLevel("WARN");
            entry3.setMessage("High memory usage detected: 85%");
            logEntryRepository.save(entry3);
        }
    }

    // Log Entries CRUD
    public List<LogEntry> getAllLogEntries() {
        return logEntryRepository.findAllByOrderByTimestampDesc();
    }

    public LogEntry saveLogEntry(LogEntry logEntry) {
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(java.time.LocalDateTime.now());
        }
        return logEntryRepository.save(logEntry);
    }

    public void deleteLogEntry(Long id) {
        logEntryRepository.deleteById(id);
    }
}