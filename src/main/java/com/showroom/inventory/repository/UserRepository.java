package com.showroom.inventory.repository;

import com.showroom.inventory.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity.
 * Provides CRUD operations and custom query methods for managing
 * user accounts and authentication in the showroom system.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their username.
     * Used during authentication to look up user credentials.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a user with the given username already exists.
     * Used for duplicate validation before creating a new user.
     *
     * @param username the username to check
     * @return true if a user with the given username exists
     */
    boolean existsByUsername(String username);
}
