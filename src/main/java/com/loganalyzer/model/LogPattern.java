package com.loganalyzer.model;

public class LogPattern {
    private Long id;
    private String logLevel;
    private String logTemplate;

    public LogPattern() {}

    public LogPattern(Long id, String logLevel, String logTemplate) {
        this.id = id;
        this.logLevel = logLevel;
        this.logTemplate = logTemplate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogTemplate() {
        return logTemplate;
    }

    public void setLogTemplate(String logTemplate) {
        this.logTemplate = logTemplate;
    }
}