package com.showroom.inventory.enums;

/**
 * Category Enum - Categorizes products available in the mobile showroom.
 *
 * This enum defines all the product categories that the showroom can manage.
 * Each category has a human-readable display name that is shown in the UI
 * (e.g., "Mobile Phone" instead of "MOBILE_PHONE").
 *
 * Usage in Entity:
 *   @Enumerated(EnumType.STRING)
 *   private Category category;
 *
 * Usage in Thymeleaf:
 *   th:each="cat : ${T(com.showroom.inventory.enums.Category).values()}"
 *   th:text="${cat.displayName}"
 */
public enum Category {

    // Each constant is defined with its user-friendly display name
    MOBILE_PHONE("Mobile Phone"),
    TABLET("Tablet"),
    SMART_WATCH("Smart Watch"),
    ACCESSORY("Accessory"),
    EARPHONE("Earphone"),
    CHARGER("Charger"),
    CASE_COVER("Case Cover"),
    SCREEN_PROTECTOR("Screen Protector"),
    POWER_BANK("Power Bank"),
    OTHER("Other");

    /**
     * The human-readable name displayed in the user interface.
     * For example, MOBILE_PHONE -> "Mobile Phone"
     */
    private final String displayName;

    /**
     * Constructor for the Category enum.
     * Each enum constant must provide a display name when declared.
     *
     * @param displayName the user-friendly name for this category
     */
    Category(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this category.
     * This is used in Thymeleaf templates and UI dropdowns.
     *
     * @return the display name string
     */
    public String getDisplayName() {
        return displayName;
    }
}
