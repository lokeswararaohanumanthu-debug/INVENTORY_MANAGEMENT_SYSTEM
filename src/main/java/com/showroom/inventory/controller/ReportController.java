package com.showroom.inventory.controller;

import com.showroom.inventory.dto.ReportDTO;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Controller serving Financial Reports, P&L Statement, Business Intelligence Insights,
 * and authorized Financial Report CSV exports.
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final ActivityLogService activityLogService;

    public ReportController(ReportService reportService, ActivityLogService activityLogService) {
        this.reportService = reportService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public String viewReports(@RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                              @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                              Model model) {
        YearMonth current = YearMonth.now(com.showroom.inventory.service.SaleService.IST_ZONE);
        if (startDate == null && endDate == null) {
            startDate = current.atDay(1);
            endDate = current.atEndOfMonth();
        } else if (startDate == null) {
            startDate = YearMonth.from(endDate).atDay(1);
        } else if (endDate == null) {
            endDate = YearMonth.from(startDate).atEndOfMonth();
        }

        if (startDate.isAfter(endDate)) {
            model.addAttribute("dateWarning", "The 'From Date' (" + startDate + ") cannot be later than the 'To Date' (" + endDate + "). The reporting range has been automatically adjusted.");
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        ReportDTO report = reportService.generateReport(startDate, endDate);

        model.addAttribute("report", report);
        model.addAttribute("startDate", report.getStartDate());
        model.addAttribute("endDate", report.getEndDate());
        model.addAttribute("activeNav", "reports");
        return "reports/dashboard";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFinancialReport(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser == null || !currentUser.hasPermission(Permission.REPORT_EXPORT)) {
            throw new AccessDeniedException("You do not have permission to export financial reports.");
        }

        YearMonth current = YearMonth.now(com.showroom.inventory.service.SaleService.IST_ZONE);
        if (startDate == null) startDate = current.atDay(1);
        if (endDate == null) endDate = current.atEndOfMonth();
        if (startDate.isAfter(endDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        ReportDTO report = reportService.generateReport(startDate, endDate);
        StringBuilder csv = new StringBuilder();
        csv.append("MobileZone Pro - Financial Statement & Profit/Loss Summary\n");
        csv.append("Reporting Period,").append(report.getStartDate()).append(" to ").append(report.getEndDate()).append("\n");
        csv.append("Metric,Value (INR)\n");
        csv.append("Total Sales Revenue,").append(report.getRevenue()).append("\n");
        csv.append("Cost of Goods Sold (COGS),").append(report.getCostOfGoodsSold()).append("\n");
        csv.append("Gross Profit,").append(report.getGrossProfit()).append("\n");
        csv.append("Operating Expenses,").append(report.getTotalExpenses()).append("\n");
        csv.append("Net Profit / Loss,").append(report.getNetProfit()).append("\n");
        csv.append("Gross Margin (%),").append(report.getGrossMargin()).append("%\n");
        csv.append("Net Profit Margin (%),").append(report.getProfitMargin()).append("%\n");
        csv.append("Total Inventory Purchases (Stock-In),").append(report.getTotalPurchases()).append("\n");
        csv.append("Completed Sales Invoices,").append(report.getSalesCount()).append("\n");
        csv.append("Total Units Sold,").append(report.getTotalUnitsSold()).append("\n");

        activityLogService.logCurrentUserAction(
                "EXPORT_FINANCIAL_REPORT",
                "REPORTS",
                "Exported P&L Financial Report CSV for period " + report.getStartDate() + " to " + report.getEndDate(),
                null,
                "Period: " + report.getPeriodLabel()
        );

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"financial-pnl-report-" + report.getStartDate() + "-to-" + report.getEndDate() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
