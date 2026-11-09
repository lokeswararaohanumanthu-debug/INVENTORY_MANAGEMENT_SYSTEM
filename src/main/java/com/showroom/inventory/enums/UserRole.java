package com.showroom.inventory.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Defines the seven predefined roles for the Showroom Role-Based Access Control (RBAC) system.
 * Each role carries a display name, hierarchy level, badge styling, and default granular permissions.
 */
public enum UserRole {

    SUPER_ADMIN(
            "Super Admin",
            100,
            "role-badge-superadmin",
            "Highest-level authority with unrestricted access across all modules, users, permissions, approvals, and settings.",
            EnumSet.allOf(Permission.class)
    ),

    ADMIN(
            "Admin",
            80,
            "role-badge-admin",
            "Manages product catalogue, inventory, suppliers, purchases, sales, expenses, reports, and regular employee accounts.",
            EnumSet.of(
                    Permission.DASHBOARD_VIEW,
                    Permission.DASHBOARD_FULL,
                    Permission.DASHBOARD_FINANCIAL,
                    Permission.SALES_CREATE,
                    Permission.SALES_VIEW_ALL,
                    Permission.SALES_VIEW_OWN,
                    Permission.PRODUCT_VIEW,
                    Permission.PRODUCT_VIEW_COST,
                    Permission.PRODUCT_ADD,
                    Permission.PRODUCT_EDIT,
                    Permission.PRODUCT_DEACTIVATE,
                    Permission.PRODUCT_DELETE,
                    Permission.STOCK_VIEW,
                    Permission.STOCK_ADJUST,
                    Permission.STOCK_APPROVE,
                    Permission.INVENTORY_REPORT_VIEW,
                    Permission.PURCHASE_VIEW,
                    Permission.PURCHASE_CREATE,
                    Permission.PURCHASE_APPROVE,
                    Permission.SUPPLIER_VIEW,
                    Permission.SUPPLIER_MANAGE,
                    Permission.EXPENSE_VIEW,
                    Permission.EXPENSE_MANAGE,
                    Permission.REPORT_PNL_VIEW,
                    Permission.REPORT_FINANCIAL_VIEW,
                    Permission.REPORT_EXPORT,
                    Permission.USER_VIEW,
                    Permission.USER_MANAGE,
                    Permission.SETTINGS_VIEW,
                    Permission.ACTIVITY_LOG_VIEW_ALL,
                    Permission.ACTIVITY_LOG_VIEW_OWN,
                    Permission.APPROVAL_VIEW,
                    Permission.APPROVAL_REQUEST,
                    Permission.APPROVAL_DECIDE
            )
    ),

    SHOWROOM_MANAGER(
            "Showroom Manager",
            60,
            "role-badge-manager",
            "Monitors daily/monthly sales, stock levels, employee operations, Profit & Loss reports, and approves purchase/stock requests.",
            EnumSet.of(
                    Permission.DASHBOARD_VIEW,
                    Permission.DASHBOARD_FULL,
                    Permission.DASHBOARD_FINANCIAL,
                    Permission.SALES_CREATE,
                    Permission.SALES_VIEW_ALL,
                    Permission.SALES_VIEW_OWN,
                    Permission.PRODUCT_VIEW,
                    Permission.PRODUCT_VIEW_COST,
                    Permission.STOCK_VIEW,
                    Permission.STOCK_APPROVE,
                    Permission.INVENTORY_REPORT_VIEW,
                    Permission.PURCHASE_VIEW,
                    Permission.PURCHASE_APPROVE,
                    Permission.SUPPLIER_VIEW,
                    Permission.EXPENSE_VIEW,
                    Permission.REPORT_PNL_VIEW,
                    Permission.REPORT_FINANCIAL_VIEW,
                    Permission.REPORT_EXPORT,
                    Permission.ACTIVITY_LOG_VIEW_ALL,
                    Permission.ACTIVITY_LOG_VIEW_OWN,
                    Permission.APPROVAL_VIEW,
                    Permission.APPROVAL_REQUEST,
                    Permission.APPROVAL_DECIDE
            )
    ),

    SALESMAN(
            "Salesman / Cashier",
            30,
            "role-badge-salesman",
            "Operates the POS Billing Counter, scans barcodes, processes customer bills, and views own sales history.",
            EnumSet.of(
                    Permission.DASHBOARD_VIEW,
                    Permission.SALES_CREATE,
                    Permission.SALES_VIEW_OWN,
                    Permission.PRODUCT_VIEW,
                    Permission.ACTIVITY_LOG_VIEW_OWN
            )
    ),

    INVENTORY_MANAGER(
            "Inventory Manager",
            40,
            "role-badge-inventory",
            "Monitors stock levels, low-stock alerts, stock movements, records stock-in batches, and performs stock adjustments.",
            EnumSet.of(
                    Permission.DASHBOARD_VIEW,
                    Permission.PRODUCT_VIEW,
                    Permission.PRODUCT_VIEW_COST,
                    Permission.STOCK_VIEW,
                    Permission.STOCK_ADJUST,
                    Permission.INVENTORY_REPORT_VIEW,
                    Permission.PURCHASE_VIEW,
                    Permission.PURCHASE_CREATE,
                    Permission.SUPPLIER_VIEW,
                    Permission.APPROVAL_VIEW,
                    Permission.APPROVAL_REQUEST,
                    Permission.ACTIVITY_LOG_VIEW_OWN
            )
    ),

    PURCHASE_OFFICER(
            "Purchase Officer",
            40,
            "role-badge-purchase",
            "Creates purchase orders, records vendor procurement batches, manages supplier records, and tracks stock-in deliveries.",
            EnumSet.of(
                    Permission.DASHBOARD_VIEW,
                    Permission.PRODUCT_VIEW,
                    Permission.PRODUCT_VIEW_COST,
                    Permission.STOCK_VIEW,
                    Permission.INVENTORY_REPORT_VIEW,
                    Permission.PURCHASE_VIEW,
                    Permission.PURCHASE_CREATE,
                    Permission.SUPPLIER_VIEW,
                    Permission.SUPPLIER_MANAGE,
                    Permission.APPROVAL_VIEW,
                    Permission.APPROVAL_REQUEST,
                    Permission.ACTIVITY_LOG_VIEW_OWN
            )
    ),

    ACCOUNTANT(
            "Accountant / Finance Officer",
            50,
            "role-badge-accountant",
            "Accesses financial analytics, Profit & Loss statements, sales revenue, purchase expenses, and manages showroom expenses.",
            EnumSet.of(
                    Permission.DASHBOARD_VIEW,
                    Permission.DASHBOARD_FINANCIAL,
                    Permission.SALES_VIEW_ALL,
                    Permission.SALES_VIEW_OWN,
                    Permission.PRODUCT_VIEW,
                    Permission.PRODUCT_VIEW_COST,
                    Permission.PURCHASE_VIEW,
                    Permission.SUPPLIER_VIEW,
                    Permission.EXPENSE_VIEW,
                    Permission.EXPENSE_MANAGE,
                    Permission.REPORT_PNL_VIEW,
                    Permission.REPORT_FINANCIAL_VIEW,
                    Permission.REPORT_EXPORT,
                    Permission.APPROVAL_VIEW,
                    Permission.APPROVAL_REQUEST,
                    Permission.ACTIVITY_LOG_VIEW_OWN
            )
    );

    private final String displayName;
    private final int hierarchyLevel;
    private final String badgeClass;
    private final String description;
    private final Set<Permission> defaultPermissions;

    UserRole(String displayName,
             int hierarchyLevel,
             String badgeClass,
             String description,
             Set<Permission> defaultPermissions) {
        this.displayName = displayName;
        this.hierarchyLevel = hierarchyLevel;
        this.badgeClass = badgeClass;
        this.description = description;
        this.defaultPermissions = Collections.unmodifiableSet(defaultPermissions);
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getDescription() {
        return description;
    }

    public Set<Permission> getDefaultPermissions() {
        return defaultPermissions;
    }
}
