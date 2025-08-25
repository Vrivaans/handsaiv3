package org.dynamcorp.handsaiv2.controller;

import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.dto.ApiToolResponse;
import org.dynamcorp.handsaiv2.dto.CreateApiToolRequest;
import org.dynamcorp.handsaiv2.dto.UpdateApiToolRequest;
import org.dynamcorp.handsaiv2.service.ApiToolService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/admin/tools")
@RequiredArgsConstructor
public class AdminToolController {

    private final ApiToolService apiToolService;

    @PostMapping("/api")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiToolResponse> createApiTool(@RequestBody CreateApiToolRequest request) {
        return Mono.fromFuture(apiToolService.createApiTool(request))
                .onErrorMap(ex -> new RuntimeException("Error al crear API Tool: " + ex.getMessage(), ex));
    }

    @PutMapping("/api/{id}")
    public Mono<ApiToolResponse> updateApiTool(
            @PathVariable Long id,
            @RequestBody UpdateApiToolRequest request) {
        return Mono.fromFuture(apiToolService.updateApiTool(id, request))
                .onErrorMap(ex -> new RuntimeException("Error al actualizar API Tool con ID " + id + ": " + ex.getMessage(), ex));
    }

    @GetMapping("/api/{id}")
    public Mono<ApiToolResponse> getApiTool(@PathVariable Long id) {
        return Mono.fromFuture(apiToolService.getApiTool(id))
                .onErrorMap(ex -> new RuntimeException("Error al obtener API Tool con ID " + id + ": " + ex.getMessage(), ex));
    }

    @GetMapping("/api")
    public Mono<List<ApiToolResponse>> getAllApiTools() {
        return Mono.fromFuture(apiToolService.getAllApiTools())
                .onErrorMap(ex -> new RuntimeException("Error al obtener todas las API Tools: " + ex.getMessage(), ex));
    }

    @DeleteMapping("/api/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteApiTool(@PathVariable Long id) {
        return Mono.fromFuture(apiToolService.deleteApiTool(id))
                .onErrorMap(ex -> new RuntimeException("Error al eliminar API Tool con ID " + id + ": " + ex.getMessage(), ex));
    }

    @PostMapping("/api/{id}/validate")
    public Mono<ApiToolResponse> validateApiToolHealth(@PathVariable Long id) {
        return Mono.fromFuture(apiToolService.validateApiToolHealth(id))
                .onErrorMap(ex -> new RuntimeException("Error al validar salud de API Tool con ID " + id + ": " + ex.getMessage(), ex));
    }
}
