package org.dynamcorp.handsaiv2.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "intent_memories")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IntentMemory extends BaseModel {

    @Column(nullable = false, length = 255)
    private String agentId;

    @Column(length = 255)
    private String sessionId;

    @Column(nullable = false, length = 2000)
    private String intent;

    @Column(columnDefinition = "TEXT")
    private String verified;

    @Column(nullable = false)
    private Double confidence;

    @Column(length = 2000)
    private String boundaryHit;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(nullable = false)
    private boolean completed;
}
