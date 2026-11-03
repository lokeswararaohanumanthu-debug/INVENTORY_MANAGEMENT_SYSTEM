package com.showroom.inventory.controller;

import com.showroom.inventory.dto.PurchaseDTO;
import com.showroom.inventory.dto.PurchaseItemDTO;
import com.showroom.inventory.entity.Purchase;
import com.showroom.inventory.service.ProductService;
import com.showroom.inventory.service.PurchaseService;
import com.showroom.inventory.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
    public String listPurchases(Model model) {
        List<Purchase> purchases = purchaseService.getAllPurchases();
        model.addAttribute("purchases", purchases);
        model.addAttribute("activeNav", "purchases");
        return "purchases/list";
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
