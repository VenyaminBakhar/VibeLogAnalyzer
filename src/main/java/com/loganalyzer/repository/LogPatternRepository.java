package com.loganalyzer.repository;

import com.loganalyzer.model.LogPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogPatternRepository extends JpaRepository<LogPattern, Long> {
}