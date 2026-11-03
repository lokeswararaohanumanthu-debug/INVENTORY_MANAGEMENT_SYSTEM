package com.showroom.inventory.repository;

import com.showroom.inventory.entity.Sale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Sale entity.
 * Provides CRUD operations and custom query methods for managing
 * sales transactions in the mobile showroom.
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    /**
     * Find a sale by its unique bill number.
     *
     * @param billNumber the unique bill number to search for
     * @return an Optional containing the sale if found
     */
    Optional<Sale> findByBillNumber(String billNumber);

    /**
     * Find all sales ordered by creation date in descending order (newest first).
     *
     * @return list of all sales sorted by creation date descending
     */
    List<Sale> findAllByOrderByCreatedAtDesc();

    /**
     * Find sales within a specific date range.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return list of sales within the date range
     */
    List<Sale> findBySaleDateBetween(LocalDate start, LocalDate end);

    /**
     * Find the most recent sales using pagination.
     * Typically used to fetch the top N most recent sales for dashboard display.
     *
     * @param pageable pagination information (e.g., PageRequest.of(0, 10))
     * @return list of recent sales limited by the pageable parameter
     */
    @Query("SELECT s FROM Sale s ORDER BY s.saleDate DESC, s.createdAt DESC")
    List<Sale> findRecentSales(Pageable pageable);

    /**
     * Calculate the total net sales amount for a specific date (today's sales).
     * Returns 0 if no sales exist for the date (using COALESCE).
     *
     * @param date the date to calculate sales for
     * @return the sum of net amounts for sales on the given date
     */
    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Sale s WHERE s.saleDate = :date")
    BigDecimal sumNetAmountByDate(@Param("date") LocalDate date);

    /**
     * Calculate the total net sales amount within a specific date range.
     * Used for calculating monthly or periodic revenue.
     * Returns 0 if no sales exist in the range (using COALESCE).
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return the sum of net amounts for sales in the date range
     */
    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    BigDecimal sumNetAmountBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Count the number of sales within a specific date range.
     * Useful for monthly sales statistics.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return the count of sales in the date range
     */
    long countBySaleDateBetween(LocalDate start, LocalDate end);
}
