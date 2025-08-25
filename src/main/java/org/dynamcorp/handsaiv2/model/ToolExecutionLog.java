package org.dynamcorp.handsaiv2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "tool_execution_logs")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ToolExecutionLog extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "api_tool_id")
    private ApiTool apiTool;

    private String sessionId;
    private String requestPayload;
    private String responsePayload;
    private Long executionTimeMs;
    private boolean success;
    private String errorMessage;
    private Instant executedAt;
}
