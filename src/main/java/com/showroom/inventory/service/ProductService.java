package com.showroom.inventory.service;

import com.showroom.inventory.dto.ProductDTO;
import com.showroom.inventory.entity.Product;
import com.showroom.inventory.enums.Category;
import com.showroom.inventory.exception.DuplicateProductException;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling Product business logic.
 * Encapsulates CRUD operations, search, duplicate code validation, and stock thresholds.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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

        Product product = new Product();
        mapDtoToEntity(dto, product);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, ProductDTO dto) {
        Product existing = getProductById(id);

        // If code changed, check uniqueness
        if (!existing.getProductCode().equalsIgnoreCase(dto.getProductCode())
                && productRepository.existsByProductCode(dto.getProductCode())) {
            throw new DuplicateProductException(dto.getProductCode());
        }

        mapDtoToEntity(dto, existing);
        return productRepository.save(existing);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public void reactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(true);
        productRepository.save(product);
    }

    private void mapDtoToEntity(ProductDTO dto, Product product) {
        product.setProductCode(dto.getProductCode().trim().toUpperCase());
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
