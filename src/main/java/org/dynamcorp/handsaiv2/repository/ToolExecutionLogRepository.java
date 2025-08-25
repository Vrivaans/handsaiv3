package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.ToolExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolExecutionLogRepository extends JpaRepository<ToolExecutionLog, Long> {
}
