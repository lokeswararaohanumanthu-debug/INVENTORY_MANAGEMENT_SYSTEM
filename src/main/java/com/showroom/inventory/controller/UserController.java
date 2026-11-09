package com.showroom.inventory.controller;

import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.enums.UserRole;
import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for Employee User Management, Role Assignment, Account Status,
 * Password Reset, and Granular Permission Customization.
 */
@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final ActivityLogService activityLogService;

    public UserController(UserService userService, ActivityLogService activityLogService) {
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public String listUsers(@RequestParam(value = "role", required = false) String roleParam,
                            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
                            @RequestParam(value = "search", required = false) String search,
                            Model model) {
        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser == null || !currentUser.hasPermission(Permission.USER_VIEW)) {
            throw new AccessDeniedException("You do not have permission to access User Management.");
        }

        UserRole roleFilter = null;
        if (roleParam != null && !roleParam.isBlank()) {
            try {
                roleFilter = UserRole.valueOf(roleParam.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        final UserRole finalRoleFilter = roleFilter;
        final String normalizedStatus = (status != null) ? status.trim().toUpperCase() : "ALL";

        List<User> allUsers = userService.getAllUsers();
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;

        List<User> filteredUsers = allUsers.stream()
                .filter(u -> finalRoleFilter == null || u.getRole() == finalRoleFilter)
                .filter(u -> {
                    if ("ACTIVE".equals(normalizedStatus)) return u.isActive();
                    if ("INACTIVE".equals(normalizedStatus)) return !u.isActive();
                    return true;
                })
                .filter(u -> {
                    if (trimmedSearch == null) return true;
                    return (u.getUsername() != null && u.getUsername().toLowerCase().contains(trimmedSearch))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(trimmedSearch))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(trimmedSearch))
                            || (u.getPhone() != null && u.getPhone().toLowerCase().contains(trimmedSearch))
                            || (u.getRole() != null && u.getRole().getDisplayName().toLowerCase().contains(trimmedSearch));
                })
                .toList();

        long activeUsersCount = allUsers.stream().filter(User::isActive).count();
        long inactiveUsersCount = allUsers.size() - activeUsersCount;

        model.addAttribute("users", filteredUsers);
        model.addAttribute("totalUsersCount", allUsers.size());
        model.addAttribute("activeUsersCount", activeUsersCount);
        model.addAttribute("inactiveUsersCount", inactiveUsersCount);
        model.addAttribute("allRoles", UserRole.values());
        model.addAttribute("assignableRoles", userService.getAssignableRolesFor(currentUser));
        model.addAttribute("allPermissions", Permission.values());
        model.addAttribute("canCustomizePermissions", currentUser.hasPermission(Permission.USER_PERMISSIONS_CUSTOMIZE));
        model.addAttribute("selectedRole", finalRoleFilter != null ? finalRoleFilter.name() : "");
        model.addAttribute("selectedStatus", normalizedStatus);
        model.addAttribute("searchKeyword", search != null ? search.trim() : "");
        model.addAttribute("activeNav", "users");
        return "users/list";
    }

    @PostMapping({"/save", "/create"})
    public String createUser(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("fullName") String fullName,
                             @RequestParam(value = "email", required = false) String email,
                             @RequestParam(value = "phone", required = false) String phone,
                             @RequestParam("role") UserRole role,
                             @RequestParam(value = "requirePasswordChange", defaultValue = "true") boolean requirePasswordChange,
                             RedirectAttributes redirectAttributes) {
        User actor = activityLogService.getCurrentAuthenticatedUser();
        User created = userService.createEmployeeAccount(
                actor, username, password, fullName, email, phone, role, requirePasswordChange
        );
        redirectAttributes.addFlashAttribute("successMessage",
                "Employee account '" + created.getUsername() + "' (" + created.getRole().getDisplayName() + ") created successfully!");
        return "redirect:/users";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable Long id,
                             @RequestParam("fullName") String fullName,
                             @RequestParam(value = "email", required = false) String email,
                             @RequestParam(value = "phone", required = false) String phone,
                             @RequestParam("role") UserRole role,
                             RedirectAttributes redirectAttributes) {
        User actor = activityLogService.getCurrentAuthenticatedUser();
        User updated = userService.updateEmployeeAccount(actor, id, fullName, email, phone, role);
        redirectAttributes.addFlashAttribute("successMessage",
                "Employee '" + updated.getUsername() + "' updated successfully.");
        return "redirect:/users";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User actor = activityLogService.getCurrentAuthenticatedUser();
        User updated = userService.toggleAccountStatus(actor, id);
        redirectAttributes.addFlashAttribute("successMessage",
                "Employee account '" + updated.getUsername() + "' is now " + (updated.isActive() ? "Active" : "Inactive") + ".");
        return "redirect:/users";
    }

    @PostMapping("/reset-password/{id}")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam(value = "forceChangeOnLogin", defaultValue = "true") boolean forceChangeOnLogin,
                                RedirectAttributes redirectAttributes) {
        User actor = activityLogService.getCurrentAuthenticatedUser();
        User target = userService.getById(id);
        userService.adminResetPassword(actor, id, newPassword, forceChangeOnLogin);
        redirectAttributes.addFlashAttribute("successMessage",
                "Password reset successfully for employee '" + target.getUsername() + "'.");
        return "redirect:/users";
    }

    @PostMapping("/permissions/{id}")
    public String updatePermissions(@PathVariable Long id,
                                    @RequestParam(value = "permissions", required = false) List<String> permissions,
                                    @RequestParam(value = "grantedPermissions", required = false) List<String> grantedPermissions,
                                    RedirectAttributes redirectAttributes) {
        User actor = activityLogService.getCurrentAuthenticatedUser();
        User target = userService.getById(id);
        List<String> effectiveList = (permissions != null && !permissions.isEmpty()) ? permissions : grantedPermissions;
        userService.customizeUserPermissions(actor, id, effectiveList);
        redirectAttributes.addFlashAttribute("successMessage",
                "Granular permissions updated for employee '" + target.getUsername() + "' (" + target.getRole().getDisplayName() + ").");
        return "redirect:/users";
    }
}
