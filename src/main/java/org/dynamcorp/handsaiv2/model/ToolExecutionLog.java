package org.dynamcorp.handsaiv2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "tool_execution_logs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionLog extends BaseModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_tool_id")
    private ApiTool apiTool;

    @Column(length = 8000)
    private String sessionId;
    @Column(length = 8000)
    private String requestPayload;
    @Column(length = 8000)
    private String responsePayload;
    private Long executionTimeMs;
    private boolean success;
    @Column(length = 8000)
    private String errorMessage;
    private String systemToolName;
    private Instant executedAt;
}
