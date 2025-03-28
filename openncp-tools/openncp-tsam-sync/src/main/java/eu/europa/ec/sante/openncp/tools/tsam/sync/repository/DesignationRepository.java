package eu.europa.ec.sante.openncp.tools.tsam.sync.repository;

import eu.europa.ec.sante.openncp.tools.tsam.sync.domain.Designation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignationRepository extends JpaRepository<Designation, Long> {
}
