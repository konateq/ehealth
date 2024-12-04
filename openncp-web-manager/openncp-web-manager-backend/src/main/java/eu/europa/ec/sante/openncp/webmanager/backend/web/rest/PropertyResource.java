package eu.europa.ec.sante.openncp.webmanager.backend.web.rest;

import eu.europa.ec.sante.openncp.common.property.Property;
import eu.europa.ec.sante.openncp.common.property.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api")
public class PropertyResource {

    private final PropertyService propertyService;

    public PropertyResource(final PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping(path = "/properties")
    public ResponseEntity<List<Property>> listProperties() {
        return ResponseEntity.ok(propertyService.getAll());
    }

    @PutMapping(path = "/properties/{key}")
    public ResponseEntity<Property> updateProperty(@PathVariable final String key, @RequestParam("value") String value) {
        Optional<Property> p = propertyService.findByKey(key);
        if(p.isPresent()) {
            final Property property = Property.of(p.get().getKey(), value);
            propertyService.createOrUpdate(property);
            return ResponseEntity.ok(property);
        }
        return ResponseEntity.notFound().build();
    }
}
