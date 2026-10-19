package tr.kitaptakip.mappers;

import org.springframework.stereotype.Component;
import tr.kitaptakip.dtos.BookRequest;
import tr.kitaptakip.dtos.BookResponse;
import tr.kitaptakip.entities.Book;

@Component
public class BookMapper {
    public Book toEntity(BookRequest request) {
        return new Book(request.title().trim(), request.author().trim(), request.isbn(), request.publisher(),
                request.publicationYear(), request.category(), request.description());
    }

    public void updateEntity(Book book, BookRequest request) {
        book.updateDetails(request.title().trim(), request.author().trim(), request.isbn(), request.publisher(),
                request.publicationYear(), request.category(), request.description());
    }

    public BookResponse toResponse(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(),
                book.getPublisher(), book.getPublicationYear(), book.getCategory(), book.getDescription(),
                book.isActive(), book.getCreatedAt(), book.getUpdatedAt());
    }
}
