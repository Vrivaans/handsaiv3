package org.dynamcorp.handsaiv2.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dynamcorp.handsaiv2.dto.ExportApiProviderDto;
import org.dynamcorp.handsaiv2.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;

    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ExportApiProviderDto>> exportProviders(@RequestParam(required = false) List<Long> ids) {
        // TODO: Hacer capaz el sistema para exportar proveedores basándonos en el
        // código (string) en lugar del ID
        log.info("Received request to export providers. IDs: {}", ids);
        List<ExportApiProviderDto> exportData = exportService.exportProviders(ids);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=handsai_tools_export.json");

        return ResponseEntity.ok()
                .headers(headers)
                .body(exportData);
    }
}
