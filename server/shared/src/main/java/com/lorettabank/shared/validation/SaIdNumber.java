package com.lorettabank.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A 13-digit South African ID number with a valid birth date and Luhn check digit. */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SaIdNumberValidator.class)
public @interface SaIdNumber {
    String message() default "Must be a valid South African ID number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
