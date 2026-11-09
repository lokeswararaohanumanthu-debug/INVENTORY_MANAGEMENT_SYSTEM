package com.showroom.inventory.service;

import com.showroom.inventory.entity.ActivityLog;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.enums.UserRole;
import com.showroom.inventory.repository.ActivityLogRepository;
import com.showroom.inventory.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Service managing the immutable Activity / Audit Log across all modules.
 */
@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void logAction(User actor,
                          String actionPerformed,
                          String moduleAffected,
                          String description,
                          String previousValue,
                          String updatedValue) {
        String username = actor != null ? actor.getUsername() : "system";
        String fullName = actor != null ? actor.getFullName() : "System Process";
        String roleName = (actor != null && actor.getRole() != null) ? actor.getRole().getDisplayName() : "SYSTEM";

        ActivityLog log = new ActivityLog(
                username,
                fullName,
                roleName,
                actionPerformed,
                moduleAffected,
                description,
                previousValue,
                updatedValue
        );
        activityLogRepository.save(log);
    }

    @Transactional
    public void logCurrentUserAction(String actionPerformed,
                                     String moduleAffected,
                                     String description,
                                     String previousValue,
                                     String updatedValue) {
        User currentUser = getCurrentAuthenticatedUser();
        logAction(currentUser, actionPerformed, moduleAffected, description, previousValue, updatedValue);
    }

    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> getVisibleLogsFor(User viewer, String moduleFilter, String searchFilter) {
        if (viewer == null) {
            return List.of();
        }
        List<ActivityLog> baseLogs;
        if (viewer.getRole() == UserRole.SUPER_ADMIN) {
            baseLogs = activityLogRepository.findAllByOrderByCreatedAtDesc();
        } else if (viewer.hasPermission(Permission.ACTIVITY_LOG_VIEW_ALL)) {
            // Admin & Showroom Manager see operational & employee logs (excluding Super Admin internal security logs)
            baseLogs = activityLogRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(l -> !"Super Admin".equalsIgnoreCase(l.getUserRole())
                            || l.getUsername().equalsIgnoreCase(viewer.getUsername()))
                    .toList();
        } else {
            // Other roles view only their own activity logs
            baseLogs = activityLogRepository.findByUsernameIgnoreCaseOrderByCreatedAtDesc(viewer.getUsername());
        }

        String mod = (moduleFilter != null && !moduleFilter.isBlank() && !"ALL".equalsIgnoreCase(moduleFilter))
                ? moduleFilter.trim().toUpperCase(Locale.ROOT) : null;
        String q = (searchFilter != null && !searchFilter.isBlank())
                ? searchFilter.trim().toLowerCase(Locale.ROOT) : null;

        return baseLogs.stream()
                .filter(l -> mod == null || (l.getModuleAffected() != null && l.getModuleAffected().equalsIgnoreCase(mod)))
                .filter(l -> {
                    if (q == null) return true;
                    return (l.getUsername() != null && l.getUsername().toLowerCase(Locale.ROOT).contains(q))
                            || (l.getEmployeeName() != null && l.getEmployeeName().toLowerCase(Locale.ROOT).contains(q))
                            || (l.getActionPerformed() != null && l.getActionPerformed().toLowerCase(Locale.ROOT).contains(q))
                            || (l.getDescription() != null && l.getDescription().toLowerCase(Locale.ROOT).contains(q))
                            || (l.getModuleAffected() != null && l.getModuleAffected().toLowerCase(Locale.ROOT).contains(q));
                })
                .toList();
    }
}

