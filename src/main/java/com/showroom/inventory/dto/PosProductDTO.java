package com.showroom.inventory.dto;

import com.showroom.inventory.entity.Product;

import java.math.BigDecimal;

/**
 * Lightweight, read-only product view returned to the POS billing counter
 * (barcode scan, manual barcode entry and product search).
 * Avoids serialising JPA relationships of the Product entity.
 */
public record PosProductDTO(
        Long id,
        String productCode,
        String barcode,
        String name,
        String brand,
        String model,
        String category,
        BigDecimal sellingPrice,
        int quantity
) {
    public static PosProductDTO from(Product p) {
        return new PosProductDTO(
                p.getId(),
                p.getProductCode(),
                p.getBarcode(),
                p.getName(),
                p.getBrand(),
                p.getModel(),
                p.getCategory() != null ? p.getCategory().getDisplayName() : null,
                p.getSellingPrice(),
                p.getQuantity()
        );
    }
}
