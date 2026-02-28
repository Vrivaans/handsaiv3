package org.dynamcorp.handsaiv2.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "knowledge_memories")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeMemory extends BaseModel {

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private KnowledgeCategoryEnum category;

    @Column(columnDefinition = "TEXT")
    private String contentWhat;

    @Column(columnDefinition = "TEXT")
    private String contentWhy;

    @Column(columnDefinition = "TEXT")
    private String contentWhere;

    @Column(columnDefinition = "TEXT")
    private String contentLearned;
}
