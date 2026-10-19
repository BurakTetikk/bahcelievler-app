package tr.kitaptakip.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BorrowerRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @Size(max = 10000) String notes
) {
}
