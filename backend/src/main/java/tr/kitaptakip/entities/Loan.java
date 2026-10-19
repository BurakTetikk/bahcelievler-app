package tr.kitaptakip.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loans")
public class Loan extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_copy_id", nullable = false)
    private BookCopy bookCopy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(columnDefinition = "text")
    private String notes;

    protected Loan() {
    }

    public Loan(BookCopy bookCopy, Borrower borrower, LocalDate loanDate, LocalDate dueDate, String notes) {
        this.bookCopy = bookCopy;
        this.borrower = borrower;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.notes = notes;
        bookCopy.addLoan(this);
        borrower.addLoan(this);
    }

    public boolean isActive() {
        return returnDate == null;
    }

    public boolean isOverdue(LocalDate today) {
        return isActive() && dueDate.isBefore(today);
    }

    public void markReturned(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public Long getId() { return id; }
    public BookCopy getBookCopy() { return bookCopy; }
    public Borrower getBorrower() { return borrower; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getNotes() { return notes; }
}
