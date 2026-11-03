package com.showroom.inventory.repository;

import com.showroom.inventory.entity.Expense;
import com.showroom.inventory.enums.ExpenseCategory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Expense entity.
 * Provides CRUD operations and custom query methods for managing
 * showroom expenses and expense analytics.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Find expenses within a specific date range.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return list of expenses within the date range
     */
    List<Expense> findByExpenseDateBetween(LocalDate start, LocalDate end);

    /**
     * Find all expenses ordered by expense date in descending order (newest first).
     *
     * @return list of all expenses sorted by expense date descending
     */
    List<Expense> findAllByOrderByExpenseDateDesc();

    /**
     * Calculate the total expense amount within a specific date range.
     * Returns 0 if no expenses exist in the range (using COALESCE).
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return the sum of expense amounts in the date range
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumAmountBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Calculate total expenses grouped by category within a specific date range.
     * Returns an array of [ExpenseCategory, totalAmount] for each category
     * that has expenses in the date range.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return list of Object arrays containing [ExpenseCategory, totalAmount]
     */
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e " +
           "WHERE e.expenseDate BETWEEN :start AND :end " +
           "GROUP BY e.category " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumAmountByCategoryBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Find all expenses belonging to a specific expense category.
     *
     * @param category the expense category to filter by
     * @return list of expenses in the given category
     */
    List<Expense> findByCategory(ExpenseCategory category);
}
