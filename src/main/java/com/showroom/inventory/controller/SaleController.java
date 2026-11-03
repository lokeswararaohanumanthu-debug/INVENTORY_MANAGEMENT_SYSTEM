package com.showroom.inventory.controller;

import com.showroom.inventory.dto.SaleDTO;
import com.showroom.inventory.dto.SaleItemDTO;
import com.showroom.inventory.entity.Sale;
import com.showroom.inventory.exception.InsufficientStockException;
import com.showroom.inventory.service.ProductService;
import com.showroom.inventory.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling Customer Billing Counter, Invoice Generation, and Sales History.
 */
@Controller
@RequestMapping("/sales")
public class SaleController {

    private final SaleService saleService;
    private final ProductService productService;

    public SaleController(SaleService saleService, ProductService productService) {
        this.saleService = saleService;
        this.productService = productService;
    }

    @GetMapping
    public String listSales(Model model) {
        List<Sale> sales = saleService.getAllSales();
        model.addAttribute("sales", sales);
        model.addAttribute("todayTotal", saleService.getTodaySalesTotal());
        model.addAttribute("activeNav", "sales");
        return "sales/list";
    }

    @GetMapping("/billing")
    public String billingPage(Model model) {
        SaleDTO saleDto = new SaleDTO();
        saleDto.getItems().add(new SaleItemDTO());

        model.addAttribute("sale", saleDto);
        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("activeNav", "billing");
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
                    "Bill " + completedSale.getBillNumber() + " generated successfully!");
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
        model.addAttribute("sale", sale);
        model.addAttribute("activeNav", "sales");
        return "sales/view";
    }
}
