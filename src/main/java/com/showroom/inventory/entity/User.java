package com.showroom.inventory.entity;

import com.showroom.inventory.enums.UserRole;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User Entity — Represents a system user (admin, manager, staff) who can
 * log in and perform operations in the inventory management system.
 *
 * Security notes:
 *   - The password field stores a BCrypt hash, NEVER the plaintext password.
 *   - BCrypt is a one-way hashing algorithm designed for passwords; it
 *     automatically includes a salt and is intentionally slow to resist
 *     brute-force attacks.
 *   - The role field determines what actions the user is authorized to perform.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Login username — must be unique across the system.
     * unique = true creates a UNIQUE index in the database,
     * preventing duplicate usernames at the DB level.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /**
     * BCrypt-hashed password.
     * The @Column has no length limit specified because BCrypt hashes
     * are typically 60 characters, and the default VARCHAR(255) is sufficient.
     */
    @Column(nullable = false)
    private String password;

    /** User's full display name — e.g., "Lokesh Kumar". */
    @Column(nullable = false, length = 100)
    private String fullName;

    /** User's email address — used for password resets and notifications. */
    @Column(length = 100)
    private String email;

    /**
     * User's role in the system.
     * Stored as a String in the database (e.g., "ADMIN", "MANAGER", "STAFF").
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /** Whether the user account is active. Inactive users cannot log in. */
    @Column
    private boolean active = true;

    /** Timestamp when this user account was created. */
    @Column
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────────────────────
    //  Lifecycle Callbacks
    // ──────────────────────────────────────────────────────────────

    /**
     * @PrePersist — Sets defaults before the first save:
     *   - createdAt timestamp
     *   - active defaults to true (user accounts are active on creation)
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (!this.active) {
            this.active = true;
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Constructors
    // ──────────────────────────────────────────────────────────────

    /** Default no-arg constructor — Required by JPA. */
    public User() {
    }

    /**
     * Parameterized constructor.
     * Excludes id (auto-generated) and createdAt (set by @PrePersist).
     */
    public User(String username, String password, String fullName,
                String email, UserRole role, boolean active) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    // ──────────────────────────────────────────────────────────────
    //  Getters and Setters
    // ──────────────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ──────────────────────────────────────────────────────────────
    //  toString — NEVER include the password hash in toString()
    //  to avoid accidentally logging sensitive information.
    // ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
