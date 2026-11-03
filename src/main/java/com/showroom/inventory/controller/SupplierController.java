package com.showroom.inventory.controller;

import com.showroom.inventory.entity.Supplier;
import com.showroom.inventory.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling Supplier management.
 */
@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String listSuppliers(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Supplier> suppliers = supplierService.searchSuppliers(search);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("activeNav", "suppliers");
        return "suppliers/list";
    }

    @GetMapping("/new")
    public String newSupplierForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("isNew", true);
        model.addAttribute("activeNav", "suppliers");
        return "suppliers/form";
    }

    @GetMapping("/edit/{id}")
    public String editSupplierForm(@PathVariable Long id, Model model) {
        Supplier supplier = supplierService.getSupplierById(id);
        model.addAttribute("supplier", supplier);
        model.addAttribute("isNew", false);
        model.addAttribute("activeNav", "suppliers");
        return "suppliers/form";
    }

    @PostMapping("/save")
    public String saveSupplier(@Valid @ModelAttribute("supplier") Supplier supplier,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", supplier.getId() == null);
            model.addAttribute("activeNav", "suppliers");
            return "suppliers/form";
        }

        supplierService.saveSupplier(supplier);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier saved successfully!");
        return "redirect:/suppliers";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateSupplier(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        supplierService.deactivateSupplier(id);
        redirectAttributes.addFlashAttribute("successMessage", "Supplier deactivated successfully.");
        return "redirect:/suppliers";
    }
}
