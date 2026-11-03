package com.showroom.inventory.repository;

import com.showroom.inventory.enums.Category;
import com.showroom.inventory.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Product entity.
 * Provides CRUD operations and custom query methods for managing
 * mobile products in the showroom inventory.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find a product by its unique product code.
     *
     * @param productCode the unique product code to search for
     * @return an Optional containing the product if found
     */
    Optional<Product> findByProductCode(String productCode);

    /**
     * Check if a product with the given product code already exists.
     * Used for duplicate validation before creating a new product.
     *
     * @param productCode the product code to check
     * @return true if a product with the given code exists
     */
    boolean existsByProductCode(String productCode);

    /**
     * Search active products by keyword across name, brand, model, and product code.
     * The search is case-insensitive and matches partial strings.
     *
     * @param keyword the search keyword to match against product fields
     * @return list of matching active products
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.model) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.productCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProducts(@Param("keyword") String keyword);

    /**
     * Find all products that are currently active (not soft-deleted).
     *
     * @return list of all active products
     */
    List<Product> findByActiveTrue();

    /**
     * Find products whose current quantity is at or below their minimum stock level.
     * Used to generate low-stock alerts for inventory management.
     *
     * @return list of active products with low stock
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.quantity <= p.minStockLevel")
    List<Product> findLowStockProducts();

    /**
     * Find products that are completely out of stock (quantity is zero or negative).
     *
     * @return list of active products with zero or negative stock
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.quantity <= 0")
    List<Product> findOutOfStockProducts();

    /**
     * Count the total number of active products in the inventory.
     *
     * @return the count of active products
     */
    long countByActiveTrue();

    /**
     * Calculate the sum of all quantities across active products.
     * Returns 0 if no active products exist (using COALESCE).
     *
     * @return total stock quantity across all active products
     */
    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM Product p WHERE p.active = true")
    long sumTotalStock();

    /**
     * Find all active products belonging to a specific category.
     *
     * @param category the category to filter by
     * @return list of active products in the given category
     */
    List<Product> findByCategoryAndActiveTrue(Category category);
}
