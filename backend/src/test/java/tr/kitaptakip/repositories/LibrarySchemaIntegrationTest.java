package tr.kitaptakip.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tr.kitaptakip.entities.Book;
import tr.kitaptakip.entities.BookCopy;
import tr.kitaptakip.entities.Borrower;
import tr.kitaptakip.entities.Loan;

@SpringBootTest
@Testcontainers
@Transactional
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class LibrarySchemaIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6-alpine");

    @Autowired BookRepository bookRepository;
    @Autowired BookCopyRepository bookCopyRepository;
    @Autowired BorrowerRepository borrowerRepository;
    @Autowired LoanRepository loanRepository;

    @Test
    void appliesMigrationAndPersistsLibraryRelationships() {
        Book book = bookRepository.saveAndFlush(book("The Left Hand of Darkness", "Ursula K. Le Guin", null));
        BookCopy copy = bookCopyRepository.saveAndFlush(new BookCopy(book, "BK-0001"));
        Borrower borrower = borrowerRepository.saveAndFlush(new Borrower("Ada", "Yılmaz", null, null, null));
        Loan loan = loanRepository.saveAndFlush(new Loan(copy, borrower,
                LocalDate.parse("2026-10-18"), LocalDate.parse("2026-10-25"), null));

        assertThat(bookRepository.findById(book.getId())).get().extracting(Book::getTitle)
                .isEqualTo("The Left Hand of Darkness");
        assertThat(loanRepository.findById(loan.getId())).get().extracting(Loan::getBookCopy)
                .extracting(BookCopy::getInventoryCode).isEqualTo("BK-0001");
        assertThat(book.getCopies()).containsExactly(copy);
        assertThat(copy.getLoans()).containsExactly(loan);
        assertThat(borrower.getLoans()).containsExactly(loan);
    }

    @Test
    void rejectsDuplicateNormalizedIsbnButAllowsMoreThanOneMissingIsbn() {
        bookRepository.saveAndFlush(book("First", "Author", "978-0-306-40615-7"));

        assertThatThrownBy(() -> bookRepository.saveAndFlush(book("Second", "Author", "978 0 306 40615 7")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsMoreThanOneMissingIsbn() {
        bookRepository.saveAndFlush(book("No ISBN One", "Author", null));
        bookRepository.saveAndFlush(book("No ISBN Two", "Author", null));
    }

    @Test
    void preventsDuplicateInventoryCodes() {
        Book book = bookRepository.saveAndFlush(book("Book", "Author", null));
        bookCopyRepository.saveAndFlush(new BookCopy(book, "SHELF-12"));

        assertThatThrownBy(() -> bookCopyRepository.saveAndFlush(new BookCopy(book, "SHELF-12")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsOneActiveLoanAfterHistoryAndRejectsASecondActiveLoanForSameCopy() {
        Book book = bookRepository.saveAndFlush(book("Book", "Author", null));
        BookCopy copy = bookCopyRepository.saveAndFlush(new BookCopy(book, "SHELF-13"));
        Borrower firstBorrower = borrowerRepository.saveAndFlush(new Borrower("Ada", "One", null, null, null));
        Borrower secondBorrower = borrowerRepository.saveAndFlush(new Borrower("Ada", "Two", null, null, null));

        Loan previousLoan = new Loan(copy, firstBorrower,
                LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-10"), null);
        previousLoan.markReturned(LocalDate.parse("2026-09-08"));
        loanRepository.saveAndFlush(previousLoan);
        loanRepository.saveAndFlush(new Loan(copy, firstBorrower,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-10"), null));

        assertThatThrownBy(() -> loanRepository.saveAndFlush(new Loan(copy, secondBorrower,
                LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-11"), null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDueDateBeforeLoanDateAtDatabaseLevel() {
        Book book = bookRepository.saveAndFlush(book("Book", "Author", null));
        BookCopy copy = bookCopyRepository.saveAndFlush(new BookCopy(book, "SHELF-14"));
        Borrower borrower = borrowerRepository.saveAndFlush(new Borrower("Ada", "Three", null, null, null));

        assertThatThrownBy(() -> loanRepository.saveAndFlush(new Loan(copy, borrower,
                LocalDate.parse("2026-10-18"), LocalDate.parse("2026-10-17"), null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsPublicationYearOutsideDatabaseRange() {
        assertThatThrownBy(() -> bookRepository.saveAndFlush(
                new Book("Future Book", "Author", null, null, 9999, null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Book book(String title, String author, String isbn) {
        return new Book(title, author, isbn, null, null, null, null);
    }
}
