package tr.kitaptakip.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tr.kitaptakip.dtos.BorrowerRequest;
import tr.kitaptakip.dtos.BorrowerResponse;
import tr.kitaptakip.dtos.BorrowerStatusRequest;
import tr.kitaptakip.dtos.PageResponse;
import tr.kitaptakip.entities.Borrower;
import tr.kitaptakip.exceptions.ResourceNotFoundException;
import tr.kitaptakip.mappers.BorrowerMapper;
import tr.kitaptakip.repositories.BorrowerRepository;
import tr.kitaptakip.specifications.BorrowerSpecifications;

@Service
public class BorrowerService {
    private final BorrowerRepository borrowerRepository;
    private final BorrowerMapper borrowerMapper;

    public BorrowerService(BorrowerRepository borrowerRepository, BorrowerMapper borrowerMapper) {
        this.borrowerRepository = borrowerRepository;
        this.borrowerMapper = borrowerMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<BorrowerResponse> list(String query, Boolean active, Pageable pageable) {
        Specification<Borrower> specification = BorrowerSpecifications.search(query, active);
        Page<BorrowerResponse> result = borrowerRepository.findAll(specification, pageable).map(borrowerMapper::toResponse);
        return PageResponse.from(result, result.getContent());
    }

    @Transactional(readOnly = true)
    public BorrowerResponse get(long id) {
        return borrowerMapper.toResponse(findBorrower(id));
    }

    @Transactional
    public BorrowerResponse create(BorrowerRequest request) {
        return borrowerMapper.toResponse(borrowerRepository.saveAndFlush(borrowerMapper.toEntity(request)));
    }

    @Transactional
    public BorrowerResponse update(long id, BorrowerRequest request) {
        Borrower borrower = findBorrower(id);
        borrowerMapper.updateEntity(borrower, request);
        return borrowerMapper.toResponse(borrowerRepository.saveAndFlush(borrower));
    }

    @Transactional
    public BorrowerResponse changeStatus(long id, BorrowerStatusRequest request) {
        Borrower borrower = findBorrower(id);
        borrower.setActive(request.active());
        return borrowerMapper.toResponse(borrowerRepository.saveAndFlush(borrower));
    }

    private Borrower findBorrower(long id) {
        return borrowerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ödünç alan kişi bulunamadı."));
    }
}
