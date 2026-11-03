package com.showroom.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sale Entity — Represents a sales transaction (bill) at the showroom.
 *
 * Each sale has a header (customer info, totals, payment method) and
 * one or more line items (SaleItem). The discount is applied at the
 * bill level: netAmount = totalAmount - discount.
 */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique bill/invoice number — e.g., "BILL-20240101-001".
     * This is what gets printed on the customer's receipt.
     */
    @Column(unique = true, nullable = false, length = 30)
    private String billNumber;

    /** Customer's name (optional — walk-in customers may not provide this). */
    @Column(length = 100)
    private String customerName;

    /** Customer's phone number for follow-up or warranty tracking. */
    @Column(length = 20)
    private String customerPhone;

    /** The date the sale was made. */
    @Column(nullable = false)
    private LocalDate saleDate;

    /**
     * Subtotal of all items before any discount is applied.
     * This is the sum of all SaleItem.totalPrice values.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Discount applied to the entire bill.
     * Defaults to BigDecimal.ZERO (no discount).
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    /**
     * Net amount payable after discount: netAmount = totalAmount - discount.
     * This is the actual amount the customer pays.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    /**
     * Payment method used — e.g., "CASH", "CARD", "UPI".
     * Stored as a simple String for flexibility.
     */
    @Column(length = 20)
    private String paymentMethod;

    /** Optional notes or remarks about this sale. */
    @Column(length = 500)
    private String notes;

    /** Timestamp when this sale record was created. */
    @Column
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────────────────────
    //  Relationships
    // ──────────────────────────────────────────────────────────────

    /**
     * Line items in this sale.
     * cascade = ALL + orphanRemoval = true means:
     *   - Saving the Sale automatically saves all its SaleItems.
     *   - Removing a SaleItem from this list automatically deletes it from the DB.
     */
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    // ──────────────────────────────────────────────────────────────
    //  Lifecycle Callbacks
    // ──────────────────────────────────────────────────────────────

    /**
     * @PrePersist — Sets the creation timestamp and ensures discount
     * defaults to ZERO if it was not explicitly provided.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.discount == null) {
            this.discount = BigDecimal.ZERO;
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Constructors
    // ──────────────────────────────────────────────────────────────

    /** Default no-arg constructor — Required by JPA. */
    public Sale() {
    }

    /**
     * Parameterized constructor.
     * Excludes id, createdAt, and the items list.
     */
    public Sale(String billNumber, String customerName, String customerPhone,
                LocalDate saleDate, BigDecimal totalAmount, BigDecimal discount,
                BigDecimal netAmount, String paymentMethod, String notes) {
        this.billNumber = billNumber;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.saleDate = saleDate;
        this.totalAmount = totalAmount;
        this.discount = discount;
        this.netAmount = netAmount;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helper Methods — Bidirectional relationship management
    // ──────────────────────────────────────────────────────────────

    /**
     * Adds a SaleItem to this sale and sets the back-reference.
     *
     * @param item the sale line item to add
     */
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    /**
     * Removes a SaleItem from this sale and clears the back-reference.
     * The orphanRemoval setting will delete the item from the database.
     *
     * @param item the sale line item to remove
     */
    public void removeItem(SaleItem item) {
        items.remove(item);
        item.setSale(null);
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

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    // ──────────────────────────────────────────────────────────────
    //  toString
    // ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", billNumber='" + billNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", saleDate=" + saleDate +
                ", totalAmount=" + totalAmount +
                ", discount=" + discount +
                ", netAmount=" + netAmount +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}
