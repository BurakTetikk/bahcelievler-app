package tr.kitaptakip.dtos.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tr.kitaptakip.dtos.CreateLoanRequest;

class LoanDatesValidatorTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory factory;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void acceptsDueDateEqualToLoanDate() {
        CreateLoanRequest request = request(LocalDate.parse("2026-10-18"), LocalDate.parse("2026-10-18"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsDueDateBeforeLoanDateOnDueDateField() {
        CreateLoanRequest request = request(LocalDate.parse("2026-10-18"), LocalDate.parse("2026-10-17"));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("dueDate");
    }

    private CreateLoanRequest request(LocalDate loanDate, LocalDate dueDate) {
        return new CreateLoanRequest(1L, 1L, loanDate, dueDate, null);
    }
}
