package eu.europa.ec.sante.openncp.core.common.fhir.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationService.class);

    private final FhirValidator fhirValidator;
    private final FhirContext fhirContext;
    private final ServerContext serverContext;

    @Value("${hapi.fhir.validation.enabled}")
    private boolean validationEnabled;

    public ValidationService(final FhirValidator fhirValidator, final FhirContext fhirContext, final ServerContext serverContext) {
        this.fhirValidator = Validate.notNull(fhirValidator, "fhirValidator cannot be null" );
        this.fhirContext = Validate.notNull(fhirContext, "fhirContext cannot be null" );
        this.serverContext = Validate.notNull(serverContext, "serverContext cannot be null" );
    }

    public ValidationResult validate(final IBaseResource baseResource, final RestOperationTypeEnum restOperationTypeEnum) {
        if (validationEnabled) {
            final ValidationResult validationResult = fhirValidator.validateWithResult(baseResource);
            final Bundle bundle = ((Bundle) baseResource);
            switch (bundle.getType()) {
                case SEARCHSET:
                    final List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
                    for (Bundle.BundleEntryComponent entry : entries) {
                        final Resource resource = entry.getResource();
                        OpenNCPValidation.validateFhirResource(fhirContext.newJsonParser().encodeResourceToString(resource), serverContext.getNcpSide(), resource.fhirType(), Boolean.TRUE);
                    }
                    break;
                case DOCUMENT:
                    OpenNCPValidation.validateFhirResource(fhirContext.newJsonParser().encodeResourceToString(bundle), serverContext.getNcpSide(), bundle.fhirType(), Boolean.TRUE);
                    break;
                default:
                    throw new RuntimeException("Unexpected Bundle type: [" + bundle.getType() + "]");
            }
            if (validationResult.isSuccessful()) {
                LOGGER.info("Successfully validated the received [{}] resource obtained with the [{}] operation", baseResource.getClass(), restOperationTypeEnum);
            } else {
                LOGGER.error("Validation error for the received [{}] obtained with the [{}] operation", baseResource.getClass(), restOperationTypeEnum);
                for (final SingleValidationMessage validationMessage : validationResult.getMessages()) {
                    LOGGER.info(validationMessage.getMessage());
                }
            }
            return validationResult;
        }
        return null;
    }
}
