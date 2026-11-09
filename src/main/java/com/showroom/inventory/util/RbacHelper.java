package com.showroom.inventory.util;

import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.enums.UserRole;
import com.showroom.inventory.service.ActivityLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Thymeleaf & Controller helper bean ("@rbac") for checking granular permissions and roles.
 */
@Component("rbac")
public class RbacHelper {

    private final ActivityLogService activityLogService;

    public RbacHelper(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    public boolean has(String permissionName) {
        if (permissionName == null || permissionName.isBlank()) {
            return false;
        }
        String targetAuth = permissionName.startsWith("PERM_")
                ? permissionName.trim().toUpperCase()
                : "PERM_" + permissionName.trim().toUpperCase();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_SUPER_ADMIN".equals(ga.getAuthority()) || targetAuth.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public boolean has(Permission permission) {
        return permission != null && has(permission.name());
    }

    public boolean hasAny(String... permissionNames) {
        if (permissionNames == null) {
            return false;
        }
        for (String p : permissionNames) {
            if (has(p)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        String targetRole = roleName.startsWith("ROLE_")
                ? roleName.trim().toUpperCase()
                : "ROLE_" + roleName.trim().toUpperCase();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (targetRole.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public User getCurrentUser() {
        return activityLogService.getCurrentAuthenticatedUser();
    }

    public String getCurrentEmployeeName() {
        User u = getCurrentUser();
        return u != null ? u.getFullName() : "Employee";
    }

    public String getCurrentRoleDisplay() {
        User u = getCurrentUser();
        return (u != null && u.getRole() != null) ? u.getRole().getDisplayName() : "Authenticated User";
    }

    public String getCurrentRoleBadgeClass() {
        User u = getCurrentUser();
        return (u != null && u.getRole() != null) ? u.getRole().getBadgeClass() : "bg-secondary";
    }

    public String getAvatarInitials() {
        User u = getCurrentUser();
        if (u == null || u.getFullName() == null || u.getFullName().isBlank()) {
            return "U";
        }
        String[] parts = u.getFullName().trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    public boolean canManageTargetUser(User target) {
        User actor = getCurrentUser();
        if (actor == null || target == null) return false;
        if (actor.getRole() == UserRole.SUPER_ADMIN) return true;
        if (!has(Permission.USER_MANAGE)) return false;
        if (target.isSuperAdmin() || target.isProtectedAccount()) return false;
        return actor.getRole().getHierarchyLevel() > target.getRole().getHierarchyLevel();
    }
}

