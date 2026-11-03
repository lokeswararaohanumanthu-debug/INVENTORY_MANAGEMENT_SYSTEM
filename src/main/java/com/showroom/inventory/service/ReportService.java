package com.showroom.inventory.service;

import com.showroom.inventory.dto.DashboardDTO;
import com.showroom.inventory.dto.ReportDTO;
import com.showroom.inventory.entity.Product;
import com.showroom.inventory.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class performing Financial Reporting and Business Intelligence Analysis.
 *
 * Implements strict accounting formulas:
 *   Revenue = SUM(Sale.netAmount)
 *   Cost of Goods Sold (COGS) = SUM(SaleItem.quantity * SaleItem.costPrice)
 *   Gross Profit = Revenue - COGS
 *   Operating Expenses = SUM(Expense.amount)
 *   Net Profit = Gross Profit - Operating Expenses
 *
 * Transparent rule-based recommendations provide actionable business guidance.
 */
@Service
public class ReportService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;

    public ReportService(ProductRepository productRepository,
                         SaleRepository saleRepository,
                         SaleItemRepository saleItemRepository,
                         PurchaseRepository purchaseRepository,
                         ExpenseRepository expenseRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Aggregates live data to populate the executive dashboard.
     */
    public DashboardDTO getDashboardData() {
        DashboardDTO dashboard = new DashboardDTO();

        // Stock metrics
        dashboard.setTotalProducts(productRepository.countByActiveTrue());
        dashboard.setTotalStockQuantity(productRepository.sumTotalStock());

        // Low stock alerts
        List<Product> lowStock = productRepository.findLowStockProducts();
        dashboard.setLowStockCount(lowStock.size());
        dashboard.setLowStockProducts(lowStock);

        // Sales today
        LocalDate today = LocalDate.now();
        BigDecimal todayTotal = saleRepository.sumNetAmountByDate(today);
        dashboard.setTodaySales(todayTotal != null ? todayTotal : BigDecimal.ZERO);

        // Current Month metrics
        YearMonth currentYearMonth = YearMonth.now();
        LocalDate startOfMonth = currentYearMonth.atDay(1);
        LocalDate endOfMonth = currentYearMonth.atEndOfMonth();

        BigDecimal monthlyRev = saleRepository.sumNetAmountBetween(startOfMonth, endOfMonth);
        monthlyRev = monthlyRev != null ? monthlyRev : BigDecimal.ZERO;
        dashboard.setMonthlyRevenue(monthlyRev);

        BigDecimal monthlyExp = expenseRepository.sumAmountBetween(startOfMonth, endOfMonth);
        monthlyExp = monthlyExp != null ? monthlyExp : BigDecimal.ZERO;
        dashboard.setMonthlyExpenses(monthlyExp);

        // COGS for month
        BigDecimal monthlyCogs = saleItemRepository.sumCostOfGoodsSoldBetween(startOfMonth, endOfMonth);
        monthlyCogs = monthlyCogs != null ? monthlyCogs : BigDecimal.ZERO;

        BigDecimal grossProfit = monthlyRev.subtract(monthlyCogs);
        BigDecimal netProfit = grossProfit.subtract(monthlyExp);
        dashboard.setMonthlyNetProfit(netProfit);

        // Recent activity
        dashboard.setRecentSales(saleRepository.findRecentSales(PageRequest.of(0, 5)));
        dashboard.setRecentPurchases(purchaseRepository.findRecentPurchases(PageRequest.of(0, 5)));
        dashboard.setBestSellingProducts(saleItemRepository.findBestSellingProducts(PageRequest.of(0, 5)));

        return dashboard;
    }

    /**
     * Generates a comprehensive financial & performance report for a custom date range.
     */
    public ReportDTO generateReport(LocalDate start, LocalDate end) {
        ReportDTO report = new ReportDTO();
        report.setStartDate(start);
        report.setEndDate(end);

        // 1. Revenue
        BigDecimal revenue = saleRepository.sumNetAmountBetween(start, end);
        report.setRevenue(revenue != null ? revenue : BigDecimal.ZERO);

        // 2. Cost of Goods Sold (COGS)
        BigDecimal cogs = saleItemRepository.sumCostOfGoodsSoldBetween(start, end);
        report.setCostOfGoodsSold(cogs != null ? cogs : BigDecimal.ZERO);

        // 3. Gross Profit = Revenue - COGS
        BigDecimal grossProfit = report.getRevenue().subtract(report.getCostOfGoodsSold());
        report.setGrossProfit(grossProfit);

        // 4. Operating Expenses
        BigDecimal expenses = expenseRepository.sumAmountBetween(start, end);
        report.setTotalExpenses(expenses != null ? expenses : BigDecimal.ZERO);

        // 5. Net Profit = Gross Profit - Operating Expenses
        BigDecimal netProfit = grossProfit.subtract(report.getTotalExpenses());
        report.setNetProfit(netProfit);

        // 6. Counts
        report.setSalesCount(saleRepository.countBySaleDateBetween(start, end));
        report.setPurchasesCount(purchaseRepository.countByPurchaseDateBetween(start, end));

        // 7. Breakdowns
        report.setExpenseBreakdown(expenseRepository.sumAmountByCategoryBetween(start, end));
        report.setBestSellingProducts(saleItemRepository.findBestSellingProducts(PageRequest.of(0, 10)));
        report.setSlowMovingProducts(saleItemRepository.findSlowMovingProducts(PageRequest.of(0, 10)));

        // 8. Generate transparent rule-based recommendations
        report.setBusinessSuggestions(generateSuggestions(report));

        return report;
    }

    /**
     * Transparent rule-based business suggestion engine.
     */
    private List<String> generateSuggestions(ReportDTO report) {
        List<String> suggestions = new ArrayList<>();

        BigDecimal rev = report.getRevenue();
        BigDecimal gross = report.getGrossProfit();
        BigDecimal net = report.getNetProfit();
        BigDecimal exp = report.getTotalExpenses();

        // Rule 1: Net Profit Check
        if (net.compareTo(BigDecimal.ZERO) < 0) {
            suggestions.add("⚠️ Net Loss Alert: Operating expenses exceed gross profits by ₹" + net.abs() + ". Evaluate rent and utility expenses, or optimize staff scheduling.");
        } else if (rev.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal netMargin = net.multiply(BigDecimal.valueOf(100)).divide(rev, 2, RoundingMode.HALF_UP);
            if (netMargin.compareTo(BigDecimal.valueOf(10)) < 0) {
                suggestions.add("💡 Net margin is at " + netMargin + "%. Consider cross-selling high-margin mobile accessories (cases, screen guards, chargers) with phone sales.");
            } else {
                suggestions.add("✅ Healthy net profit margin of " + netMargin + "%. Maintain your current inventory turnover and cost control measures.");
            }
        }

        // Rule 2: Gross Margin Check
        if (rev.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal grossMargin = gross.multiply(BigDecimal.valueOf(100)).divide(rev, 2, RoundingMode.HALF_UP);
            if (grossMargin.compareTo(BigDecimal.valueOf(15)) < 0) {
                suggestions.add("🔍 Low Gross Margin (" + grossMargin + "%): Review vendor purchase agreements or negotiate bulk volume discounts on flagship smartphones.");
            }
        }

        // Rule 3: Expense vs Revenue Ratio
        if (rev.compareTo(BigDecimal.ZERO) > 0 && exp.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal expenseRatio = exp.multiply(BigDecimal.valueOf(100)).divide(rev, 2, RoundingMode.HALF_UP);
            if (expenseRatio.compareTo(BigDecimal.valueOf(30)) > 0) {
                suggestions.add("📊 Operational expenses represent " + expenseRatio + "% of total revenue. Look for potential overhead reductions.");
            }
        }

        // Rule 4: Slow Moving Products
        if (report.getSlowMovingProducts() != null && !report.getSlowMovingProducts().isEmpty()) {
            suggestions.add("🏷️ Clearance Strategy: Identify bottom-performing SKUs and bundle them as promotional gift packages to unlock working capital.");
        }

        // Rule 5: Low stock check
        long lowStockCount = productRepository.findLowStockProducts().size();
        if (lowStockCount > 0) {
            suggestions.add("📦 Restock Warning: " + lowStockCount + " items are currently at or below minimum threshold. Place purchase orders to prevent stockouts.");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("📈 Performance is stable. Continue tracking sales patterns and monitor weekly inventory levels.");
        }

        return suggestions;
    }
}
