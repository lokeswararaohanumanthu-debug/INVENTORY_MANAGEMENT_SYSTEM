package com.showroom.inventory.controller;

import com.showroom.inventory.dto.ExpenseDTO;
import com.showroom.inventory.entity.Expense;
import com.showroom.inventory.enums.ExpenseCategory;
import com.showroom.inventory.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Controller handling Showroom Expense Tracking.
 */
@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public String listExpenses(@RequestParam(value = "month", required = false) Integer month,
                               @RequestParam(value = "year", required = false) Integer year,
                               Model model) {
        List<Expense> expenses;
        BigDecimal totalAmount;

        if (month != null && year != null) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            expenses = expenseService.getExpensesBetween(start, end);
            totalAmount = expenseService.getTotalExpensesBetween(start, end);
        } else {
            expenses = expenseService.getAllExpenses();
            YearMonth ym = YearMonth.now();
            totalAmount = expenseService.getTotalExpensesBetween(ym.atDay(1), ym.atEndOfMonth());
        }

        model.addAttribute("expenses", expenses);
        model.addAttribute("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("currentMonth", month != null ? month : YearMonth.now().getMonthValue());
        model.addAttribute("currentYear", year != null ? year : YearMonth.now().getYear());
        model.addAttribute("activeNav", "expenses");
        return "expenses/list";
    }

    @GetMapping("/new")
    public String newExpenseForm(Model model) {
        model.addAttribute("expense", new ExpenseDTO());
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("isNew", true);
        model.addAttribute("activeNav", "expenses");
        return "expenses/form";
    }

    @GetMapping("/edit/{id}")
    public String editExpenseForm(@PathVariable Long id, Model model) {
        Expense expense = expenseService.getExpenseById(id);
        model.addAttribute("expense", expenseService.convertToDto(expense));
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("isNew", false);
        model.addAttribute("activeNav", "expenses");
        return "expenses/form";
    }

    @PostMapping("/save")
    public String saveExpense(@Valid @ModelAttribute("expense") ExpenseDTO expenseDto,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", ExpenseCategory.values());
            model.addAttribute("isNew", expenseDto.getId() == null);
            model.addAttribute("activeNav", "expenses");
            return "expenses/form";
        }

        if (expenseDto.getId() == null) {
            expenseService.createExpense(expenseDto);
            redirectAttributes.addFlashAttribute("successMessage", "Expense recorded successfully!");
        } else {
            expenseService.updateExpense(expenseDto.getId(), expenseDto);
            redirectAttributes.addFlashAttribute("successMessage", "Expense updated successfully!");
        }

        return "redirect:/expenses";
    }

    @PostMapping("/delete/{id}")
    public String deleteExpense(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        expenseService.deleteExpense(id);
        redirectAttributes.addFlashAttribute("successMessage", "Expense deleted successfully.");
        return "redirect:/expenses";
    }
}
