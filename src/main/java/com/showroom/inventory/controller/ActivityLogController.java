package com.showroom.inventory.controller;

import com.showroom.inventory.entity.ActivityLog;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.service.ActivityLogService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller serving the immutable Activity Log / Audit Trail module.
 * Read-only by design: activity logs cannot be modified or deleted by any user.
 */
@Controller
@RequestMapping("/activity-logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public String viewActivityLogs(@RequestParam(value = "module", required = false, defaultValue = "ALL") String module,
                                   @RequestParam(value = "search", required = false) String search,
                                   Model model) {
        User viewer = activityLogService.getCurrentAuthenticatedUser();
        if (viewer == null) {
            throw new AccessDeniedException("Authentication required to view activity logs.");
        }

        List<ActivityLog> logs = activityLogService.getVisibleLogsFor(viewer, module, search);
        String scopeDesc = viewer.hasPermission(Permission.ACTIVITY_LOG_VIEW_ALL)
                ? "Viewing All Showroom Employee Activity Logs"
                : "Viewing My Own Activity Logs (@" + viewer.getUsername() + ")";

        model.addAttribute("logs", logs);
        model.addAttribute("totalLogsCount", logs.size());
        model.addAttribute("scopeDescription", scopeDesc);
        model.addAttribute("modules", List.of("AUTHENTICATION", "USERS_RBAC", "PRODUCTS", "SALES", "PURCHASES", "INVENTORY", "SUPPLIERS", "EXPENSES", "APPROVALS", "SETTINGS"));
        model.addAttribute("selectedModule", module != null ? module.toUpperCase() : "ALL");
        model.addAttribute("searchQuery", search != null ? search.trim() : "");
        model.addAttribute("searchKeyword", search != null ? search.trim() : "");
        model.addAttribute("activeNav", "activity-logs");
        return "activity-logs/list";
    }
}
