package com.showroom.inventory.controller;

import com.showroom.inventory.dto.ProductDTO;
import com.showroom.inventory.entity.Product;
import com.showroom.inventory.enums.Category;
import com.showroom.inventory.exception.DuplicateProductException;
import com.showroom.inventory.service.InventoryService;
import com.showroom.inventory.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling Product Management CRUD operations, search and filtering.
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final InventoryService inventoryService;

    public ProductController(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String listProducts(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "category", required = false) Category category,
                               Model model) {
        List<Product> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(search);
        } else if (category != null) {
            products = productService.getProductsByCategory(category);
        } else {
            products = productService.getAllActiveProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", Category.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("activeNav", "products");
        return "products/list";
    }

    @GetMapping("/new")
    public String newProductForm(Model model) {
        ProductDTO productDto = new ProductDTO();
        model.addAttribute("product", productDto);
        model.addAttribute("categories", Category.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeNav", "products");
        return "products/form";
    }

    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        ProductDTO productDto = productService.convertToDto(product);
        model.addAttribute("product", productDto);
        model.addAttribute("categories", Category.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeNav", "products");
        return "products/form";
    }

    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("product") ProductDTO productDto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", Category.values());
            model.addAttribute("isNew", productDto.getId() == null);
            model.addAttribute("activeNav", "products");
            return "products/form";
        }

        try {
            if (productDto.getId() == null) {
                productService.createProduct(productDto);
                redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
            } else {
                productService.updateProduct(productDto.getId(), productDto);
                redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
            }
        } catch (DuplicateProductException ex) {
            bindingResult.rejectValue("productCode", "duplicate", ex.getMessage());
            model.addAttribute("categories", Category.values());
            model.addAttribute("isNew", productDto.getId() == null);
            model.addAttribute("activeNav", "products");
            return "products/form";
        }

        return "redirect:/products";
    }

    @GetMapping("/view/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("movements", inventoryService.getProductMovements(id));
        model.addAttribute("activeNav", "products");
        return "products/view";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.deactivateProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Product deactivated successfully.");
        return "redirect:/products";
    }

    @PostMapping("/reactivate/{id}")
    public String reactivateProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.reactivateProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Product reactivated successfully.");
        return "redirect:/products";
    }
}
