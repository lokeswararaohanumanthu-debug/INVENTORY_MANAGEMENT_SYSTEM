package com.showroom.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Mobile Showroom Inventory Management System.
 *
 * This class configures:
 * - URL-based authorization rules (which pages are public vs. protected)
 * - Form-based login with a custom login page
 * - Logout behavior
 * - Password encoding using BCrypt
 * - H2 console access for development (frame options and CSRF exceptions)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain that processes all HTTP requests.
     *
     * - Public resources (login page, static assets, H2 console) are accessible without authentication.
     * - All other requests require the user to be authenticated.
     * - Uses a custom login page at "/login" with a default redirect to "/dashboard" after successful login.
     * - Configures logout to redirect to the login page with a logout confirmation parameter.
     * - Allows H2 console to render in frames (same-origin) and disables CSRF for H2 console endpoints.
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configure URL-based authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow public access to login page, static resources, and H2 console
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/h2-console/**").permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Configure form-based login
            .formLogin(form -> form
                // Custom login page URL (served by a controller)
                .loginPage("/login")
                // After successful login, always redirect to dashboard
                .defaultSuccessUrl("/dashboard", true)
                // Allow everyone to access the login page
                .permitAll()
            )
            // Configure logout behavior
            .logout(logout -> logout
                // After logout, redirect to login page with a logout parameter
                .logoutSuccessUrl("/login?logout")
                // Allow everyone to perform logout
                .permitAll()
            )
            // Configure headers for H2 console compatibility
            // H2 console uses iframes, so we need to allow same-origin framing
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            // Disable CSRF for H2 console endpoints (development only)
            // In production, H2 console should be disabled and this exception removed
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            );

        return http.build();
    }

    /**
     * Provides a BCrypt password encoder bean for hashing user passwords.
     * BCrypt is a strong adaptive hashing function designed for passwords —
     * it automatically handles salting and is resistant to brute-force attacks.
     *
     * @return a BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
