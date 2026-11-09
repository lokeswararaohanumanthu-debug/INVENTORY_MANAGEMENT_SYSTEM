package com.showroom.inventory.controller;

import com.showroom.inventory.dto.PurchaseDTO;
import com.showroom.inventory.dto.PurchaseItemDTO;
import com.showroom.inventory.entity.Purchase;
import com.showroom.inventory.entity.PurchaseItem;
import com.showroom.inventory.entity.Supplier;
import com.showroom.inventory.service.ProductService;
import com.showroom.inventory.service.PurchaseService;
import com.showroom.inventory.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller handling Supplier Purchase Orders, Restocking, and Purchase History.
 */
@Controller
@RequestMapping("/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final SupplierService supplierService;
    private final ProductService productService;

    public PurchaseController(PurchaseService purchaseService,
                              SupplierService supplierService,
                              ProductService productService) {
        this.purchaseService = purchaseService;
        this.supplierService = supplierService;
        this.productService = productService;
    }

    @GetMapping
    public String listPurchases(@RequestParam(value = "supplierId", required = false) Long supplierId,
                                @RequestParam(value = "search", required = false) String search,
                                Model model) {
        List<Purchase> allPurchases = purchaseService.getAllPurchases();
        List<Supplier> suppliers = supplierService.getAllActiveSuppliers();

        // Build per-vendor summary stats across all purchases
        Map<Long, VendorPurchaseSummary> vendorStatsMap = new LinkedHashMap<>();
        for (Supplier s : suppliers) {
            vendorStatsMap.put(s.getId(), new VendorPurchaseSummary(s));
        }
        for (Purchase p : allPurchases) {
            if (p.getSupplier() != null) {
                VendorPurchaseSummary summary = vendorStatsMap.computeIfAbsent(
                        p.getSupplier().getId(),
                        k -> new VendorPurchaseSummary(p.getSupplier())
                );
                summary.addPurchase(p);
            }
        }

        // Filter purchases by supplierId and search query
        List<Purchase> filtered = new ArrayList<>();
        String q = (search != null && !search.isBlank()) ? search.trim().toLowerCase(Locale.ROOT) : null;
        for (Purchase p : allPurchases) {
            if (supplierId != null && (p.getSupplier() == null || !supplierId.equals(p.getSupplier().getId()))) {
                continue;
            }
            if (q != null) {
                boolean matchesPo = p.getPurchaseNumber() != null && p.getPurchaseNumber().toLowerCase(Locale.ROOT).contains(q);
                boolean matchesVendor = p.getSupplier() != null && p.getSupplier().getName() != null
                        && p.getSupplier().getName().toLowerCase(Locale.ROOT).contains(q);
                boolean matchesNotes = p.getNotes() != null && p.getNotes().toLowerCase(Locale.ROOT).contains(q);
                boolean matchesProduct = false;
                if (p.getItems() != null) {
                    for (PurchaseItem item : p.getItems()) {
                        if (item.getProduct() != null && item.getProduct().getName() != null
                                && item.getProduct().getName().toLowerCase(Locale.ROOT).contains(q)) {
                            matchesProduct = true;
                            break;
                        }
                    }
                }
                if (!matchesPo && !matchesVendor && !matchesNotes && !matchesProduct) {
                    continue;
                }
            }
            filtered.add(p);
        }

        // Compute KPIs for displayed purchases
        long totalUnits = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Purchase p : filtered) {
            totalUnits += p.getTotalQuantity();
            if (p.getTotalAmount() != null) {
                totalAmount = totalAmount.add(p.getTotalAmount());
            }
        }

        model.addAttribute("purchases", filtered);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("vendorSummaries", new ArrayList<>(vendorStatsMap.values()));
        model.addAttribute("selectedSupplierId", supplierId);
        model.addAttribute("searchQuery", search != null ? search.trim() : "");
        model.addAttribute("totalOrdersCount", filtered.size());
        model.addAttribute("totalUnitsReceived", totalUnits);
        model.addAttribute("totalProcurementAmount", totalAmount);
        model.addAttribute("activeNav", "purchases");
        return "purchases/list";
    }

    public static class VendorPurchaseSummary {
        private final Supplier supplier;
        private int orderCount;
        private int totalUnits;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        public VendorPurchaseSummary(Supplier supplier) {
            this.supplier = supplier;
        }

        public void addPurchase(Purchase p) {
            this.orderCount++;
            this.totalUnits += p.getTotalQuantity();
            if (p.getTotalAmount() != null) {
                this.totalAmount = this.totalAmount.add(p.getTotalAmount());
            }
        }

        public Supplier getSupplier() {
            return supplier;
        }

        public int getOrderCount() {
            return orderCount;
        }

        public int getTotalUnits() {
            return totalUnits;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

    @GetMapping("/new")
    public String newPurchaseForm(Model model) {
        PurchaseDTO purchaseDto = new PurchaseDTO();
        // Start with one empty item row
        purchaseDto.getItems().add(new PurchaseItemDTO());

        model.addAttribute("purchase", purchaseDto);
        model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("activeNav", "purchases");
        return "purchases/form";
    }

    @PostMapping("/save")
    public String savePurchase(@Valid @ModelAttribute("purchase") PurchaseDTO purchaseDto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        // Filter out empty rows
        purchaseDto.getItems().removeIf(item -> item.getProductId() == null || item.getQuantity() <= 0);

        if (purchaseDto.getItems().isEmpty()) {
            bindingResult.rejectValue("items", "empty", "At least one valid item is required in the purchase.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
            model.addAttribute("products", productService.getAllActiveProducts());
            model.addAttribute("activeNav", "purchases");
            return "purchases/form";
        }

        Purchase created = purchaseService.createPurchase(purchaseDto);
        redirectAttributes.addFlashAttribute("successMessage",
                "Purchase order " + created.getPurchaseNumber() + " created and stock successfully updated!");
        return "redirect:/purchases/view/" + created.getId();
    }

    @GetMapping("/view/{id}")
    public String viewPurchase(@PathVariable Long id, Model model) {
        Purchase purchase = purchaseService.getPurchaseById(id);
        model.addAttribute("purchase", purchase);
        model.addAttribute("activeNav", "purchases");
        return "purchases/view";
    }
}
