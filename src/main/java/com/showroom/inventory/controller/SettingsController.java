package com.showroom.inventory.controller;

import com.showroom.inventory.entity.SystemSetting;
import com.showroom.inventory.entity.User;
import com.showroom.inventory.enums.Permission;
import com.showroom.inventory.enums.UserRole;
import com.showroom.inventory.repository.SystemSettingRepository;
import com.showroom.inventory.service.ActivityLogService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Controller for Application & Security Settings.
 * Viewable by Super Admin & Admin; security-critical settings can only be modified by Super Admin.
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final SystemSettingRepository systemSettingRepository;
    private final ActivityLogService activityLogService;

    public SettingsController(SystemSettingRepository systemSettingRepository,
                              ActivityLogService activityLogService) {
        this.systemSettingRepository = systemSettingRepository;
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public String viewSettings(Model model) {
        User user = activityLogService.getCurrentAuthenticatedUser();
        if (user == null || !user.hasPermission(Permission.SETTINGS_VIEW)) {
            throw new AccessDeniedException("You do not have permission to view System Settings.");
        }
        model.addAttribute("settings", systemSettingRepository.findAll());
        model.addAttribute("canEditSecuritySettings", user.getRole() == UserRole.SUPER_ADMIN && user.hasPermission(Permission.SETTINGS_MANAGE));
        model.addAttribute("activeNav", "settings");
        return "settings/index";
    }

    @PostMapping("/save")
    public String saveSettings(@RequestParam Map<String, String> formParams,
                               RedirectAttributes redirectAttributes) {
        User actor = activityLogService.getCurrentAuthenticatedUser();
        if (actor == null || actor.getRole() != UserRole.SUPER_ADMIN || !actor.hasPermission(Permission.SETTINGS_MANAGE)) {
            throw new AccessDeniedException("Only Super Admin can modify system-level security settings.");
        }

        int updatedCount = 0;
        for (SystemSetting setting : systemSettingRepository.findAll()) {
            String paramKey = "setting_" + setting.getSettingKey();
            if (formParams.containsKey(paramKey)) {
                String newVal = formParams.get(paramKey) != null ? formParams.get(paramKey).trim() : "";
                if (!newVal.equals(setting.getSettingValue())) {
                    String oldVal = setting.getSettingValue();
                    setting.setSettingValue(newVal);
                    setting.setUpdatedBy(actor.getUsername());
                    setting.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
                    systemSettingRepository.save(setting);
                    updatedCount++;

                    activityLogService.logAction(
                            actor,
                            "UPDATE_SYSTEM_SETTING",
                            "SYSTEM_SETTINGS",
                            "Updated setting '" + setting.getSettingKey() + "'",
                            setting.getSettingKey() + "=" + oldVal,
                            setting.getSettingKey() + "=" + newVal
                    );
                }
            }
        }

        redirectAttributes.addFlashAttribute("successMessage",
                updatedCount > 0 ? "Updated " + updatedCount + " system setting(s) successfully." : "No setting changes detected.");
        return "redirect:/settings";
    }
}

