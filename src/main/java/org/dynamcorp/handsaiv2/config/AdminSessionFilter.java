package org.dynamcorp.handsaiv2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.service.AdminAuthService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Protects admin management endpoints (/admin/token/** and /admin/providers/**,
 * /admin/tools/**)
 * with the admin session cookie. Auth endpoints (/admin/auth/**) are excluded.
 */
@Component
@RequiredArgsConstructor
public class AdminSessionFilter extends OncePerRequestFilter {

    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Auth management itself is open
        return path.startsWith("/admin/auth/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply to /admin/** (not already excluded above)
        if (!path.startsWith("/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!adminAuthService.isSessionValid(request)) {
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String body = objectMapper.writeValueAsString(Map.of(
                "error", "Not authenticated",
                "message", "Log in with your admin credentials."));
        response.getWriter().write(body);
    }
}
