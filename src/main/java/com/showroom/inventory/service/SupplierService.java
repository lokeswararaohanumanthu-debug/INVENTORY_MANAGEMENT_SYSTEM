package com.showroom.inventory.service;

import com.showroom.inventory.entity.Supplier;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for Supplier operations.
 */
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> getAllActiveSuppliers() {
        return supplierRepository.findByActiveTrue();
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
    }

    public List<Supplier> searchSuppliers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveSuppliers();
        }
        return supplierRepository.findByNameContainingIgnoreCase(keyword.trim());
    }

    public long getActiveSupplierCount() {
        return supplierRepository.countByActiveTrue();
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deactivateSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }
}
