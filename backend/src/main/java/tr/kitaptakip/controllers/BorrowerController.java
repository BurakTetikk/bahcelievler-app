package tr.kitaptakip.controllers;

import java.net.URI;
import java.util.Set;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tr.kitaptakip.dtos.BorrowerRequest;
import tr.kitaptakip.dtos.BorrowerResponse;
import tr.kitaptakip.dtos.BorrowerStatusRequest;
import tr.kitaptakip.dtos.LoanResponse;
import tr.kitaptakip.dtos.PageResponse;
import tr.kitaptakip.services.BorrowerService;
import tr.kitaptakip.services.LoanService;
import tr.kitaptakip.utils.PageRequestFactory;

@RestController
@RequestMapping("/api/v1/borrowers")
public class BorrowerController {
    private static final Set<String> SORT_FIELDS = Set.of("firstName", "lastName", "email", "active", "createdAt", "updatedAt");
    private final BorrowerService borrowerService;
    private final LoanService loanService;

    public BorrowerController(BorrowerService borrowerService, LoanService loanService) {
        this.borrowerService = borrowerService;
        this.loanService = loanService;
    }

    @GetMapping
    public PageResponse<BorrowerResponse> list(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) Boolean active,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "lastName,asc") String sort) {
        return borrowerService.list(q, active, PageRequestFactory.create(page, size, sort, SORT_FIELDS, "lastName,asc"));
    }

    @PostMapping
    public ResponseEntity<BorrowerResponse> create(@Valid @RequestBody BorrowerRequest request) {
        BorrowerResponse created = borrowerService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/borrowers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public BorrowerResponse get(@PathVariable long id) { return borrowerService.get(id); }

    @PutMapping("/{id}")
    public BorrowerResponse update(@PathVariable long id, @Valid @RequestBody BorrowerRequest request) {
        return borrowerService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public BorrowerResponse changeStatus(@PathVariable long id, @Valid @RequestBody BorrowerStatusRequest request) {
        return borrowerService.changeStatus(id, request);
    }

    @GetMapping("/{id}/loans")
    public PageResponse<LoanResponse> history(@PathVariable long id,
                                               @RequestParam(required = false) Boolean active,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "loanDate,desc") String sort) {
        borrowerService.get(id);
        Set<String> loanSortFields = Set.of("loanDate", "dueDate", "returnDate", "createdAt", "updatedAt", "id");
        return loanService.list(null, id, active, null, null, null, null, null,
                PageRequestFactory.create(page, size, sort, loanSortFields, "loanDate,desc"));
    }
}
