package com.showroom.inventory.entity;

import com.showroom.inventory.enums.Category;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product Entity — Represents a mobile phone or accessory in the showroom inventory.
 *
 * @Entity   — Marks this class as a JPA entity (a persistent domain object mapped to a DB table).
 * @Table    — Specifies the database table name. Without it, JPA would use the class name.
 */
@Entity
@Table(name = "products")
public class Product {

    /**
     * @Id                — Marks this field as the primary key.
     * @GeneratedValue    — Tells JPA to auto-generate the primary key value.
     * GenerationType.IDENTITY — Uses the database's auto-increment feature (e.g., MySQL AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A unique product code like "MOB-001".
     * unique = true  — Adds a UNIQUE constraint in the database.
     * nullable = false — Adds a NOT NULL constraint (this column cannot be empty).
     * length = 20    — Sets the VARCHAR length to 20 characters.
     */
    @Column(unique = true, nullable = false, length = 20)
    private String productCode;

    /**
     * Optional manufacturer barcode printed on the product box (EAN-13, UPC-A, Code-128, etc.).
     * Used by the POS billing counter to identify products via camera scan or manual entry.
     * unique = true — Two products can never share the same barcode (NULLs are allowed for many rows).
     */
    @Column(unique = true, length = 50)
    private String barcode;

    /** Product name — e.g., "Samsung Galaxy S24 Ultra" */
    @Column(nullable = false, length = 100)
    private String name;

    /** Brand name — e.g., "Samsung", "Apple", "OnePlus" */
    @Column(nullable = false, length = 50)
    private String brand;

    /** Model identifier — e.g., "S24 Ultra", "iPhone 15 Pro" */
    @Column(length = 50)
    private String model;

    /**
     * Product category using an enum.
     * @Enumerated(EnumType.STRING) — Stores the enum name as a String in the database
     *     (e.g., "SMARTPHONE") instead of its ordinal index (0, 1, 2...).
     *     STRING is preferred over ORDINAL because it is resistant to enum reordering.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    /**
     * Purchase price (cost price) of the product.
     * BigDecimal is used for monetary values to avoid floating-point precision errors.
     * precision = 12 — Total number of digits (including decimal).
     * scale = 2      — Number of digits after the decimal point.
     * This allows values up to 9,999,999,999.99
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    /** Selling price (MRP or retail price) of the product. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    /** Current stock quantity available in the showroom. */
    @Column(nullable = false)
    private int quantity;

    /**
     * Minimum stock level — when quantity falls to or below this value,
     * the product is considered "low stock" and may trigger a reorder alert.
     * Defaults to 5 if not explicitly set.
     */
    @Column(nullable = false)
    private int minStockLevel = 5;

    /** Optional description or notes about the product. */
    @Column(length = 500)
    private String description;

    /** Whether this product is active (visible) in the system. Defaults to true. */
    @Column
    private boolean active = true;

    /** Timestamp when this record was first created. Set automatically via @PrePersist. */
    @Column
    private LocalDateTime createdAt;

    /** Timestamp when this record was last updated. Set automatically via @PreUpdate. */
    @Column
    private LocalDateTime updatedAt;

    // ──────────────────────────────────────────────────────────────
    //  Relationships (all LAZY to avoid the N+1 select problem)
    // ──────────────────────────────────────────────────────────────

    /**
     * @OneToMany — One product can appear in many purchase line-items.
     * mappedBy = "product" — The PurchaseItem entity owns the foreign key (product_id).
     * FetchType.LAZY       — The collection is NOT loaded from the DB until accessed.
     *     This is the default for @OneToMany but we state it explicitly for clarity.
     */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<PurchaseItem> purchaseItems = new ArrayList<>();

    /** One product can appear in many sale line-items. */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<SaleItem> saleItems = new ArrayList<>();

    /** One product can have many stock movement records (IN, OUT, ADJUSTMENT, etc.). */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<StockMovement> stockMovements = new ArrayList<>();

    // ──────────────────────────────────────────────────────────────
    //  JPA Lifecycle Callbacks
    // ──────────────────────────────────────────────────────────────

    /**
     * @PrePersist — Called automatically by JPA just BEFORE the entity is
     * first inserted into the database. Perfect for setting default values
     * and audit timestamps.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (!this.active) {
            this.active = true;
        }
        if (this.minStockLevel == 0) {
            this.minStockLevel = 5;
        }
    }

    /**
     * @PreUpdate — Called automatically by JPA just BEFORE the entity is
     * updated in the database. Used here to keep the updatedAt timestamp current.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ──────────────────────────────────────────────────────────────
    //  Constructors
    // ──────────────────────────────────────────────────────────────

    /**
     * Default no-arg constructor — Required by JPA.
     * JPA uses reflection to create entity instances, so a public or protected
     * no-arg constructor must exist.
     */
    public Product() {
    }

    /**
     * Parameterized constructor for convenient object creation.
     * Excludes id (auto-generated) and timestamps (set by lifecycle callbacks).
     */
    public Product(String productCode, String name, String brand, String model,
                   Category category, BigDecimal purchasePrice, BigDecimal sellingPrice,
                   int quantity, int minStockLevel, String description, boolean active) {
        this.productCode = productCode;
        this.name = name;
        this.brand = brand;
        this.model = model;
        this.category = category;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.minStockLevel = minStockLevel;
        this.description = description;
        this.active = active;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helper / Business Methods
    // ──────────────────────────────────────────────────────────────

    /**
     * Checks whether the current stock is at or below the minimum stock level.
     * Useful for triggering reorder alerts on the dashboard.
     *
     * @return true if stock is low
     */
    public boolean isLowStock() {
        return this.quantity <= this.minStockLevel;
    }

    /**
     * Checks whether the product is completely out of stock.
     *
     * @return true if quantity is zero or negative
     */
    public boolean isOutOfStock() {
        return this.quantity <= 0;
    }

    // ──────────────────────────────────────────────────────────────
    //  Getters and Setters
    // ──────────────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PurchaseItem> getPurchaseItems() {
        return purchaseItems;
    }

    public void setPurchaseItems(List<PurchaseItem> purchaseItems) {
        this.purchaseItems = purchaseItems;
    }

    public List<SaleItem> getSaleItems() {
        return saleItems;
    }

    public void setSaleItems(List<SaleItem> saleItems) {
        this.saleItems = saleItems;
    }

    public List<StockMovement> getStockMovements() {
        return stockMovements;
    }

    public void setStockMovements(List<StockMovement> stockMovements) {
        this.stockMovements = stockMovements;
    }

    // ──────────────────────────────────────────────────────────────
    //  toString — avoids lazy-loaded relationships to prevent
    //  LazyInitializationException outside a transaction context.
    // ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", productCode='" + productCode + '\'' +
                ", barcode='" + barcode + '\'' +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", category=" + category +
                ", purchasePrice=" + purchasePrice +
                ", sellingPrice=" + sellingPrice +
                ", quantity=" + quantity +
                ", minStockLevel=" + minStockLevel +
                ", active=" + active +
                '}';
    }
}
