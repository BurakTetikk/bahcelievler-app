package tr.kitaptakip.services;

import java.time.LocalDate;
import java.time.Clock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kitaptakip.dtos.CreateLoanRequest;
import tr.kitaptakip.dtos.LoanResponse;
import tr.kitaptakip.dtos.PageResponse;
import tr.kitaptakip.dtos.ReturnLoanRequest;
import tr.kitaptakip.entities.BookCopy;
import tr.kitaptakip.entities.Borrower;
import tr.kitaptakip.entities.Loan;
import tr.kitaptakip.enums.BookCopyStatus;
import tr.kitaptakip.exceptions.BusinessRuleException;
import tr.kitaptakip.exceptions.ConflictException;
import tr.kitaptakip.exceptions.ResourceNotFoundException;
import tr.kitaptakip.mappers.LoanMapper;
import tr.kitaptakip.repositories.BookCopyRepository;
import tr.kitaptakip.repositories.BorrowerRepository;
import tr.kitaptakip.repositories.LoanRepository;
import tr.kitaptakip.specifications.LoanSpecifications;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BorrowerRepository borrowerRepository;
    private final LoanMapper loanMapper;
    private final Clock businessClock;

    public LoanService(LoanRepository loanRepository, BookCopyRepository bookCopyRepository,
                       BorrowerRepository borrowerRepository, LoanMapper loanMapper, Clock businessClock) {
        this.loanRepository = loanRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.borrowerRepository = borrowerRepository;
        this.loanMapper = loanMapper;
        this.businessClock = businessClock;
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> list(String query, Long borrowerId, Boolean active, Boolean overdue,
                                           LocalDate loanDateFrom, LocalDate loanDateTo,
                                           LocalDate dueDateFrom, LocalDate dueDateTo, Pageable pageable) {
        validateRange(loanDateFrom, loanDateTo, "Ödünç tarihi");
        validateRange(dueDateFrom, dueDateTo, "Son teslim tarihi");
        Specification<Loan> specification = LoanSpecifications.filter(query, borrowerId, active, overdue,
                loanDateFrom, loanDateTo, dueDateFrom, dueDateTo, LocalDate.now(businessClock));
        Page<LoanResponse> result = loanRepository.findAll(specification, pageable).map(loanMapper::toResponse);
        return PageResponse.from(result, result.getContent());
    }

    @Transactional(readOnly = true)
    public LoanResponse get(long id) {
        return loanMapper.toResponse(findLoan(id));
    }

    @Transactional
    public LoanResponse create(CreateLoanRequest request) {
        BookCopy copy = bookCopyRepository.findWithBookForUpdate(request.copyId())
                .orElseThrow(() -> new ResourceNotFoundException("Fiziksel kopya bulunamadı."));
        if (!copy.getBook().isActive()) {
            throw new BusinessRuleException("Arşivlenmiş bir kitabın kopyası ödünç verilemez.");
        }
        if (copy.getStatus() != BookCopyStatus.AVAILABLE) {
            throw new ConflictException("Yalnızca AVAILABLE durumundaki kopyalar ödünç verilebilir.");
        }
        Borrower borrower = borrowerRepository.findById(request.borrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Ödünç alan kişi bulunamadı."));
        if (!borrower.isActive()) {
            throw new BusinessRuleException("Pasif durumdaki kişiye ödünç verilemez.");
        }
        if (request.dueDate().isBefore(request.loanDate())) {
            throw new BusinessRuleException("Son teslim tarihi ödünç tarihinden önce olamaz.");
        }

        Loan loan = new Loan(copy, borrower, request.loanDate(), request.dueDate(), clean(request.notes()));
        copy.changeStatus(BookCopyStatus.ON_LOAN);
        return loanMapper.toResponse(loanRepository.saveAndFlush(loan));
    }

    @Transactional
    public LoanResponse returnLoan(long id, ReturnLoanRequest request) {
        Loan loan = loanRepository.findWithDetailsForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödünç kaydı bulunamadı."));
        if (!loan.isActive()) throw new ConflictException("Bu ödünç kaydı daha önce iade edilmiş.");
        if (request.returnDate().isBefore(loan.getLoanDate())) {
            throw new BusinessRuleException("İade tarihi ödünç tarihinden önce olamaz.");
        }
        if (request.returnDate().isAfter(LocalDate.now(businessClock))) {
            throw new BusinessRuleException("İade tarihi gelecek bir tarih olamaz.");
        }
        BookCopyStatus finalStatus = request.copyStatus() == null ? BookCopyStatus.AVAILABLE : request.copyStatus();
        if (finalStatus == BookCopyStatus.ON_LOAN) {
            throw new BusinessRuleException("İade sonrası kopya durumu ON_LOAN olamaz.");
        }

        loan.markReturned(request.returnDate());
        loan.getBookCopy().changeStatus(finalStatus);
        return loanMapper.toResponse(loanRepository.saveAndFlush(loan));
    }

    private Loan findLoan(long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödünç kaydı bulunamadı."));
    }

    private void validateRange(LocalDate from, LocalDate to, String label) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException(label + " başlangıcı bitişinden sonra olamaz.");
        }
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
