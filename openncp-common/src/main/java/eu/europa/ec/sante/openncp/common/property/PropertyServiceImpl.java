package eu.europa.ec.sante.openncp.common.property;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional("propertiesTransactionManager")
public class PropertyServiceImpl implements PropertyService {
    private final PropertyRepository propertyRepository;

    public PropertyServiceImpl(final PropertyRepository propertyRepository) {
        this.propertyRepository = Validate.notNull(propertyRepository);
    }

    @Override
    public Optional<Property> findByKey(final String key) {
        return propertyRepository.findById(key).map(PropertyEntity::asProperty);

    }

    @Override
    public List<Property> getAll() {
        return propertyRepository.findAll().stream().map(PropertyEntity::asProperty).collect(Collectors.toList());
    }

    @Override
    public void createOrUpdate(final Property property) {
        propertyRepository.save(new PropertyEntity(property));
    }
}
