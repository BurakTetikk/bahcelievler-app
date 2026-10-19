package tr.kitaptakip.dtos;

import java.time.Instant;

public record BorrowerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
