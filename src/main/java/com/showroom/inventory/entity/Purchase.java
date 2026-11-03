package com.showroom.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase Entity — Represents a purchase order made to a supplier.
 *
 * A purchase consists of a header (this entity) and one or more line items (PurchaseItem).
 * This is a classic master-detail (header-detail) pattern in inventory systems.
 *
 * @Entity — Maps this class to a row in the "purchases" table.
 */
@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique purchase order number — e.g., "PUR-20240101-001".
     * Used as a human-readable reference instead of the numeric id.
     */
    @Column(unique = true, nullable = false, length = 30)
    private String purchaseNumber;

    /**
     * The supplier from whom this purchase was made.
     *
     * @ManyToOne  — Many purchases can belong to one supplier.
     * fetch = LAZY — The Supplier object is loaded only when accessed (not eagerly
     *     with the Purchase). This improves performance when you only need the
     *     purchase data without supplier details.
     * @JoinColumn — Specifies the foreign key column name in the "purchases" table.
     *     nullable = false means every purchase MUST have a supplier.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /** The date the purchase was made. Uses LocalDate (no time component needed). */
    @Column(nullable = false)
    private LocalDate purchaseDate;

    /** Total amount of the entire purchase order. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Optional notes or remarks about this purchase. */
    @Column(length = 500)
    private String notes;

    /** Timestamp when this purchase record was created. */
    @Column
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────────────────────
    //  Relationships
    // ──────────────────────────────────────────────────────────────

    /**
     * The line items that make up this purchase.
     *
     * cascade = CascadeType.ALL — When we save/update/delete the Purchase,
     *     all its PurchaseItems are automatically saved/updated/deleted too.
     *     This is convenient because items are "owned" by the purchase.
     *
     * orphanRemoval = true — If a PurchaseItem is removed from this list,
     *     JPA will automatically DELETE it from the database. Without this,
     *     the item would just be disassociated (foreign key set to null) but
     *     the row would remain in the database.
     *
     * mappedBy = "purchase" — PurchaseItem owns the foreign key (purchase_id).
     */
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();

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
    public Purchase() {
    }

    /**
     * Parameterized constructor.
     * Excludes id, createdAt, and the items list (items are added via addItem()).
     */
    public Purchase(String purchaseNumber, Supplier supplier, LocalDate purchaseDate,
                    BigDecimal totalAmount, String notes) {
        this.purchaseNumber = purchaseNumber;
        this.supplier = supplier;
        this.purchaseDate = purchaseDate;
        this.totalAmount = totalAmount;
        this.notes = notes;
    }

    // ──────────────────────────────────────────────────────────────
    //  Helper Methods — Manage the bidirectional relationship
    // ──────────────────────────────────────────────────────────────

    /**
     * Adds a PurchaseItem to this purchase and sets the back-reference.
     *
     * In a bidirectional @OneToMany / @ManyToOne relationship, you must
     * keep BOTH sides in sync. This helper ensures the PurchaseItem's
     * "purchase" field points back to this Purchase.
     *
     * @param item the line item to add
     */
    public void addItem(PurchaseItem item) {
        items.add(item);
        item.setPurchase(this);
    }

    /**
     * Removes a PurchaseItem from this purchase and clears the back-reference.
     * Because orphanRemoval = true, the removed item will be deleted from the DB.
     *
     * @param item the line item to remove
     */
    public void removeItem(PurchaseItem item) {
        items.remove(item);
        item.setPurchase(null);
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

    public String getPurchaseNumber() {
        return purchaseNumber;
    }

    public void setPurchaseNumber(String purchaseNumber) {
        this.purchaseNumber = purchaseNumber;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public List<PurchaseItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItem> items) {
        this.items = items;
    }

    // ──────────────────────────────────────────────────────────────
    //  toString
    // ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", purchaseNumber='" + purchaseNumber + '\'' +
                ", purchaseDate=" + purchaseDate +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
