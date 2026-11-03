package com.showroom.inventory.entity;

import com.showroom.inventory.enums.ExpenseCategory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expense Entity — Tracks operational expenses of the showroom.
 *
 * Examples: rent, electricity bills, salaries, marketing costs, etc.
 * Recording expenses alongside sales and purchases allows the system
 * to generate comprehensive profit & loss reports.
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Category of the expense using an enum.
     * @Enumerated(EnumType.STRING) stores the enum name as a readable string
     * in the database (e.g., "RENT", "SALARY", "ELECTRICITY").
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    /** Expense amount. Always use BigDecimal for monetary values. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** The date the expense was incurred. */
    @Column(nullable = false)
    private LocalDate expenseDate;

    /** Description or details of the expense — e.g., "November electricity bill". */
    @Column(length = 500)
    private String description;

    /** Timestamp when this expense record was created. */
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
    public Expense() {
    }

    /**
     * Parameterized constructor.
     * Excludes id (auto-generated) and createdAt (set by @PrePersist).
     */
    public Expense(ExpenseCategory category, BigDecimal amount,
                   LocalDate expenseDate, String description) {
        this.category = category;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.description = description;
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

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        return "Expense{" +
                "id=" + id +
                ", category=" + category +
                ", amount=" + amount +
                ", expenseDate=" + expenseDate +
                ", description='" + description + '\'' +
                '}';
    }
}
