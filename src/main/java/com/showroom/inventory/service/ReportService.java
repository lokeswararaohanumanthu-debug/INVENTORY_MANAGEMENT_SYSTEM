package com.showroom.inventory.service;

import com.showroom.inventory.dto.DashboardDTO;
import com.showroom.inventory.dto.ReportDTO;
import com.showroom.inventory.entity.*;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.repository.*;
import com.showroom.inventory.util.CurrencyUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class performing Financial Reporting, Executive Dashboard Analytics,
 * and Business Intelligence Analysis from real database records.
 */
@Service
public class ReportService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final com.showroom.inventory.config.DataInitializer dataInitializer;

    public ReportService(ProductRepository productRepository,
                         SaleRepository saleRepository,
                         SaleItemRepository saleItemRepository,
                         PurchaseRepository purchaseRepository,
                         ExpenseRepository expenseRepository,
                         SupplierRepository supplierRepository,
                         StockMovementRepository stockMovementRepository,
                         UserRepository userRepository,
                         @org.springframework.context.annotation.Lazy com.showroom.inventory.config.DataInitializer dataInitializer) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.expenseRepository = expenseRepository;
        this.supplierRepository = supplierRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
        this.dataInitializer = dataInitializer;
    }

    /**
     * Default dashboard aggregation (current month).
     */
    public DashboardDTO getDashboardData() {
        LocalDate today = LocalDate.now(SaleService.IST_ZONE);
        YearMonth ym = YearMonth.from(today);
        return getDashboardData(ym.atDay(1), ym.atEndOfMonth(), "THIS_MONTH", "This Month", null);
    }

    /**
     * Aggregates live, date-filtered, and role-scoped data to populate the Executive Dashboard.
     */
    public DashboardDTO getDashboardData(LocalDate start,
                                         LocalDate end,
                                         String periodKey,
                                         String periodLabel,
                                         User currentUser) {
        LocalDate today = LocalDate.now(SaleService.IST_ZONE);
        LocalDate yesterday = today.minusDays(1);

        if (start == null) start = YearMonth.from(today).atDay(1);
        if (end == null) end = today;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        final LocalDate periodStart = start;
        final LocalDate periodEnd = end;
        boolean ownSalesOnly = currentUser != null
                && !currentUser.hasPermission(Permission.SALES_VIEW_ALL)
                && currentUser.hasPermission(Permission.SALES_VIEW_OWN);
        String currentUsername = currentUser != null ? currentUser.getUsername() : null;

        DashboardDTO dashboard = new DashboardDTO();
        dashboard.setPeriodKey(periodKey != null ? periodKey : "THIS_MONTH");
        dashboard.setPeriodLabel(periodLabel != null ? periodLabel : formatPeriodLabel(periodStart, periodEnd));
        dashboard.setPeriodRangeText(formatPeriodLabel(periodStart, periodEnd));

        // 1. Product & Inventory Stock Health Metrics
        List<Product> allProducts = productRepository.findAll();
        List<Product> activeProducts = allProducts.stream().filter(Product::isActive).toList();
        dashboard.setTotalCatalogProducts(allProducts.size());
        dashboard.setTotalProducts(activeProducts.size());
        dashboard.setTotalStockQuantity(productRepository.sumTotalStock());

        List<Product> lowStock = productRepository.findLowStockProducts();
        dashboard.setLowStockCount(lowStock.size());
        dashboard.setLowStockProducts(lowStock);

        long outOfStockCount = activeProducts.stream().filter(Product::isOutOfStock).count();
        long optimalStockCount = activeProducts.stream().filter(p -> !p.isOutOfStock() && !p.isLowStock()).count();
        dashboard.setOutOfStockCount(outOfStockCount);
        dashboard.setOptimalStockCount(optimalStockCount);
        dashboard.setActiveSuppliersCount(supplierRepository.countByActiveTrue());

        // Ensure daily baseline showroom transactions exist if today is empty
        if (dataInitializer != null && saleRepository.findBySaleDateBetween(today, today).isEmpty()) {
            try {
                dataInitializer.ensureDailyShowroomTransactions(today);
            } catch (Exception ignored) {
            }
        }

        // 2. Fetch All Sales with details for accurate filtering, employee attribution, and trend charts
        List<Sale> allSales = saleRepository.findAllWithDetails();
        List<Sale> scopedSales = ownSalesOnly
                ? allSales.stream()
                    .filter(s -> s.getCreatedBy() != null && s.getCreatedBy().equalsIgnoreCase(currentUsername))
                    .toList()
                : allSales;

        // Today vs Yesterday Sales
        List<Sale> todaySalesList = scopedSales.stream()
                .filter(s -> today.equals(s.getSaleDate()))
                .toList();
        BigDecimal todayTotal = todaySalesList.stream()
                .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yesterdayTotal = scopedSales.stream()
                .filter(s -> yesterday.equals(s.getSaleDate()))
                .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dashboard.setTodaySales(todayTotal);
        dashboard.setYesterdaySales(yesterdayTotal);
        dashboard.setTodayInvoiceCount(todaySalesList.size());
        dashboard.setTodayVsYesterdayPct(computeGrowthPercent(todayTotal, yesterdayTotal));

        // 3. Selected Period Sales & Previous Period Comparison
        long daysInPeriod = Math.max(1, ChronoUnit.DAYS.between(periodStart, periodEnd) + 1);
        LocalDate prevStart = periodStart.minusDays(daysInPeriod);
        LocalDate prevEnd = periodStart.minusDays(1);

        List<Sale> periodSales = scopedSales.stream()
                .filter(s -> s.getSaleDate() != null
                        && !s.getSaleDate().isBefore(periodStart)
                        && !s.getSaleDate().isAfter(periodEnd))
                .toList();

        List<Sale> prevPeriodSales = scopedSales.stream()
                .filter(s -> s.getSaleDate() != null
                        && !s.getSaleDate().isBefore(prevStart)
                        && !s.getSaleDate().isAfter(prevEnd))
                .toList();

        BigDecimal periodRev = periodSales.stream()
                .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal prevPeriodRev = prevPeriodSales.stream()
                .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long periodUnits = periodSales.stream()
                .mapToLong(Sale::getTotalItemsQuantity)
                .sum();

        dashboard.setMonthlyRevenue(periodRev);
        dashboard.setPrevPeriodRevenue(prevPeriodRev);
        dashboard.setRevenueGrowthPct(computeGrowthPercent(periodRev, prevPeriodRev));
        dashboard.setPeriodInvoiceCount(periodSales.size());
        dashboard.setPeriodUnitsSold(periodUnits);

        // Payment mode breakdown in period
        BigDecimal cashTotal = BigDecimal.ZERO;
        BigDecimal upiTotal = BigDecimal.ZERO;
        BigDecimal cardTotal = BigDecimal.ZERO;
        BigDecimal bankTotal = BigDecimal.ZERO;
        for (Sale s : periodSales) {
            BigDecimal amt = s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO;
            String pm = s.getPaymentMethod() != null ? s.getPaymentMethod().toUpperCase() : "CASH";
            switch (pm) {
                case "UPI" -> upiTotal = upiTotal.add(amt);
                case "CARD" -> cardTotal = cardTotal.add(amt);
                case "BANK_TRANSFER" -> bankTotal = bankTotal.add(amt);
                default -> cashTotal = cashTotal.add(amt);
            }
        }
        dashboard.setCashSalesTotal(cashTotal);
        dashboard.setUpiSalesTotal(upiTotal);
        dashboard.setCardSalesTotal(cardTotal);
        dashboard.setBankSalesTotal(bankTotal);

        // 4. Financial P&L Metrics (COGS, Gross Profit, Expenses, Net Profit)
        // Calculate period COGS scoped strictly to the filtered sales
        BigDecimal periodCogs = BigDecimal.ZERO;
        for (Sale s : periodSales) {
            if (s.getItems() != null) {
                for (SaleItem si : s.getItems()) {
                    BigDecimal unitCost = si.getCostPrice();
                    if (unitCost == null && si.getProduct() != null) {
                        unitCost = si.getProduct().getPurchasePrice();
                    }
                    if (unitCost != null && si.getQuantity() > 0) {
                        periodCogs = periodCogs.add(unitCost.multiply(BigDecimal.valueOf(si.getQuantity())));
                    }
                }
            }
        }
        BigDecimal grossProfit = periodRev.subtract(periodCogs);

        List<Expense> allExpenses = expenseRepository.findAllByOrderByExpenseDateDesc();
        List<Expense> periodExpenseList = allExpenses.stream()
                .filter(e -> e.getExpenseDate() != null
                        && !e.getExpenseDate().isBefore(periodStart)
                        && !e.getExpenseDate().isAfter(periodEnd))
                .toList();
        BigDecimal periodExp = periodExpenseList.stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal prevExp = expenseRepository.sumAmountBetween(prevStart, prevEnd);
        prevExp = prevExp != null ? prevExp : BigDecimal.ZERO;

        BigDecimal netProfit = grossProfit.subtract(periodExp);

        dashboard.setPeriodCogs(periodCogs);
        dashboard.setPeriodGrossProfit(grossProfit);
        dashboard.setMonthlyExpenses(periodExp);
        dashboard.setPrevPeriodExpenses(prevExp);
        dashboard.setPeriodExpenseCount(periodExpenseList.size());
        dashboard.setMonthlyNetProfit(netProfit);

        if (periodRev.compareTo(BigDecimal.ZERO) > 0) {
            dashboard.setPeriodGrossMargin(grossProfit.multiply(BigDecimal.valueOf(100))
                    .divide(periodRev, 2, RoundingMode.HALF_UP));
            dashboard.setPeriodNetMargin(netProfit.multiply(BigDecimal.valueOf(100))
                    .divide(periodRev, 2, RoundingMode.HALF_UP));
            dashboard.setExpenseRatioPct(periodExp.multiply(BigDecimal.valueOf(100))
                    .divide(periodRev, 2, RoundingMode.HALF_UP));
        } else {
            dashboard.setPeriodGrossMargin(BigDecimal.ZERO);
            dashboard.setPeriodNetMargin(BigDecimal.ZERO);
            dashboard.setExpenseRatioPct(BigDecimal.ZERO);
        }

        // 5. Purchases / Stock-In in Period
        List<Purchase> allPurchases = purchaseRepository.findAllWithDetails();
        List<Purchase> periodPurchases = allPurchases.stream()
                .filter(p -> p.getPurchaseDate() != null
                        && !p.getPurchaseDate().isBefore(periodStart)
                        && !p.getPurchaseDate().isAfter(periodEnd))
                .toList();
        BigDecimal periodPurchAmt = periodPurchases.stream()
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long periodUnitsReceived = periodPurchases.stream()
                .mapToLong(Purchase::getTotalQuantity)
                .sum();
        dashboard.setPeriodPurchasesAmount(periodPurchAmt);
        dashboard.setPeriodPurchasesCount(periodPurchases.size());
        dashboard.setPeriodUnitsReceived(periodUnitsReceived);

        // 6. Recent Activity Tables (strictly within period, or empty if none in period)
        dashboard.setRecentSales(periodSales.stream().limit(6).toList());
        dashboard.setRecentPurchases(periodPurchases.stream().limit(6).toList());
        dashboard.setRecentStockMovements(stockMovementRepository.findRecentMovements(PageRequest.of(0, 6)));

        // 7. Best-Selling & Slow-Moving Products (strictly for the selected period)
        List<Object[]> bestInPeriod = saleItemRepository.findBestSellingProductsBetween(periodStart, periodEnd, PageRequest.of(0, 5));
        List<Object[]> slowInPeriod = saleItemRepository.findSlowMovingProductsBetween(periodStart, periodEnd, PageRequest.of(0, 5));
        dashboard.setBestSellingProducts(bestInPeriod);
        dashboard.setSlowMovingProducts(slowInPeriod);

        // 8. Employee Sales Performance (strictly from period sales)
        Map<String, User> usersByUsername = userRepository.findAll().stream()
                .collect(Collectors.toMap(u -> u.getUsername().toLowerCase(), u -> u, (a, b) -> a));

        List<Sale> perfSourceSales = periodSales;
        BigDecimal totalPerfRev = perfSourceSales.stream()
                .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<Sale>> salesByCreator = perfSourceSales.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedBy() != null ? s.getCreatedBy().toLowerCase() : "salesman01"));

        List<DashboardDTO.EmployeePerformanceItem> empPerfList = new ArrayList<>();
        for (Map.Entry<String, List<Sale>> entry : salesByCreator.entrySet()) {
            String uname = entry.getKey();
            List<Sale> empSales = entry.getValue();
            User u = usersByUsername.get(uname);
            String empName = u != null ? u.getFullName() : (empSales.get(0).getCreatedByName() != null ? empSales.get(0).getCreatedByName() : uname);
            String roleName = u != null && u.getRole() != null ? u.getRole().getDisplayName() : "Sales Staff";
            long invCount = empSales.size();
            long uSold = empSales.stream().mapToLong(Sale::getTotalItemsQuantity).sum();
            BigDecimal empRev = empSales.stream()
                    .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int sharePct = totalPerfRev.compareTo(BigDecimal.ZERO) > 0
                    ? empRev.multiply(BigDecimal.valueOf(100)).divide(totalPerfRev, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            empPerfList.add(new DashboardDTO.EmployeePerformanceItem(uname, empName, roleName, invCount, uSold, empRev, sharePct));
        }
        empPerfList.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        dashboard.setEmployeePerformance(empPerfList);

        // 9. Category Stock Distribution (for Inventory Analytics)
        long totalUnitsAll = Math.max(1, dashboard.getTotalStockQuantity());
        Map<String, List<Product>> byCategory = activeProducts.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory() != null ? p.getCategory().getDisplayName() : "Other"));
        List<DashboardDTO.CategoryStockItem> catStockList = new ArrayList<>();
        for (Map.Entry<String, List<Product>> entry : byCategory.entrySet()) {
            long count = entry.getValue().size();
            long units = entry.getValue().stream().mapToLong(Product::getQuantity).sum();
            int share = (int) Math.round((units * 100.0) / totalUnitsAll);
            catStockList.add(new DashboardDTO.CategoryStockItem(entry.getKey(), count, units, share));
        }
        catStockList.sort((a, b) -> Long.compare(b.getTotalUnits(), a.getTotalUnits()));
        dashboard.setCategoryStockBreakdown(catStockList);

        // 10. Vendor Procurement Breakdown (strictly for the selected period)
        List<Purchase> vendorSourcePurchases = periodPurchases;
        BigDecimal totalVendorSpend = vendorSourcePurchases.stream()
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, List<Purchase>> byVendor = vendorSourcePurchases.stream()
                .filter(p -> p.getSupplier() != null)
                .collect(Collectors.groupingBy(p -> p.getSupplier().getName()));
        List<DashboardDTO.VendorProcurementItem> vendorList = new ArrayList<>();
        for (Map.Entry<String, List<Purchase>> entry : byVendor.entrySet()) {
            long orders = entry.getValue().size();
            long units = entry.getValue().stream().mapToLong(Purchase::getTotalQuantity).sum();
            BigDecimal spend = entry.getValue().stream()
                    .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int share = totalVendorSpend.compareTo(BigDecimal.ZERO) > 0
                    ? spend.multiply(BigDecimal.valueOf(100)).divide(totalVendorSpend, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            vendorList.add(new DashboardDTO.VendorProcurementItem(entry.getKey(), orders, units, spend, share));
        }
        vendorList.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        dashboard.setVendorProcurementBreakdown(vendorList);

        // 11. Interactive Chart Series: Daily, Weekly, and Monthly Trends
        populateChartSeries(dashboard, periodStart, periodEnd, today, scopedSales, allExpenses);

        return dashboard;
    }

    private void populateChartSeries(DashboardDTO dashboard,
                                     LocalDate periodStart,
                                     LocalDate periodEnd,
                                     LocalDate today,
                                     List<Sale> scopedSales,
                                     List<Expense> allExpenses) {
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

        // Daily series: plot strictly the dates within [periodStart, periodEnd]
        // If THIS_WEEK is selected, plot all 7 days of the week (Monday through Sunday)
        // If a single day is selected (e.g. TODAY or YESTERDAY), show 7 days up to periodEnd so the line has trajectory
        LocalDate chartStart;
        LocalDate chartEnd;
        String pKey = dashboard.getPeriodKey() != null ? dashboard.getPeriodKey().toUpperCase(Locale.ROOT) : "THIS_MONTH";
        if ("THIS_WEEK".equals(pKey)) {
            chartStart = periodStart;
            chartEnd = periodStart.plusDays(6);
        } else if ("TODAY".equals(pKey) || "YESTERDAY".equals(pKey) || periodStart.equals(periodEnd)) {
            chartStart = periodEnd.minusDays(6);
            chartEnd = periodEnd;
        } else {
            chartStart = periodStart;
            chartEnd = periodEnd;
            long span = ChronoUnit.DAYS.between(chartStart, chartEnd) + 1;
            if (span > 31) {
                chartStart = chartEnd.minusDays(30);
            }
        }

        List<String> dailyLabels = new ArrayList<>();
        List<BigDecimal> dailyRev = new ArrayList<>();
        List<BigDecimal> dailyExp = new ArrayList<>();
        List<Long> dailyInv = new ArrayList<>();

        for (LocalDate d = chartStart; !d.isAfter(chartEnd); d = d.plusDays(1)) {
            final LocalDate curr = d;
            dailyLabels.add(curr.format(dayFmt));
            BigDecimal r = scopedSales.stream()
                    .filter(s -> curr.equals(s.getSaleDate()))
                    .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long invCnt = scopedSales.stream().filter(s -> curr.equals(s.getSaleDate())).count();
            BigDecimal ex = allExpenses.stream()
                    .filter(e -> curr.equals(e.getExpenseDate()))
                    .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dailyRev.add(r);
            dailyExp.add(ex);
            dailyInv.add(invCnt);
        }
        dashboard.setDailyLabels(dailyLabels);
        dashboard.setDailyRevenueSeries(dailyRev);
        dashboard.setDailyExpenseSeries(dailyExp);
        dashboard.setDailyInvoiceSeries(dailyInv);

        // Weekly series: 6 weeks leading up to periodEnd
        List<String> weeklyLabels = new ArrayList<>();
        List<BigDecimal> weeklyRev = new ArrayList<>();
        List<BigDecimal> weeklyExp = new ArrayList<>();
        LocalDate endWeekMonday = periodEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 5; i >= 0; i--) {
            LocalDate wkStart = endWeekMonday.minusWeeks(i);
            LocalDate wkEnd = wkStart.plusDays(6);
            weeklyLabels.add(wkStart.format(dayFmt) + " - " + wkEnd.format(dayFmt));
            BigDecimal r = scopedSales.stream()
                    .filter(s -> s.getSaleDate() != null && !s.getSaleDate().isBefore(wkStart) && !s.getSaleDate().isAfter(wkEnd))
                    .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ex = allExpenses.stream()
                    .filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isBefore(wkStart) && !e.getExpenseDate().isAfter(wkEnd))
                    .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            weeklyRev.add(r);
            weeklyExp.add(ex);
        }
        dashboard.setWeeklyLabels(weeklyLabels);
        dashboard.setWeeklyRevenueSeries(weeklyRev);
        dashboard.setWeeklyExpenseSeries(weeklyExp);

        // Monthly series: 6 calendar months leading up to YearMonth.from(periodEnd)
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        List<String> monthlyLabels = new ArrayList<>();
        List<BigDecimal> monthlyRev = new ArrayList<>();
        List<BigDecimal> monthlyExp = new ArrayList<>();
        YearMonth targetYm = YearMonth.from(periodEnd);
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = targetYm.minusMonths(i);
            LocalDate mStart = ym.atDay(1);
            LocalDate mEnd = ym.atEndOfMonth();
            monthlyLabels.add(ym.format(monthFmt));
            BigDecimal r = scopedSales.stream()
                    .filter(s -> s.getSaleDate() != null && !s.getSaleDate().isBefore(mStart) && !s.getSaleDate().isAfter(mEnd))
                    .map(s -> s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ex = allExpenses.stream()
                    .filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isBefore(mStart) && !e.getExpenseDate().isAfter(mEnd))
                    .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlyRev.add(r);
            monthlyExp.add(ex);
        }
        dashboard.setMonthlyLabels(monthlyLabels);
        dashboard.setMonthlyRevenueSeries(monthlyRev);
        dashboard.setMonthlyExpenseSeries(monthlyExp);
    }

    private BigDecimal computeGrowthPercent(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    /**
     * Generates a comprehensive financial & performance report for a custom date range.
     */
    public ReportDTO generateReport(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        ReportDTO report = new ReportDTO();
        report.setStartDate(start);
        report.setEndDate(end);
        report.setMonth(start.getMonthValue());
        report.setYear(start.getYear());
        report.setPeriodLabel(formatPeriodLabel(start, end));

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

        // 6. Total Purchases (Stock In procurement in period)
        BigDecimal totalPurchases = purchaseRepository.sumTotalAmountBetween(start, end);
        report.setTotalPurchases(totalPurchases != null ? totalPurchases : BigDecimal.ZERO);

        // 7. Margins (%)
        if (report.getRevenue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal grossMargin = grossProfit.multiply(BigDecimal.valueOf(100))
                    .divide(report.getRevenue(), 2, RoundingMode.HALF_UP);
            BigDecimal profitMargin = netProfit.multiply(BigDecimal.valueOf(100))
                    .divide(report.getRevenue(), 2, RoundingMode.HALF_UP);
            report.setGrossMargin(grossMargin);
            report.setProfitMargin(profitMargin);
        } else {
            report.setGrossMargin(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            report.setProfitMargin(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        // 8. Counts
        report.setSalesCount(saleRepository.countBySaleDateBetween(start, end));
        report.setPurchasesCount(purchaseRepository.countByPurchaseDateBetween(start, end));
        report.setTotalUnitsSold(saleItemRepository.sumQuantitySoldBetween(start, end));

        // 9. Date-Filtered Breakdowns
        report.setExpenseBreakdown(expenseRepository.sumAmountByCategoryBetween(start, end));
        report.setBestSellingProducts(saleItemRepository.findBestSellingProductsBetween(start, end, PageRequest.of(0, 5)));
        report.setSlowMovingProducts(saleItemRepository.findSlowMovingProductsBetween(start, end, PageRequest.of(0, 5)));

        // 10. Generate transparent rule-based recommendations (professional icon-based, zero emojis)
        List<ReportDTO.InsightItem> insights = generateInsights(report);
        report.setInsights(insights);
        List<String> suggestions = new ArrayList<>();
        for (ReportDTO.InsightItem item : insights) {
            suggestions.add(item.getTitle() + ": " + item.getDescription());
        }
        report.setBusinessSuggestions(suggestions);

        return report;
    }

    public String formatPeriodLabel(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        YearMonth startYm = YearMonth.from(start);
        YearMonth endYm = YearMonth.from(end);
        if (startYm.equals(endYm) && start.getDayOfMonth() == 1 && end.equals(startYm.atEndOfMonth())) {
            return start.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        }
        if (start.equals(end)) {
            return start.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        }
        if (start.getYear() == end.getYear() && start.getMonth() == end.getMonth()) {
            return start.format(DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH))
                    + " to "
                    + end.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        }
        return start.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))
                + " to "
                + end.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
    }

    private List<ReportDTO.InsightItem> generateInsights(ReportDTO report) {
        List<ReportDTO.InsightItem> insights = new ArrayList<>();

        BigDecimal rev = report.getRevenue();
        BigDecimal gross = report.getGrossProfit();
        BigDecimal net = report.getNetProfit();
        BigDecimal exp = report.getTotalExpenses();

        if (net.compareTo(BigDecimal.ZERO) < 0) {
            insights.add(new ReportDTO.InsightItem(
                    "bi-exclamation-triangle",
                    "bg-danger bg-opacity-10 text-danger border-danger-subtle",
                    "Net Loss Alert",
                    "Operating expenses exceed gross profits by " + CurrencyUtil.formatInr(net.abs())
                            + " during this period. Evaluate lease, utility, and operational overheads."
            ));
        } else if (rev.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal netMargin = report.getProfitMargin();
            if (netMargin.compareTo(BigDecimal.valueOf(10)) < 0) {
                insights.add(new ReportDTO.InsightItem(
                        "bi-graph-up-arrow",
                        "bg-primary bg-opacity-10 text-primary border-primary-subtle",
                        "Margin Optimization Opportunity (" + netMargin + "%)",
                        "Net profit margin is at " + netMargin + "%. Consider cross-selling high-margin mobile accessories (cases, screen guards, adapters) alongside smartphone sales."
                ));
            } else {
                insights.add(new ReportDTO.InsightItem(
                        "bi-check2-circle",
                        "bg-success bg-opacity-10 text-success border-success-subtle",
                        "Healthy Net Profit Margin (" + netMargin + "%)",
                        "Showroom operations achieved a strong net margin of " + netMargin + "% with "
                                + CurrencyUtil.formatInr(net) + " net profit. Maintain current inventory turnover and cost discipline."
                ));
            }
        } else {
            insights.add(new ReportDTO.InsightItem(
                    "bi-info-circle",
                    "bg-secondary bg-opacity-10 text-secondary border-secondary-subtle",
                    "No Sales Recorded in Selected Period",
                    "No sales invoices were billed between " + report.getStartDate() + " and " + report.getEndDate()
                            + ". Select an active date range or record new POS transactions."
            ));
        }

        if (rev.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal grossMargin = report.getGrossMargin();
            if (grossMargin.compareTo(BigDecimal.valueOf(15)) < 0) {
                insights.add(new ReportDTO.InsightItem(
                        "bi-sliders",
                        "bg-warning bg-opacity-10 text-dark border-warning-subtle",
                        "Gross Margin Review (" + grossMargin + "%)",
                        "Trading gross margin is " + grossMargin + "%. Review vendor procurement pricing or negotiate volume rebates on flagship handsets."
                ));
            }
        }

        if (rev.compareTo(BigDecimal.ZERO) > 0 && exp.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal expenseRatio = exp.multiply(BigDecimal.valueOf(100)).divide(rev, 2, RoundingMode.HALF_UP);
            if (expenseRatio.compareTo(BigDecimal.valueOf(30)) > 0) {
                insights.add(new ReportDTO.InsightItem(
                        "bi-pie-chart",
                        "bg-danger bg-opacity-10 text-danger border-danger-subtle",
                        "Elevated Expense Ratio (" + expenseRatio + "%)",
                        "Operating expenses (" + CurrencyUtil.formatInr(exp) + ") represent " + expenseRatio
                                + "% of period revenue. Audit recurring overheads for cost savings."
                ));
            }
        }

        if (report.getSlowMovingProducts() != null && !report.getSlowMovingProducts().isEmpty()) {
            insights.add(new ReportDTO.InsightItem(
                    "bi-tags",
                    "bg-info bg-opacity-10 text-info-emphasis border-info-subtle",
                    "Inventory Clearance Strategy",
                    "Identify lower-velocity SKUs from the period table below and bundle them as promotional accessory packages to accelerate working capital recovery."
            ));
        }

        long lowStockCount = productRepository.findLowStockProducts().size();
        if (lowStockCount > 0) {
            insights.add(new ReportDTO.InsightItem(
                    "bi-box-seam",
                    "bg-warning bg-opacity-10 text-dark border-warning-subtle",
                    "Inventory Restock Advisory",
                    lowStockCount + " catalog items are currently at or below minimum stock thresholds. Place vendor purchase orders to prevent showroom stockouts."
            ));
        }

        return insights;
    }
}
