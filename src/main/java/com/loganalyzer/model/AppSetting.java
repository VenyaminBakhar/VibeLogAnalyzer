package com.loganalyzer.model;

public class AppSetting {
    private Long id;
    private String settingKey;
    private String settingValue;

    public AppSetting() {}

    public AppSetting(Long id, String settingKey, String settingValue) {
        this.id = id;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
}