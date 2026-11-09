package com.showroom.inventory.controller;

import com.showroom.inventory.entity.ApprovalRequest;
import com.showroom.inventory.entity.Product;
import com.showroom.inventory.entity.StockMovement;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Category;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.enums.UserRole;
import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.ApprovalService;
import com.showroom.inventory.service.InventoryService;
import com.showroom.inventory.service.ProductService;
import com.showroom.inventory.service.ReportService;
import com.showroom.inventory.dto.ReportDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller handling Stock and Inventory tracking, low stock alerts,
 * manual adjustments (with approval workflow for large adjustments), and inventory report exports.
 */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private static final int LARGE_ADJUSTMENT_THRESHOLD = 15;

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final ActivityLogService activityLogService;
    private final ApprovalService approvalService;
    private final ReportService reportService;

    public InventoryController(ProductService productService,
                               InventoryService inventoryService,
                               ActivityLogService activityLogService,
                               ApprovalService approvalService,
                               ReportService reportService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.activityLogService = activityLogService;
        this.approvalService = approvalService;
        this.reportService = reportService;
    }

    @GetMapping
    public String stockOverview(@RequestParam(value = "tab", required = false, defaultValue = "all") String tab,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "category", required = false) String category,
                                Model model) {
        String normalizedTab = (tab != null && !tab.isBlank()) ? tab.trim().toLowerCase() : "all";

        List<Product> allProducts = productService.getAllActiveProducts();
        List<Product> lowStockProducts = productService.getLowStockProducts();
        List<Product> outOfStockProducts = productService.getOutOfStockProducts();
        List<StockMovement> movements = inventoryService.getAllMovements();

        // Filter products if search or category provided
        List<Product> filteredProducts = allProducts;
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            filteredProducts = filteredProducts.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                            || (p.getProductCode() != null && p.getProductCode().toLowerCase().contains(q))
                            || (p.getBrand() != null && p.getBrand().toLowerCase().contains(q))
                            || (p.getModel() != null && p.getModel().toLowerCase().contains(q)))
                    .toList();
        }
        if (category != null && !category.isBlank()) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().name().equalsIgnoreCase(category.trim()))
                    .toList();
        }

        // Generate automated inventory intelligence diagnostics
        java.time.LocalDate today = java.time.LocalDate.now(com.showroom.inventory.service.SaleService.IST_ZONE);
        java.time.LocalDate monthStart = java.time.YearMonth.from(today).atDay(1);
        ReportDTO reportDto = reportService.generateReport(monthStart, today);

        model.addAttribute("products", filteredProducts);
        model.addAttribute("allProducts", allProducts);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);
        model.addAttribute("movements", movements);
        model.addAttribute("totalProducts", productService.getTotalProductCount());
        model.addAttribute("totalStock", productService.getTotalStockQuantity());
        model.addAttribute("lowStockCount", lowStockProducts.size());
        model.addAttribute("outOfStockCount", outOfStockProducts.size());
        model.addAttribute("movementCount", movements.size());
        model.addAttribute("intelligenceReport", reportDto);
        model.addAttribute("activeTab", normalizedTab);
        model.addAttribute("activeNav", "inventory");
        model.addAttribute("activeSubNav", normalizedTab);
        model.addAttribute("categories", Category.values());
        model.addAttribute("searchQuery", search != null ? search.trim() : "");
        model.addAttribute("selectedCategory", category != null ? category.trim() : "");

        return "inventory/list";
    }

    @GetMapping("/low-stock")
    public String lowStockRedirect() {
        return "redirect:/inventory?tab=low-stock";
    }

    @GetMapping("/movements")
    public String movementsRedirect() {
        return "redirect:/inventory?tab=movements";
    }

    @PostMapping("/adjust")
    public String adjustStock(@RequestParam("productId") Long productId,
                              @RequestParam("newQuantity") int newQuantity,
                              @RequestParam(value = "reason", required = false) String reason,
                              RedirectAttributes redirectAttributes) {
        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser == null || !currentUser.hasPermission(Permission.STOCK_ADJUST)) {
            throw new AccessDeniedException("You do not have permission to perform stock adjustments.");
        }

        Product product = productService.getProductById(productId);
        int delta = Math.abs(newQuantity - product.getQuantity());

        // Large stock adjustments by non-Super-Admin/Admin require approval
        if (delta >= LARGE_ADJUSTMENT_THRESHOLD
                && currentUser.getRole() != UserRole.SUPER_ADMIN
                && currentUser.getRole() != UserRole.ADMIN) {
            ApprovalRequest req = approvalService.createRequest(
                    currentUser,
                    "LARGE_STOCK_ADJUSTMENT",
                    "INVENTORY",
                    product.getId(),
                    product.getProductCode() + " - " + product.getName() + " (Current: " + product.getQuantity() + " -> Requested: " + newQuantity + ")",
                    "NEW_QTY:" + newQuantity,
                    reason != null && !reason.isBlank() ? reason : "Large physical inventory count adjustment (" + delta + " units delta)"
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Large stock adjustment (" + delta + " units) requires management approval. Request " + req.getRequestCode() + " has been submitted.");
            return "redirect:/inventory";
        }

        inventoryService.adjustStock(productId, newQuantity, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Stock level adjusted successfully!");
        return "redirect:/inventory";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInventoryReport() {
        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser == null || !currentUser.hasPermission(Permission.INVENTORY_REPORT_VIEW)) {
            throw new AccessDeniedException("You do not have permission to export inventory reports.");
        }

        List<Product> products = productService.getAllProducts();
        StringBuilder csv = new StringBuilder();
        csv.append("Product Code,Product Name,Brand,Model,Category,Current Stock,Min Alert Level,Stock Status,Catalog Status\n");
        for (Product p : products) {
            String stockHealth = p.isOutOfStock() ? "OUT_OF_STOCK" : (p.isLowStock() ? "LOW_STOCK" : "AVAILABLE");
            csv.append(escapeCsv(p.getProductCode())).append(",")
               .append(escapeCsv(p.getName())).append(",")
               .append(escapeCsv(p.getBrand())).append(",")
               .append(escapeCsv(p.getModel())).append(",")
               .append(escapeCsv(p.getCategory() != null ? p.getCategory().getDisplayName() : "")).append(",")
               .append(p.getQuantity()).append(",")
               .append(p.getMinStockLevel()).append(",")
               .append(stockHealth).append(",")
               .append(p.isActive() ? "ACTIVE" : "INACTIVE").append("\n");
        }

        activityLogService.logCurrentUserAction(
                "EXPORT_INVENTORY_REPORT",
                "INVENTORY",
                "Exported showroom inventory stock report CSV (" + products.size() + " SKUs)",
                null,
                "Format: CSV"
        );

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventory-stock-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
