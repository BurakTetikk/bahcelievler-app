package tr.kitaptakip.dtos;

import java.time.Instant;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String publisher,
        Integer publicationYear,
        String category,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
