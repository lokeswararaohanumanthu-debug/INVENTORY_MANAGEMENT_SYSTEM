package com.showroom.inventory.controller;

import com.showroom.inventory.dto.PosProductDTO;
import com.showroom.inventory.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * JSON endpoints used by the Showroom POS &amp; Billing Counter to identify products
 * from the existing inventory via barcode (camera scan / manual entry) or keyword search.
 * Read-only: never creates or modifies products.
 */
@RestController
@RequestMapping("/sales/api/products")
public class PosApiController {

    private static final int MAX_SEARCH_RESULTS = 15;

    private final ProductService productService;

    public PosApiController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Identify a product by its barcode (falls back to product code / SKU).
     * Returns 404 with a readable message when no active product matches.
     */
    @GetMapping("/barcode")
    public ResponseEntity<?> findByBarcode(@RequestParam("code") String code) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please enter a barcode number."));
        }
        return productService.findActiveProductByBarcode(code)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(PosProductDTO.from(p)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "message", "No product found for barcode \"" + code.trim() + "\".")));
    }

    /**
     * Search active products by name, brand, model, product code or barcode.
     */
    @GetMapping("/search")
    public List<PosProductDTO> search(@RequestParam(value = "q", required = false) String q) {
        return productService.searchProducts(q).stream()
                .limit(MAX_SEARCH_RESULTS)
                .map(PosProductDTO::from)
                .toList();
    }
}
