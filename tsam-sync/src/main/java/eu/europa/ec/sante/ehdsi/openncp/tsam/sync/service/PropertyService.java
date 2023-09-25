package eu.europa.ec.sante.ehdsi.openncp.tsam.sync.service;

import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.domain.Property;
import eu.europa.ec.sante.ehdsi.openncp.tsam.sync.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PropertyService {
    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    public Property save(Property property){
        return propertyRepository.save(property);
    }

    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    public Property getProperty(String name) {
        return propertyRepository.findById(name).orElseThrow();
    }
}
