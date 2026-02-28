package org.dynamcorp.handsaiv2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.model.IntentMemory;
import org.dynamcorp.handsaiv2.model.KnowledgeCategoryEnum;
import org.dynamcorp.handsaiv2.model.KnowledgeMemory;
import org.dynamcorp.handsaiv2.repository.IntentMemoryRepository;
import org.dynamcorp.handsaiv2.repository.KnowledgeMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryService {

    private final IntentMemoryRepository intentRepository;
    private final KnowledgeMemoryRepository knowledgeRepository;

    // --- Intent Memory (Tactical) ---

    @Transactional
    public IntentMemory saveIntent(String agentId, String sessionId, String intent, String verified,
            Double confidence, String boundaryHit, String tags) {

        log.info("Saving tactical intent for agent: {}", agentId);

        IntentMemory newIntent = IntentMemory.builder()
                .agentId(agentId)
                .sessionId(sessionId)
                .intent(intent)
                .verified(verified)
                .confidence(confidence)
                .boundaryHit(boundaryHit)
                .tags(tags)
                .completed(false)
                .createdAt(Instant.now())
                .build();

        return intentRepository.save(newIntent);
    }

    public List<IntentMemory> getActiveIntents(String agentId, String tagFilter) {
        if (tagFilter != null && !tagFilter.isBlank()) {
            return intentRepository.findByTagsContainingAndCompletedOrderByCreatedAtDesc(tagFilter, false);
        } else if (agentId != null && !agentId.isBlank()) {
            return intentRepository.findByAgentIdAndCompletedOrderByCreatedAtDesc(agentId, false);
        } else {
            return intentRepository.findAll().stream()
                    .filter(i -> !i.isCompleted())
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        }
    }

    @Transactional
    public Optional<IntentMemory> completeIntent(Long id) {
        return intentRepository.findById(id).map(intent -> {
            intent.setCompleted(true);
            intent.setUpdatedAt(Instant.now());
            log.info("Marking intent {} as completed", id);
            return intentRepository.save(intent);
        });
    }

    @Transactional
    public void deleteIntent(Long id) {
        log.warn("Permanently deleting tactical intent {}", id);
        intentRepository.deleteById(id);
    }

    // --- Knowledge Memory (Strategic) ---

    @Transactional
    public KnowledgeMemory saveKnowledge(String title, KnowledgeCategoryEnum category,
            String contentWhat, String contentWhy,
            String contentWhere, String contentLearned) {
        log.info("Saving strategic knowledge: [{}] {}", category, title);

        KnowledgeMemory knowledge = KnowledgeMemory.builder()
                .title(title)
                .category(category)
                .contentWhat(contentWhat)
                .contentWhy(contentWhy)
                .contentWhere(contentWhere)
                .contentLearned(contentLearned)
                .createdAt(Instant.now())
                .build();

        return knowledgeRepository.save(knowledge);
    }

    public List<KnowledgeMemory> searchKnowledge(String query, String categoryStr) {
        List<KnowledgeMemory> results;

        if (query != null && !query.isBlank()) {
            results = knowledgeRepository.searchByContentOrTitleIgnoreCase(query);
            // Optionally filter by category if both are provided
            if (categoryStr != null && !categoryStr.isBlank()) {
                try {
                    KnowledgeCategoryEnum cat = KnowledgeCategoryEnum.valueOf(categoryStr.toUpperCase());
                    return results.stream()
                            .filter(k -> k.getCategory() == cat)
                            .toList();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid search category: {}", categoryStr);
                    // Ignore valid category filter, just return query results
                }
            }
            return results;
        } else if (categoryStr != null && !categoryStr.isBlank()) {
            try {
                return knowledgeRepository
                        .findByCategoryOrderByCreatedAtDesc(KnowledgeCategoryEnum.valueOf(categoryStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid category for search: {}", categoryStr);
            }
        }

        return List.of(); // Empty response if no query or category
    }

    @Transactional
    public void deleteKnowledge(Long id) {
        log.warn("Permanently deleting strategic knowledge {}", id);
        knowledgeRepository.deleteById(id);
    }
}
