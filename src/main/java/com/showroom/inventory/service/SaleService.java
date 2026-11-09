package com.showroom.inventory.service;

import com.showroom.inventory.dto.SaleDTO;
import com.showroom.inventory.dto.SaleItemDTO;
import com.showroom.inventory.entity.Product;
import com.showroom.inventory.entity.Sale;
import com.showroom.inventory.entity.SaleItem;
import com.showroom.inventory.exception.InsufficientStockException;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.ProductRepository;
import com.showroom.inventory.repository.SaleItemRepository;
import com.showroom.inventory.repository.SaleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service class handling Sales and Customer Billing logic.
 *
 * Implements strict transaction safety:
 * 1. Checks available stock for all requested items BEFORE deducting.
 * 2. Deducts stock and records stock movements.
 * 3. Records selling price and cost price on each item to accurately compute
 *    Cost of Goods Sold (COGS) and Gross Profit later.
 */
@Service
public class SaleService {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final ActivityLogService activityLogService;

    public SaleService(SaleRepository saleRepository,
                       SaleItemRepository saleItemRepository,
                       ProductRepository productRepository,
                       InventoryService inventoryService,
                       ActivityLogService activityLogService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        return saleRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Sale> getSalesBetween(LocalDate start, LocalDate end) {
        return saleRepository.findBySaleDateBetweenWithDetails(start, end);
    }

    @Transactional(readOnly = true)
    public List<Sale> getRecentSales(int limit) {
        return saleRepository.findRecentSales(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Sale getSaleById(Long id) {
        return saleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", id));
    }

    @Transactional(readOnly = true)
    public Sale getSaleByBillNumber(String billNumber) {
        return saleRepository.findByBillNumber(billNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "billNumber", billNumber));
    }

    @Transactional(readOnly = true)
    public BigDecimal getTodaySalesTotal() {
        return saleRepository.sumNetAmountByDate(LocalDate.now(IST_ZONE));
    }

    public BigDecimal getMonthlyRevenue(LocalDate start, LocalDate end) {
        return saleRepository.sumNetAmountBetween(start, end);
    }

    public List<Object[]> getBestSellingProducts(int limit) {
        return saleItemRepository.findBestSellingProducts(PageRequest.of(0, limit));
    }

    public List<Object[]> getSlowMovingProducts(int limit) {
        return saleItemRepository.findSlowMovingProducts(PageRequest.of(0, limit));
    }

    @Transactional
    public Sale createSale(SaleDTO dto) {
        // Step 1: Pre-validate that all products exist and have sufficient stock
        for (SaleItemDTO itemDto : dto.getItems()) {
            if (itemDto.getProductId() == null || itemDto.getQuantity() <= 0) {
                continue;
            }
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemDto.getProductId()));

            if (product.getQuantity() < itemDto.getQuantity()) {
                throw new InsufficientStockException(product.getName(), product.getQuantity(), itemDto.getQuantity());
            }
        }

        // Step 2: Prepare Sale entity with permanent Asia/Kolkata transaction date & timestamp
        LocalDateTime nowIst = LocalDateTime.now(IST_ZONE);
        LocalDate actualSaleDate = dto.getSaleDate() != null ? dto.getSaleDate() : nowIst.toLocalDate();
        String billNumber = generateUniqueBillNumber(actualSaleDate);

        Sale sale = new Sale();
        sale.setBillNumber(billNumber);
        sale.setCustomerName(dto.getCustomerName() != null && !dto.getCustomerName().trim().isEmpty() ? dto.getCustomerName().trim() : "Walk-in Customer");
        sale.setCustomerPhone(dto.getCustomerPhone());
        sale.setSaleDate(actualSaleDate);
        sale.setCreatedAt(nowIst);
        sale.setPaymentMethod(dto.getPaymentMethod() != null ? dto.getPaymentMethod() : "CASH");
        sale.setNotes(dto.getNotes());
        sale.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO);

        BigDecimal subtotal = BigDecimal.ZERO;

        // Step 3: Attach items and deduct stock
        for (SaleItemDTO itemDto : dto.getItems()) {
            if (itemDto.getProductId() == null || itemDto.getQuantity() <= 0) {
                continue;
            }
            Product product = productRepository.findById(itemDto.getProductId()).get();

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setCostPrice(product.getPurchasePrice()); // Captures exact cost at time of sale for COGS!
            item.calculateTotalPrice();

            sale.addItem(item);
            subtotal = subtotal.add(item.getTotalPrice());

            // Deduct stock and record movement
            inventoryService.recordSaleStockDecrease(product, itemDto.getQuantity(), billNumber);
        }

        sale.setTotalAmount(subtotal);
        BigDecimal net = subtotal.subtract(sale.getDiscount());
        sale.setNetAmount(net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net);

        com.showroom.inventory.entity.User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser != null) {
            sale.setCreatedBy(currentUser.getUsername());
            sale.setCreatedByName(currentUser.getFullName());
        }

        Sale saved = saleRepository.save(sale);
        activityLogService.logCurrentUserAction(
                "CREATE_SALE",
                "SALES_POS",
                "Generated sales invoice " + saved.getBillNumber() + " for customer '" + saved.getCustomerName() + "'",
                "None",
                "Bill: " + saved.getBillNumber() + ", Net Paid: ₹" + saved.getNetAmount() + ", Payment: " + saved.getPaymentMethod()
        );
        return saved;
    }

    private String generateUniqueBillNumber(LocalDate saleDate) {
        LocalDate targetDate = saleDate != null ? saleDate : LocalDate.now(IST_ZONE);
        String dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String billNum;
        do {
            int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 9999);
            billNum = "BILL-" + dateStr + "-" + randomSuffix;
        } while (saleRepository.findByBillNumber(billNum).isPresent());
        return billNum;
    }
}
