package com.showroom.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * InventoryApplication - Entry Point of the Mobile Showroom Inventory Management System.
 *
 * This is the main class that bootstraps the entire Spring Boot application.
 * The @SpringBootApplication annotation combines three key annotations:
 *   - @Configuration:       Marks this class as a source of bean definitions.
 *   - @EnableAutoConfiguration: Tells Spring Boot to automatically configure
 *                               beans based on the dependencies in the classpath.
 *   - @ComponentScan:       Scans the 'com.showroom.inventory' package (and sub-packages)
 *                           for Spring components like @Controller, @Service, @Repository.
 *
 * When this application starts, Spring Boot will:
 *   1. Start the embedded Tomcat web server on the configured port (default 8080).
 *   2. Initialize the database connection and create/update tables via JPA/Hibernate.
 *   3. Register all controllers, services, and repositories found in sub-packages.
 *   4. Apply Spring Security configuration for authentication and authorization.
 */
@SpringBootApplication
public class InventoryApplication {

    /**
     * The main method - Java's standard entry point.
     * SpringApplication.run() launches the Spring Boot application by:
     *   - Creating the Spring ApplicationContext
     *   - Performing classpath scanning for components
     *   - Starting the embedded web server
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        // Print a welcome banner to the console when the application starts
        System.out.println("=========================================================");
        System.out.println("   Mobile Showroom Inventory Management System");
        System.out.println("   Starting up... Please wait.");
        System.out.println("=========================================================");

        // Launch the Spring Boot application
        SpringApplication.run(InventoryApplication.class, args);

        // Print success message after startup is complete
        System.out.println("=========================================================");
        System.out.println("   Application Started Successfully!");
        System.out.println("   Access the app at: http://localhost:8080");
        System.out.println("   H2 Console at:     http://localhost:8080/h2-console");
        System.out.println("=========================================================");
    }
}
