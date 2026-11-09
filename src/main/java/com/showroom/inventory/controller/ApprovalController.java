package com.showroom.inventory.controller;

import com.showroom.inventory.entity.ApprovalRequest;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.ApprovalService;
import com.showroom.inventory.service.ProductService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling Sensitive Operation Approval Workflows:
 * - Large Stock Adjustments
 * - Purchase Approvals
 * - Product Deletions
 * - Financial Corrections
 */
@Controller
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final ActivityLogService activityLogService;
    private final ProductService productService;

    public ApprovalController(ApprovalService approvalService,
                              ActivityLogService activityLogService,
                              ProductService productService) {
        this.approvalService = approvalService;
        this.activityLogService = activityLogService;
        this.productService = productService;
    }

    @GetMapping
    public String listApprovals(@RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                                Model model) {
        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required.");
        }

        List<ApprovalRequest> allVisible = approvalService.getVisibleRequestsFor(currentUser, "ALL");
        List<ApprovalRequest> requests = approvalService.getVisibleRequestsFor(currentUser, status);

        long pendingCount = allVisible.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long approvedCount = allVisible.stream().filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus())).count();
        long rejectedCount = allVisible.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus())).count();

        model.addAttribute("requests", requests);
        model.addAttribute("approvals", requests);
        model.addAttribute("totalCount", allVisible.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("canManageApprovals", currentUser.hasPermission(Permission.APPROVAL_DECIDE));
        model.addAttribute("selectedStatus", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("activeNav", "approvals");
        return "approvals/list";
    }

    @PostMapping("/request")
    public String submitApprovalRequest(@RequestParam("operationType") String operationType,
                                        @RequestParam(value = "moduleAffected", required = false, defaultValue = "OPERATIONS") String moduleAffected,
                                        @RequestParam(value = "targetEntityId", required = false) Long targetEntityId,
                                        @RequestParam(value = "targetIdentifier", required = false) String targetIdentifier,
                                        @RequestParam(value = "targetDescription", required = false) String targetDescription,
                                        @RequestParam(value = "payloadData", required = false) String payloadData,
                                        @RequestParam(value = "reason", required = false) String reason,
                                        @RequestParam(value = "details", required = false) String details,
                                        RedirectAttributes redirectAttributes) {
        User requester = activityLogService.getCurrentAuthenticatedUser();
        String effectiveTarget = (targetIdentifier != null && !targetIdentifier.isBlank()) ? targetIdentifier : targetDescription;
        String effectiveReason = (reason != null && !reason.isBlank()) ? reason : details;
        ApprovalRequest created = approvalService.createRequest(
                requester, operationType, moduleAffected, targetEntityId, effectiveTarget, payloadData, effectiveReason
        );
        redirectAttributes.addFlashAttribute("successMessage",
                "Approval request " + created.getRequestCode() + " submitted for review.");
        return "redirect:/approvals";
    }

    @PostMapping("/decide/{id}")
    public String decideApproval(@PathVariable Long id,
                                 @RequestParam("decision") String decision,
                                 @RequestParam(value = "reviewRemarks", required = false) String reviewRemarks,
                                 RedirectAttributes redirectAttributes) {
        User reviewer = activityLogService.getCurrentAuthenticatedUser();
        ApprovalRequest updated = approvalService.decideRequest(id, decision, reviewRemarks, reviewer);
        redirectAttributes.addFlashAttribute("successMessage",
                "Approval request " + updated.getRequestCode() + " has been " + updated.getStatus() + ".");
        return "redirect:/approvals";
    }

    @PostMapping("/approve/{id}")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam(value = "remarks", required = false) String remarks,
                                 RedirectAttributes redirectAttributes) {
        User reviewer = activityLogService.getCurrentAuthenticatedUser();
        ApprovalRequest updated = approvalService.decideRequest(id, "APPROVED", remarks, reviewer);
        redirectAttributes.addFlashAttribute("successMessage",
                "Approval request " + updated.getRequestCode() + " has been APPROVED.");
        return "redirect:/approvals";
    }

    @PostMapping("/reject/{id}")
    public String rejectRequest(@PathVariable Long id,
                                @RequestParam(value = "remarks", required = false) String remarks,
                                RedirectAttributes redirectAttributes) {
        User reviewer = activityLogService.getCurrentAuthenticatedUser();
        ApprovalRequest updated = approvalService.decideRequest(id, "REJECTED", remarks, reviewer);
        redirectAttributes.addFlashAttribute("successMessage",
                "Approval request " + updated.getRequestCode() + " has been REJECTED.");
        return "redirect:/approvals";
    }
}
