package eu.europa.ec.sante.openncp.core.common.tsam.repository;

import eu.europa.ec.sante.openncp.core.common.tsam.domain.CodeSystemVersion;
import eu.europa.ec.sante.openncp.core.common.tsam.domain.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DesignationRepository extends JpaRepository<CodeSystemVersion, String> {

    @Query("select d from Designation d " +
            "join d.codeSystemConcept csc " +
            "join csc.valueSetVersions vsv " +
            "where vsv.id = :valueSetVersionId")
    List<Designation> findByValueSetVersionId(@Param("valueSetVersionId") long valueSetVersionId);

    @Query("select distinct d.languageCode from Designation d")
    List<String> findAllAvailableLanguageCodes();
}
