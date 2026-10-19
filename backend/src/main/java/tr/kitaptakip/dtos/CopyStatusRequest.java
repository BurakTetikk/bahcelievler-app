package tr.kitaptakip.dtos;

import jakarta.validation.constraints.NotNull;
import tr.kitaptakip.enums.BookCopyStatus;

public record CopyStatusRequest(@NotNull BookCopyStatus status) {
}
