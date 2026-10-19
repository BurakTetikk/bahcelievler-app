package tr.kitaptakip.dtos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tr.kitaptakip.dtos.CreateLoanRequest;

public class LoanDatesValidator implements ConstraintValidator<ValidLoanDates, CreateLoanRequest> {
    @Override
    public boolean isValid(CreateLoanRequest request, ConstraintValidatorContext context) {
        if (request == null || request.loanDate() == null || request.dueDate() == null) {
            return true;
        }
        if (!request.dueDate().isBefore(request.loanDate())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("dueDate")
                .addConstraintViolation();
        return false;
    }
}
