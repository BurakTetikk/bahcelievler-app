package tr.kitaptakip.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kitaptakip.dtos.BookRequest;
import tr.kitaptakip.dtos.BookResponse;
import tr.kitaptakip.dtos.PageResponse;
import tr.kitaptakip.entities.Book;
import tr.kitaptakip.enums.BookCopyStatus;
import tr.kitaptakip.exceptions.ConflictException;
import tr.kitaptakip.exceptions.ResourceNotFoundException;
import tr.kitaptakip.mappers.BookMapper;
import tr.kitaptakip.repositories.BookRepository;
import tr.kitaptakip.specifications.BookSpecifications;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> list(String query, String category, BookCopyStatus copyStatus,
                                           boolean includeArchived, Pageable pageable) {
        Specification<Book> specification = BookSpecifications.search(query, category, copyStatus, includeArchived);
        Page<BookResponse> result = bookRepository.findAll(specification, pageable).map(bookMapper::toResponse);
        return PageResponse.from(result, result.getContent());
    }

    @Transactional(readOnly = true)
    public BookResponse get(long id) {
        return bookMapper.toResponse(findBook(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        Book book = bookMapper.toEntity(request);
        ensureIsbnAvailable(book.getIsbn(), null);
        return bookMapper.toResponse(bookRepository.saveAndFlush(book));
    }

    @Transactional
    public BookResponse update(long id, BookRequest request) {
        Book book = findBook(id);
        bookMapper.updateEntity(book, request);
        ensureIsbnAvailable(book.getIsbn(), book.getId());
        return bookMapper.toResponse(bookRepository.saveAndFlush(book));
    }

    @Transactional
    public void archive(long id) {
        Book book = findBook(id);
        book.archive();
    }

    private Book findBook(long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kitap bulunamadı."));
    }

    private void ensureIsbnAvailable(String isbn, Long currentBookId) {
        if (isbn == null) {
            return;
        }
        bookRepository.findByIsbn(isbn)
                .filter(book -> !book.getId().equals(currentBookId))
                .ifPresent(book -> {
                    throw new ConflictException("Bu ISBN ile kayıtlı başka bir kitap var.");
                });
    }
}
