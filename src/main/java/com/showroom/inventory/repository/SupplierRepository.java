package com.showroom.inventory.repository;

import com.showroom.inventory.entity.Supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Supplier entity.
 * Provides CRUD operations and custom query methods for managing
 * suppliers in the mobile showroom system.
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Find all suppliers that are currently active (not soft-deleted).
     *
     * @return list of all active suppliers
     */
    List<Supplier> findByActiveTrue();

    /**
     * Search suppliers by name using case-insensitive partial matching.
     *
     * @param name the name substring to search for
     * @return list of suppliers whose names contain the given string
     */
    List<Supplier> findByNameContainingIgnoreCase(String name);

    /**
     * Count the total number of active suppliers.
     *
     * @return the count of active suppliers
     */
    long countByActiveTrue();
}
