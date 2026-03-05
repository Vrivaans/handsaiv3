package org.dynamcorp.handsaiv2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.service.AccessTokenService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Protects all endpoints requiring a PAT token via the X-HandsAI-Token header.
 * Excluded paths: /admin/auth/**, all static SPA resources.
 * Admin management paths (/admin/token/**, etc.) are separately protected by
 * AdminSessionFilter.
 */
@Component
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "X-HandsAI-Token";

    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();

        // Auth endpoints (login, setup, status) — no token needed
        if (path.startsWith("/admin/auth/"))
            return true;

        // Admin token management — protected by AdminSessionFilter instead
        if (path.startsWith("/admin/token"))
            return true;

        // Admin tool/provider management — protected by AdminSessionFilter instead
        if (path.startsWith("/admin/"))
            return true;

        // SPA static resources
        if (isStaticResource(path))
            return true;

        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String rawToken = request.getHeader(TOKEN_HEADER);

        if (rawToken == null || rawToken.isBlank()) {
            if (!accessTokenService.hasToken()) {
                sendUnauthorized(response, "No token configured",
                        "Generate a token from the HandsAI admin panel first.");
            } else {
                sendUnauthorized(response, "Missing " + TOKEN_HEADER + " header",
                        "Set the token in your MCP bridge configuration.");
            }
            return;
        }

        if (!accessTokenService.validateToken(rawToken)) {
            sendUnauthorized(response, "Invalid token",
                    "Token mismatch. If you regenerated the token, update your MCP bridge configuration.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String error, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String body = objectMapper.writeValueAsString(Map.of("error", error, "message", message));
        response.getWriter().write(body);
    }

    private boolean isStaticResource(String path) {
        return path.equals("/") || path.equals("/index.html")
                || path.endsWith(".js") || path.endsWith(".css")
                || path.endsWith(".ico") || path.endsWith(".html")
                || path.endsWith(".txt") || path.endsWith(".json")
                || path.endsWith(".png") || path.endsWith(".svg")
                || path.endsWith(".woff") || path.endsWith(".woff2");
    }
}
