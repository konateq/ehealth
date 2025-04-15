package eu.europa.ec.sante.openncp.tools.tsam.sync.repository;

import eu.europa.ec.sante.openncp.tools.tsam.sync.domain.CodeSystemVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeSystemVersionRepository extends JpaRepository<CodeSystemVersion, Long> {
}
