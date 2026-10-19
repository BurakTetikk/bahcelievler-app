package tr.kitaptakip.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "books", uniqueConstraints =
        @UniqueConstraint(name = "uk_books_isbn", columnNames = "isbn"))
public class Book extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(length = 20)
    private String isbn;

    @Column(length = 150)
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    private List<BookCopy> copies = new ArrayList<>();

    protected Book() {
    }

    public Book(String title, String author, String isbn, String publisher,
                Integer publicationYear, String category, String description) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.category = category;
        this.description = description;
        normalizeIsbn();
    }

    @PrePersist
    @PreUpdate
    private void normalizeIsbn() {
        if (isbn != null) {
            isbn = isbn.replaceAll("[\\s-]", "").toUpperCase(java.util.Locale.ROOT);
            if (isbn.isBlank()) {
                isbn = null;
            }
        }
    }

    public void updateDetails(String title, String author, String isbn, String publisher,
                              Integer publicationYear, String category, String description) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.category = category;
        this.description = description;
        normalizeIsbn();
    }

    public void archive() {
        active = false;
    }

    public void reactivate() {
        active = true;
    }

    public Long getId() { return id; }
    void addCopy(BookCopy copy) { copies.add(copy); }
    public List<BookCopy> getCopies() { return List.copyOf(copies); }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getPublisher() { return publisher; }
    public Integer getPublicationYear() { return publicationYear; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }

}
