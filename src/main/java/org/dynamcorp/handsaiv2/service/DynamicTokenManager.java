package org.dynamcorp.handsaiv2.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.exception.ToolExecutionException;
import org.dynamcorp.handsaiv2.model.ApiProvider;
import org.dynamcorp.handsaiv2.model.DynamicAuthMethodEnum;
import org.dynamcorp.handsaiv2.model.DynamicAuthPayloadLocationEnum;
import org.dynamcorp.handsaiv2.model.DynamicAuthPayloadTypeEnum;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicTokenManager {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final EncryptionService encryptionService;

    // Cache: Provider ID -> CachedToken
    private final Map<Long, CachedToken> tokenCache = new ConcurrentHashMap<>();
    private static final long TOKEN_TTL_SECONDS = 300; // 5 minutes

    public String getToken(ApiProvider provider) {
        if (!provider.isDynamicAuth()) {
            return null;
        }

        CachedToken cachedToken = tokenCache.get(provider.getId());
        if (cachedToken != null && cachedToken.expiresAt().isAfter(Instant.now())) {
            log.debug("Returning cached dynamic token for provider {}", provider.getId());
            return cachedToken.token();
        }

        log.info("Fetching new dynamic token for provider {}", provider.getId());
        String newToken = fetchNewToken(provider);

        tokenCache.put(provider.getId(), new CachedToken(newToken, Instant.now().plusSeconds(TOKEN_TTL_SECONDS)));
        return newToken;
    }

    public void invalidateToken(Long providerId) {
        log.info("Invalidating dynamic token cache for provider {}", providerId);
        tokenCache.remove(providerId);
    }

    private String fetchNewToken(ApiProvider provider) {
        try {
            RestClient client = restClientBuilder.baseUrl(provider.getDynamicAuthUrl()).build();
            HttpMethod method = provider.getDynamicAuthMethod() == DynamicAuthMethodEnum.GET ? HttpMethod.GET
                    : HttpMethod.POST;

            // Parse Payload
            Map<String, Object> payloadMap = null;
            if (provider.getDynamicAuthPayload() != null && !provider.getDynamicAuthPayload().isBlank()) {
                payloadMap = objectMapper.readValue(provider.getDynamicAuthPayload(),
                        new TypeReference<Map<String, Object>>() {
                        });
                
                // Decrypt payload values
                for (Map.Entry<String, Object> entry : payloadMap.entrySet()) {
                    if (entry.getValue() instanceof String strVal && !strVal.isBlank()) {
                        entry.setValue(encryptionService.decrypt(strVal));
                    }
                }
            }

            String finalUri = provider.getDynamicAuthUrl();
            DynamicAuthPayloadLocationEnum location = provider.getDynamicAuthPayloadLocation();
            if (location == null)
                location = DynamicAuthPayloadLocationEnum.BODY; // default

            if (payloadMap != null && !payloadMap.isEmpty()
                    && location == DynamicAuthPayloadLocationEnum.QUERY_PARAMETERS) {
                StringBuilder query = new StringBuilder("?");
                payloadMap.forEach((k, v) -> query.append(k).append("=").append(v).append("&"));
                finalUri += query.substring(0, query.length() - 1); // append to base URL
            }

            RestClient.RequestBodySpec requestSpec = client.method(method).uri(finalUri);

            // Handle Payload Location
            if (payloadMap != null && !payloadMap.isEmpty()) {
                switch (location) {
                    case HEADERS:
                        for (Map.Entry<String, Object> entry : payloadMap.entrySet()) {
                            requestSpec.header(entry.getKey(), String.valueOf(entry.getValue()));
                        }
                        break;
                    case QUERY_PARAMETERS:
                        // Already handled above
                        break;
                    case BODY:
                        if (method == HttpMethod.POST) {
                            DynamicAuthPayloadTypeEnum type = provider.getDynamicAuthPayloadType();
                            if (type == DynamicAuthPayloadTypeEnum.FORM_DATA) {
                                requestSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED);
                                StringBuilder formData = new StringBuilder();
                                payloadMap.forEach((k, v) -> formData.append(k).append("=").append(v).append("&"));
                                String body = formData.substring(0, formData.length() - 1);
                                requestSpec.body(body);
                            } else {
                                requestSpec.contentType(MediaType.APPLICATION_JSON);
                                requestSpec.body(payloadMap);
                            }
                        }
                        break;
                }
            }

            String responseBody = requestSpec.retrieve().body(String.class);

            // Extract Token
            String extractionPath = provider.getDynamicAuthTokenExtractionPath();
            if (extractionPath == null || extractionPath.isBlank()) {
                // If empty path, assuming the raw response is the token text
                return responseBody;
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            String[] pathParts = extractionPath.split("\\.");
            JsonNode currentNode = rootNode;

            for (String part : pathParts) {
                if (currentNode != null && currentNode.has(part)) {
                    currentNode = currentNode.get(part);
                } else {
                    currentNode = null;
                    break;
                }
            }

            if (currentNode != null && !currentNode.isNull()) {
                if (currentNode.isTextual()) {
                    return currentNode.textValue();
                } else {
                    return currentNode.toString();
                }
            }

            throw new ToolExecutionException(
                    "Could not extract token from auth response using path: " + extractionPath);

        } catch (Exception e) {
            log.error("Failed to fetch dynamic token for provider {}", provider.getId(), e);
            throw new ToolExecutionException("Failed to fetch dynamic auth token: " + e.getMessage());
        }
    }

    private record CachedToken(String token, Instant expiresAt) {
    }
}
