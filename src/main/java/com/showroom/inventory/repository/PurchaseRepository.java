package com.showroom.inventory.repository;

import com.showroom.inventory.entity.Purchase;

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
 * Repository interface for Purchase entity.
 * Provides CRUD operations and custom query methods for managing
 * purchase orders from suppliers.
 */
@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    /**
     * Find a purchase by its unique purchase number.
     *
     * @param purchaseNumber the unique purchase number to search for
     * @return an Optional containing the purchase if found
     */
    Optional<Purchase> findByPurchaseNumber(String purchaseNumber);

    /**
     * Find all purchases ordered by creation date in descending order (newest first).
     *
     * @return list of all purchases sorted by creation date descending
     */
    List<Purchase> findAllByOrderByCreatedAtDesc();

    /**
     * Find purchases within a specific date range.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return list of purchases within the date range
     */
    List<Purchase> findByPurchaseDateBetween(LocalDate start, LocalDate end);

    /**
     * Find the most recent purchases using pagination.
     * Typically used to fetch the top N most recent purchases (e.g., top 10).
     *
     * @param pageable pagination information (e.g., PageRequest.of(0, 10))
     * @return list of recent purchases limited by the pageable parameter
     */
    @Query("SELECT p FROM Purchase p ORDER BY p.purchaseDate DESC, p.createdAt DESC")
    List<Purchase> findRecentPurchases(Pageable pageable);

    /**
     * Calculate the total purchase amount within a specific date range.
     * Returns 0 if no purchases exist in the range (using COALESCE).
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return the sum of total amounts for purchases in the date range
     */
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Purchase p WHERE p.purchaseDate BETWEEN :start AND :end")
    BigDecimal sumTotalAmountBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Count the number of purchases within a specific date range.
     * Useful for monthly purchase statistics.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return the count of purchases in the date range
     */
    long countByPurchaseDateBetween(LocalDate start, LocalDate end);
}
