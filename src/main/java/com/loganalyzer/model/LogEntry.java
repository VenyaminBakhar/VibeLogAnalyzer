package com.loganalyzer.model;

import java.time.LocalDateTime;

public class LogEntry {
    private Long id;
    private LocalDateTime timestamp;
    private String logLevel;
    private String message;

    public LogEntry() {}

    public LogEntry(Long id, LocalDateTime timestamp, String logLevel, String message) {
        this.id = id;
        this.timestamp = timestamp;
        this.logLevel = logLevel;
        this.message = message;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}