package com.loganalyzer.controller;

import com.loganalyzer.model.LogPattern;
import com.loganalyzer.service.LogAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patterns")
@CrossOrigin(origins = "http://localhost:5000")
public class LogPatternController {

    @Autowired
    private LogAnalyzerService service;

    @GetMapping
    public ResponseEntity<List<LogPattern>> getAllPatterns() {
        List<LogPattern> patterns = service.getAllLogPatterns();
        return ResponseEntity.ok(patterns);
    }

    @PostMapping
    public ResponseEntity<LogPattern> createPattern(@RequestBody LogPattern pattern) {
        LogPattern savedPattern = service.saveLogPattern(pattern);
        return ResponseEntity.ok(savedPattern);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LogPattern> updatePattern(@PathVariable Long id, @RequestBody LogPattern pattern) {
        pattern.setId(id);
        LogPattern updatedPattern = service.saveLogPattern(pattern);
        return ResponseEntity.ok(updatedPattern);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePattern(@PathVariable Long id) {
        service.deleteLogPattern(id);
        return ResponseEntity.ok().build();
    }
}