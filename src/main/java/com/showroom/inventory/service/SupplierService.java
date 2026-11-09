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
    private final ActivityLogService activityLogService;

    public SupplierService(SupplierRepository supplierRepository, ActivityLogService activityLogService) {
        this.supplierRepository = supplierRepository;
        this.activityLogService = activityLogService;
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
        boolean isNew = (supplier.getId() == null);
        Supplier saved = supplierRepository.save(supplier);
        activityLogService.logCurrentUserAction(
                isNew ? "CREATE_SUPPLIER" : "UPDATE_SUPPLIER",
                "SUPPLIERS",
                (isNew ? "Added new supplier '" : "Updated supplier '") + saved.getName() + "'",
                isNew ? "None" : "Supplier ID: " + saved.getId(),
                "Name: " + saved.getName() + ", Contact: " + saved.getContactPerson() + ", Phone: " + saved.getPhone()
        );
        return saved;
    }

    @Transactional
    public void deactivateSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        supplier.setActive(false);
        supplierRepository.save(supplier);
        activityLogService.logCurrentUserAction(
                "DEACTIVATE_SUPPLIER",
                "SUPPLIERS",
                "Deactivated supplier '" + supplier.getName() + "'",
                "Status: ACTIVE",
                "Status: INACTIVE"
        );
    }
}
