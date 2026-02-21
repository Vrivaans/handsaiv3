package org.dynamcorp.handsaiv2.controller;

import lombok.RequiredArgsConstructor;
import org.dynamcorp.handsaiv2.dto.AnalyticsSummaryResponse;
import org.dynamcorp.handsaiv2.dto.ToolExecutionLogResponse;
import org.dynamcorp.handsaiv2.service.AnalyticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public AnalyticsSummaryResponse getSummary(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.getSummaryMetrics(days);
    }

    @GetMapping("/logs")
    public Page<ToolExecutionLogResponse> getLogs(@PageableDefault(size = 20) Pageable pageable) {
        return analyticsService.getExecutionLogs(pageable);
    }
}
