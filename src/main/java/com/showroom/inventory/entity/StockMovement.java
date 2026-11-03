package com.showroom.inventory.entity;

import com.showroom.inventory.enums.StockMovementType;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * StockMovement Entity — Records every change in a product's stock quantity.
 *
 * This serves as an audit trail for inventory changes. Every time stock
 * increases (purchase, return) or decreases (sale, damage), a StockMovement
 * record is created with the "before" and "after" quantities.
 *
 * This is critical for:
 *   - Audit compliance (who changed what and when)
 *   - Debugging discrepancies between expected and actual stock
 *   - Generating stock history reports
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The product whose stock was affected. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Type of stock movement — e.g., PURCHASE_IN, SALE_OUT, ADJUSTMENT, DAMAGE, RETURN.
     * Using an enum ensures only valid movement types are recorded.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType movementType;

    /** Number of units involved in this movement. */
    @Column(nullable = false)
    private int quantity;

    /** Stock quantity BEFORE this movement was applied. */
    @Column(nullable = false)
    private int stockBefore;

    /** Stock quantity AFTER this movement was applied. */
    @Column(nullable = false)
    private int stockAfter;

    /**
     * Reference to the source document — e.g., the purchase number or bill number
     * that triggered this stock change. Helps trace movements back to their origin.
     */
    @Column(length = 30)
    private String referenceNumber;

    /** Additional notes explaining the movement — e.g., "Damaged during transit". */
    @Column(length = 500)
    private String notes;

    /** Timestamp when this movement was recorded. */
    @Column
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────────────────────
    //  Lifecycle Callbacks
    // ──────────────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ──────────────────────────────────────────────────────────────
    //  Constructors
    // ──────────────────────────────────────────────────────────────

    /** Default no-arg constructor — Required by JPA. */
    public StockMovement() {
    }

    /**
     * Parameterized constructor.
     * Excludes id (auto-generated) and createdAt (set by @PrePersist).
     */
    public StockMovement(Product product, StockMovementType movementType, int quantity,
                         int stockBefore, int stockAfter, String referenceNumber, String notes) {
        this.product = product;
        this.movementType = movementType;
        this.quantity = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(StockMovementType movementType) {
        this.movementType = movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(int stockBefore) {
        this.stockBefore = stockBefore;
    }

    public int getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(int stockAfter) {
        this.stockAfter = stockAfter;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ──────────────────────────────────────────────────────────────
    //  toString
    // ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "StockMovement{" +
                "id=" + id +
                ", movementType=" + movementType +
                ", quantity=" + quantity +
                ", stockBefore=" + stockBefore +
                ", stockAfter=" + stockAfter +
                ", referenceNumber='" + referenceNumber + '\'' +
                '}';
    }
}
