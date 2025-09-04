package com.loganalyzer.config;

import com.loganalyzer.service.LogAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private LogAnalyzerService logAnalyzerService;

    @Override
    public void run(String... args) throws Exception {
        logAnalyzerService.initializeDatabase();
    }
}