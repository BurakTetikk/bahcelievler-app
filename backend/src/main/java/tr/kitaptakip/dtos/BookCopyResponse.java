package tr.kitaptakip.dtos;

import java.time.Instant;
import tr.kitaptakip.enums.BookCopyStatus;

public record BookCopyResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String inventoryCode,
        BookCopyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
