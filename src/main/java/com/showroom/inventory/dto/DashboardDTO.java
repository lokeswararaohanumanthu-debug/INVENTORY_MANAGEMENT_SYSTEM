package com.showroom.inventory.dto;

import com.showroom.inventory.entity.Product;
import com.showroom.inventory.entity.Purchase;
import com.showroom.inventory.entity.Sale;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO carrying aggregated metrics and recent activity for the showroom dashboard.
 */
public class DashboardDTO {

    private long totalProducts;
    private long totalStockQuantity;
    private BigDecimal todaySales;
    private BigDecimal monthlyRevenue;
    private BigDecimal monthlyExpenses;
    private BigDecimal monthlyNetProfit;
    private long lowStockCount;
    private List<Product> lowStockProducts;
    private List<Sale> recentSales;
    private List<Purchase> recentPurchases;
    private List<Object[]> bestSellingProducts;

    public DashboardDTO() {
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalStockQuantity() {
        return totalStockQuantity;
    }

    public void setTotalStockQuantity(long totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
    }

    public BigDecimal getTodaySales() {
        return todaySales != null ? todaySales : BigDecimal.ZERO;
    }

    public void setTodaySales(BigDecimal todaySales) {
        this.todaySales = todaySales;
    }

    public BigDecimal getMonthlyRevenue() {
        return monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO;
    }

    public void setMonthlyRevenue(BigDecimal monthlyRevenue) {
        this.monthlyRevenue = monthlyRevenue;
    }

    public BigDecimal getMonthlyExpenses() {
        return monthlyExpenses != null ? monthlyExpenses : BigDecimal.ZERO;
    }

    public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses;
    }

    public BigDecimal getMonthlyNetProfit() {
        return monthlyNetProfit != null ? monthlyNetProfit : BigDecimal.ZERO;
    }

    public void setMonthlyNetProfit(BigDecimal monthlyNetProfit) {
        this.monthlyNetProfit = monthlyNetProfit;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public List<Product> getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(List<Product> lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public List<Sale> getRecentSales() {
        return recentSales;
    }

    public void setRecentSales(List<Sale> recentSales) {
        this.recentSales = recentSales;
    }

    public List<Purchase> getRecentPurchases() {
        return recentPurchases;
    }

    public void setRecentPurchases(List<Purchase> recentPurchases) {
        this.recentPurchases = recentPurchases;
    }

    public List<Object[]> getBestSellingProducts() {
        return bestSellingProducts;
    }

    public void setBestSellingProducts(List<Object[]> bestSellingProducts) {
        this.bestSellingProducts = bestSellingProducts;
    }
}
