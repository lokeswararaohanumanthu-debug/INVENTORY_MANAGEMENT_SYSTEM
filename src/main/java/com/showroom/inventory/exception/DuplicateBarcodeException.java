package com.showroom.inventory.exception;

/**
 * Exception thrown when attempting to assign a barcode that is already
 * linked to another product. Guarantees that every scanned barcode maps
 * to exactly one product at the POS billing counter.
 */
public class DuplicateBarcodeException extends RuntimeException {

    private final String barcode;

    public DuplicateBarcodeException(String barcode) {
        super(String.format("Barcode '%s' is already assigned to another product", barcode));
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}
