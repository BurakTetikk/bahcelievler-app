package tr.kitaptakip.services;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kitaptakip.dtos.DashboardResponse;
import tr.kitaptakip.mappers.LoanMapper;
import tr.kitaptakip.repositories.BookCopyRepository;
import tr.kitaptakip.repositories.BookRepository;
import tr.kitaptakip.repositories.LoanRepository;
import tr.kitaptakip.enums.BookCopyStatus;

@Service
public class DashboardService {
    private static final int RECENT_LIMIT = 5;
    private static final int OVERDUE_LIMIT = 10;

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final Clock businessClock;

    public DashboardService(BookRepository bookRepository, BookCopyRepository bookCopyRepository,
                            LoanRepository loanRepository, LoanMapper loanMapper, Clock businessClock) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.businessClock = businessClock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary() {
        LocalDate today = LocalDate.now(businessClock);
        var recent = loanRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_LIMIT))
                .map(loanMapper::toResponse).getContent();
        var overdue = loanRepository.findAllByReturnDateIsNullAndDueDateBeforeOrderByDueDateAsc(today,
                        PageRequest.of(0, OVERDUE_LIMIT))
                .map(loanMapper::toResponse).getContent();
        return new DashboardResponse(bookRepository.count(), bookRepository.countByActiveTrue(), bookCopyRepository.count(),
                bookCopyRepository.countByStatus(BookCopyStatus.AVAILABLE),
                bookCopyRepository.countByStatus(BookCopyStatus.ON_LOAN), loanRepository.countByReturnDateIsNull(),
                loanRepository.countByReturnDateIsNullAndDueDateBefore(today), recent, overdue);
    }
}
