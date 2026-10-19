package tr.kitaptakip.dtos;

import java.time.Instant;
import java.time.LocalDate;

public record LoanResponse(
        Long id,
        Long copyId,
        String inventoryCode,
        Long bookId,
        String bookTitle,
        Long borrowerId,
        String borrowerFirstName,
        String borrowerLastName,
        LocalDate loanDate,
        LocalDate dueDate,
        LocalDate returnDate,
        boolean active,
        boolean overdue,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
