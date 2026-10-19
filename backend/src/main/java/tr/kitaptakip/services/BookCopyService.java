package tr.kitaptakip.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kitaptakip.dtos.BookCopyRequest;
import tr.kitaptakip.dtos.BookCopyResponse;
import tr.kitaptakip.dtos.PageResponse;
import tr.kitaptakip.dtos.CopyStatusRequest;
import tr.kitaptakip.entities.Book;
import tr.kitaptakip.entities.BookCopy;
import tr.kitaptakip.enums.BookCopyStatus;
import tr.kitaptakip.exceptions.BusinessRuleException;
import tr.kitaptakip.exceptions.ConflictException;
import tr.kitaptakip.exceptions.ResourceNotFoundException;
import tr.kitaptakip.mappers.BookCopyMapper;
import tr.kitaptakip.repositories.BookCopyRepository;
import tr.kitaptakip.repositories.BookRepository;

@Service
public class BookCopyService {
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookCopyMapper bookCopyMapper;

    public BookCopyService(BookRepository bookRepository, BookCopyRepository bookCopyRepository,
                           BookCopyMapper bookCopyMapper) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.bookCopyMapper = bookCopyMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookCopyResponse> listForBook(long bookId, BookCopyStatus status, Pageable pageable) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Kitap bulunamadı.");
        }
        Page<BookCopy> copies = status == null
                ? bookCopyRepository.findAllByBookId(bookId, pageable)
                : bookCopyRepository.findAllByBookIdAndStatus(bookId, status, pageable);
        Page<BookCopyResponse> result = copies.map(bookCopyMapper::toResponse);
        return PageResponse.from(result, result.getContent());
    }

    @Transactional(readOnly = true)
    public BookCopyResponse get(long copyId) {
        return bookCopyMapper.toResponse(findCopy(copyId));
    }

    @Transactional
    public BookCopyResponse create(long bookId, BookCopyRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Kitap bulunamadı."));
        if (!book.isActive()) {
            throw new BusinessRuleException("Arşivlenmiş bir kitaba yeni fiziksel kopya eklenemez.");
        }

        String inventoryCode = request.inventoryCode().trim();
        if (bookCopyRepository.existsByInventoryCode(inventoryCode)) {
            throw new ConflictException("Bu envanter kodu zaten kullanılıyor.");
        }

        BookCopy copy = bookCopyRepository.saveAndFlush(new BookCopy(book, inventoryCode));
        return bookCopyMapper.toResponse(copy);
    }

    @Transactional
    public BookCopyResponse changeStatus(long copyId, CopyStatusRequest request) {
        BookCopy copy = findCopy(copyId);
        BookCopyStatus current = copy.getStatus();
        BookCopyStatus target = request.status();

        if (current == target) {
            return bookCopyMapper.toResponse(copy);
        }
        if (current == BookCopyStatus.ON_LOAN) {
            throw new ConflictException("Ödünçteki kopyanın durumu iade işlemi üzerinden değiştirilmelidir.");
        }
        if (target == BookCopyStatus.ON_LOAN) {
            throw new BusinessRuleException("ON_LOAN durumu yalnızca ödünç verme işlemiyle atanabilir.");
        }

        boolean allowed = current == BookCopyStatus.AVAILABLE
                || target == BookCopyStatus.AVAILABLE;
        if (!allowed) {
            throw new BusinessRuleException("Kayıp veya hasarlı kopya yalnızca AVAILABLE durumuna alınabilir.");
        }

        copy.changeStatus(target);
        return bookCopyMapper.toResponse(bookCopyRepository.saveAndFlush(copy));
    }

    private BookCopy findCopy(long copyId) {
        return bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ResourceNotFoundException("Fiziksel kopya bulunamadı."));
    }
}
