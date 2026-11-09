package com.showroom.inventory.service;

import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.enums.UserRole;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service class for Employee Account Management, RBAC Hierarchy Enforcement,
 * Granular Permission Customization, and Spring Security Authentication.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    /**
     * Spring Security hook to load user details, assigned role, and granular permissions.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        for (Permission perm : user.getEffectivePermissions()) {
            authorities.add(new SimpleGrantedAuthority(perm.getAuthority()));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(), // enabled flag: false triggers DisabledException when deactivated
                true,
                true,
                true,
                authorities
        );
    }

    @Transactional
    public void recordSuccessfulLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
            userRepository.save(user);
            activityLogService.logAction(
                    user,
                    "USER_LOGIN",
                    "AUTHENTICATION",
                    "Employee '" + user.getFullName() + "' (" + user.getUsername() + ") signed in with role " + user.getRole().getDisplayName(),
                    null,
                    "Session started at " + user.getLastLoginAt()
            );
        });
    }

    @Transactional
    public void recordLogout(String username) {
        if (username == null) return;
        userRepository.findByUsername(username).ifPresent(user -> {
            activityLogService.logAction(
                    user,
                    "USER_LOGOUT",
                    "AUTHENTICATION",
                    "Employee '" + user.getFullName() + "' (" + user.getUsername() + ") signed out",
                    "Session Active",
                    "Session Terminated"
            );
        });
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    public Optional<User> findByUsernameOptional(String username) {
        return userRepository.findByUsername(username);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparingInt((User u) -> u.getRole() != null ? -u.getRole().getHierarchyLevel() : 0)
                        .thenComparing(User::getUsername))
                .toList();
    }

    /**
     * Checks whether the acting administrator is permitted to manage the target user or assign the target role.
     */
    public boolean canActorManageTarget(User actor, User target) {
        if (actor == null || target == null) {
            return false;
        }
        if (!actor.hasPermission(Permission.USER_MANAGE)) {
            return false;
        }
        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        // Non-Super-Admin cannot manage Super Admin or protected accounts or accounts at/above their own hierarchy level
        if (target.isSuperAdmin() || target.isProtectedAccount()) {
            return false;
        }
        return actor.getRole().getHierarchyLevel() > target.getRole().getHierarchyLevel();
    }

    public boolean canActorAssignRole(User actor, UserRole roleToAssign) {
        if (actor == null || roleToAssign == null || !actor.hasPermission(Permission.USER_MANAGE)) {
            return false;
        }
        if (actor.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        // Admin can only assign roles strictly below Admin hierarchy level (cannot assign SUPER_ADMIN or ADMIN)
        return actor.getRole().getHierarchyLevel() > roleToAssign.getHierarchyLevel()
                && roleToAssign != UserRole.SUPER_ADMIN;
    }

    public List<UserRole> getAssignableRolesFor(User actor) {
        if (actor == null) {
            return List.of();
        }
        List<UserRole> roles = new ArrayList<>();
        for (UserRole r : UserRole.values()) {
            if (canActorAssignRole(actor, r)) {
                roles.add(r);
            }
        }
        return roles;
    }

    @Transactional
    public User createEmployeeAccount(User actor,
                                      String username,
                                      String rawPassword,
                                      String fullName,
                                      String email,
                                      String phone,
                                      UserRole assignedRole,
                                      boolean requirePasswordChange) {
        if (actor == null || !actor.hasPermission(Permission.USER_MANAGE)) {
            throw new AccessDeniedException("You do not have permission to create employee accounts.");
        }
        if (!canActorAssignRole(actor, assignedRole)) {
            throw new AccessDeniedException("You are not authorized to assign the role: " + assignedRole.getDisplayName());
        }
        String normalizedUsername = username != null ? username.trim() : "";
        if (normalizedUsername.isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username '" + normalizedUsername + "' already exists.");
        }
        if (rawPassword == null || rawPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        User newUser = new User();
        newUser.setUsername(normalizedUsername);
        newUser.setPassword(passwordEncoder.encode(rawPassword.trim()));
        newUser.setFullName(fullName != null && !fullName.isBlank() ? fullName.trim() : normalizedUsername);
        newUser.setEmail(email != null ? email.trim() : null);
        newUser.setPhone(phone != null ? phone.trim() : null);
        newUser.setRole(assignedRole);
        newUser.setActive(true);
        newUser.setProtectedAccount(assignedRole == UserRole.SUPER_ADMIN);
        newUser.setPasswordChangeRequired(requirePasswordChange);

        User saved = userRepository.save(newUser);

        activityLogService.logAction(
                actor,
                "CREATE_USER",
                "USER_MANAGEMENT",
                "Created employee account '" + saved.getUsername() + "' (" + saved.getFullName() + ") with role " + assignedRole.getDisplayName(),
                "None",
                "Username: " + saved.getUsername() + ", Role: " + assignedRole.name() + ", Status: ACTIVE"
        );

        return saved;
    }

    @Transactional
    public User updateEmployeeAccount(User actor,
                                      Long targetUserId,
                                      String fullName,
                                      String email,
                                      String phone,
                                      UserRole newRole) {
        User target = getById(targetUserId);
        if (!canActorManageTarget(actor, target)) {
            throw new AccessDeniedException("You are not authorized to modify this employee account.");
        }

        String prevSummary = "Name: " + target.getFullName() + ", Role: " + target.getRole().name() + ", Email: " + target.getEmail() + ", Phone: " + target.getPhone();

        if (fullName != null && !fullName.isBlank()) {
            target.setFullName(fullName.trim());
        }
        target.setEmail(email != null ? email.trim() : null);
        target.setPhone(phone != null ? phone.trim() : null);

        // Role change check: users cannot change their own role, and primary Super Admin role cannot be changed
        if (newRole != null && newRole != target.getRole()) {
            if (actor.getUsername().equalsIgnoreCase(target.getUsername())) {
                throw new AccessDeniedException("Employees cannot change their own assigned role.");
            }
            if ("superadmin".equalsIgnoreCase(target.getUsername()) || target.isProtectedAccount()) {
                throw new AccessDeniedException("The primary Super Admin role is protected and cannot be modified.");
            }
            if (!canActorAssignRole(actor, newRole)) {
                throw new AccessDeniedException("You are not authorized to assign the role: " + newRole.getDisplayName());
            }
            UserRole oldRole = target.getRole();
            target.setRole(newRole);
            activityLogService.logAction(
                    actor,
                    "CHANGE_ROLE",
                    "USER_MANAGEMENT",
                    "Changed role for employee '" + target.getUsername() + "' from " + oldRole.getDisplayName() + " to " + newRole.getDisplayName(),
                    "Role: " + oldRole.name(),
                    "Role: " + newRole.name()
            );
        }

        User saved = userRepository.save(target);
        String newSummary = "Name: " + saved.getFullName() + ", Role: " + saved.getRole().name() + ", Email: " + saved.getEmail() + ", Phone: " + saved.getPhone();

        activityLogService.logAction(
                actor,
                "UPDATE_USER",
                "USER_MANAGEMENT",
                "Updated profile details for employee '" + saved.getUsername() + "'",
                prevSummary,
                newSummary
        );

        return saved;
    }

    @Transactional
    public User toggleAccountStatus(User actor, Long targetUserId) {
        User target = getById(targetUserId);
        if (!canActorManageTarget(actor, target)) {
            throw new AccessDeniedException("You are not authorized to activate or deactivate this account.");
        }
        if ("superadmin".equalsIgnoreCase(target.getUsername()) || target.isSuperAdmin() || target.isProtectedAccount()) {
            throw new AccessDeniedException("The Super Admin account is protected and cannot be deactivated.");
        }
        if (actor.getUsername().equalsIgnoreCase(target.getUsername())) {
            throw new AccessDeniedException("You cannot deactivate your own currently logged-in account.");
        }

        boolean previous = target.isActive();
        target.setActive(!previous);
        User saved = userRepository.save(target);

        activityLogService.logAction(
                actor,
                saved.isActive() ? "ACTIVATE_USER" : "DEACTIVATE_USER",
                "USER_MANAGEMENT",
                (saved.isActive() ? "Activated" : "Deactivated") + " employee account '" + saved.getUsername() + "' (" + saved.getFullName() + ")",
                "Status: " + (previous ? "ACTIVE" : "INACTIVE"),
                "Status: " + (saved.isActive() ? "ACTIVE" : "INACTIVE")
        );

        return saved;
    }

    @Transactional
    public void adminResetPassword(User actor, Long targetUserId, String newRawPassword, boolean forceChangeOnLogin) {
        User target = getById(targetUserId);
        if (!canActorManageTarget(actor, target)) {
            throw new AccessDeniedException("You are not authorized to reset the password for this account.");
        }
        if (target.isSuperAdmin() && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Only Super Admin can reset a Super Admin password.");
        }
        if (newRawPassword == null || newRawPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        target.setPassword(passwordEncoder.encode(newRawPassword.trim()));
        target.setPasswordChangeRequired(forceChangeOnLogin);
        userRepository.save(target);

        activityLogService.logAction(
                actor,
                "RESET_PASSWORD",
                "USER_MANAGEMENT",
                "Reset password for employee '" + target.getUsername() + "' (" + target.getFullName() + ")",
                "Password: [PROTECTED HASH]",
                "Password: [NEW HASH], ForceChangeOnNextLogin: " + forceChangeOnLogin
        );
    }

    @Transactional
    public void customizeUserPermissions(User actor, Long targetUserId, List<String> selectedPermissionNames) {
        if (actor == null || actor.getRole() != UserRole.SUPER_ADMIN || !actor.hasPermission(Permission.USER_PERMISSIONS_CUSTOMIZE)) {
            throw new AccessDeniedException("Only Super Admin can customize granular employee permissions.");
        }
        User target = getById(targetUserId);
        if (target.isSuperAdmin() || "superadmin".equalsIgnoreCase(target.getUsername())) {
            throw new AccessDeniedException("Super Admin always retains all permissions and cannot be restricted.");
        }
        if (actor.getUsername().equalsIgnoreCase(target.getUsername())) {
            throw new AccessDeniedException("Users cannot modify their own permission set.");
        }

        Set<Permission> roleDefaults = target.getRole().getDefaultPermissions();
        Set<Permission> desired = EnumSet.noneOf(Permission.class);
        if (selectedPermissionNames != null) {
            for (String name : selectedPermissionNames) {
                try {
                    desired.add(Permission.valueOf(name.trim()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // Compute granted overrides (in desired but not in roleDefaults)
        String grantedCsv = desired.stream()
                .filter(p -> !roleDefaults.contains(p))
                .map(Enum::name)
                .collect(Collectors.joining(","));

        // Compute revoked overrides (in roleDefaults but not in desired)
        String revokedCsv = roleDefaults.stream()
                .filter(p -> !desired.contains(p))
                .map(Enum::name)
                .collect(Collectors.joining(","));

        String prevGranted = target.getCustomGrantedPermissions();
        String prevRevoked = target.getCustomRevokedPermissions();

        target.setCustomGrantedPermissions(grantedCsv.isEmpty() ? null : grantedCsv);
        target.setCustomRevokedPermissions(revokedCsv.isEmpty() ? null : revokedCsv);
        userRepository.save(target);

        activityLogService.logAction(
                actor,
                "CUSTOMIZE_PERMISSIONS",
                "USER_MANAGEMENT",
                "Customized granular permissions for employee '" + target.getUsername() + "' (" + target.getRole().getDisplayName() + ")",
                "Granted: [" + (prevGranted != null ? prevGranted : "") + "], Revoked: [" + (prevRevoked != null ? prevRevoked : "") + "]",
                "Granted: [" + grantedCsv + "], Revoked: [" + revokedCsv + "]"
        );
    }

    @Transactional
    public void changeOwnPassword(String username, String oldPassword, String newPassword) {
        User user = getByUsername(username);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match.");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        activityLogService.logAction(
                user,
                "CHANGE_OWN_PASSWORD",
                "AUTHENTICATION",
                "Employee '" + user.getUsername() + "' updated their own password",
                "PasswordChangeRequired: true/false",
                "PasswordChangeRequired: false"
        );
    }
}
