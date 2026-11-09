package com.showroom.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * SystemSetting Entity — Stores application and security configuration parameters.
 */
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 80)
    private String settingKey;

    @Column(nullable = false, length = 500)
    private String settingValue;

    @Column(length = 300)
    private String description;

    @Column(nullable = false)
    private boolean securityCritical = false;

    @Column(length = 60)
    private String updatedBy;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        }
    }

    public SystemSetting() {
    }

    public SystemSetting(String settingKey, String settingValue, String description, boolean securityCritical) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.securityCritical = securityCritical;
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    public Long getId() {
        return id;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSecurityCritical() {
        return securityCritical;
    }

    public void setSecurityCritical(boolean securityCritical) {
        this.securityCritical = securityCritical;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

