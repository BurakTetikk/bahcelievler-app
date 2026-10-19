package tr.kitaptakip.dtos;

import jakarta.validation.constraints.NotNull;

public record BorrowerStatusRequest(@NotNull Boolean active) {
}
