package tr.kitaptakip.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 150) String author,
        @Size(max = 20) String isbn,
        @Size(max = 150) String publisher,
        @Min(0) @Max(3000) Integer publicationYear,
        @Size(max = 100) String category,
        @Size(max = 10000) String description
) {
}
