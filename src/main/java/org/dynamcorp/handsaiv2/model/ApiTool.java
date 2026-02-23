package org.dynamcorp.handsaiv2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "api_tools")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApiTool extends BaseModel {

    private String name;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ApiProvider provider;

    private String endpointPath;

    @Enumerated(EnumType.STRING)
    private HttpMethodEnum httpMethod;

    private Instant lastHealthCheck;
    private boolean healthy;

    @OneToMany(mappedBy = "apiTool", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @lombok.Builder.Default
    private List<ToolParameter> parameters = new ArrayList<>();
}
