package org.dynamcorp.handsaiv2.repository;

import org.dynamcorp.handsaiv2.model.KnowledgeCategoryEnum;
import org.dynamcorp.handsaiv2.model.KnowledgeMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeMemoryRepository extends JpaRepository<KnowledgeMemory, Long> {

    List<KnowledgeMemory> findByCategoryOrderByCreatedAtDesc(KnowledgeCategoryEnum category);

    // Basic full-text search simulation using LIKE on all content fields
    @Query("SELECT k FROM KnowledgeMemory k WHERE " +
            "LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(k.contentWhat) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(k.contentWhy) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(k.contentLearned) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY k.createdAt DESC")
    List<KnowledgeMemory> searchByContentOrTitleIgnoreCase(@Param("query") String query);
}
