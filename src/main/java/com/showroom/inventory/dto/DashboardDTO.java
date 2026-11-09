package com.showroom.inventory.dto;

import com.showroom.inventory.entity.Product;
import com.showroom.inventory.entity.Purchase;
import com.showroom.inventory.entity.Sale;
import com.showroom.inventory.entity.StockMovement;
import com.showroom.inventory.util.CurrencyUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive DTO carrying role-scoped KPIs, period comparisons, interactive chart series,
 * employee sales performance, product velocity, and recent transactions for the Executive Dashboard.
 */
public class DashboardDTO {

    // Catalog & Stock KPIs
    private long totalProducts;
    private long totalCatalogProducts;
    private long totalStockQuantity;
    private long lowStockCount;
    private long outOfStockCount;
    private long optimalStockCount;
    private long activeSuppliersCount;

    // Today & Yesterday Sales KPIs
    private BigDecimal todaySales = BigDecimal.ZERO;
    private BigDecimal yesterdaySales = BigDecimal.ZERO;
    private BigDecimal todayVsYesterdayPct = BigDecimal.ZERO;
    private long todayInvoiceCount;

    // Selected Period Financial & Sales KPIs (also mapped to monthly* getters for backward compatibility)
    private String periodKey = "THIS_MONTH";
    private String periodLabel = "This Month";
    private String periodRangeText = "";
    private BigDecimal monthlyRevenue = BigDecimal.ZERO;
    private BigDecimal prevPeriodRevenue = BigDecimal.ZERO;
    private BigDecimal revenueGrowthPct = BigDecimal.ZERO;
    private long periodInvoiceCount;
    private long periodUnitsSold;

    private BigDecimal periodCogs = BigDecimal.ZERO;
    private BigDecimal periodGrossProfit = BigDecimal.ZERO;
    private BigDecimal periodGrossMargin = BigDecimal.ZERO;

    private BigDecimal monthlyExpenses = BigDecimal.ZERO;
    private BigDecimal prevPeriodExpenses = BigDecimal.ZERO;
    private BigDecimal expenseRatioPct = BigDecimal.ZERO;
    private long periodExpenseCount;

    private BigDecimal monthlyNetProfit = BigDecimal.ZERO;
    private BigDecimal periodNetMargin = BigDecimal.ZERO;

    // Procurement / Stock-In KPIs for Selected Period
    private BigDecimal periodPurchasesAmount = BigDecimal.ZERO;
    private long periodPurchasesCount;
    private long periodUnitsReceived;

    // Payment Mode Breakdown in Selected Period
    private BigDecimal cashSalesTotal = BigDecimal.ZERO;
    private BigDecimal upiSalesTotal = BigDecimal.ZERO;
    private BigDecimal cardSalesTotal = BigDecimal.ZERO;
    private BigDecimal bankSalesTotal = BigDecimal.ZERO;

    // Lists & Tables
    private List<Product> lowStockProducts = new ArrayList<>();
    private List<Sale> recentSales = new ArrayList<>();
    private List<Purchase> recentPurchases = new ArrayList<>();
    private List<StockMovement> recentStockMovements = new ArrayList<>();
    private List<Object[]> bestSellingProducts = new ArrayList<>();
    private List<Object[]> slowMovingProducts = new ArrayList<>();
    private List<EmployeePerformanceItem> employeePerformance = new ArrayList<>();
    private List<CategoryStockItem> categoryStockBreakdown = new ArrayList<>();
    private List<VendorProcurementItem> vendorProcurementBreakdown = new ArrayList<>();

    // Interactive Chart Series (JSON-safe lists)
    private List<String> dailyLabels = new ArrayList<>();
    private List<BigDecimal> dailyRevenueSeries = new ArrayList<>();
    private List<BigDecimal> dailyExpenseSeries = new ArrayList<>();
    private List<Long> dailyInvoiceSeries = new ArrayList<>();

    private List<String> weeklyLabels = new ArrayList<>();
    private List<BigDecimal> weeklyRevenueSeries = new ArrayList<>();
    private List<BigDecimal> weeklyExpenseSeries = new ArrayList<>();

    private List<String> monthlyLabels = new ArrayList<>();
    private List<BigDecimal> monthlyRevenueSeries = new ArrayList<>();
    private List<BigDecimal> monthlyExpenseSeries = new ArrayList<>();

    public DashboardDTO() {
    }

    public static class EmployeePerformanceItem {
        private final String username;
        private final String employeeName;
        private final String roleName;
        private final long invoiceCount;
        private final long unitsSold;
        private final BigDecimal totalRevenue;
        private final int sharePercent;

        public EmployeePerformanceItem(String username,
                                       String employeeName,
                                       String roleName,
                                       long invoiceCount,
                                       long unitsSold,
                                       BigDecimal totalRevenue,
                                       int sharePercent) {
            this.username = username;
            this.employeeName = employeeName;
            this.roleName = roleName;
            this.invoiceCount = invoiceCount;
            this.unitsSold = unitsSold;
            this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
            this.sharePercent = sharePercent;
        }

        public String getUsername() { return username; }
        public String getEmployeeName() { return employeeName; }
        public String getRoleName() { return roleName; }
        public long getInvoiceCount() { return invoiceCount; }
        public long getUnitsSold() { return unitsSold; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public int getSharePercent() { return sharePercent; }
    }

    public static class CategoryStockItem {
        private final String categoryName;
        private final long productCount;
        private final long totalUnits;
        private final int sharePercent;

        public CategoryStockItem(String categoryName, long productCount, long totalUnits, int sharePercent) {
            this.categoryName = categoryName;
            this.productCount = productCount;
            this.totalUnits = totalUnits;
            this.sharePercent = sharePercent;
        }

        public String getCategoryName() { return categoryName; }
        public long getProductCount() { return productCount; }
        public long getTotalUnits() { return totalUnits; }
        public int getSharePercent() { return sharePercent; }
    }

    public static class VendorProcurementItem {
        private final String vendorName;
        private final long orderCount;
        private final long unitsReceived;
        private final BigDecimal totalAmount;
        private final int sharePercent;

        public VendorProcurementItem(String vendorName, long orderCount, long unitsReceived, BigDecimal totalAmount, int sharePercent) {
            this.vendorName = vendorName;
            this.orderCount = orderCount;
            this.unitsReceived = unitsReceived;
            this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
            this.sharePercent = sharePercent;
        }

        public String getVendorName() { return vendorName; }
        public long getOrderCount() { return orderCount; }
        public long getUnitsReceived() { return unitsReceived; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public int getSharePercent() { return sharePercent; }
    }

    // Getters & Setters
    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }

    public long getTotalCatalogProducts() { return totalCatalogProducts; }
    public void setTotalCatalogProducts(long totalCatalogProducts) { this.totalCatalogProducts = totalCatalogProducts; }

    public long getTotalStockQuantity() { return totalStockQuantity; }
    public void setTotalStockQuantity(long totalStockQuantity) { this.totalStockQuantity = totalStockQuantity; }

    public long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }

    public long getOutOfStockCount() { return outOfStockCount; }
    public void setOutOfStockCount(long outOfStockCount) { this.outOfStockCount = outOfStockCount; }

    public long getOptimalStockCount() { return optimalStockCount; }
    public void setOptimalStockCount(long optimalStockCount) { this.optimalStockCount = optimalStockCount; }

    public long getActiveSuppliersCount() { return activeSuppliersCount; }
    public void setActiveSuppliersCount(long activeSuppliersCount) { this.activeSuppliersCount = activeSuppliersCount; }

    public BigDecimal getTodaySales() { return todaySales != null ? todaySales : BigDecimal.ZERO; }
    public void setTodaySales(BigDecimal todaySales) { this.todaySales = todaySales; }

    public BigDecimal getYesterdaySales() { return yesterdaySales != null ? yesterdaySales : BigDecimal.ZERO; }
    public void setYesterdaySales(BigDecimal yesterdaySales) { this.yesterdaySales = yesterdaySales; }

    public BigDecimal getTodayVsYesterdayPct() { return todayVsYesterdayPct != null ? todayVsYesterdayPct : BigDecimal.ZERO; }
    public void setTodayVsYesterdayPct(BigDecimal todayVsYesterdayPct) { this.todayVsYesterdayPct = todayVsYesterdayPct; }

    public long getTodayInvoiceCount() { return todayInvoiceCount; }
    public void setTodayInvoiceCount(long todayInvoiceCount) { this.todayInvoiceCount = todayInvoiceCount; }

    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }

    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }

    public String getPeriodRangeText() { return periodRangeText; }
    public void setPeriodRangeText(String periodRangeText) { this.periodRangeText = periodRangeText; }

    public BigDecimal getMonthlyRevenue() { return monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO; }
    public void setMonthlyRevenue(BigDecimal monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }

    public BigDecimal getPrevPeriodRevenue() { return prevPeriodRevenue != null ? prevPeriodRevenue : BigDecimal.ZERO; }
    public void setPrevPeriodRevenue(BigDecimal prevPeriodRevenue) { this.prevPeriodRevenue = prevPeriodRevenue; }

    public BigDecimal getRevenueGrowthPct() { return revenueGrowthPct != null ? revenueGrowthPct : BigDecimal.ZERO; }
    public void setRevenueGrowthPct(BigDecimal revenueGrowthPct) { this.revenueGrowthPct = revenueGrowthPct; }

    public long getPeriodInvoiceCount() { return periodInvoiceCount; }
    public void setPeriodInvoiceCount(long periodInvoiceCount) { this.periodInvoiceCount = periodInvoiceCount; }

    public long getPeriodUnitsSold() { return periodUnitsSold; }
    public void setPeriodUnitsSold(long periodUnitsSold) { this.periodUnitsSold = periodUnitsSold; }

    public BigDecimal getPeriodCogs() { return periodCogs != null ? periodCogs : BigDecimal.ZERO; }
    public void setPeriodCogs(BigDecimal periodCogs) { this.periodCogs = periodCogs; }

    public BigDecimal getPeriodGrossProfit() { return periodGrossProfit != null ? periodGrossProfit : BigDecimal.ZERO; }
    public void setPeriodGrossProfit(BigDecimal periodGrossProfit) { this.periodGrossProfit = periodGrossProfit; }

    public BigDecimal getPeriodGrossMargin() { return periodGrossMargin != null ? periodGrossMargin : BigDecimal.ZERO; }
    public void setPeriodGrossMargin(BigDecimal periodGrossMargin) { this.periodGrossMargin = periodGrossMargin; }

    public BigDecimal getMonthlyExpenses() { return monthlyExpenses != null ? monthlyExpenses : BigDecimal.ZERO; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }

    public BigDecimal getPrevPeriodExpenses() { return prevPeriodExpenses != null ? prevPeriodExpenses : BigDecimal.ZERO; }
    public void setPrevPeriodExpenses(BigDecimal prevPeriodExpenses) { this.prevPeriodExpenses = prevPeriodExpenses; }

    public BigDecimal getExpenseRatioPct() { return expenseRatioPct != null ? expenseRatioPct : BigDecimal.ZERO; }
    public void setExpenseRatioPct(BigDecimal expenseRatioPct) { this.expenseRatioPct = expenseRatioPct; }

    public long getPeriodExpenseCount() { return periodExpenseCount; }
    public void setPeriodExpenseCount(long periodExpenseCount) { this.periodExpenseCount = periodExpenseCount; }

    public BigDecimal getMonthlyNetProfit() { return monthlyNetProfit != null ? monthlyNetProfit : BigDecimal.ZERO; }
    public void setMonthlyNetProfit(BigDecimal monthlyNetProfit) { this.monthlyNetProfit = monthlyNetProfit; }

    public BigDecimal getPeriodNetMargin() { return periodNetMargin != null ? periodNetMargin : BigDecimal.ZERO; }
    public void setPeriodNetMargin(BigDecimal periodNetMargin) { this.periodNetMargin = periodNetMargin; }

    public BigDecimal getPeriodPurchasesAmount() { return periodPurchasesAmount != null ? periodPurchasesAmount : BigDecimal.ZERO; }
    public void setPeriodPurchasesAmount(BigDecimal periodPurchasesAmount) { this.periodPurchasesAmount = periodPurchasesAmount; }

    public long getPeriodPurchasesCount() { return periodPurchasesCount; }
    public void setPeriodPurchasesCount(long periodPurchasesCount) { this.periodPurchasesCount = periodPurchasesCount; }

    public long getPeriodUnitsReceived() { return periodUnitsReceived; }
    public void setPeriodUnitsReceived(long periodUnitsReceived) { this.periodUnitsReceived = periodUnitsReceived; }

    public BigDecimal getCashSalesTotal() { return cashSalesTotal != null ? cashSalesTotal : BigDecimal.ZERO; }
    public void setCashSalesTotal(BigDecimal cashSalesTotal) { this.cashSalesTotal = cashSalesTotal; }

    public BigDecimal getUpiSalesTotal() { return upiSalesTotal != null ? upiSalesTotal : BigDecimal.ZERO; }
    public void setUpiSalesTotal(BigDecimal upiSalesTotal) { this.upiSalesTotal = upiSalesTotal; }

    public BigDecimal getCardSalesTotal() { return cardSalesTotal != null ? cardSalesTotal : BigDecimal.ZERO; }
    public void setCardSalesTotal(BigDecimal cardSalesTotal) { this.cardSalesTotal = cardSalesTotal; }

    public BigDecimal getBankSalesTotal() { return bankSalesTotal != null ? bankSalesTotal : BigDecimal.ZERO; }
    public void setBankSalesTotal(BigDecimal bankSalesTotal) { this.bankSalesTotal = bankSalesTotal; }

    public List<Product> getLowStockProducts() { return lowStockProducts; }
    public void setLowStockProducts(List<Product> lowStockProducts) { this.lowStockProducts = lowStockProducts; }

    public List<Sale> getRecentSales() { return recentSales; }
    public void setRecentSales(List<Sale> recentSales) { this.recentSales = recentSales; }

    public List<Purchase> getRecentPurchases() { return recentPurchases; }
    public void setRecentPurchases(List<Purchase> recentPurchases) { this.recentPurchases = recentPurchases; }

    public List<StockMovement> getRecentStockMovements() { return recentStockMovements; }
    public void setRecentStockMovements(List<StockMovement> recentStockMovements) { this.recentStockMovements = recentStockMovements; }

    public List<Object[]> getBestSellingProducts() { return bestSellingProducts; }
    public void setBestSellingProducts(List<Object[]> bestSellingProducts) { this.bestSellingProducts = bestSellingProducts; }

    public List<Object[]> getSlowMovingProducts() { return slowMovingProducts; }
    public void setSlowMovingProducts(List<Object[]> slowMovingProducts) { this.slowMovingProducts = slowMovingProducts; }

    public List<EmployeePerformanceItem> getEmployeePerformance() { return employeePerformance; }
    public void setEmployeePerformance(List<EmployeePerformanceItem> employeePerformance) { this.employeePerformance = employeePerformance; }

    public List<CategoryStockItem> getCategoryStockBreakdown() { return categoryStockBreakdown; }
    public void setCategoryStockBreakdown(List<CategoryStockItem> categoryStockBreakdown) { this.categoryStockBreakdown = categoryStockBreakdown; }

    public List<VendorProcurementItem> getVendorProcurementBreakdown() { return vendorProcurementBreakdown; }
    public void setVendorProcurementBreakdown(List<VendorProcurementItem> vendorProcurementBreakdown) { this.vendorProcurementBreakdown = vendorProcurementBreakdown; }

    public List<String> getDailyLabels() { return dailyLabels; }
    public void setDailyLabels(List<String> dailyLabels) { this.dailyLabels = dailyLabels; }

    public List<BigDecimal> getDailyRevenueSeries() { return dailyRevenueSeries; }
    public void setDailyRevenueSeries(List<BigDecimal> dailyRevenueSeries) { this.dailyRevenueSeries = dailyRevenueSeries; }

    public List<BigDecimal> getDailyExpenseSeries() { return dailyExpenseSeries; }
    public void setDailyExpenseSeries(List<BigDecimal> dailyExpenseSeries) { this.dailyExpenseSeries = dailyExpenseSeries; }

    public List<Long> getDailyInvoiceSeries() { return dailyInvoiceSeries; }
    public void setDailyInvoiceSeries(List<Long> dailyInvoiceSeries) { this.dailyInvoiceSeries = dailyInvoiceSeries; }

    public List<String> getWeeklyLabels() { return weeklyLabels; }
    public void setWeeklyLabels(List<String> weeklyLabels) { this.weeklyLabels = weeklyLabels; }

    public List<BigDecimal> getWeeklyRevenueSeries() { return weeklyRevenueSeries; }
    public void setWeeklyRevenueSeries(List<BigDecimal> weeklyRevenueSeries) { this.weeklyRevenueSeries = weeklyRevenueSeries; }

    public List<BigDecimal> getWeeklyExpenseSeries() { return weeklyExpenseSeries; }
    public void setWeeklyExpenseSeries(List<BigDecimal> weeklyExpenseSeries) { this.weeklyExpenseSeries = weeklyExpenseSeries; }

    public List<String> getMonthlyLabels() { return monthlyLabels; }
    public void setMonthlyLabels(List<String> monthlyLabels) { this.monthlyLabels = monthlyLabels; }

    public List<BigDecimal> getMonthlyRevenueSeries() { return monthlyRevenueSeries; }
    public void setMonthlyRevenueSeries(List<BigDecimal> monthlyRevenueSeries) { this.monthlyRevenueSeries = monthlyRevenueSeries; }

    public List<BigDecimal> getMonthlyExpenseSeries() { return monthlyExpenseSeries; }
    public void setMonthlyExpenseSeries(List<BigDecimal> monthlyExpenseSeries) { this.monthlyExpenseSeries = monthlyExpenseSeries; }

    // Formatted currency helpers using Indian Numbering System
    public String getFormattedTodaySales() { return CurrencyUtil.formatInr(getTodaySales()); }
    public String getFormattedMonthlyRevenue() { return CurrencyUtil.formatInr(getMonthlyRevenue()); }
    public String getFormattedMonthlyExpenses() { return CurrencyUtil.formatInr(getMonthlyExpenses()); }
    public String getFormattedMonthlyNetProfit() { return CurrencyUtil.formatInr(getMonthlyNetProfit()); }
}
