package com.showroom.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for recording customer sales and invoice generation.
 */
public class SaleDTO {

    private String billNumber;
    private String customerName;
    private String customerPhone;

    @NotNull(message = "Sale date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate saleDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    private BigDecimal discount = BigDecimal.ZERO;

    /** No default — the cashier must explicitly select a payment mode for every bill. */
    @NotBlank(message = "Please select a payment mode")
    private String paymentMethod;
    private String notes;

    @NotEmpty(message = "Sale must contain at least one product")
    @Valid
    private List<SaleItemDTO> items = new ArrayList<>();

    public SaleDTO() {
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getDiscount() {
        return discount != null ? discount : BigDecimal.ZERO;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<SaleItemDTO> getItems() {
        return items;
    }

    public void setItems(List<SaleItemDTO> items) {
        this.items = items;
    }

    public BigDecimal calculateSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (SaleItemDTO item : items) {
                if (item.getUnitPrice() != null && item.getQuantity() > 0) {
                    total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }
        }
        return total;
    }

    public BigDecimal calculateNetAmount() {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal disc = getDiscount();
        BigDecimal net = subtotal.subtract(disc);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }
}
