package com.showroom.inventory.repository;

import com.showroom.inventory.entity.StockMovement;
import com.showroom.inventory.enums.StockMovementType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for StockMovement entity.
 * Provides CRUD operations and custom query methods for tracking
 * all inventory stock movements (purchases, sales, adjustments, returns).
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Find all stock movements for a specific product, ordered by creation date descending.
     * Useful for viewing the complete stock history of a product.
     *
     * @param productId the ID of the product
     * @return list of stock movements for the given product, newest first
     */
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Find all stock movements ordered by creation date in descending order (newest first).
     *
     * @return list of all stock movements sorted by creation date descending
     */
    List<StockMovement> findAllByOrderByCreatedAtDesc();

    /**
     * Find all stock movements of a specific type (e.g., PURCHASE, SALE, ADJUSTMENT).
     *
     * @param type the stock movement type to filter by
     * @return list of stock movements of the given type
     */
    List<StockMovement> findByMovementType(StockMovementType type);

    /**
     * Find the most recent stock movements using pagination.
     * Typically used to fetch the top N most recent movements for dashboard display.
     *
     * @param pageable pagination information (e.g., PageRequest.of(0, 10))
     * @return list of recent stock movements limited by the pageable parameter
     */
    @Query("SELECT sm FROM StockMovement sm ORDER BY sm.createdAt DESC")
    List<StockMovement> findRecentMovements(Pageable pageable);
}
