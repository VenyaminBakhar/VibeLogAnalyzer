package com.loganalyzer.controller;

import com.loganalyzer.model.LogEntry;
import com.loganalyzer.service.LogAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "http://localhost:5000")
public class LogEntryController {

    @Autowired
    private LogAnalyzerService service;

    @GetMapping
    public ResponseEntity<List<LogEntry>> getAllLogs() {
        List<LogEntry> logs = service.getAllLogEntries();
        return ResponseEntity.ok(logs);
    }

    @PostMapping
    public ResponseEntity<LogEntry> createLog(@RequestBody LogEntry logEntry) {
        LogEntry savedLog = service.saveLogEntry(logEntry);
        return ResponseEntity.ok(savedLog);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LogEntry> updateLog(@PathVariable Long id, @RequestBody LogEntry logEntry) {
        logEntry.setId(id);
        LogEntry updatedLog = service.saveLogEntry(logEntry);
        return ResponseEntity.ok(updatedLog);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable Long id) {
        service.deleteLogEntry(id);
        return ResponseEntity.ok().build();
    }
}