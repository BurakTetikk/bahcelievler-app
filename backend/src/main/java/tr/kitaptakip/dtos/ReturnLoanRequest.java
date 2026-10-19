package tr.kitaptakip.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import tr.kitaptakip.enums.BookCopyStatus;

public record ReturnLoanRequest(
        @NotNull LocalDate returnDate,
        BookCopyStatus copyStatus
) {
}
