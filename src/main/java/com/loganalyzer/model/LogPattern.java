package com.loganalyzer.model;

import jakarta.persistence.*;

@Entity
@Table(name = "log_patterns")
public class LogPattern {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "log_level", nullable = false)
    private String logLevel;
    
    @Column(name = "log_template", nullable = false, length = 500)
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