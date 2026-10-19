package tr.kitaptakip.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tr.kitaptakip.dtos.DashboardResponse;
import tr.kitaptakip.services.DashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping
    public DashboardResponse summary() { return dashboardService.getSummary(); }
}
