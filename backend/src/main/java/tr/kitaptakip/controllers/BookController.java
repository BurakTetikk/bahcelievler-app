package tr.kitaptakip.controllers;

import java.net.URI;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.kitaptakip.dtos.BookRequest;
import tr.kitaptakip.dtos.BookResponse;
import tr.kitaptakip.dtos.PageResponse;
import tr.kitaptakip.enums.BookCopyStatus;
import tr.kitaptakip.services.BookService;
import tr.kitaptakip.utils.PageRequestFactory;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {
    private static final Set<String> SORT_FIELDS = Set.of(
            "title", "author", "publicationYear", "category", "createdAt", "updatedAt");

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public PageResponse<BookResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BookCopyStatus copyStatus,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort
    ) {
        return bookService.list(q, category, copyStatus, includeArchived,
                PageRequestFactory.create(page, size, sort, SORT_FIELDS, "title,asc"));
    }

    @GetMapping("/{id}")
    public BookResponse get(@PathVariable long id) {
        return bookService.get(id);
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        BookResponse created = bookService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/books/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable long id, @Valid @RequestBody BookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable long id) {
        bookService.archive(id);
        return ResponseEntity.noContent().build();
    }
}
