package com.showroom.inventory.repository;

import com.showroom.inventory.entity.PurchaseItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for PurchaseItem entity.
 * Provides CRUD operations and custom query methods for managing
 * individual line items within a purchase order.
 */
@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    /**
     * Find all purchase items belonging to a specific purchase order.
     *
     * @param purchaseId the ID of the parent purchase
     * @return list of purchase items for the given purchase
     */
    List<PurchaseItem> findByPurchaseId(Long purchaseId);

    /**
     * Find all purchase items for a specific product.
     * Useful for viewing the purchase history of a particular product.
     *
     * @param productId the ID of the product
     * @return list of purchase items involving the given product
     */
    List<PurchaseItem> findByProductId(Long productId);
}
