package tr.kitaptakip.mappers;

import org.springframework.stereotype.Component;
import tr.kitaptakip.dtos.BookCopyResponse;
import tr.kitaptakip.entities.BookCopy;

@Component
public class BookCopyMapper {
    public BookCopyResponse toResponse(BookCopy copy) {
        return new BookCopyResponse(copy.getId(), copy.getBook().getId(), copy.getBook().getTitle(),
                copy.getInventoryCode(), copy.getStatus(), copy.getCreatedAt(), copy.getUpdatedAt());
    }
}
