package com.showroom.inventory.enums;

/**
 * UserRole Enum - Defines the roles available for users in the system.
 *
 * This enum is used with Spring Security to implement role-based access control (RBAC).
 * Different roles have different levels of access:
 *
 *   - ADMIN:   Full access to all features including user management,
 *              reports, and system configuration.
 *   - MANAGER: Access to day-to-day operations like managing products,
 *              recording sales and purchases, but limited administrative access.
 *
 * Each role has a human-readable display name for the user interface.
 *
 * Usage in Entity:
 *   @Enumerated(EnumType.STRING)
 *   private UserRole role;
 *
 * Usage in Spring Security:
 *   .requestMatchers("/admin/**").hasRole("ADMIN")
 */
public enum UserRole {

    // User roles with their display names
    ADMIN("Admin"),
    MANAGER("Manager");

    /**
     * The human-readable name displayed in the user interface.
     * For example, ADMIN -> "Admin"
     */
    private final String displayName;

    /**
     * Constructor for the UserRole enum.
     * Each enum constant must provide a display name when declared.
     *
     * @param displayName the user-friendly name for this role
     */
    UserRole(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this user role.
     * This is used in Thymeleaf templates, user management forms, and reports.
     *
     * @return the display name string
     */
    public String getDisplayName() {
        return displayName;
    }
}
