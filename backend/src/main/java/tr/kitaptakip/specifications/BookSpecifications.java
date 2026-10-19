package tr.kitaptakip.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import tr.kitaptakip.entities.Book;
import tr.kitaptakip.entities.BookCopy;
import tr.kitaptakip.enums.BookCopyStatus;

public final class BookSpecifications {
    private BookSpecifications() {
    }

    public static Specification<Book> search(String query, String category,
                                             BookCopyStatus copyStatus, boolean includeArchived) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!includeArchived) {
                predicates.add(criteriaBuilder.isTrue(root.get("active")));
            }
            if (query != null && !query.isBlank()) {
                String normalized = query.trim().toLowerCase(Locale.ROOT);
                String titleAuthorPattern = "%" + escapeLike(normalized) + "%";
                String isbnQuery = normalized.replaceAll("[\\s-]", "");
                Predicate titleOrAuthor = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), titleAuthorPattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("author")), titleAuthorPattern, '\\'));
                if (isbnQuery.isBlank()) {
                    predicates.add(titleOrAuthor);
                } else {
                    String isbnPattern = "%" + escapeLike(isbnQuery) + "%";
                    predicates.add(criteriaBuilder.or(titleOrAuthor,
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("isbn")), isbnPattern, '\\')));
                }
            }
            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")),
                        category.trim().toLowerCase(Locale.ROOT)));
            }
            if (copyStatus != null) {
                Subquery<Long> matchingCopy = criteriaQuery.subquery(Long.class);
                var copyRoot = matchingCopy.from(BookCopy.class);
                matchingCopy.select(copyRoot.get("id"))
                        .where(criteriaBuilder.equal(copyRoot.get("book").get("id"), root.get("id")),
                                criteriaBuilder.equal(copyRoot.get("status"), copyStatus));
                predicates.add(criteriaBuilder.exists(matchingCopy));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
