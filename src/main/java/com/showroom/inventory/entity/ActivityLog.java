package com.showroom.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Immutable Audit / Activity Log Entity.
 * Records employee actions, affected modules, timestamps, and before/after values.
 */
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String employeeName;

    @Column(nullable = false, length = 50)
    private String userRole;

    @Column(nullable = false, length = 80)
    private String actionPerformed;

    @Column(nullable = false, length = 60)
    private String moduleAffected;

    @Column(length = 600)
    private String description;

    @Column(length = 1200)
    private String previousValue;

    @Column(length = 1200)
    private String updatedValue;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        }
    }

    public ActivityLog() {
    }

    public ActivityLog(String username,
                       String employeeName,
                       String userRole,
                       String actionPerformed,
                       String moduleAffected,
                       String description,
                       String previousValue,
                       String updatedValue) {
        this.username = username;
        this.employeeName = employeeName;
        this.userRole = userRole;
        this.actionPerformed = actionPerformed;
        this.moduleAffected = moduleAffected;
        this.description = description;
        this.previousValue = previousValue;
        this.updatedValue = updatedValue;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getActionPerformed() {
        return actionPerformed;
    }

    public void setActionPerformed(String actionPerformed) {
        this.actionPerformed = actionPerformed;
    }

    public String getModuleAffected() {
        return moduleAffected;
    }

    public void setModuleAffected(String moduleAffected) {
        this.moduleAffected = moduleAffected;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }

    public String getUpdatedValue() {
        return updatedValue;
    }

    public void setUpdatedValue(String updatedValue) {
        this.updatedValue = updatedValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

