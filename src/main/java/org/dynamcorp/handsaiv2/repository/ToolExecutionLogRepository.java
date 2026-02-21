package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.ToolExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ToolExecutionLogRepository extends JpaRepository<ToolExecutionLog, Long> {

    long countByExecutedAtAfter(Instant date);

    long countBySuccessAndExecutedAtAfter(boolean success, Instant date);

    @Query("SELECT AVG(t.executionTimeMs) FROM ToolExecutionLog t WHERE t.executedAt > :date")
    Double getAverageExecutionTimeAfter(Instant date);

    Page<ToolExecutionLog> findAllByOrderByExecutedAtDesc(Pageable pageable);
}
