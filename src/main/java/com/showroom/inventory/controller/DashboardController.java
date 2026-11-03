package com.showroom.inventory.controller;

import com.showroom.inventory.dto.DashboardDTO;
import com.showroom.inventory.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller serving the executive Dashboard.
 */
@Controller
public class DashboardController {

    private final ReportService reportService;

    public DashboardController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardDTO dashboardData = reportService.getDashboardData();
        model.addAttribute("dashboard", dashboardData);
        model.addAttribute("activeNav", "dashboard");
        return "dashboard";
    }
}
