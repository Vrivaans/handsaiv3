package org.dynamcorp.handsaiv2.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "api_providers")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApiProvider extends BaseModel {

    private String name;

    @Column(nullable = false)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    private AuthenticationTypeEnum authenticationType;

    @Enumerated(EnumType.STRING)
    private ApiKeyLocationEnum apiKeyLocation;

    private String apiKeyName;

    /**
     * Optional custom headers stored as JSON string. Ex: {"User-Agent": "MyAgent"}
     */
    @Column(columnDefinition = "TEXT")
    private String customHeadersJson;

    /**
     * Stores the hashed API key or token. It is strongly recommended to encrypt
     * this value before storing it.
     */
    private String apiKeyValue;

    // --- Dynamic Authentication Fields ---

    @Column(columnDefinition = "boolean default false")
    @lombok.Builder.Default
    private boolean isDynamicAuth = false;

    private String dynamicAuthUrl;

    @Enumerated(EnumType.STRING)
    private DynamicAuthMethodEnum dynamicAuthMethod;

    @Column(columnDefinition = "TEXT")
    private String dynamicAuthPayload;

    @Enumerated(EnumType.STRING)
    private DynamicAuthPayloadTypeEnum dynamicAuthPayloadType;

    @Enumerated(EnumType.STRING)
    private DynamicAuthPayloadLocationEnum dynamicAuthPayloadLocation;

    private String dynamicAuthTokenExtractionPath;

    @Column(columnDefinition = "TEXT")
    private String dynamicAuthInvalidationKeywords;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<ApiTool> tools = new ArrayList<>();

    @Column(columnDefinition = "boolean default false")
    @lombok.Builder.Default
    private boolean isExportable = false;
}
