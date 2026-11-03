package com.showroom.inventory.controller;

import com.showroom.inventory.entity.Product;
import com.showroom.inventory.entity.StockMovement;
import com.showroom.inventory.service.InventoryService;
import com.showroom.inventory.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling Stock and Inventory tracking, low stock alerts, and manual adjustments.
 */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    public InventoryController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String stockOverview(Model model) {
        List<Product> products = productService.getAllActiveProducts();
        model.addAttribute("products", products);
        model.addAttribute("totalProducts", productService.getTotalProductCount());
        model.addAttribute("totalStock", productService.getTotalStockQuantity());
        model.addAttribute("lowStockCount", productService.getLowStockProducts().size());
        model.addAttribute("outOfStockCount", productService.getOutOfStockProducts().size());
        model.addAttribute("activeNav", "inventory");
        return "inventory/list";
    }

    @GetMapping("/low-stock")
    public String lowStockAlerts(Model model) {
        model.addAttribute("lowStockProducts", productService.getLowStockProducts());
        model.addAttribute("outOfStockProducts", productService.getOutOfStockProducts());
        model.addAttribute("activeNav", "inventory");
        return "inventory/low-stock";
    }

    @GetMapping("/movements")
    public String stockMovements(Model model) {
        List<StockMovement> movements = inventoryService.getAllMovements();
        model.addAttribute("movements", movements);
        model.addAttribute("activeNav", "inventory");
        return "inventory/movements";
    }

    @PostMapping("/adjust")
    public String adjustStock(@RequestParam("productId") Long productId,
                              @RequestParam("newQuantity") int newQuantity,
                              @RequestParam(value = "reason", required = false) String reason,
                              RedirectAttributes redirectAttributes) {
        inventoryService.adjustStock(productId, newQuantity, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Stock level adjusted successfully!");
        return "redirect:/inventory";
    }
}
