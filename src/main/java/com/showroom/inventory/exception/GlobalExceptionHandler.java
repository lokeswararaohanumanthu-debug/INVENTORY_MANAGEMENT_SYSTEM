package com.showroom.inventory.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the application using Spring's @ControllerAdvice pattern.
 * Ensures appropriate HTTP status codes (403 Forbidden, 404 Not Found) and user-friendly messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDeniedException(AccessDeniedException ex,
                                              HttpServletRequest request,
                                              HttpServletResponse response,
                                              Model model) {
        logger.warn("Access denied on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("accessDeniedMessage", ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "You do not have sufficient role permissions to perform this operation.");
        model.addAttribute("deniedUri", request.getRequestURI());
        model.addAttribute("deniedMethod", request.getMethod());
        return "access-denied";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFoundException(ResourceNotFoundException ex,
                                                  HttpServletResponse response,
                                                  Model model) {
        logger.error("Resource not found: {}", ex.getMessage());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("statusCode", 404);
        return "error";
    }

    @ExceptionHandler(InsufficientStockException.class)
    public String handleInsufficientStockException(InsufficientStockException ex,
                                                    HttpServletRequest request,
                                                    RedirectAttributes redirectAttributes) {
        logger.error("Insufficient stock: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }

    @ExceptionHandler(DuplicateProductException.class)
    public String handleDuplicateProductException(DuplicateProductException ex,
                                                   HttpServletRequest request,
                                                   RedirectAttributes redirectAttributes) {
        logger.error("Duplicate product: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/products");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex,
                                                 HttpServletRequest request,
                                                 RedirectAttributes redirectAttributes) {
        logger.warn("Validation / argument error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex,
                                          RedirectAttributes redirectAttributes) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("errorMessage",
                "An unexpected error occurred. Please try again or contact the administrator.");
        return "redirect:/error";
    }
}
