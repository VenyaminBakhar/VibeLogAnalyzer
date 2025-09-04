package com.loganalyzer.service;

import com.loganalyzer.dto.QueryResponse;
import com.loganalyzer.model.AppSetting;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogPattern;
import com.loganalyzer.repository.DatabaseRepository;
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
    private DatabaseRepository repository;

    @Autowired
    private DeepSeekService deepSeekService;

    // Two-step log analysis process
    public QueryResponse processQuery(String userQuery) {
        try {
            // Get API key
            String apiKey = getDeepSeekApiKey();
            
            // Get log patterns
            List<LogPattern> patterns = repository.findAllLogPatterns();
            
            // Step 1: Generate SQL query using DeepSeek
            logger.info("Step 1: Generating SQL query for user request: {}", userQuery);
            String sqlQuery = deepSeekService.generateSqlQuery(userQuery, patterns, apiKey);
            logger.info("Generated SQL: {}", sqlQuery);
            
            // Execute SQL query to get relevant logs
            List<LogEntry> relevantLogs = repository.executeLogQuery(sqlQuery);
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
        return repository.findAllLogPatterns();
    }

    public LogPattern saveLogPattern(LogPattern pattern) {
        return repository.saveLogPattern(pattern);
    }

    public void deleteLogPattern(Long id) {
        repository.deleteLogPattern(id);
    }

    // API Key management
    public String getDeepSeekApiKey() {
        Optional<AppSetting> setting = repository.findSettingByKey(DEEPSEEK_API_KEY);
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
        AppSetting setting = new AppSetting(null, DEEPSEEK_API_KEY, apiKey);
        repository.saveSetting(setting);
    }

    public void initializeDatabase() {
        repository.initializeTables();
    }
}