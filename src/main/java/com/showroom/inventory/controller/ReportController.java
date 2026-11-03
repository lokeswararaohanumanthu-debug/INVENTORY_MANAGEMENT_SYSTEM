package com.showroom.inventory.controller;

import com.showroom.inventory.dto.ReportDTO;
import com.showroom.inventory.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Controller serving Financial Reports, P&L Statement, and Business Intelligence Insights.
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String viewReports(@RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                              @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                              Model model) {
        // Default to current month if no dates provided
        if (startDate == null || endDate == null) {
            YearMonth current = YearMonth.now();
            startDate = current.atDay(1);
            endDate = current.atEndOfMonth();
        }

        ReportDTO report = reportService.generateReport(startDate, endDate);

        model.addAttribute("report", report);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("activeNav", "reports");
        return "reports/dashboard";
    }
}
