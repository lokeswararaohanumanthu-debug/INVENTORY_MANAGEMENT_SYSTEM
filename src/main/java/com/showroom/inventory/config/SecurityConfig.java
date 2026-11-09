package com.showroom.inventory.config;

import com.showroom.inventory.service.ActivityLogService;
import com.showroom.inventory.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security & Role-Based Access Control (RBAC) Configuration.
 * Enforces authentication and granular permission checks at both URL and method levels.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;
    private final ActivityLogService activityLogService;

    public SecurityConfig(@Lazy UserService userService, @Lazy ActivityLogService activityLogService) {
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public static assets & login page
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/vendor/**", "/h2-console/**").permitAll()

                // Access denied & authenticated profile password management
                .requestMatchers("/access-denied", "/profile/**").authenticated()

                // Dashboard (role-tailored view inside controller/template)
                .requestMatchers("/", "/dashboard").hasAuthority("PERM_DASHBOARD_VIEW")

                // POS Billing Counter & Barcode/Search APIs
                .requestMatchers("/sales/billing", "/sales/new", "/sales/save", "/sales/api/**").hasAuthority("PERM_SALES_CREATE")

                // Sales Invoices & History
                .requestMatchers("/sales", "/sales/view/**").hasAnyAuthority("PERM_SALES_VIEW_ALL", "PERM_SALES_VIEW_OWN")

                // Product Catalogue
                .requestMatchers("/products/new").hasAuthority("PERM_PRODUCT_ADD")
                .requestMatchers("/products/edit/**").hasAuthority("PERM_PRODUCT_EDIT")
                .requestMatchers("/products/save").hasAnyAuthority("PERM_PRODUCT_ADD", "PERM_PRODUCT_EDIT")
                .requestMatchers("/products/deactivate/**", "/products/reactivate/**").hasAuthority("PERM_PRODUCT_DEACTIVATE")
                .requestMatchers("/products/delete/**").hasAuthority("PERM_PRODUCT_DELETE")
                .requestMatchers("/products", "/products/view/**").hasAuthority("PERM_PRODUCT_VIEW")

                // Stock & Inventory
                .requestMatchers("/inventory/adjust").hasAuthority("PERM_STOCK_ADJUST")
                .requestMatchers("/inventory/export").hasAuthority("PERM_INVENTORY_REPORT_VIEW")
                .requestMatchers("/inventory", "/inventory/low-stock", "/inventory/movements").hasAuthority("PERM_STOCK_VIEW")

                // Purchases & Stock-In
                .requestMatchers("/purchases/new", "/purchases/save").hasAuthority("PERM_PURCHASE_CREATE")
                .requestMatchers("/purchases", "/purchases/view/**").hasAuthority("PERM_PURCHASE_VIEW")

                // Suppliers
                .requestMatchers("/suppliers/new", "/suppliers/edit/**", "/suppliers/save", "/suppliers/deactivate/**").hasAuthority("PERM_SUPPLIER_MANAGE")
                .requestMatchers("/suppliers").hasAuthority("PERM_SUPPLIER_VIEW")

                // Expenses Tracker
                .requestMatchers("/expenses/new", "/expenses/edit/**", "/expenses/save", "/expenses/delete/**").hasAuthority("PERM_EXPENSE_MANAGE")
                .requestMatchers("/expenses").hasAuthority("PERM_EXPENSE_VIEW")

                // Financial Analytics & Profit/Loss Reports
                .requestMatchers("/reports/export").hasAuthority("PERM_REPORT_EXPORT")
                .requestMatchers("/reports", "/reports/**").hasAnyAuthority("PERM_REPORT_PNL_VIEW", "PERM_REPORT_FINANCIAL_VIEW")

                // User & Role Management
                .requestMatchers("/users/permissions/**").hasAuthority("PERM_USER_PERMISSIONS_CUSTOMIZE")
                .requestMatchers("/users/new", "/users/save", "/users/edit/**", "/users/update/**", "/users/toggle-status/**", "/users/reset-password/**").hasAuthority("PERM_USER_MANAGE")
                .requestMatchers("/users", "/users/**").hasAuthority("PERM_USER_VIEW")

                // System Settings
                .requestMatchers("/settings/save").hasAuthority("PERM_SETTINGS_MANAGE")
                .requestMatchers("/settings", "/settings/**").hasAuthority("PERM_SETTINGS_VIEW")

                // Approvals Workflow
                .requestMatchers("/approvals/decide/**").hasAnyAuthority("PERM_APPROVAL_DECIDE", "PERM_STOCK_APPROVE", "PERM_PURCHASE_APPROVE")
                .requestMatchers("/approvals/request").hasAuthority("PERM_APPROVAL_REQUEST")
                .requestMatchers("/approvals", "/approvals/**").hasAuthority("PERM_APPROVAL_VIEW")

                // Activity Logs (strictly read-only; no modifications or deletions permitted)
                .requestMatchers(HttpMethod.GET, "/activity-logs").hasAnyAuthority("PERM_ACTIVITY_LOG_VIEW_ALL", "PERM_ACTIVITY_LOG_VIEW_OWN")
                .requestMatchers("/activity-logs/**").denyAll()

                // Any other endpoint requires authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    userService.recordSuccessfulLogin(authentication.getName());
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                })
                .failureHandler((request, response, exception) -> {
                    if (exception instanceof DisabledException
                            || (exception.getCause() instanceof DisabledException)) {
                        response.sendRedirect(request.getContextPath() + "/login?disabled");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login?error");
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .addLogoutHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        userService.recordLogout(authentication.getName());
                    }
                })
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    activityLogService.logCurrentUserAction(
                            "UNAUTHORIZED_ACCESS_BLOCKED",
                            "SECURITY",
                            "Blocked unauthorized " + request.getMethod() + " request to " + request.getRequestURI(),
                            "Allowed: false",
                            "HTTP 403 Forbidden"
                    );
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    String accept = request.getHeader("Accept");
                    if (request.getRequestURI().contains("/api/")
                            || (accept != null && accept.contains("application/json") && !accept.contains("text/html"))) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access Denied: Your role does not have permission to perform this operation.\"}");
                    } else {
                        request.setAttribute("deniedUri", request.getRequestURI());
                        request.setAttribute("deniedMethod", request.getMethod());
                        request.getRequestDispatcher("/access-denied").forward(request, response);
                    }
                })
                .authenticationEntryPoint((request, response, authException) -> {
                    String accept = request.getHeader("Accept");
                    if (request.getRequestURI().contains("/api/")
                            || (accept != null && accept.contains("application/json") && !accept.contains("text/html"))) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required to access this resource.\"}");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                })
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
