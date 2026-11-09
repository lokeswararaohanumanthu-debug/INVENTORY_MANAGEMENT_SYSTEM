package com.showroom.inventory.controller;

import com.showroom.inventory.entity.User;
import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller handling authentication (Login page), Access Denied page (HTTP 403),
 * and Employee Self-Service Password Change.
 */
@Controller
public class AuthController {

    private final UserService userService;
    private final ActivityLogService activityLogService;

    public AuthController(UserService userService, ActivityLogService activityLogService) {
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "disabled", required = false) String disabled,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (disabled != null) {
            model.addAttribute("errorMessage", "Account Deactivated: Your employee account is currently inactive. Please contact the Super Admin.");
        } else if (error != null) {
            model.addAttribute("errorMessage", "Invalid User ID or password. Please verify your credentials and try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been signed out securely.");
        }
        return "login";
    }

    @RequestMapping("/access-denied")
    public String accessDenied(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        Object uri = request.getAttribute("deniedUri");
        Object method = request.getAttribute("deniedMethod");
        if (uri != null) {
            model.addAttribute("deniedUri", uri.toString());
        }
        if (method != null) {
            model.addAttribute("deniedMethod", method.toString());
        }
        if (!model.containsAttribute("accessDeniedMessage")) {
            model.addAttribute("accessDeniedMessage",
                    "Your current role does not have permission to access this resource or perform this action.");
        }
        return "access-denied";
    }

    @GetMapping("/profile/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("activeNav", "profile");
        return "profile/change-password";
    }

    @PostMapping("/profile/change-password")
    public String changeOwnPassword(@RequestParam("currentPassword") String currentPassword,
                                    @RequestParam("newPassword") String newPassword,
                                    @RequestParam("confirmPassword") String confirmPassword,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = activityLogService.getCurrentAuthenticatedUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "New password and confirmation password do not match.");
            return "redirect:/profile/change-password";
        }
        try {
            userService.changeOwnPassword(currentUser.getUsername(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Your password has been updated successfully!");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile/change-password";
        }
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/dashboard";
    }
}
