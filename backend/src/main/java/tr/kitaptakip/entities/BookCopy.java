package tr.kitaptakip.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import tr.kitaptakip.enums.BookCopyStatus;

@Entity
@Table(name = "book_copies", uniqueConstraints =
        @UniqueConstraint(name = "uk_book_copies_inventory_code", columnNames = "inventory_code"))
public class BookCopy extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "inventory_code", nullable = false, length = 50)
    private String inventoryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookCopyStatus status = BookCopyStatus.AVAILABLE;

    @OneToMany(mappedBy = "bookCopy", fetch = FetchType.LAZY)
    private List<Loan> loans = new ArrayList<>();

    protected BookCopy() {
    }

    public BookCopy(Book book, String inventoryCode) {
        this.book = book;
        this.inventoryCode = inventoryCode;
        this.status = BookCopyStatus.AVAILABLE;
        book.addCopy(this);
    }

    public void changeStatus(BookCopyStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    void addLoan(Loan loan) { loans.add(loan); }
    public List<Loan> getLoans() { return List.copyOf(loans); }
    public Book getBook() { return book; }
    public String getInventoryCode() { return inventoryCode; }
    public BookCopyStatus getStatus() { return status; }
}
