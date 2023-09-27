package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain2.repository;

import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain2.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, String> {
}
