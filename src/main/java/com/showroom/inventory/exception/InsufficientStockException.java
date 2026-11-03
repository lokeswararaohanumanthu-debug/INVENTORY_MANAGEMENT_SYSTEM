package com.showroom.inventory.exception;

/**
 * Exception thrown when a sale or stock operation requests more units
 * than are currently available in the inventory.
 *
 * Example: "Insufficient stock for iPhone 15. Available: 5, Requested: 10"
 */
public class InsufficientStockException extends RuntimeException {

    private final String productName;
    private final int available;
    private final int requested;

    /**
     * Constructs a new InsufficientStockException with a descriptive message.
     *
     * @param productName the name of the product with insufficient stock
     * @param available   the number of units currently available
     * @param requested   the number of units requested
     */
    public InsufficientStockException(String productName, int available, int requested) {
        super(String.format("Insufficient stock for %s. Available: %d, Requested: %d",
                productName, available, requested));
        this.productName = productName;
        this.available = available;
        this.requested = requested;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
