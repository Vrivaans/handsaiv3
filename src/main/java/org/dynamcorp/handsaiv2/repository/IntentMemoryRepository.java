package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.IntentMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IntentMemoryRepository extends JpaRepository<IntentMemory, Long> {

    // Find latest active intents by an agent
    List<IntentMemory> findByAgentIdAndCompletedOrderByCreatedAtDesc(String agentId, boolean completed);

    // Find latest active intent by agent
    Optional<IntentMemory> findFirstByAgentIdAndCompletedOrderByCreatedAtDesc(String agentId, boolean completed);

    // Find intents by tags containing (basic string match for JSON array
    // representation)
    @Query("SELECT i FROM IntentMemory i WHERE i.tags LIKE %:tag% AND i.completed = :completed ORDER BY i.createdAt DESC")
    List<IntentMemory> findByTagsContainingAndCompletedOrderByCreatedAtDesc(@Param("tag") String tag,
            @Param("completed") boolean completed);
}
