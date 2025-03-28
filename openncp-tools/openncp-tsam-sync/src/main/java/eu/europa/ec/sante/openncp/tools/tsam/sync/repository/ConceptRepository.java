package eu.europa.ec.sante.openncp.tools.tsam.sync.repository;

import eu.europa.ec.sante.openncp.tools.tsam.sync.domain.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
    @Modifying
    @Query(value = "truncate table code_system_concept CASCADE", nativeQuery = true)
    void truncateTable();
}
