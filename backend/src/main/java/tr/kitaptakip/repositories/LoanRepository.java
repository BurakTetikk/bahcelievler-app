package tr.kitaptakip.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import tr.kitaptakip.entities.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {
    boolean existsByBookCopyIdAndReturnDateIsNull(Long bookCopyId);
    List<Loan> findAllByReturnDateIsNullOrderByDueDateAsc();
    List<Loan> findAllByReturnDateIsNullAndDueDateBeforeOrderByDueDateAsc(LocalDate date);
    List<Loan> findAllByBorrowerIdOrderByLoanDateDesc(Long borrowerId);
    long countByReturnDateIsNull();
    long countByReturnDateIsNullAndDueDateBefore(LocalDate date);

    @EntityGraph(attributePaths = {"bookCopy", "bookCopy.book", "borrower"})
    org.springframework.data.domain.Page<Loan> findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = {"bookCopy", "bookCopy.book", "borrower"})
    org.springframework.data.domain.Page<Loan> findAllByReturnDateIsNullAndDueDateBeforeOrderByDueDateAsc(
            LocalDate date, org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l join fetch l.bookCopy c join fetch c.book join fetch l.borrower where l.id = :id")
    java.util.Optional<Loan> findWithDetailsForUpdate(@Param("id") Long id);
}
