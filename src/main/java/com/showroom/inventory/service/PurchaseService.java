package com.showroom.inventory.service;

import com.showroom.inventory.dto.PurchaseDTO;
import com.showroom.inventory.dto.PurchaseItemDTO;
import com.showroom.inventory.entity.Product;
import com.showroom.inventory.entity.Purchase;
import com.showroom.inventory.entity.PurchaseItem;
import com.showroom.inventory.entity.Supplier;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.ProductRepository;
import com.showroom.inventory.repository.PurchaseRepository;
import com.showroom.inventory.repository.SupplierRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service class handling Purchase orders and Restocking logic.
 * Every purchase is executed in a database transaction to ensure stock
 * levels and purchase records remain completely consistent.
 */
@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final ActivityLogService activityLogService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           SupplierRepository supplierRepository,
                           ProductRepository productRepository,
                           InventoryService inventoryService,
                           ActivityLogService activityLogService) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesBySupplier(Long supplierId) {
        if (supplierId == null) {
            return purchaseRepository.findAllWithDetails();
        }
        return purchaseRepository.findBySupplierIdWithDetails(supplierId);
    }

    @Transactional(readOnly = true)
    public List<Purchase> getRecentPurchases(int limit) {
        return purchaseRepository.findRecentPurchases(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Purchase getPurchaseById(Long id) {
        return purchaseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", id));
    }

    public Purchase getPurchaseByNumber(String purchaseNumber) {
        return purchaseRepository.findByPurchaseNumber(purchaseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "purchaseNumber", purchaseNumber));
    }

    @Transactional
    public Purchase createPurchase(PurchaseDTO dto) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", dto.getSupplierId()));

        String purchaseNumber = generateUniquePurchaseNumber();

        Purchase purchase = new Purchase();
        purchase.setPurchaseNumber(purchaseNumber);
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDate.now(SaleService.IST_ZONE));
        purchase.setNotes(dto.getNotes());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseItemDTO itemDto : dto.getItems()) {
            if (itemDto.getProductId() == null || itemDto.getQuantity() <= 0) {
                continue;
            }

            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemDto.getProductId()));

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.calculateTotalPrice();

            purchase.addItem(item);
            totalAmount = totalAmount.add(item.getTotalPrice());

            // Update product's latest purchase price
            product.setPurchasePrice(itemDto.getUnitPrice());

            // Increase product stock and record movement
            inventoryService.recordPurchaseStockIncrease(product, itemDto.getQuantity(), purchaseNumber);
        }

        purchase.setTotalAmount(totalAmount);
        com.showroom.inventory.entity.User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser != null) {
            purchase.setCreatedBy(currentUser.getUsername());
            purchase.setCreatedByName(currentUser.getFullName());
        }
        Purchase saved = purchaseRepository.save(purchase);
        activityLogService.logCurrentUserAction(
                "CREATE_PURCHASE",
                "PURCHASES",
                "Recorded purchase order " + saved.getPurchaseNumber() + " from supplier '" + supplier.getName() + "'",
                "None",
                "PO: " + saved.getPurchaseNumber() + ", Total Amount: ₹" + saved.getTotalAmount() + ", Units: " + saved.getTotalQuantity()
        );
        return saved;
    }

    private String generateUniquePurchaseNumber() {
        String dateStr = LocalDate.now(SaleService.IST_ZONE).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String poNumber;
        do {
            int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
            poNumber = "PO-" + dateStr + "-" + randomSuffix;
        } while (purchaseRepository.findByPurchaseNumber(poNumber).isPresent());
        return poNumber;
    }
}
