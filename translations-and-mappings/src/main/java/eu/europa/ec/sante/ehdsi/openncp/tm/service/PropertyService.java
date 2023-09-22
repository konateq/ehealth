package eu.europa.ec.sante.ehdsi.openncp.tm.service;

import eu.europa.ec.sante.ehdsi.openncp.tm.exception.PropertyNotFoundException;
import eu.europa.ec.sante.ehdsi.openncp.tm.persistence.model.Property;
import eu.europa.ec.sante.ehdsi.openncp.tm.persistence.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public Property getProperty(String name) {
        return propertyRepository.findById(name).orElseThrow(()-> new PropertyNotFoundException(name));
    }
}
