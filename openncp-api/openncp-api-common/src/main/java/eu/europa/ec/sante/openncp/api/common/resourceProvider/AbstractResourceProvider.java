package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import eu.europa.ec.sante.openncp.core.common.fhir.services.ValidationService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public abstract class AbstractResourceProvider {

    private final ValidationService validationService;

    public AbstractResourceProvider(ValidationService validationService) {
        this.validationService = Validate.notNull(validationService);
    }

    public void validate(IBaseResource resource, RestOperationTypeEnum restOperationTypeEnum) {
        validationService.validate(resource, restOperationTypeEnum);
    }

    public String getJwtFromRequest(final HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header;
        }
        throw new RuntimeException("JWT Token is missing");
    }
}
