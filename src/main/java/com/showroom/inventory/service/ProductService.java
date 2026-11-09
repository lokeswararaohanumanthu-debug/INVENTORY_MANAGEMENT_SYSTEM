package com.showroom.inventory.service;

import com.showroom.inventory.dto.ProductDTO;
import com.showroom.inventory.entity.Product;
import com.showroom.inventory.enums.Category;
import com.showroom.inventory.exception.DuplicateBarcodeException;
import com.showroom.inventory.exception.DuplicateProductException;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class handling Product business logic.
 * Encapsulates CRUD operations, search, duplicate code validation, and stock thresholds.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ActivityLogService activityLogService;

    public ProductService(ProductRepository productRepository, ActivityLogService activityLogService) {
        this.productRepository = productRepository;
        this.activityLogService = activityLogService;
    }

    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    public Product getProductByCode(String code) {
        return productRepository.findByProductCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", code));
    }

    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveProducts();
        }
        return productRepository.searchProducts(keyword.trim());
    }

    public List<Product> getProductsByCategory(Category category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public List<Product> getOutOfStockProducts() {
        return productRepository.findOutOfStockProducts();
    }

    public long getTotalProductCount() {
        return productRepository.countByActiveTrue();
    }

    public long getTotalStockQuantity() {
        return productRepository.sumTotalStock();
    }

    @Transactional
    public Product createProduct(ProductDTO dto) {
        if (productRepository.existsByProductCode(dto.getProductCode())) {
            throw new DuplicateProductException(dto.getProductCode());
        }
        ensureBarcodeIsUnique(dto.getBarcode(), null);

        Product product = new Product();
        mapDtoToEntity(dto, product);
        Product saved = productRepository.save(product);

        activityLogService.logCurrentUserAction(
                "CREATE_PRODUCT",
                "PRODUCTS",
                "Added new product '" + saved.getName() + "' (" + saved.getProductCode() + ")",
                "None",
                "Code: " + saved.getProductCode() + ", Cost: ₹" + saved.getPurchasePrice() + ", Selling: ₹" + saved.getSellingPrice() + ", Qty: " + saved.getQuantity()
        );
        return saved;
    }

    @Transactional
    public Product updateProduct(Long id, ProductDTO dto) {
        Product existing = getProductById(id);

        // If code changed, check uniqueness
        if (!existing.getProductCode().equalsIgnoreCase(dto.getProductCode())
                && productRepository.existsByProductCode(dto.getProductCode())) {
            throw new DuplicateProductException(dto.getProductCode());
        }
        ensureBarcodeIsUnique(dto.getBarcode(), id);

        String prevVal = "Name: " + existing.getName() + ", Cost: ₹" + existing.getPurchasePrice() + ", Selling: ₹" + existing.getSellingPrice() + ", Qty: " + existing.getQuantity();
        mapDtoToEntity(dto, existing);
        Product saved = productRepository.save(existing);
        String newVal = "Name: " + saved.getName() + ", Cost: ₹" + saved.getPurchasePrice() + ", Selling: ₹" + saved.getSellingPrice() + ", Qty: " + saved.getQuantity();

        activityLogService.logCurrentUserAction(
                "UPDATE_PRODUCT",
                "PRODUCTS",
                "Updated product '" + saved.getName() + "' (" + saved.getProductCode() + ")",
                prevVal,
                newVal
        );
        return saved;
    }

    /**
     * Looks up an active product for the POS billing counter using a scanned / typed barcode.
     * Matches the manufacturer barcode first, then falls back to the showroom product code (SKU)
     * so that Code-128 labels printed with the SKU are also recognised.
     *
     * @param code the scanned or manually entered barcode value
     * @return the matching active product, if any
     */
    public Optional<Product> findActiveProductByBarcode(String code) {
        String value = normalizeBarcode(code);
        if (value == null) {
            return Optional.empty();
        }
        Optional<Product> match = productRepository.findByBarcode(value);
        if (match.isEmpty()) {
            match = productRepository.findByProductCodeIgnoreCase(value);
        }
        return match.filter(Product::isActive);
    }

    private void ensureBarcodeIsUnique(String barcode, Long currentProductId) {
        String value = normalizeBarcode(barcode);
        if (value == null) {
            return;
        }
        productRepository.findByBarcode(value).ifPresent(other -> {
            if (currentProductId == null || !other.getId().equals(currentProductId)) {
                throw new DuplicateBarcodeException(value);
            }
        });
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null) {
            return null;
        }
        String trimmed = barcode.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
        activityLogService.logCurrentUserAction(
                "DEACTIVATE_PRODUCT",
                "PRODUCTS",
                "Deactivated product '" + product.getName() + "' (" + product.getProductCode() + ")",
                "Status: ACTIVE",
                "Status: INACTIVE"
        );
    }

    @Transactional
    public void reactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(true);
        productRepository.save(product);
        activityLogService.logCurrentUserAction(
                "REACTIVATE_PRODUCT",
                "PRODUCTS",
                "Reactivated product '" + product.getName() + "' (" + product.getProductCode() + ")",
                "Status: INACTIVE",
                "Status: ACTIVE"
        );
    }

    private void mapDtoToEntity(ProductDTO dto, Product product) {
        product.setProductCode(dto.getProductCode().trim().toUpperCase());
        product.setBarcode(normalizeBarcode(dto.getBarcode()));
        product.setName(dto.getName().trim());
        product.setBrand(dto.getBrand().trim());
        product.setModel(dto.getModel() != null ? dto.getModel().trim() : null);
        product.setCategory(dto.getCategory());
        product.setPurchasePrice(dto.getPurchasePrice());
        product.setSellingPrice(dto.getSellingPrice());
        product.setQuantity(dto.getQuantity());
        product.setMinStockLevel(dto.getMinStockLevel());
        product.setDescription(dto.getDescription());
        product.setActive(dto.isActive());
    }

    public ProductDTO convertToDto(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setProductCode(product.getProductCode());
        dto.setBarcode(product.getBarcode());
        dto.setName(product.getName());
        dto.setBrand(product.getBrand());
        dto.setModel(product.getModel());
        dto.setCategory(product.getCategory());
        dto.setPurchasePrice(product.getPurchasePrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setQuantity(product.getQuantity());
        dto.setMinStockLevel(product.getMinStockLevel());
        dto.setDescription(product.getDescription());
        dto.setActive(product.isActive());
        return dto;
    }
}
