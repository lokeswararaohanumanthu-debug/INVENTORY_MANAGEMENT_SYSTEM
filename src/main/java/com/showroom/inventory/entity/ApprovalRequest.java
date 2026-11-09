package com.showroom.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * ApprovalRequest Entity — Supports multi-role approval workflows for sensitive operations:
 * - Large stock adjustments
 * - High-value purchase approvals
 * - Product deletion requests
 * - Important financial corrections
 */
@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String requestCode;

    /** LARGE_STOCK_ADJUSTMENT, PURCHASE_APPROVAL, PRODUCT_DELETION, FINANCIAL_CORRECTION */
    @Column(nullable = false, length = 60)
    private String operationType;

    /** INVENTORY, PURCHASES, PRODUCTS, FINANCE */
    @Column(nullable = false, length = 50)
    private String moduleAffected;

    @Column
    private Long targetEntityId;

    @Column(nullable = false, length = 200)
    private String targetIdentifier;

    @Column(length = 800)
    private String payloadData;

    @Column(nullable = false, length = 600)
    private String reason;

    /** PENDING, APPROVED, REJECTED */
    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(nullable = false, length = 50)
    private String requestedByUsername;

    @Column(nullable = false, length = 100)
    private String requestedByName;

    @Column(nullable = false, length = 50)
    private String requestedByRole;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column(length = 50)
    private String reviewedByUsername;

    @Column(length = 100)
    private String reviewedByName;

    @Column(length = 50)
    private String reviewedByRole;

    @Column
    private LocalDateTime reviewedAt;

    @Column(length = 600)
    private String reviewRemarks;

    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }

    public ApprovalRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getModuleAffected() {
        return moduleAffected;
    }

    public void setModuleAffected(String moduleAffected) {
        this.moduleAffected = moduleAffected;
    }

    public Long getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(Long targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public String getPayloadData() {
        return payloadData;
    }

    public void setPayloadData(String payloadData) {
        this.payloadData = payloadData;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public String getRequestedByRole() {
        return requestedByRole;
    }

    public void setRequestedByRole(String requestedByRole) {
        this.requestedByRole = requestedByRole;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getReviewedByUsername() {
        return reviewedByUsername;
    }

    public void setReviewedByUsername(String reviewedByUsername) {
        this.reviewedByUsername = reviewedByUsername;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public String getReviewedByRole() {
        return reviewedByRole;
    }

    public void setReviewedByRole(String reviewedByRole) {
        this.reviewedByRole = reviewedByRole;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewRemarks() {
        return reviewRemarks;
    }

    public void setReviewRemarks(String reviewRemarks) {
        this.reviewRemarks = reviewRemarks;
    }
}

