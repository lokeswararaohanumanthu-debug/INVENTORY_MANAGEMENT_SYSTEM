package com.showroom.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * PurchaseItem Entity — Represents a single line item within a Purchase order.
 *
 * This is the "detail" side of the Purchase (master-detail) pattern.
 * Each PurchaseItem records: which product was purchased, how many units,
 * at what unit price, and the computed total price for that line.
 *
 * Think of it like a single row in a purchase invoice.
 */
@Entity
@Table(name = "purchase_items")
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent purchase order this item belongs to.
     *
     * @ManyToOne  — Many items can belong to one purchase.
     * @JoinColumn — The "purchase_id" column in the purchase_items table
     *     holds the foreign key referencing the purchases table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    /**
     * The product being purchased.
     * Links this line item to a specific product in the inventory.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Number of units purchased in this line item. */
    @Column(nullable = false)
    private int quantity;

    /**
     * Price per unit at the time of purchase.
     * We store this separately because prices can change over time —
     * we want to remember what we actually paid.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Total price for this line item (quantity × unitPrice). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    // ──────────────────────────────────────────────────────────────
    //  Constructors
    // ──────────────────────────────────────────────────────────────

    /** Default no-arg constructor — Required by JPA. */
    public PurchaseItem() {
    }

    /**
     * Parameterized constructor.
     * Excludes id (auto-generated) and purchase (set via Purchase.addItem()).
     */
    public PurchaseItem(Product product, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helper Methods
    // ──────────────────────────────────────────────────────────────

    /**
     * Calculates and sets the total price based on unit price and quantity.
     *
     * Uses BigDecimal.valueOf(quantity) to convert the int to BigDecimal
     * before multiplication, ensuring precise decimal arithmetic.
     */
    public void calculateTotalPrice() {
        this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
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

    public Purchase getPurchase() {
        return purchase;
    }

    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    // ──────────────────────────────────────────────────────────────
    //  toString
    // ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PurchaseItem{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
