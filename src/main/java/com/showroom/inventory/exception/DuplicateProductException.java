package com.showroom.inventory.exception;

/**
 * Exception thrown when attempting to create a product with a product code
 * that already exists in the system.
 *
 * Example: "Product with code 'MOB-001' already exists"
 */
public class DuplicateProductException extends RuntimeException {

    private final String productCode;

    /**
     * Constructs a new DuplicateProductException with a descriptive message.
     *
     * @param productCode the duplicate product code that caused the conflict
     */
    public DuplicateProductException(String productCode) {
        super(String.format("Product with code '%s' already exists", productCode));
        this.productCode = productCode;
    }

    public String getProductCode() {
        return productCode;
    }
}
