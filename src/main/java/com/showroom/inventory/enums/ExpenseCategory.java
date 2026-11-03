package com.showroom.inventory.enums;

/**
 * ExpenseCategory Enum - Categorizes business expenses in the mobile showroom.
 *
 * This enum defines the types of expenses that the showroom may incur.
 * Tracking expenses by category helps in generating financial reports,
 * analyzing spending patterns, and making informed business decisions.
 *
 * Each category has a human-readable display name for the user interface.
 *
 * Usage in Entity:
 *   @Enumerated(EnumType.STRING)
 *   private ExpenseCategory expenseCategory;
 */
public enum ExpenseCategory {

    // Business expense categories with their display names
    RENT("Rent"),
    ELECTRICITY("Electricity"),
    SALARY("Salary"),
    MARKETING("Marketing"),
    TRANSPORT("Transport"),
    MAINTENANCE("Maintenance"),
    OFFICE_SUPPLIES("Office Supplies"),
    OTHER("Other");

    /**
     * The human-readable name displayed in the user interface.
     * For example, OFFICE_SUPPLIES -> "Office Supplies"
     */
    private final String displayName;

    /**
     * Constructor for the ExpenseCategory enum.
     * Each enum constant must provide a display name when declared.
     *
     * @param displayName the user-friendly name for this expense category
     */
    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this expense category.
     * This is used in Thymeleaf templates, forms, and reports.
     *
     * @return the display name string
     */
    public String getDisplayName() {
        return displayName;
    }
}
