package com.loganalyzer.dto;

import com.loganalyzer.model.LogEntry;
import java.util.List;

public class QueryResponse {
    private String analysis;
    private List<LogEntry> logs;

    public QueryResponse() {}

    public QueryResponse(String analysis, List<LogEntry> logs) {
        this.analysis = analysis;
        this.logs = logs;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<LogEntry> logs) {
        this.logs = logs;
    }
}