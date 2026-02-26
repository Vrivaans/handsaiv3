package org.dynamcorp.handsaiv2.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ImportApiProviderRequest;
import org.dynamcorp.handsaiv2.service.ImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
public class ImportController {

    private final ImportService importService;

    @PostMapping("/providers")
    public ResponseEntity<Map<String, String>> importProviders(
            @RequestBody List<ImportApiProviderRequest> importRequests) {
        log.info("Received request to import {} providers", importRequests.size());
        importService.importProviders(importRequests);
        return ResponseEntity.ok(Map.of("message", "Import successful"));
    }
}
