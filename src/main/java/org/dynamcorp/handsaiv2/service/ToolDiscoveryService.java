package org.dynamcorp.handsaiv2.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.dynamcorp.handsaiv2.dto.ToolDefinition;
import org.dynamcorp.handsaiv2.dto.ToolDiscoveryResponse;
import org.dynamcorp.handsaiv2.model.ApiTool;
import org.dynamcorp.handsaiv2.repository.ApiToolRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToolDiscoveryService {

        private final ToolCacheManager toolCacheManager;
        private final ApiToolRepository apiToolRepository;

        public ToolDiscoveryResponse discoverTools() {
                log.info("Discovering available tools");

                try {
                        List<ApiTool> tools = toolCacheManager.getAllCachedTools();

                        if (tools.isEmpty()) {
                                log.info("No tools available in cache, fetching from database");
                                tools = apiToolRepository.findAllEnabled();
                                log.info("Discovered {} tools from database", tools.size());
                        }

                        List<ToolDefinition> toolDefinitions = tools.stream()
                                        .map(ToolDefinition::from)
                                        .collect(Collectors.toList());

                        // --- Inject Native Memory Engine Tools ---
                        toolDefinitions.addAll(getNativeMemoryTools());

                        log.info("Discovered {} total tools (including native system tools)", toolDefinitions.size());
                        return new ToolDiscoveryResponse(
                                        toolDefinitions,
                                        toolDefinitions.size(),
                                        Instant.now());
                } catch (Exception error) {
                        log.error("Error during tool discovery", error);
                        return new ToolDiscoveryResponse(
                                        List.of(),
                                        0,
                                        Instant.now());
                }
        }

        private List<ToolDefinition> getNativeMemoryTools() {
                return List.of(
                                new ToolDefinition(
                                                "handsai_save_intent",
                                                "Guarda el estado actual del agente (Intención Táctica). Úsalo antes de detenerte, terminar tu sesión, o transferir contexto a otro agente.",
                                                "system_tool",
                                                java.util.Map.of(
                                                                "type", "object",
                                                                "properties", java.util.Map.of(
                                                                                "agent_id",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Identidad del agente (ej: claude-cursor, gpt-4)"),
                                                                                "session_id",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "ID de la sesión actual si existe"),
                                                                                "intent",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Qué intentabas hacer (ej: 'Debugging auth flow')"),
                                                                                "verified",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "JSON Array con hechos o pasos ya verificados descartados"),
                                                                                "confidence",
                                                                                java.util.Map.of("type", "number",
                                                                                                "description",
                                                                                                "Nivel de confianza en la solución actual (0.0 a 1.0)"),
                                                                                "boundary_hit",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Razón por la que te detienes o escalas"),
                                                                                "tags",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "JSON Array de tags para fácil búsqueda (ej: ['auth', 'database'])")),
                                                                "required",
                                                                List.of("agent_id", "intent", "confidence"))),
                                new ToolDefinition(
                                                "handsai_get_intent",
                                                "Recupera intenciones activas (estados) guardadas previamente por agentes para continuar el trabajo.",
                                                "system_tool",
                                                java.util.Map.of(
                                                                "type", "object",
                                                                "properties", java.util.Map.of(
                                                                                "agent_id",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Opcional: ID del agente que dejó el contexto"),
                                                                                "tags",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Opcional: Un tag para filtrar")),
                                                                "required", List.of())),
                                new ToolDefinition(
                                                "handsai_complete_intent",
                                                "Marca una intención activa como resuelta/completada exitosamente.",
                                                "system_tool",
                                                java.util.Map.of(
                                                                "type", "object",
                                                                "properties", java.util.Map.of(
                                                                                "id",
                                                                                java.util.Map.of("type", "number",
                                                                                                "description",
                                                                                                "ID de la intención a completar")),
                                                                "required", List.of("id"))),
                                new ToolDefinition(
                                                "handsai_delete_intent",
                                                "Elimina permanentemente una intención de memoria (Hard Delete).",
                                                "system_tool",
                                                java.util.Map.of(
                                                                "type", "object",
                                                                "properties", java.util.Map.of(
                                                                                "id",
                                                                                java.util.Map.of("type", "number",
                                                                                                "description",
                                                                                                "ID de la intención a eliminar")),
                                                                "required", List.of("id"))),
                                new ToolDefinition(
                                                "handsai_save_knowledge",
                                                "Guarda aprendizajes estratégicos, arquitecturales o resolución de bugs a largo plazo en el sistema.",
                                                "system_tool",
                                                java.util.Map.of(
                                                                "type", "object",
                                                                "properties", java.util.Map.of(
                                                                                "title",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Título descriptivo corto"),
                                                                                "category",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Categoría (ARCHITECTURE, BUGFIX, DECISION, PATTERN, LEARNING, DISCOVERY, CONFIG, MANUAL)"),
                                                                                "content_what",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Descripción concisa de qué se hizo/aprendió"),
                                                                                "content_why",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Razonamiento o problema que originó esto"),
                                                                                "content_where",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Archivos, módulos o paths afectados"),
                                                                                "content_learned",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Edge cases aislados, configuraciones o aprendizajes útiles")),
                                                                "required",
                                                                List.of("title", "category", "content_what"))),
                                new ToolDefinition(
                                                "handsai_search_knowledge",
                                                "Busca en el historial de conocimiento (Engram) usando un texto o categoría.",
                                                "system_tool",
                                                java.util.Map.of(
                                                                "type", "object",
                                                                "properties", java.util.Map.of(
                                                                                "query",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Texto a buscar en el título o contenidos"),
                                                                                "category",
                                                                                java.util.Map.of("type", "string",
                                                                                                "description",
                                                                                                "Filtro por nombre de categoría opcional")),
                                                                "required", List.of())),
                                new ToolDefinition(
                                                "handsai_delete_knowledge",
                                                "Elimina permanentemente una entrada de la base de conocimiento.",
                                                "system_tool",
                                                java.util.Map.of(
                                                                "type", "object",
                                                                "properties", java.util.Map.of(
                                                                                "id",
                                                                                java.util.Map.of("type", "number",
                                                                                                "description",
                                                                                                "ID del conocimiento a eliminar")),
                                                                "required", List.of("id"))));
        }
}
