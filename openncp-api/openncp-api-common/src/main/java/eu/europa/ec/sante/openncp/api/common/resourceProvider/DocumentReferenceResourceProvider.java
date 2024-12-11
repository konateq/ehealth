package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.services.DispatchingService;
import eu.europa.ec.sante.openncp.core.common.fhir.services.ValidationService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DocumentReferenceResourceProvider extends AbstractResourceProvider implements IResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentReferenceResourceProvider.class);

    private final DispatchingService dispatchingService;


    public DocumentReferenceResourceProvider(final DispatchingService dispatchingService, final ServerContext serverContext, final ValidationService validationService) {
        super(serverContext, validationService);
        this.dispatchingService = Validate.notNull(dispatchingService, "dispatchingService must not be null");
    }

    @Override
    public Class<DocumentReference> getResourceType() {
        return DocumentReference.class;
    }

    @Read
    public DocumentReference find(@IdParam final IdType id, final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse, final RequestDetails theRequestDetails) {
        final DispatchContext dispatchContext = createDispatchContext(theServletRequest, theServletResponse, theRequestDetails);
        final DocumentReference documentReference = dispatchingService.dispatchRead(dispatchContext);
        validate(documentReference, theRequestDetails.getRestOperationType());
        return documentReference;
    }

    @Search(allowUnknownParams = true)
    public IBaseBundle search(final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse,
                              final RequestDetails theRequestDetails,

                              @Description(shortDefinition = "Patient business identifier")
                              @RequiredParam(name = "patient") final ReferenceParam patient,

                              @Description(shortDefinition = "The type of the Document")
                              @RequiredParam(name = "type") final TokenParam type,

                              @Description(shortDefinition = "Date range for the search")
                              @OptionalParam(name = "date") final DateRangeParam dateRange,

                              // Lab result
                              @Description(shortDefinition = "The type of the content for the lab result")
                              @OptionalParam(name = "contenttype") final TokenParam contentType,

                              @Description(shortDefinition = "Lab result study type")
                              @OptionalParam(name = "category") final TokenParam studyType,

                              // Medical imaging
                              @Description(shortDefinition = "Modality used for imaging")
                              @OptionalParam(name = "modality") final TokenParam modality,

                              @Description(shortDefinition = "Body site imaged")
                              @OptionalParam(name = "bodysite") final TokenParam bodySite
    ) {

        final DispatchContext dispatchContext = createDispatchContext(theServletRequest, theServletResponse, theRequestDetails);
        return dispatchContext.getSupportedResourceType().map(fhirSupportedResourceType -> {
            switch (fhirSupportedResourceType) {
                case LAB_RESULT:
                    return getResponseAndValidate(theRequestDetails, dispatchContext);
                case MEDICAL_IMAGING:
                    return getResponseAndValidate(theRequestDetails, dispatchContext);
            }
            return null;
        }).orElseGet(() -> getResponseAndValidate(theRequestDetails, dispatchContext));
    }

    private Bundle getResponseAndValidate(final RequestDetails theRequestDetails, final DispatchContext dispatchContext) {
        final Bundle serverResponse = dispatchingService.dispatchSearch(dispatchContext);
        validate(serverResponse, theRequestDetails.getRestOperationType());
        return serverResponse;
    }

    @Create
    public MethodOutcome createDocumentReference(@ResourceParam final DocumentReference documentReference,
                                                 final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse,
                                                 final RequestDetails theRequestDetails) {
        final DispatchContext dispatchContext = createDispatchContext(theServletRequest, theServletResponse, theRequestDetails);

        return dispatchingService.dispatchWrite(dispatchContext, documentReference);

    }
}
