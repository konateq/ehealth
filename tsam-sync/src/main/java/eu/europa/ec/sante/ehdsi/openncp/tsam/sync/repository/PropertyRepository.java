package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.repository;

import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, String> {
}
