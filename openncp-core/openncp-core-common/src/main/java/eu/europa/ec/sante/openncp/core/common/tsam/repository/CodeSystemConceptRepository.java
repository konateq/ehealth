package eu.europa.ec.sante.openncp.core.common.tsam.repository;

import eu.europa.ec.sante.openncp.core.common.tsam.domain.CodeSystemConcept;
import eu.europa.ec.sante.openncp.core.common.tsam.domain.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface CodeSystemConceptRepository extends JpaRepository<CodeSystemConcept, String> {

    Optional<CodeSystemConcept> findByCodeAndCodeSystemVersionId(String code, Long id);

    @Query("select d from CodeSystemConcept csc inner join csc.designations d where csc.id = :id and d.languageCode = :languageCode")
    List<Designation> findDesignationByIdAndDesignationLanguageCode(@Param("id") Long id, @Param("languageCode") String languageCode);


    List<CodeSystemConcept> findCodeSystemConceptsByValueSetVersionsIdOrderByIdAsc(long id);
}
