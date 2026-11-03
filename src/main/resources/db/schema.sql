-- =====================================================================
-- Mobile Showroom Inventory Management System - Database Schema
-- =====================================================================
-- This SQL script creates the complete database schema for MySQL.
--
-- IMPORTANT NOTES:
--   - This file is a REFERENCE script for manual MySQL setup.
--   - It is NOT auto-executed by Spring Boot (we use JPA ddl-auto=update).
--   - JPA/Hibernate will automatically create tables from @Entity classes.
--   - Use this script if you want to set up the database manually or
--     if you need to understand the database structure.
--
-- To run this script manually:
--   1. Open MySQL command line or MySQL Workbench
--   2. Execute: source /path/to/schema.sql
-- =====================================================================

-- Create the database if it does not already exist
CREATE DATABASE IF NOT EXISTS mobile_showroom_db;

-- Switch to the database
USE mobile_showroom_db;

-- =====================================================================
-- TABLE 1: users
-- Stores system user accounts for authentication and authorization.
-- Linked to Spring Security for login and role-based access control.
-- =====================================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(100)    UNIQUE,
    role            VARCHAR(20)     NOT NULL DEFAULT 'MANAGER',
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- TABLE 2: suppliers
-- Stores information about product suppliers/vendors.
-- Each supplier can provide multiple products to the showroom.
-- =====================================================================
CREATE TABLE IF NOT EXISTS suppliers (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL,
    contact_person  VARCHAR(100),
    phone           VARCHAR(20),
    email           VARCHAR(100),
    address         TEXT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- TABLE 3: products
-- Stores the catalog of products available in the mobile showroom.
-- Each product belongs to a category and optionally linked to a supplier.
-- =====================================================================
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150)    NOT NULL,
    brand           VARCHAR(100),
    model           VARCHAR(100),
    category        VARCHAR(30)     NOT NULL,
    description     TEXT,
    purchase_price  DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    selling_price   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    quantity         INT             NOT NULL DEFAULT 0,
    reorder_level   INT             NOT NULL DEFAULT 5,
    supplier_id     BIGINT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- TABLE 4: purchases
-- Records purchase orders made from suppliers.
-- Each purchase can contain multiple items (see purchase_items table).
-- =====================================================================
CREATE TABLE IF NOT EXISTS purchases (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    supplier_id     BIGINT,
    purchase_date   DATE            NOT NULL,
    total_amount    DECIMAL(14, 2)  NOT NULL DEFAULT 0.00,
    notes           TEXT,
    created_by      BIGINT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- TABLE 5: purchase_items
-- Stores individual line items within a purchase order.
-- Links products to purchases with quantity and unit price details.
-- =====================================================================
CREATE TABLE IF NOT EXISTS purchase_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    purchase_id     BIGINT          NOT NULL,
    product_id      BIGINT          NOT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    unit_price      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    total_price     DECIMAL(14, 2)  NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- TABLE 6: sales
-- Records sales transactions made to customers.
-- Each sale can contain multiple items (see sale_items table).
-- =====================================================================
CREATE TABLE IF NOT EXISTS sales (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    customer_name   VARCHAR(100),
    customer_phone  VARCHAR(20),
    sale_date       DATE            NOT NULL,
    total_amount    DECIMAL(14, 2)  NOT NULL DEFAULT 0.00,
    discount        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    net_amount      DECIMAL(14, 2)  NOT NULL DEFAULT 0.00,
    notes           TEXT,
    created_by      BIGINT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- TABLE 7: sale_items
-- Stores individual line items within a sale transaction.
-- Links products to sales with quantity and unit price details.
-- =====================================================================
CREATE TABLE IF NOT EXISTS sale_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    sale_id         BIGINT          NOT NULL,
    product_id      BIGINT          NOT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    unit_price      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    total_price     DECIMAL(14, 2)  NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- TABLE 8: expenses
-- Tracks business expenses incurred by the mobile showroom.
-- Categorized by type (rent, electricity, salary, etc.) for reporting.
-- =====================================================================
CREATE TABLE IF NOT EXISTS expenses (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    expense_category    VARCHAR(30)     NOT NULL,
    amount              DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    expense_date        DATE            NOT NULL,
    description         TEXT,
    created_by          BIGINT,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- TABLE 9: stock_movements
-- Logs all inventory stock movements for audit and tracking purposes.
-- Records every change in product quantity: purchases, sales, adjustments.
-- =====================================================================
CREATE TABLE IF NOT EXISTS stock_movements (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    product_id      BIGINT          NOT NULL,
    movement_type   VARCHAR(20)     NOT NULL,
    quantity        INT             NOT NULL,
    reference_id    BIGINT,
    notes           TEXT,
    created_by      BIGINT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- END OF SCHEMA
-- =====================================================================
-- After running this script, 9 tables will be created:
--   1. users            - System user accounts
--   2. suppliers         - Product suppliers/vendors
--   3. products          - Product catalog
--   4. purchases         - Purchase orders from suppliers
--   5. purchase_items    - Line items in purchase orders
--   6. sales             - Sales transactions to customers
--   7. sale_items        - Line items in sales transactions
--   8. expenses          - Business expense records
--   9. stock_movements   - Inventory movement audit log
-- =====================================================================
