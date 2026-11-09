package com.showroom.inventory.controller;

import com.showroom.inventory.dto.SaleDTO;
import com.showroom.inventory.dto.SaleItemDTO;
import com.showroom.inventory.entity.Sale;
import com.showroom.inventory.entity.SaleItem;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.exception.InsufficientStockException;
import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.ProductService;
import com.showroom.inventory.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller handling Customer Billing Counter, Invoice Generation, and Date-Wise Sales History.
 */
@Controller
@RequestMapping("/sales")
public class SaleController {

    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final SaleService saleService;
    private final ProductService productService;
    private final ActivityLogService activityLogService;

    public SaleController(SaleService saleService,
                          ProductService productService,
                          ActivityLogService activityLogService) {
        this.saleService = saleService;
        this.productService = productService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public String listSales(@RequestParam(value = "period", required = false, defaultValue = "ALL") String period,
                            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                            @RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                            Model model) {

        LocalDate today = LocalDate.now(SaleService.IST_ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBeforeYesterday = today.minusDays(2);
        LocalDate last7DaysStart = today.minusDays(6);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        YearMonth lastMonthYm = currentMonth.minusMonths(1);
        LocalDate lastMonthStart = lastMonthYm.atDay(1);
        LocalDate lastMonthEnd = lastMonthYm.atEndOfMonth();

        String normalizedPeriod = period != null ? period.trim().toUpperCase(Locale.ROOT) : "ALL";
        // If startDate or endDate was explicitly passed without period, treat as CUSTOM
        if ("ALL".equals(normalizedPeriod) && (startDate != null || endDate != null)) {
            normalizedPeriod = "CUSTOM";
        }

        LocalDate filterStart = null;
        LocalDate filterEnd = null;
        String periodTitle;
        String periodSubtitle;

        switch (normalizedPeriod) {
            case "TODAY":
                filterStart = today;
                filterEnd = today;
                periodTitle = "Today's Sales";
                periodSubtitle = today.format(SHORT_DATE_FMT);
                break;
            case "YESTERDAY":
                filterStart = yesterday;
                filterEnd = yesterday;
                periodTitle = "Yesterday's Sales";
                periodSubtitle = yesterday.format(SHORT_DATE_FMT);
                break;
            case "DAY_BEFORE_YESTERDAY":
                filterStart = dayBeforeYesterday;
                filterEnd = dayBeforeYesterday;
                periodTitle = "Day Before Yesterday";
                periodSubtitle = dayBeforeYesterday.format(SHORT_DATE_FMT);
                break;
            case "LAST_7_DAYS":
                filterStart = last7DaysStart;
                filterEnd = today;
                periodTitle = "Last 7 Days Sales";
                periodSubtitle = last7DaysStart.format(SHORT_DATE_FMT) + " – " + today.format(SHORT_DATE_FMT);
                break;
            case "THIS_WEEK":
                filterStart = weekStart;
                filterEnd = today;
                periodTitle = "This Week's Sales";
                periodSubtitle = weekStart.format(SHORT_DATE_FMT) + " – " + today.format(SHORT_DATE_FMT);
                break;
            case "THIS_MONTH":
                filterStart = monthStart;
                filterEnd = today;
                periodTitle = "This Month's Sales";
                periodSubtitle = monthStart.format(SHORT_DATE_FMT) + " – " + today.format(SHORT_DATE_FMT);
                break;
            case "LAST_MONTH":
                filterStart = lastMonthStart;
                filterEnd = lastMonthEnd;
                periodTitle = "Last Month's Sales";
                periodSubtitle = lastMonthStart.format(SHORT_DATE_FMT) + " – " + lastMonthEnd.format(SHORT_DATE_FMT);
                break;
            case "CUSTOM":
                if (startDate == null && endDate == null) {
                    filterStart = monthStart;
                    filterEnd = today;
                } else if (startDate == null) {
                    filterStart = endDate;
                    filterEnd = endDate;
                } else if (endDate == null) {
                    filterStart = startDate;
                    filterEnd = startDate;
                } else {
                    filterStart = startDate;
                    filterEnd = endDate;
                }
                if (filterStart.isAfter(filterEnd)) {
                    model.addAttribute("dateWarning", "The 'From Date' (" + filterStart + ") cannot be later than 'To Date' (" + filterEnd + "). Range was adjusted automatically.");
                    LocalDate tmp = filterStart;
                    filterStart = filterEnd;
                    filterEnd = tmp;
                }
                periodTitle = "Custom Date Range";
                if (filterStart.equals(filterEnd)) {
                    periodSubtitle = filterStart.format(SHORT_DATE_FMT);
                } else {
                    periodSubtitle = filterStart.format(SHORT_DATE_FMT) + " – " + filterEnd.format(SHORT_DATE_FMT);
                }
                break;
            case "ALL":
            default:
                normalizedPeriod = "ALL";
                periodTitle = "All Sales History";
                periodSubtitle = "Complete historical sales ledger";
                break;
        }

        // Fetch date-filtered sales from repository
        List<Sale> rawSales;
        if (filterStart != null && filterEnd != null) {
            rawSales = saleService.getSalesBetween(filterStart, filterEnd);
        } else {
            rawSales = saleService.getAllSales();
        }

        // Enforce record-level RBAC: Salesman without SALES_VIEW_ALL sees only their own sales
        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        boolean ownSalesOnly = (currentUser != null && !currentUser.hasPermission(Permission.SALES_VIEW_ALL));
        if (ownSalesOnly) {
            String myUsername = currentUser.getUsername();
            rawSales = rawSales.stream()
                    .filter(s -> s.getCreatedBy() != null && s.getCreatedBy().equalsIgnoreCase(myUsername))
                    .toList();
        }
        model.addAttribute("ownSalesOnly", ownSalesOnly);

        // Apply optional search & payment method filters
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : "";
        String searchLower = trimmedSearch.toLowerCase(Locale.ROOT);
        String trimmedPay = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod.trim().toUpperCase(Locale.ROOT) : "";

        List<Sale> filteredSales = new ArrayList<>();
        for (Sale s : rawSales) {
            if (!trimmedPay.isEmpty()) {
                if (s.getPaymentMethod() == null || !trimmedPay.equalsIgnoreCase(s.getPaymentMethod())) {
                    continue;
                }
            }
            if (!searchLower.isEmpty()) {
                boolean matchBill = s.getBillNumber() != null && s.getBillNumber().toLowerCase(Locale.ROOT).contains(searchLower);
                boolean matchCustomer = s.getCustomerName() != null && s.getCustomerName().toLowerCase(Locale.ROOT).contains(searchLower);
                boolean matchPhone = s.getCustomerPhone() != null && s.getCustomerPhone().toLowerCase(Locale.ROOT).contains(searchLower);
                boolean matchProduct = false;
                if (s.getItems() != null) {
                    for (SaleItem item : s.getItems()) {
                        if (item.getProduct() != null) {
                            String pName = item.getProduct().getName() != null ? item.getProduct().getName().toLowerCase(Locale.ROOT) : "";
                            String pBrand = item.getProduct().getBrand() != null ? item.getProduct().getBrand().toLowerCase(Locale.ROOT) : "";
                            String pModel = item.getProduct().getModel() != null ? item.getProduct().getModel().toLowerCase(Locale.ROOT) : "";
                            String pCode = item.getProduct().getProductCode() != null ? item.getProduct().getProductCode().toLowerCase(Locale.ROOT) : "";
                            if (pName.contains(searchLower) || pBrand.contains(searchLower) || pModel.contains(searchLower) || pCode.contains(searchLower)) {
                                matchProduct = true;
                                break;
                            }
                        }
                    }
                }
                if (!matchBill && !matchCustomer && !matchPhone && !matchProduct) {
                    continue;
                }
            }
            filteredSales.add(s);
        }

        // Compute dynamic summary metrics for the selected period & filters
        BigDecimal summaryRevenue = BigDecimal.ZERO;
        BigDecimal summarySubtotal = BigDecimal.ZERO;
        BigDecimal summaryDiscount = BigDecimal.ZERO;
        long summaryItemsSold = 0;

        for (Sale s : filteredSales) {
            if (s.getNetAmount() != null) {
                summaryRevenue = summaryRevenue.add(s.getNetAmount());
            }
            if (s.getTotalAmount() != null) {
                summarySubtotal = summarySubtotal.add(s.getTotalAmount());
            }
            if (s.getDiscount() != null) {
                summaryDiscount = summaryDiscount.add(s.getDiscount());
            }
            summaryItemsSold += s.getTotalItemsQuantity();
        }

        boolean isFutureSelection = (filterStart != null && filterStart.isAfter(today));

        model.addAttribute("sales", filteredSales);
        model.addAttribute("selectedPeriod", normalizedPeriod);
        model.addAttribute("periodTitle", periodTitle);
        model.addAttribute("periodSubtitle", periodSubtitle);
        model.addAttribute("startDate", filterStart);
        model.addAttribute("endDate", filterEnd);
        model.addAttribute("customStartDate", filterStart);
        model.addAttribute("customEndDate", filterEnd);
        model.addAttribute("searchQuery", trimmedSearch);
        model.addAttribute("selectedPaymentMethod", trimmedPay);
        model.addAttribute("isFutureSelection", isFutureSelection);

        // Dynamic Period Summary KPIs
        model.addAttribute("summaryRevenue", summaryRevenue);
        model.addAttribute("summarySubtotal", summarySubtotal);
        model.addAttribute("summaryDiscount", summaryDiscount);
        model.addAttribute("summaryInvoiceCount", filteredSales.size());
        model.addAttribute("summaryItemsSold", summaryItemsSold);

        // Reference dates for UI badges
        model.addAttribute("todayDate", today);
        model.addAttribute("yesterdayDate", yesterday);
        model.addAttribute("dayBeforeYesterdayDate", dayBeforeYesterday);
        model.addAttribute("todayTotal", saleService.getTodaySalesTotal());
        model.addAttribute("activeNav", "pos");
        model.addAttribute("activeSubNav", "history");
        return "sales/list";
    }

    @GetMapping("/history")
    public String historyRedirect() {
        return "redirect:/sales";
    }

    @GetMapping({"/billing", "/new"})
    public String billingPage(Model model) {
        SaleDTO saleDto = new SaleDTO();
        saleDto.getItems().add(new SaleItemDTO());

        model.addAttribute("sale", saleDto);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("activeNav", "pos");
        model.addAttribute("activeSubNav", "billing");
        return "sales/billing";
    }

    @PostMapping("/save")
    public String processSale(@Valid @ModelAttribute("sale") SaleDTO saleDto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Filter out empty rows
        saleDto.getItems().removeIf(item -> item.getProductId() == null || item.getQuantity() <= 0);

        if (saleDto.getItems().isEmpty()) {
            bindingResult.rejectValue("items", "empty", "At least one product must be added to the bill.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("activeNav", "billing");
            return "sales/billing";
        }

        try {
            Sale completedSale = saleService.createSale(saleDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bill " + completedSale.getBillNumber() + " generated on " + completedSale.getSaleDate() + " successfully!");
            return "redirect:/sales/view/" + completedSale.getId();
        } catch (InsufficientStockException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("activeNav", "billing");
            return "sales/billing";
        }
    }

    @GetMapping("/view/{id}")
    public String viewInvoice(@PathVariable Long id, Model model) {
        Sale sale = saleService.getSaleById(id);
        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser != null && !currentUser.hasPermission(Permission.SALES_VIEW_ALL)) {
            if (sale.getCreatedBy() == null || !sale.getCreatedBy().equalsIgnoreCase(currentUser.getUsername())) {
                throw new AccessDeniedException("Salesman accounts are restricted to viewing their own sales invoices.");
            }
        }
        model.addAttribute("sale", sale);
        model.addAttribute("activeNav", "pos");
        model.addAttribute("activeSubNav", "history");
        return "sales/view";
    }
}
