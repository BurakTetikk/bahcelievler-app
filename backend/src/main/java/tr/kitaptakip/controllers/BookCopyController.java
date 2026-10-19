package tr.kitaptakip.controllers;

import java.net.URI;
import java.util.Set;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.kitaptakip.dtos.BookCopyRequest;
import tr.kitaptakip.dtos.BookCopyResponse;
import tr.kitaptakip.dtos.CopyStatusRequest;
import tr.kitaptakip.dtos.PageResponse;
import tr.kitaptakip.enums.BookCopyStatus;
import tr.kitaptakip.services.BookCopyService;
import tr.kitaptakip.utils.PageRequestFactory;

@RestController
@RequestMapping("/api/v1")
public class BookCopyController {
    private static final Set<String> SORT_FIELDS = Set.of("inventoryCode", "status", "createdAt", "updatedAt");

    private final BookCopyService bookCopyService;

    public BookCopyController(BookCopyService bookCopyService) {
        this.bookCopyService = bookCopyService;
    }

    @GetMapping("/books/{bookId}/copies")
    public PageResponse<BookCopyResponse> listForBook(
            @PathVariable long bookId,
            @RequestParam(required = false) BookCopyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "inventoryCode,asc") String sort
    ) {
        return bookCopyService.listForBook(bookId, status,
                PageRequestFactory.create(page, size, sort, SORT_FIELDS, "inventoryCode,asc"));
    }

    @PostMapping("/books/{bookId}/copies")
    public ResponseEntity<BookCopyResponse> create(
            @PathVariable long bookId,
            @Valid @RequestBody BookCopyRequest request
    ) {
        BookCopyResponse created = bookCopyService.create(bookId, request);
        return ResponseEntity.created(URI.create("/api/v1/copies/" + created.id())).body(created);
    }

    @GetMapping("/copies/{copyId}")
    public BookCopyResponse get(@PathVariable long copyId) {
        return bookCopyService.get(copyId);
    }

    @PatchMapping("/copies/{copyId}/status")
    public BookCopyResponse changeStatus(@PathVariable long copyId, @Valid @RequestBody CopyStatusRequest request) {
        return bookCopyService.changeStatus(copyId, request);
    }
}
