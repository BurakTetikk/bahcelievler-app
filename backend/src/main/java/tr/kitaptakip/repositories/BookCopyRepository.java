package tr.kitaptakip.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tr.kitaptakip.entities.BookCopy;
import tr.kitaptakip.enums.BookCopyStatus;

import jakarta.persistence.LockModeType;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    List<BookCopy> findAllByBookIdOrderByInventoryCodeAsc(Long bookId);
    Page<BookCopy> findAllByBookId(Long bookId, Pageable pageable);
    Page<BookCopy> findAllByBookIdAndStatus(Long bookId, BookCopyStatus status, Pageable pageable);
    List<BookCopy> findAllByStatus(BookCopyStatus status);
    boolean existsByInventoryCode(String inventoryCode);
    long countByStatus(BookCopyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopy c join fetch c.book where c.id = :id")
    java.util.Optional<BookCopy> findWithBookForUpdate(@Param("id") Long id);
}
