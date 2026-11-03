package com.showroom.inventory.service;

import com.showroom.inventory.dto.ExpenseDTO;
import com.showroom.inventory.entity.Expense;
import com.showroom.inventory.enums.ExpenseCategory;
import com.showroom.inventory.exception.ResourceNotFoundException;
import com.showroom.inventory.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service class handling operational and showroom expense tracking.
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAllByOrderByExpenseDateDesc();
    }

    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
    }

    public List<Expense> getExpensesBetween(LocalDate start, LocalDate end) {
        return expenseRepository.findByExpenseDateBetween(start, end);
    }

    public BigDecimal getTotalExpensesBetween(LocalDate start, LocalDate end) {
        return expenseRepository.sumAmountBetween(start, end);
    }

    public List<Object[]> getExpenseBreakdownBetween(LocalDate start, LocalDate end) {
        return expenseRepository.sumAmountByCategoryBetween(start, end);
    }

    @Transactional
    public Expense createExpense(ExpenseDTO dto) {
        Expense expense = new Expense();
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setExpenseDate(dto.getExpenseDate() != null ? dto.getExpenseDate() : LocalDate.now());
        expense.setDescription(dto.getDescription());
        return expenseRepository.save(expense);
    }

    @Transactional
    public Expense updateExpense(Long id, ExpenseDTO dto) {
        Expense expense = getExpenseById(id);
        expense.setCategory(dto.getCategory());
        expense.setAmount(dto.getAmount());
        expense.setExpenseDate(dto.getExpenseDate());
        expense.setDescription(dto.getDescription());
        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = getExpenseById(id);
        expenseRepository.delete(expense);
    }

    public ExpenseDTO convertToDto(Expense expense) {
        ExpenseDTO dto = new ExpenseDTO();
        dto.setId(expense.getId());
        dto.setCategory(expense.getCategory());
        dto.setAmount(expense.getAmount());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setDescription(expense.getDescription());
        return dto;
    }
}
