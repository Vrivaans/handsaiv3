package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.TaskMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskMemoryRepository extends JpaRepository<TaskMemory, Long> {

    // Allows listing tasks by specific status (e.g. PENDING, IN_PROGRESS)
    List<TaskMemory> findByStatusOrderByCreatedAtDesc(String status);

    // Useful for a dashboard widget that just pulls everything not completed
    List<TaskMemory> findByStatusNotOrderByPriorityDescCreatedAtAsc(String excludedStatus);
}
