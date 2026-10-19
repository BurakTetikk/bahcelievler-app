package tr.kitaptakip.dtos.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = LoanDatesValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLoanDates {
    String message() default "Ödünç tarihi, son iade tarihinden sonra olamaz.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
