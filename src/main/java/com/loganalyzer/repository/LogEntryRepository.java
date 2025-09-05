package com.loganalyzer.repository;

import com.loganalyzer.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    List<LogEntry> findByLogLevelOrderByTimestampDesc(String logLevel);
    
    @Query("SELECT l FROM LogEntry l WHERE l.message LIKE %:searchTerm% ORDER BY l.timestamp DESC")
    List<LogEntry> findByMessageContainingOrderByTimestampDesc(@Param("searchTerm") String searchTerm);
    
    List<LogEntry> findAllByOrderByTimestampDesc();
}