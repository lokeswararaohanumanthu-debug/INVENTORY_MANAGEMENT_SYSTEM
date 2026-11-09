package com.showroom.inventory.service;

import com.showroom.inventory.entity.Product;
import com.showroom.inventory.entity.StockMovement;
import com.showroom.inventory.enums.StockMovementType;
import com.showroom.inventory.exception.InsufficientStockException;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.ProductRepository;
import com.showroom.inventory.repository.StockMovementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling Stock and Inventory management.
 * Maintains stock movements audit trail and enforces stock sufficiency rules.
 */
@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ActivityLogService activityLogService;

    public InventoryService(ProductRepository productRepository,
                            StockMovementRepository stockMovementRepository,
                            ActivityLogService activityLogService) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.activityLogService = activityLogService;
    }

    public List<StockMovement> getAllMovements() {
        return stockMovementRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<StockMovement> getRecentMovements(int limit) {
        return stockMovementRepository.findRecentMovements(PageRequest.of(0, limit));
    }

    public List<StockMovement> getProductMovements(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional
    public void recordPurchaseStockIncrease(Product product, int quantity, String purchaseNumber) {
        int before = product.getQuantity();
        int after = before + quantity;
        product.setQuantity(after);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(StockMovementType.PURCHASE_IN);
        movement.setQuantity(quantity);
        movement.setStockBefore(before);
        movement.setStockAfter(after);
        movement.setReferenceNumber(purchaseNumber);
        movement.setNotes("Stock received via purchase order " + purchaseNumber);
        stockMovementRepository.save(movement);
    }

    @Transactional
    public void recordSaleStockDecrease(Product product, int quantity, String billNumber) {
        int before = product.getQuantity();
        if (before < quantity) {
            throw new InsufficientStockException(product.getName(), before, quantity);
        }
        int after = before - quantity;
        product.setQuantity(after);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(StockMovementType.SALE_OUT);
        movement.setQuantity(quantity);
        movement.setStockBefore(before);
        movement.setStockAfter(after);
        movement.setReferenceNumber(billNumber);
        movement.setNotes("Stock deducted via sales invoice " + billNumber);
        stockMovementRepository.save(movement);
    }

    @Transactional
    public void adjustStock(Long productId, int newQuantity, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        int before = product.getQuantity();
        int delta = newQuantity - before;
        product.setQuantity(newQuantity);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(StockMovementType.ADJUSTMENT);
        movement.setQuantity(delta);
        movement.setStockBefore(before);
        movement.setStockAfter(newQuantity);
        movement.setReferenceNumber("ADJ-" + System.currentTimeMillis() % 100000);
        movement.setNotes("Manual stock adjustment: " + (reason != null && !reason.trim().isEmpty() ? reason : "Inventory count correction"));
        stockMovementRepository.save(movement);

        activityLogService.logCurrentUserAction(
                "STOCK_ADJUSTMENT",
                "INVENTORY",
                "Adjusted stock for '" + product.getName() + "' (" + product.getProductCode() + ") from " + before + " to " + newQuantity,
                "Stock Quantity: " + before,
                "Stock Quantity: " + newQuantity + " (Reason: " + (reason != null ? reason : "Count correction") + ")"
        );
    }
}
