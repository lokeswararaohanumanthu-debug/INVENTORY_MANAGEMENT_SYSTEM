package com.showroom.inventory.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for the application using Spring's @ControllerAdvice pattern.
 *
 * @ControllerAdvice makes this class intercept exceptions thrown by any @Controller
 * across the entire application. Each @ExceptionHandler method handles a specific
 * exception type and returns an appropriate response — in this case, redirects
 * with flash messages displayed to the user.
 *
 * Flash attributes survive one redirect and are then automatically removed,
 * making them ideal for one-time error/success messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle ResourceNotFoundException.
     * Redirects to the error page with the resource-not-found message.
     *
     * @param ex                 the ResourceNotFoundException that was thrown
     * @param redirectAttributes Spring's redirect attributes for flash messages
     * @return redirect to the error page
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFoundException(ResourceNotFoundException ex,
                                                   RedirectAttributes redirectAttributes) {
        logger.error("Resource not found: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/error";
    }

    /**
     * Handle InsufficientStockException.
     * Redirects back to the previous page with an error message about stock availability.
     *
     * @param ex                 the InsufficientStockException that was thrown
     * @param request            the current HTTP request (used to determine the referer URL)
     * @param redirectAttributes Spring's redirect attributes for flash messages
     * @return redirect back to the referring page, or dashboard if no referer
     */
    @ExceptionHandler(InsufficientStockException.class)
    public String handleInsufficientStockException(InsufficientStockException ex,
                                                    HttpServletRequest request,
                                                    RedirectAttributes redirectAttributes) {
        logger.error("Insufficient stock: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        // Redirect back to the page the user came from
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }

    /**
     * Handle DuplicateProductException.
     * Redirects back to the previous page with an error message about the duplicate product code.
     *
     * @param ex                 the DuplicateProductException that was thrown
     * @param request            the current HTTP request (used to determine the referer URL)
     * @param redirectAttributes Spring's redirect attributes for flash messages
     * @return redirect back to the referring page, or products list if no referer
     */
    @ExceptionHandler(DuplicateProductException.class)
    public String handleDuplicateProductException(DuplicateProductException ex,
                                                   HttpServletRequest request,
                                                   RedirectAttributes redirectAttributes) {
        logger.error("Duplicate product: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        // Redirect back to the page the user came from
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/products");
    }

    /**
     * Handle all other uncaught exceptions.
     * Acts as a safety net for any exception not specifically handled above.
     * Redirects to the error page with a generic error message.
     *
     * @param ex                 the Exception that was thrown
     * @param redirectAttributes Spring's redirect attributes for flash messages
     * @return redirect to the error page
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex,
                                          RedirectAttributes redirectAttributes) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("errorMessage",
                "An unexpected error occurred. Please try again or contact the administrator.");
        return "redirect:/error";
    }
}
