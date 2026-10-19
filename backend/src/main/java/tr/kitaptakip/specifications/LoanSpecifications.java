package tr.kitaptakip.specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import tr.kitaptakip.entities.Loan;

public final class LoanSpecifications {
    private LoanSpecifications() { }

    public static Specification<Loan> filter(String query, Long borrowerId, Boolean active, Boolean overdue,
                                              LocalDate loanDateFrom, LocalDate loanDateTo,
                                              LocalDate dueDateFrom, LocalDate dueDateTo, LocalDate today) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                var copy = root.join("bookCopy");
                var borrower = root.join("borrower");
                var book = copy.join("book");
                predicates.add(cb.or(cb.like(cb.lower(book.get("title")), pattern, '\\'),
                        cb.like(cb.lower(copy.get("inventoryCode")), pattern, '\\'),
                        cb.like(cb.lower(borrower.get("firstName")), pattern, '\\'),
                        cb.like(cb.lower(borrower.get("lastName")), pattern, '\\')));
            }
            if (borrowerId != null) predicates.add(cb.equal(root.get("borrower").get("id"), borrowerId));
            if (active != null) predicates.add(active ? cb.isNull(root.get("returnDate")) : cb.isNotNull(root.get("returnDate")));
            if (Boolean.TRUE.equals(overdue)) {
                predicates.add(cb.isNull(root.get("returnDate")));
                predicates.add(cb.lessThan(root.get("dueDate"), today));
            } else if (Boolean.FALSE.equals(overdue)) {
                predicates.add(cb.or(cb.isNotNull(root.get("returnDate")),
                        cb.greaterThanOrEqualTo(root.get("dueDate"), today)));
            }
            if (loanDateFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("loanDate"), loanDateFrom));
            if (loanDateTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("loanDate"), loanDateTo));
            if (dueDateFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueDateFrom));
            if (dueDateTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueDateTo));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
