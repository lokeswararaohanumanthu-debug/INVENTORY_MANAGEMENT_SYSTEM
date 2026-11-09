package com.showroom.inventory.enums;

/**
 * Granular permissions used across the Role-Based Access Control (RBAC) system.
 * Each permission is mapped to a Spring Security GrantedAuthority ("PERM_<NAME>")
 * and enforced at both the backend controller/service layer and frontend UI templates.
 */
public enum Permission {

    // 1. Dashboard
    DASHBOARD_VIEW("View Dashboard", "Dashboard"),
    DASHBOARD_FULL("Full Executive Dashboard", "Dashboard"),
    DASHBOARD_FINANCIAL("Financial Dashboard Metrics", "Dashboard"),

    // 2. Sales & POS Billing
    SALES_CREATE("Create Sales & POS Billing", "Sales & POS"),
    SALES_VIEW_ALL("View All Sales Invoices", "Sales & POS"),
    SALES_VIEW_OWN("View Own Sales Invoices", "Sales & POS"),

    // 3. Product Catalogue
    PRODUCT_VIEW("View Product Catalogue", "Products"),
    PRODUCT_VIEW_COST("View Product Purchase Cost", "Products"),
    PRODUCT_ADD("Add New Products", "Products"),
    PRODUCT_EDIT("Edit Products", "Products"),
    PRODUCT_DEACTIVATE("Activate / Deactivate Products", "Products"),
    PRODUCT_DELETE("Delete Products", "Products"),

    // 4. Inventory & Stock Management
    STOCK_VIEW("View Stock Levels & Movements", "Inventory"),
    STOCK_ADJUST("Perform Stock Adjustments", "Inventory"),
    STOCK_APPROVE("Approve Stock Adjustments", "Inventory"),
    INVENTORY_REPORT_VIEW("Generate Inventory Reports", "Inventory"),

    // 5. Purchases & Procurement
    PURCHASE_VIEW("View Purchases & Stock-In History", "Purchases"),
    PURCHASE_CREATE("Record Purchases & Stock-In", "Purchases"),
    PURCHASE_APPROVE("Approve Purchase Requests", "Purchases"),

    // 6. Suppliers Management
    SUPPLIER_VIEW("View Suppliers", "Suppliers"),
    SUPPLIER_MANAGE("Add, Edit & Deactivate Suppliers", "Suppliers"),

    // 7. Expenses Tracker
    EXPENSE_VIEW("View Showroom Expenses", "Expenses"),
    EXPENSE_MANAGE("Create, Edit & Delete Expenses", "Expenses"),

    // 8. Financial Reports & Profit/Loss
    REPORT_PNL_VIEW("Access Profit & Loss Summary", "Financial Reports"),
    REPORT_FINANCIAL_VIEW("Access Financial Analytics & Reports", "Financial Reports"),
    REPORT_EXPORT("Export Financial Reports", "Financial Reports"),

    // 9. User & Role Management
    USER_VIEW("View Employee Accounts", "User Management"),
    USER_MANAGE("Create, Edit & Manage Employee Accounts", "User Management"),
    USER_PERMISSIONS_CUSTOMIZE("Customize Granular User Permissions", "User Management"),

    // 10. System Settings
    SETTINGS_VIEW("View System Settings", "System Settings"),
    SETTINGS_MANAGE("Modify System-Level Security Settings", "System Settings"),

    // 11. Activity Logs
    ACTIVITY_LOG_VIEW_ALL("View All Employee Activity Logs", "Activity Logs"),
    ACTIVITY_LOG_VIEW_OWN("View Own Activity Logs", "Activity Logs"),

    // 12. Approvals Workflow
    APPROVAL_VIEW("View Approval Requests", "Approvals"),
    APPROVAL_REQUEST("Submit Approval Requests", "Approvals"),
    APPROVAL_DECIDE("Approve or Reject Sensitive Operations", "Approvals");

    private final String displayName;
    private final String moduleName;

    Permission(String displayName, String moduleName) {
        this.displayName = displayName;
        this.moduleName = moduleName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getModule() {
        return moduleName;
    }

    public String getAuthority() {
        return "PERM_" + this.name();
    }
}

