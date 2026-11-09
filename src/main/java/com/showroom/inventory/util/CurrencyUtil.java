package com.showroom.inventory.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for formatting currency and numbers according to the Indian Numbering System (Lakhs and Crores).
 * <p>
 * Examples:
 * <ul>
 *   <li>1,000 &rarr; ₹1,000</li>
 *   <li>10,000 &rarr; ₹10,000</li>
 *   <li>1,00,000 &rarr; ₹1,00,000</li>
 *   <li>10,00,000 &rarr; ₹10,00,000</li>
 *   <li>1,23,45,678 &rarr; ₹1,23,45,678</li>
 * </ul>
 */
@Component("currencyUtil")
public class CurrencyUtil {

    /**
     * Formats a BigDecimal number into Indian currency format (e.g. ₹12,34,567 or -₹86,953).
     *
     * @param amount the numerical amount to format
     * @return formatted INR currency string
     */
    public static String formatInr(BigDecimal amount) {
        if (amount == null) {
            return "₹0";
        }
        boolean isNegative = amount.compareTo(BigDecimal.ZERO) < 0;
        String formatted = formatIndianNumber(amount.abs());
        return (isNegative ? "-₹" : "₹") + formatted;
    }

    /**
     * Overload for Number types (Double, Long, Integer, etc.).
     */
    public static String formatInr(Number amount) {
        if (amount == null) {
            return "₹0";
        }
        if (amount instanceof BigDecimal bd) {
            return formatInr(bd);
        }
        return formatInr(new BigDecimal(amount.toString()));
    }

    /**
     * Formats a number into Indian Numbering System grouping without currency symbol (e.g. 12,34,567).
     *
     * @param amount the numerical amount to format
     * @return formatted number string
     */
    public static String formatIndianNumber(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        boolean isNegative = amount.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal abs = amount.abs();

        BigDecimal integerPartBd = abs.setScale(0, RoundingMode.FLOOR);
        String intStr = integerPartBd.toPlainString();

        StringBuilder sb = new StringBuilder();
        int len = intStr.length();
        if (len <= 3) {
            sb.append(intStr);
        } else {
            String lastThree = intStr.substring(len - 3);
            String leading = intStr.substring(0, len - 3);
            StringBuilder leadSb = new StringBuilder();
            int count = 0;
            for (int i = leading.length() - 1; i >= 0; i--) {
                leadSb.insert(0, leading.charAt(i));
                count++;
                if (count == 2 && i > 0) {
                    leadSb.insert(0, ",");
                    count = 0;
                }
            }
            sb.append(leadSb).append(",").append(lastThree);
        }

        // Include decimal part only if non-zero
        if (abs.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal fraction = abs.remainder(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
            String fracStr = fraction.toPlainString();
            if (fracStr.contains(".")) {
                sb.append(".").append(fracStr.substring(fracStr.indexOf('.') + 1));
            }
        }

        return (isNegative ? "-" : "") + sb.toString();
    }

    /**
     * Overload for Number types without currency symbol.
     */
    public static String formatIndianNumber(Number amount) {
        if (amount == null) {
            return "0";
        }
        if (amount instanceof BigDecimal bd) {
            return formatIndianNumber(bd);
        }
        return formatIndianNumber(new BigDecimal(amount.toString()));
    }
}

