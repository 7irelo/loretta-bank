package com.lorettabank.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.DateTimeException;
import java.time.LocalDate;

public class SaIdNumberValidator implements ConstraintValidator<SaIdNumber, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || isValid(value);
    }

    public static boolean isValid(String id) {
        if (id == null || !id.matches("\\d{13}")) {
            return false;
        }
        int yy = Integer.parseInt(id.substring(0, 2));
        int mm = Integer.parseInt(id.substring(2, 4));
        int dd = Integer.parseInt(id.substring(4, 6));
        try {
            LocalDate.of(2000 + yy, mm, dd);
        } catch (DateTimeException e) {
            try {
                LocalDate.of(1900 + yy, mm, dd);
            } catch (DateTimeException again) {
                return false;
            }
        }
        char citizenship = id.charAt(10);
        if (citizenship != '0' && citizenship != '1') {
            return false;
        }
        return luhn(id);
    }

    private static boolean luhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
