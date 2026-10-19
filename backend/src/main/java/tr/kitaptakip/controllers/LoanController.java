package tr.kitaptakip.controllers;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.kitaptakip.dtos.CreateLoanRequest;
import tr.kitaptakip.dtos.LoanResponse;
import tr.kitaptakip.dtos.PageResponse;
import tr.kitaptakip.dtos.ReturnLoanRequest;
import tr.kitaptakip.services.LoanService;
import tr.kitaptakip.utils.PageRequestFactory;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {
    private static final Set<String> SORT_FIELDS = Set.of("loanDate", "dueDate", "returnDate", "createdAt", "updatedAt", "id");
    private final LoanService loanService;

    public LoanController(LoanService loanService) { this.loanService = loanService; }

    @GetMapping
    public PageResponse<LoanResponse> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Long borrowerId,
                                           @RequestParam(required = false) Boolean active,
                                           @RequestParam(required = false) Boolean overdue,
                                           @RequestParam(required = false) LocalDate loanDateFrom,
                                           @RequestParam(required = false) LocalDate loanDateTo,
                                           @RequestParam(required = false) LocalDate dueDateFrom,
                                           @RequestParam(required = false) LocalDate dueDateTo,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(defaultValue = "loanDate,desc") String sort) {
        return loanService.list(q, borrowerId, active, overdue, loanDateFrom, loanDateTo, dueDateFrom, dueDateTo,
                PageRequestFactory.create(page, size, sort, SORT_FIELDS, "loanDate,desc"));
    }

    @GetMapping("/active")
    public PageResponse<LoanResponse> active(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(defaultValue = "dueDate,asc") String sort) {
        return loanService.list(null, null, true, null, null, null, null, null,
                PageRequestFactory.create(page, size, sort, SORT_FIELDS, "dueDate,asc"));
    }

    @GetMapping("/overdue")
    public PageResponse<LoanResponse> overdue(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "dueDate,asc") String sort) {
        return loanService.list(null, null, true, true, null, null, null, null,
                PageRequestFactory.create(page, size, sort, SORT_FIELDS, "dueDate,asc"));
    }

    @GetMapping("/{id}")
    public LoanResponse get(@PathVariable long id) { return loanService.get(id); }

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody CreateLoanRequest request) {
        LoanResponse created = loanService.create(request);
        return ResponseEntity.created(java.net.URI.create("/api/v1/loans/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/return")
    public LoanResponse returnLoan(@PathVariable long id, @Valid @RequestBody ReturnLoanRequest request) {
        return loanService.returnLoan(id, request);
    }
}
