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
    private String baseUrl;
    private String endpointPath;

    @Enumerated(EnumType.STRING)
    private HttpMethodEnum httpMethod;

    @Enumerated(EnumType.STRING)
    private AuthenticationTypeEnum authenticationType;

    @Enumerated(EnumType.STRING)
    private ApiKeyLocationEnum apiKeyLocation;

    private String apiKeyName;

    /**
     * Stores the hashed API key or token. It is strongly recommended to encrypt this value before storing it.
     */
    private String apiKeyValue;

    private Instant lastHealthCheck;
    private boolean healthy;

    @OneToMany(mappedBy = "apiTool", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ToolParameter> parameters = new ArrayList<>();
}
