package org.dynamcorp.handsaiv2.controller;

import java.util.List;

import org.dynamcorp.handsaiv2.dto.ApiToolResponse;
import org.dynamcorp.handsaiv2.dto.CreateApiToolRequest;
import org.dynamcorp.handsaiv2.dto.UpdateApiToolRequest;
import org.dynamcorp.handsaiv2.service.ApiToolService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/tools")
@RequiredArgsConstructor
public class AdminToolController {

    private final ApiToolService apiToolService;

    @PostMapping("/api")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiToolResponse createApiTool(@RequestBody CreateApiToolRequest request) {
        return apiToolService.createApiTool(request);
    }

    @PutMapping("/api/{id}")
    public ApiToolResponse updateApiTool(
            @PathVariable Long id,
            @RequestBody UpdateApiToolRequest request) {
        return apiToolService.updateApiTool(id, request);
    }

    @GetMapping("/api/{id}")
    public ApiToolResponse getApiTool(@PathVariable Long id) {
        return apiToolService.getApiTool(id);
    }

    @GetMapping("/api")
    public List<ApiToolResponse> getAllApiTools() {
        return apiToolService.getAllApiTools();
    }

    @DeleteMapping("/api/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApiTool(@PathVariable Long id) {
        apiToolService.deleteApiTool(id);
    }

    @PostMapping("/api/{id}/validate")
    public ApiToolResponse validateApiToolHealth(@PathVariable Long id) {
        return apiToolService.validateApiToolHealth(id);
    }
}
