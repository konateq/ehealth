package eu.europa.ec.sante.ehdsi.openncp.tm.persistence.repository;


import eu.europa.ec.sante.ehdsi.openncp.tm.persistence.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, String> {
}
