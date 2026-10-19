package tr.kitaptakip.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class LoanTest {
    private static final LocalDate TODAY = LocalDate.parse("2026-10-18");

    @Test
    void dueTodayIsNotOverdue() {
        Loan loan = loanWithDueDate(TODAY);

        assertThat(loan.isOverdue(TODAY)).isFalse();
    }

    @Test
    void activeLoanDueBeforeTodayIsOverdue() {
        Loan loan = loanWithDueDate(TODAY.minusDays(1));

        assertThat(loan.isOverdue(TODAY)).isTrue();
    }

    @Test
    void returnedLoanIsNotCurrentlyOverdue() {
        Loan loan = loanWithDueDate(TODAY.minusDays(2));
        loan.markReturned(TODAY);

        assertThat(loan.isOverdue(TODAY)).isFalse();
    }

    private Loan loanWithDueDate(LocalDate dueDate) {
        Book book = new Book("Book", "Author", null, null, null, null, null);
        BookCopy copy = new BookCopy(book, "TEST-1");
        Borrower borrower = new Borrower("Ada", "Yılmaz", null, null, null);
        return new Loan(copy, borrower, TODAY.minusDays(7), dueDate, null);
    }
}
