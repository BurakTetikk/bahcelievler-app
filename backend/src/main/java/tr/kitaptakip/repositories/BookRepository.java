package tr.kitaptakip.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tr.kitaptakip.entities.Book;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    long countByActiveTrue();
    boolean existsByIsbn(String isbn);
    Optional<Book> findByIsbn(String isbn);
}
