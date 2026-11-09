package com.showroom.inventory.controller;

import com.showroom.inventory.dto.DashboardDTO;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.ReportService;
import com.showroom.inventory.service.SaleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

/**
 * Controller serving the Role-Aware Executive & Operational Dashboard with
 * interactive date filters (Today, Yesterday, Last 7 Days, This Week, This Month,
 * Last Month, Custom Date Range) and real-time database analytics.
 */
@Controller
public class DashboardController {

    private final ReportService reportService;
    private final ActivityLogService activityLogService;

    public DashboardController(ReportService reportService,
                               ActivityLogService activityLogService) {
        this.reportService = reportService;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "period", required = false, defaultValue = "THIS_MONTH") String period,
                            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customStart,
                            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate customEnd,
                            Model model) {
        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        LocalDate today = LocalDate.now(SaleService.IST_ZONE);

        String normalizedPeriod = period != null ? period.trim().toUpperCase() : "THIS_MONTH";
        LocalDate resolvedStart;
        LocalDate resolvedEnd;
        String periodLabel;

        switch (normalizedPeriod) {
            case "TODAY" -> {
                resolvedStart = today;
                resolvedEnd = today;
                periodLabel = "Today";
            }
            case "YESTERDAY" -> {
                resolvedStart = today.minusDays(1);
                resolvedEnd = today.minusDays(1);
                periodLabel = "Yesterday";
            }
            case "LAST_7_DAYS" -> {
                resolvedStart = today.minusDays(6);
                resolvedEnd = today;
                periodLabel = "Last 7 Days";
            }
            case "THIS_WEEK" -> {
                resolvedStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                resolvedEnd = today;
                periodLabel = "This Week";
            }
            case "THIS_MONTH" -> {
                YearMonth thisYm = YearMonth.from(today);
                resolvedStart = thisYm.atDay(1);
                resolvedEnd = thisYm.atEndOfMonth();
                periodLabel = "This Month";
            }
            case "LAST_MONTH" -> {
                YearMonth lastYm = YearMonth.from(today).minusMonths(1);
                resolvedStart = lastYm.atDay(1);
                resolvedEnd = lastYm.atEndOfMonth();
                periodLabel = "Last Month";
            }
            case "CUSTOM" -> {
                resolvedStart = customStart != null ? customStart : YearMonth.from(today).atDay(1);
                resolvedEnd = customEnd != null ? customEnd : today;
                if (resolvedStart.isAfter(resolvedEnd)) {
                    LocalDate tmp = resolvedStart;
                    resolvedStart = resolvedEnd;
                    resolvedEnd = tmp;
                }
                periodLabel = "Custom Range";
            }
            default -> {
                normalizedPeriod = "THIS_MONTH";
                YearMonth thisYm = YearMonth.from(today);
                resolvedStart = thisYm.atDay(1);
                resolvedEnd = thisYm.atEndOfMonth();
                periodLabel = "This Month";
            }
        }

        DashboardDTO dashboardData = reportService.getDashboardData(
                resolvedStart, resolvedEnd, normalizedPeriod, periodLabel, currentUser
        );

        boolean ownSalesOnly = currentUser != null
                && !currentUser.hasPermission(Permission.SALES_VIEW_ALL)
                && currentUser.hasPermission(Permission.SALES_VIEW_OWN);

        model.addAttribute("dashboard", dashboardData);
        model.addAttribute("selectedPeriod", normalizedPeriod);
        model.addAttribute("startDate", resolvedStart);
        model.addAttribute("endDate", resolvedEnd);
        model.addAttribute("ownSalesOnly", ownSalesOnly);
        model.addAttribute("activeNav", "dashboard");
        return "dashboard";
    }
}
