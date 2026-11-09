package com.showroom.inventory.service;

import com.showroom.inventory.entity.ApprovalRequest;
import com.showroom.inventory.entity.Product;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.enums.UserRole;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.ApprovalRequestRepository;
import com.showroom.inventory.repository.ProductRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service handling sensitive operation Approval Workflows:
 * - Large Stock Adjustments
 * - Purchase Approvals
 * - Product Deletion Requests
 * - Financial Corrections
 */
@Service
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final ActivityLogService activityLogService;

    public ApprovalService(ApprovalRequestRepository approvalRequestRepository,
                           InventoryService inventoryService,
                           ProductRepository productRepository,
                           ActivityLogService activityLogService) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public ApprovalRequest createRequest(User requester,
                                         String operationType,
                                         String moduleAffected,
                                         Long targetEntityId,
                                         String targetIdentifier,
                                         String payloadData,
                                         String reason) {
        if (requester == null) {
            throw new AccessDeniedException("Authentication required to submit an approval request.");
        }

        ApprovalRequest req = new ApprovalRequest();
        req.setRequestCode(generateUniqueCode());
        req.setOperationType(operationType);
        req.setModuleAffected(moduleAffected);
        req.setTargetEntityId(targetEntityId);
        req.setTargetIdentifier(targetIdentifier);
        req.setPayloadData(payloadData);
        req.setReason(reason != null && !reason.isBlank() ? reason.trim() : "Requires management authorization");
        req.setStatus("PENDING");
        req.setRequestedByUsername(requester.getUsername());
        req.setRequestedByName(requester.getFullName());
        req.setRequestedByRole(requester.getRole().getDisplayName());
        req.setRequestedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

        ApprovalRequest saved = approvalRequestRepository.save(req);

        activityLogService.logAction(
                requester,
                "APPROVAL_REQUESTED",
                moduleAffected,
                "Submitted approval request " + saved.getRequestCode() + " (" + operationType + ") for " + targetIdentifier,
                "Status: NONE",
                "Status: PENDING | Reason: " + req.getReason()
        );

        return saved;
    }

    @Transactional
    public ApprovalRequest decideRequest(Long requestId,
                                         String decision,
                                         String reviewRemarks,
                                         User reviewer) {
        if (reviewer == null || (!reviewer.hasPermission(Permission.APPROVAL_DECIDE)
                && !reviewer.hasPermission(Permission.STOCK_APPROVE)
                && !reviewer.hasPermission(Permission.PURCHASE_APPROVE))) {
            throw new AccessDeniedException("You are not authorized to approve or reject sensitive operations.");
        }

        ApprovalRequest req = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ApprovalRequest", "id", requestId));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalArgumentException("Approval request " + req.getRequestCode() + " has already been " + req.getStatus() + ".");
        }

        boolean approve = "APPROVE".equalsIgnoreCase(decision) || "APPROVED".equalsIgnoreCase(decision);
        String newStatus = approve ? "APPROVED" : "REJECTED";

        req.setStatus(newStatus);
        req.setReviewedByUsername(reviewer.getUsername());
        req.setReviewedByName(reviewer.getFullName());
        req.setReviewedByRole(reviewer.getRole().getDisplayName());
        req.setReviewedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        req.setReviewRemarks(reviewRemarks != null && !reviewRemarks.isBlank()
                ? reviewRemarks.trim()
                : (approve ? "Approved by " + reviewer.getFullName() : "Rejected by " + reviewer.getFullName()));

        // Execute underlying action if approved
        if (approve) {
            if ("LARGE_STOCK_ADJUSTMENT".equalsIgnoreCase(req.getOperationType())
                    && req.getTargetEntityId() != null
                    && req.getPayloadData() != null
                    && req.getPayloadData().startsWith("NEW_QTY:")) {
                try {
                    int newQty = Integer.parseInt(req.getPayloadData().substring("NEW_QTY:".length()).trim());
                    inventoryService.adjustStock(
                            req.getTargetEntityId(),
                            newQty,
                            "Approved via " + req.getRequestCode() + " by " + reviewer.getUsername() + " (" + req.getReason() + ")"
                    );
                } catch (NumberFormatException ignored) {
                }
            } else if ("PRODUCT_DELETION".equalsIgnoreCase(req.getOperationType()) && req.getTargetEntityId() != null) {
                productRepository.findById(req.getTargetEntityId()).ifPresent(product -> {
                    // Preserve historical integrity by archiving/deactivating product master record
                    product.setActive(false);
                    productRepository.save(product);
                });
            }
        }

        ApprovalRequest saved = approvalRequestRepository.save(req);

        activityLogService.logAction(
                reviewer,
                "APPROVAL_" + newStatus,
                req.getModuleAffected(),
                newStatus + " request " + req.getRequestCode() + " (" + req.getOperationType() + ") for " + req.getTargetIdentifier(),
                "Status: PENDING",
                "Status: " + newStatus + " | Remarks: " + req.getReviewRemarks()
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getVisibleRequestsFor(User user, String statusFilter) {
        if (user == null) {
            return List.of();
        }
        List<ApprovalRequest> list;
        if (user.getRole() == UserRole.SUPER_ADMIN
                || user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.SHOWROOM_MANAGER
                || user.hasPermission(Permission.APPROVAL_DECIDE)) {
            list = approvalRequestRepository.findAllByOrderByRequestedAtDesc();
        } else {
            list = approvalRequestRepository.findByRequestedByUsernameIgnoreCaseOrderByRequestedAtDesc(user.getUsername());
        }

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            String normalized = statusFilter.trim().toUpperCase();
            list = list.stream().filter(r -> normalized.equalsIgnoreCase(r.getStatus())).toList();
        }
        return list;
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return approvalRequestRepository.countByStatus("PENDING");
    }

    private String generateUniqueCode() {
        String datePart = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            code = "APR-" + datePart + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
        } while (approvalRequestRepository.findByRequestCode(code).isPresent());
        return code;
    }
}

