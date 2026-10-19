package tr.kitaptakip.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookCopyRequest(
        @NotBlank @Size(max = 50) String inventoryCode
) {
}
