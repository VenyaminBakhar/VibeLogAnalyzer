package com.loganalyzer.repository;

import com.loganalyzer.model.AppSetting;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.LogPattern;

import java.util.List;
import java.util.Optional;

public interface DatabaseRepository {
    List<LogPattern> findAllLogPatterns();
    LogPattern saveLogPattern(LogPattern pattern);
    void deleteLogPattern(Long id);
    Optional<AppSetting> findSettingByKey(String key);
    AppSetting saveSetting(AppSetting setting);
    List<LogEntry> executeLogQuery(String sql);
    void initializeTables();
}