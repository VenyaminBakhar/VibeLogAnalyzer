package com.loganalyzer.controller;

import com.loganalyzer.service.LogAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:5000")
public class SettingsController {

    @Autowired
    private LogAnalyzerService service;

    @GetMapping("/deepseek_api_key")
    public ResponseEntity<Map<String, String>> getApiKey() {
        String maskedKey = service.getMaskedApiKey();
        return ResponseEntity.ok(Map.of("apiKey", maskedKey));
    }

    @PostMapping("/deepseek_api_key")
    public ResponseEntity<Map<String, String>> saveApiKey(@RequestBody Map<String, String> request) {
        String apiKey = request.get("apiKey");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "API key cannot be empty"));
        }
        
        service.saveDeepSeekApiKey(apiKey.trim());
        return ResponseEntity.ok(Map.of("message", "API key saved successfully"));
    }
}