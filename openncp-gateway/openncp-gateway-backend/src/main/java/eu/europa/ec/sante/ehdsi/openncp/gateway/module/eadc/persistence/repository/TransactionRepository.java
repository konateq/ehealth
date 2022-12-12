package eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc.persistence.repository;

import eu.europa.ec.sante.ehdsi.openncp.gateway.module.eadc.persistence.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    @Query(
            // @formatter:off
            "select trim(substring(t.startTime, 12, 5)) as year from Transaction t where trim(substring(t.startTime, 12, 5)) <> '' group by year order by trim(substring(t.startTime, 12, 5)) asc"
            // @formatter:on
    )
    List<String> findAllYears();
}
