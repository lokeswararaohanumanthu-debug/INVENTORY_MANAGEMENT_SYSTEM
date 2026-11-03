package com.showroom.inventory.enums;

/**
 * StockMovementType Enum - Defines the types of stock/inventory movements.
 *
 * This enum tracks how inventory quantities change over time:
 *   - PURCHASE_IN:  Stock increases when products are purchased from suppliers.
 *   - SALE_OUT:     Stock decreases when products are sold to customers.
 *   - ADJUSTMENT:   Manual corrections to stock levels (e.g., damaged goods,
 *                   stock count corrections, returns).
 *
 * Each movement type has a human-readable display name for the user interface.
 *
 * Usage in Entity:
 *   @Enumerated(EnumType.STRING)
 *   private StockMovementType movementType;
 */
public enum StockMovementType {

    // Stock movement types with their display names
    PURCHASE_IN("Purchase In"),
    SALE_OUT("Sale Out"),
    ADJUSTMENT("Adjustment");

    /**
     * The human-readable name displayed in the user interface.
     * For example, PURCHASE_IN -> "Purchase In"
     */
    private final String displayName;

    /**
     * Constructor for the StockMovementType enum.
     * Each enum constant must provide a display name when declared.
     *
     * @param displayName the user-friendly name for this movement type
     */
    StockMovementType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this stock movement type.
     * This is used in Thymeleaf templates and stock movement logs.
     *
     * @return the display name string
     */
    public String getDisplayName() {
        return displayName;
    }
}
