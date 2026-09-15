package com.verdefluxo.acelera.controller;

import com.verdefluxo.acelera.model.dto.MetricsDTO;
import com.verdefluxo.acelera.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    public ResponseEntity<MetricsDTO> getMetrics() {
        return ResponseEntity.ok(dashboardService.getMetrics());
    }
}
