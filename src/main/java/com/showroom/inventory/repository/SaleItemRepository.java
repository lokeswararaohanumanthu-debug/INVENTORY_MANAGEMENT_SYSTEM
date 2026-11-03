package com.showroom.inventory.repository;

import com.showroom.inventory.entity.SaleItem;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for SaleItem entity.
 * Provides CRUD operations and custom query methods for managing
 * individual line items within a sale transaction.
 * Also provides analytics queries for best-selling and slow-moving products.
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /**
     * Find all sale items belonging to a specific sale transaction.
     *
     * @param saleId the ID of the parent sale
     * @return list of sale items for the given sale
     */
    List<SaleItem> findBySaleId(Long saleId);

    /**
     * Find all sale items for a specific product.
     * Useful for viewing the sales history of a particular product.
     *
     * @param productId the ID of the product
     * @return list of sale items involving the given product
     */
    List<SaleItem> findByProductId(Long productId);

    /**
     * Find the best-selling products by total quantity sold.
     * Returns an array of [productName, brand, totalQuantitySold] grouped by product,
     * ordered by total quantity sold in descending order.
     * Use Pageable to limit results (e.g., top 10 best sellers).
     *
     * @param pageable pagination information to limit the number of results
     * @return list of Object arrays containing [productName, brand, totalQuantitySold]
     */
    @Query("SELECT si.product.name, si.product.brand, SUM(si.quantity) AS totalSold " +
           "FROM SaleItem si " +
           "GROUP BY si.product.name, si.product.brand " +
           "ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProducts(Pageable pageable);

    /**
     * Calculate the Cost of Goods Sold (COGS) for a given date range.
     * COGS is computed as the sum of (quantity * costPrice) for all sale items
     * where the parent sale's date falls within the specified range.
     * Returns 0 if no sales exist in the range (using COALESCE).
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @return the total cost of goods sold in the date range
     */
    @Query("SELECT COALESCE(SUM(si.quantity * si.costPrice), 0) " +
           "FROM SaleItem si " +
           "WHERE si.sale.saleDate BETWEEN :start AND :end")
    BigDecimal sumCostOfGoodsSoldBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Find slow-moving products by lowest total quantity sold.
     * Returns an array of [productName, brand, totalQuantitySold] grouped by product,
     * ordered by total quantity sold in ascending order (least sold first).
     * Use Pageable to limit results (e.g., bottom 10 slow movers).
     *
     * @param pageable pagination information to limit the number of results
     * @return list of Object arrays containing [productName, brand, totalQuantitySold]
     */
    @Query("SELECT si.product.name, si.product.brand, SUM(si.quantity) AS totalSold " +
           "FROM SaleItem si " +
           "GROUP BY si.product.name, si.product.brand " +
           "ORDER BY totalSold ASC")
    List<Object[]> findSlowMovingProducts(Pageable pageable);
}
