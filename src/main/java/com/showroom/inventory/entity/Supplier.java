package com.showroom.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplier Entity — Represents a vendor/supplier who provides products to the showroom.
 *
 * Suppliers are linked to Purchase records. Tracking supplier information helps
 * manage procurement and maintain vendor relationships.
 *
 * @Entity  — Tells JPA that this class maps to a database table.
 * @Table   — Specifies the exact table name in the database.
 */
@Entity
@Table(name = "suppliers")
public class Supplier {

    /**
     * Primary key with auto-increment.
     * @Id marks the field as the table's primary key.
     * GenerationType.IDENTITY delegates key generation to the database engine.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Supplier company or individual name. */
    @Column(nullable = false, length = 100)
    private String name;

    /** Name of the point-of-contact person at the supplier. */
    @Column(length = 100)
    private String contactPerson;

    /** Phone number of the supplier. */
    @Column(length = 20)
    private String phone;

    /** Email address of the supplier. */
    @Column(length = 100)
    private String email;

    /**
     * Full address of the supplier.
     * columnDefinition = "TEXT" — Overrides the default VARCHAR and uses the
     * database's TEXT type, which can store longer strings without a fixed limit.
     */
    @Column(columnDefinition = "TEXT")
    private String address;

    /** Whether the supplier is currently active. Defaults to true. */
    @Column
    private boolean active = true;

    /** Timestamp when this supplier record was created. */
    @Column
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────────────────────
    //  Relationships
    // ──────────────────────────────────────────────────────────────

    /**
     * A supplier can have many purchases.
     * mappedBy = "supplier" — The Purchase entity owns the relationship
     *     (it has the supplier_id foreign key column).
     */
    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    private List<Purchase> purchases = new ArrayList<>();

    // ──────────────────────────────────────────────────────────────
    //  Lifecycle Callbacks
    // ──────────────────────────────────────────────────────────────

    /**
     * @PrePersist — Automatically called before the entity is first saved.
     * Sets the creation timestamp and ensures active defaults to true.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (!this.active) {
            this.active = true;
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Constructors
    // ──────────────────────────────────────────────────────────────

    /** Default no-arg constructor — Required by JPA for reflective instantiation. */
    public Supplier() {
    }

    /**
     * Parameterized constructor.
     * Excludes id (auto-generated) and createdAt (set by @PrePersist).
     */
    public Supplier(String name, String contactPerson, String phone,
                    String email, String address, boolean active) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = active;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public List<Purchase> getPurchases() {
        return purchases;
    }

    public void setPurchases(List<Purchase> purchases) {
        this.purchases = purchases;
    }

    // ──────────────────────────────────────────────────────────────
    //  toString — excludes relationships to avoid lazy loading issues.
    // ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Supplier{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contactPerson='" + contactPerson + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", active=" + active +
                '}';
    }
}
