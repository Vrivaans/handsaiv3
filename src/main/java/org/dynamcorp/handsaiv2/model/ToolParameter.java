package org.dynamcorp.handsaiv2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tool_parameters")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ToolParameter extends BaseModel {

    private String name;

    @Enumerated(EnumType.STRING)
    private ParameterType type;

    private String description;
    private Boolean required;
    private String defaultValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_tool_id")
    private ApiTool apiTool;
}
