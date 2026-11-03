package com.showroom.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for recording a supplier purchase order with multiple items.
 */
public class PurchaseDTO {

    private String purchaseNumber;

    @NotNull(message = "Supplier must be selected")
    private Long supplierId;

    @NotNull(message = "Purchase date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate = LocalDate.now();

    private String notes;

    @NotEmpty(message = "Purchase must contain at least one product")
    @Valid
    private List<PurchaseItemDTO> items = new ArrayList<>();

    public PurchaseDTO() {
    }

    public String getPurchaseNumber() {
        return purchaseNumber;
    }

    public void setPurchaseNumber(String purchaseNumber) {
        this.purchaseNumber = purchaseNumber;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PurchaseItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItemDTO> items) {
        this.items = items;
    }

    public BigDecimal calculateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (PurchaseItemDTO item : items) {
                if (item.getUnitPrice() != null && item.getQuantity() > 0) {
                    total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }
        return total;
    }
}
