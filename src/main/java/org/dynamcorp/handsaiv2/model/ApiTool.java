package org.dynamcorp.handsaiv2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "api_tools")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
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

    @OneToMany(mappedBy = "apiTool", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @lombok.Builder.Default
    private Set<ToolParameter> parameters = new LinkedHashSet<>();

    @Column(columnDefinition = "boolean default false")
    @lombok.Builder.Default
    private boolean isExportable = false;
}
