package com.showroom.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.enums.UserRole;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * User Entity — Represents an employee account in the Showroom RBAC system.
 *
 * Security notes:
 *   - The password field stores a BCrypt hash, NEVER the plaintext password.
 *   - Role + optional custom permission overrides determine granular authorities.
 *   - Super Admin accounts are protected from unauthorized modification or deletion.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 100)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private UserRole role;

    @Column
    private boolean active = true;

    @Column
    private Boolean protectedAccount = false;

    @Column
    private Boolean passwordChangeRequired = false;

    @Column(length = 1500)
    private String customGrantedPermissions;

    @Column(length = 1500)
    private String customRevokedPermissions;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        }
        if (this.protectedAccount == null) {
            this.protectedAccount = (this.role == UserRole.SUPER_ADMIN);
        }
        if (this.passwordChangeRequired == null) {
            this.passwordChangeRequired = false;
        }
    }

    public User() {
    }

    public User(String username, String password, String fullName,
                String email, UserRole role, boolean active) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
        this.protectedAccount = (role == UserRole.SUPER_ADMIN);
    }

    /**
     * Computes the effective set of granular permissions for this employee.
     * Super Admin always retains all permissions and cannot be restricted.
     */
    public Set<Permission> getEffectivePermissions() {
        if (this.role == null) {
            return Collections.emptySet();
        }
        if (this.role == UserRole.SUPER_ADMIN) {
            return EnumSet.allOf(Permission.class);
        }
        EnumSet<Permission> effective = EnumSet.copyOf(this.role.getDefaultPermissions());
        if (this.customGrantedPermissions != null && !this.customGrantedPermissions.isBlank()) {
            for (String token : this.customGrantedPermissions.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        effective.add(Permission.valueOf(trimmed));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        if (this.customRevokedPermissions != null && !this.customRevokedPermissions.isBlank()) {
            for (String token : this.customRevokedPermissions.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        effective.remove(Permission.valueOf(trimmed));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        return Collections.unmodifiableSet(effective);
    }

    public boolean hasPermission(Permission permission) {
        if (permission == null) {
            return false;
        }
        return getEffectivePermissions().contains(permission);
    }

    public boolean hasPermission(String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            return false;
        }
        try {
            return hasPermission(Permission.valueOf(permissionName.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isSuperAdmin() {
        return this.role == UserRole.SUPER_ADMIN;
    }

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

    @JsonIgnore
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public boolean isProtectedAccount() {
        return Boolean.TRUE.equals(protectedAccount) || this.role == UserRole.SUPER_ADMIN;
    }

    public void setProtectedAccount(Boolean protectedAccount) {
        this.protectedAccount = protectedAccount;
    }

    public boolean isPasswordChangeRequired() {
        return Boolean.TRUE.equals(passwordChangeRequired);
    }

    public void setPasswordChangeRequired(Boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public String getCustomGrantedPermissions() {
        return customGrantedPermissions;
    }

    public void setCustomGrantedPermissions(String customGrantedPermissions) {
        this.customGrantedPermissions = customGrantedPermissions;
    }

    public String getCustomRevokedPermissions() {
        return customRevokedPermissions;
    }

    public void setCustomRevokedPermissions(String customRevokedPermissions) {
        this.customRevokedPermissions = customRevokedPermissions;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
