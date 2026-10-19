package tr.kitaptakip.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import tr.kitaptakip.entities.Borrower;

public final class BorrowerSpecifications {
    private BorrowerSpecifications() { }

    public static Specification<Borrower> search(String query, Boolean active) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern, '\\')));
            }
            if (active != null) predicates.add(criteriaBuilder.equal(root.get("active"), active));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
