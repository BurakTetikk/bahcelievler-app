package tr.kitaptakip.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tr.kitaptakip.dtos.validation.ValidLoanDates;

@ValidLoanDates
public record CreateLoanRequest(
        @NotNull Long copyId,
        @NotNull Long borrowerId,
        @NotNull LocalDate loanDate,
        @NotNull LocalDate dueDate,
        @Size(max = 10000) String notes
) {
}
