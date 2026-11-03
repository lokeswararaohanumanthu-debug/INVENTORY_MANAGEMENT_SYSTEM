package com.showroom.inventory.exception;

/**
 * Exception thrown when a requested resource is not found in the system.
 * Used across the application when performing lookups by ID, code, or other identifiers.
 *
 * Example: "Product not found with id: 42"
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Constructs a new ResourceNotFoundException with a descriptive message.
     *
     * @param resourceName the name of the resource type (e.g., "Product", "Supplier")
     * @param fieldName    the field used for the lookup (e.g., "id", "productCode")
     * @param fieldValue   the value that was searched for
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
