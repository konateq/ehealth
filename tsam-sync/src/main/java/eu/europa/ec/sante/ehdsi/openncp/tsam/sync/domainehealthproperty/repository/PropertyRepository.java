package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domainehealthproperty.repository;

import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domainehealthproperty.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, String> {
}
