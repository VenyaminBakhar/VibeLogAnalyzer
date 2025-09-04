package com.loganalyzer.controller;

import com.loganalyzer.dto.QueryRequest;
import com.loganalyzer.dto.QueryResponse;
import com.loganalyzer.service.LogAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "http://localhost:5000")
public class QueryController {

    @Autowired
    private LogAnalyzerService service;

    @PostMapping
    public ResponseEntity<QueryResponse> processQuery(@RequestBody QueryRequest request) {
        try {
            QueryResponse response = service.processQuery(request.getQuery());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            QueryResponse errorResponse = new QueryResponse(
                "Error processing query: " + e.getMessage(), 
                null
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}