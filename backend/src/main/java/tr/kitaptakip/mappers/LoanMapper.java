package tr.kitaptakip.mappers;

import java.time.LocalDate;
import java.time.Clock;

import org.springframework.stereotype.Component;
import tr.kitaptakip.dtos.LoanResponse;
import tr.kitaptakip.entities.Loan;

@Component
public class LoanMapper {
    private final Clock businessClock;

    public LoanMapper(Clock businessClock) { this.businessClock = businessClock; }

    public LoanResponse toResponse(Loan loan) {
        var copy = loan.getBookCopy();
        var book = copy.getBook();
        var borrower = loan.getBorrower();
        return new LoanResponse(loan.getId(), copy.getId(), copy.getInventoryCode(), book.getId(), book.getTitle(),
                borrower.getId(), borrower.getFirstName(), borrower.getLastName(), loan.getLoanDate(),
                loan.getDueDate(), loan.getReturnDate(), loan.isActive(), loan.isOverdue(LocalDate.now(businessClock)),
                loan.getNotes(), loan.getCreatedAt(), loan.getUpdatedAt());
    }
}
