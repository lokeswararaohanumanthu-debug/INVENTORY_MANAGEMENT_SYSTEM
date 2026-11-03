package com.showroom.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * SaleItem Entity — Represents a single line item within a Sale (bill).
 *
 * Each SaleItem captures:
 *   - Which product was sold
 *   - How many units
 *   - The selling price at the time of sale (unitPrice)
 *   - The cost price at the time of sale (costPrice) — used for COGS calculation
 *   - The total price for this line (quantity × unitPrice)
 *
 * Storing both unitPrice and costPrice at the time of sale is important because
 * prices can change later. This lets us accurately calculate profit margins
 * for historical sales.
 */
@Entity
@Table(name = "sale_items")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent sale this item belongs to.
     * @ManyToOne — Many sale items can belong to one sale.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    /** The product that was sold. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Number of units sold in this line item. */
    @Column(nullable = false)
    private int quantity;

    /**
     * Selling price per unit at the time of sale.
     * Captured here because the product's selling price may change in the future.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Cost (purchase) price per unit at the time of sale.
     * Used to calculate Cost of Goods Sold (COGS) and profit margins.
     * Profit per unit = unitPrice - costPrice
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice;

    /** Total price for this line item: quantity × unitPrice. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    // ──────────────────────────────────────────────────────────────
    //  Constructors
    // ──────────────────────────────────────────────────────────────

    /** Default no-arg constructor — Required by JPA. */
    public SaleItem() {
    }

    /**
     * Parameterized constructor.
     * Excludes id (auto-generated) and sale (set via Sale.addItem()).
     */
    public SaleItem(Product product, int quantity, BigDecimal unitPrice,
                    BigDecimal costPrice, BigDecimal totalPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.costPrice = costPrice;
        this.totalPrice = totalPrice;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helper Methods
    // ──────────────────────────────────────────────────────────────

    /**
     * Calculates and sets the total price based on unit price and quantity.
     * totalPrice = unitPrice × quantity
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

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
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

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
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
        return "SaleItem{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", costPrice=" + costPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
