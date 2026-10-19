package tr.kitaptakip.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tr.kitaptakip.entities.Borrower;

public interface BorrowerRepository extends JpaRepository<Borrower, Long>, JpaSpecificationExecutor<Borrower> {
}
