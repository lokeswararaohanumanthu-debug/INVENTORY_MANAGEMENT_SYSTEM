package com.showroom.inventory.config;

import com.showroom.inventory.entity.User;
import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.ApprovalService;
import com.showroom.inventory.service.ProductService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Populates common RBAC and sidebar badge model attributes
 * (currentUser, pendingApprovalsCount, lowStockCount) for all views.
 */
@ControllerAdvice
public class RbacModelAdvice {

    private final ActivityLogService activityLogService;
    private final ApprovalService approvalService;
    private final ProductService productService;

    public RbacModelAdvice(ActivityLogService activityLogService,
                           ApprovalService approvalService,
                           ProductService productService) {
        this.activityLogService = activityLogService;
        this.approvalService = approvalService;
        this.productService = productService;
    }

    @ModelAttribute("currentUser")
    public User populateCurrentUser() {
        return activityLogService.getCurrentAuthenticatedUser();
    }

    @ModelAttribute("pendingApprovalsCount")
    public long populatePendingApprovalsCount() {
        User user = activityLogService.getCurrentAuthenticatedUser();
        if (user == null) {
            return 0L;
        }
        return approvalService.getPendingCount();
    }

    @ModelAttribute("lowStockCount")
    public int populateLowStockCount() {
        try {
            return productService.getLowStockProducts().size();
        } catch (Exception ex) {
            return 0;
        }
    }
}

