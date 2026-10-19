package tr.kitaptakip.mappers;

import org.springframework.stereotype.Component;
import tr.kitaptakip.dtos.BorrowerRequest;
import tr.kitaptakip.dtos.BorrowerResponse;
import tr.kitaptakip.entities.Borrower;

@Component
public class BorrowerMapper {
    public Borrower toEntity(BorrowerRequest request) {
        return new Borrower(clean(request.firstName()), clean(request.lastName()), clean(request.email()),
                clean(request.phone()), clean(request.notes()));
    }

    public void updateEntity(Borrower borrower, BorrowerRequest request) {
        borrower.updateDetails(clean(request.firstName()), clean(request.lastName()), clean(request.email()),
                clean(request.phone()), clean(request.notes()));
    }

    public BorrowerResponse toResponse(Borrower borrower) {
        return new BorrowerResponse(borrower.getId(), borrower.getFirstName(), borrower.getLastName(),
                borrower.getEmail(), borrower.getPhone(), borrower.getNotes(), borrower.isActive(),
                borrower.getCreatedAt(), borrower.getUpdatedAt());
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
