package com.showroom.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO carrying financial report metrics and business analysis for a specified period.
 *
 * Implements strict business formulas:
 *   Gross Profit = Revenue - Cost of Goods Sold (COGS)
 *   Net Profit = Gross Profit - Operating Expenses
 */
public class ReportDTO {

    private LocalDate startDate;
    private LocalDate endDate;
    private int month;
    private int year;
    private String periodLabel;

    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal costOfGoodsSold = BigDecimal.ZERO;
    private BigDecimal grossProfit = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;
    private BigDecimal totalPurchases = BigDecimal.ZERO;
    private BigDecimal grossMargin = BigDecimal.ZERO;
    private BigDecimal profitMargin = BigDecimal.ZERO;

    private long salesCount;
    private long purchasesCount;
    private long totalUnitsSold;

    private List<Object[]> expenseBreakdown = new ArrayList<>();
    private List<Object[]> bestSellingProducts = new ArrayList<>();
    private List<Object[]> slowMovingProducts = new ArrayList<>();
    private List<String> businessSuggestions = new ArrayList<>();
    private List<InsightItem> insights = new ArrayList<>();

    public static class InsightItem {
        private final String icon;
        private final String badgeClass;
        private final String title;
        private final String description;

        public InsightItem(String icon, String badgeClass, String title, String description) {
            this.icon = icon;
            this.badgeClass = badgeClass;
            this.title = title;
            this.description = description;
        }

        public String getIcon() {
            return icon;
        }

        public String getBadgeClass() {
            return badgeClass;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    public ReportDTO() {
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public BigDecimal getRevenue() {
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getCostOfGoodsSold() {
        return costOfGoodsSold != null ? costOfGoodsSold : BigDecimal.ZERO;
    }

    public void setCostOfGoodsSold(BigDecimal costOfGoodsSold) {
        this.costOfGoodsSold = costOfGoodsSold;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit != null ? grossProfit : BigDecimal.ZERO;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses != null ? totalExpenses : BigDecimal.ZERO;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetProfit() {
        return netProfit != null ? netProfit : BigDecimal.ZERO;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }

    public long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(long salesCount) {
        this.salesCount = salesCount;
    }

    public long getPurchasesCount() {
        return purchasesCount;
    }

    public void setPurchasesCount(long purchasesCount) {
        this.purchasesCount = purchasesCount;
    }

    public List<Object[]> getExpenseBreakdown() {
        return expenseBreakdown;
    }

    public void setExpenseBreakdown(List<Object[]> expenseBreakdown) {
        this.expenseBreakdown = expenseBreakdown;
    }

    public List<Object[]> getBestSellingProducts() {
        return bestSellingProducts;
    }

    public void setBestSellingProducts(List<Object[]> bestSellingProducts) {
        this.bestSellingProducts = bestSellingProducts;
    }

    public List<Object[]> getSlowMovingProducts() {
        return slowMovingProducts;
    }

    public void setSlowMovingProducts(List<Object[]> slowMovingProducts) {
        this.slowMovingProducts = slowMovingProducts;
    }

    public List<String> getBusinessSuggestions() {
        return businessSuggestions;
    }

    public void setBusinessSuggestions(List<String> businessSuggestions) {
        this.businessSuggestions = businessSuggestions;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public BigDecimal getTotalPurchases() {
        return totalPurchases != null ? totalPurchases : BigDecimal.ZERO;
    }

    public void setTotalPurchases(BigDecimal totalPurchases) {
        this.totalPurchases = totalPurchases;
    }

    public BigDecimal getGrossMargin() {
        return grossMargin != null ? grossMargin : BigDecimal.ZERO;
    }

    public void setGrossMargin(BigDecimal grossMargin) {
        this.grossMargin = grossMargin;
    }

    public BigDecimal getProfitMargin() {
        return profitMargin != null ? profitMargin : BigDecimal.ZERO;
    }

    public void setProfitMargin(BigDecimal profitMargin) {
        this.profitMargin = profitMargin;
    }

    public long getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(long totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    public List<InsightItem> getInsights() {
        return insights;
    }

    public void setInsights(List<InsightItem> insights) {
        this.insights = insights;
    }
}
