package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.*;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import eu.europa.ec.sante.openncp.api.common.handler.BundleHandler;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.services.DispatchingService;
import eu.europa.ec.sante.openncp.core.common.fhir.services.ValidationService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

@Component
public class BundleResourceProvider extends AbstractResourceProvider implements IResourceProvider {

    private final DispatchingService dispatchingService;
    private final BundleHandler bundleHandler;

    public BundleResourceProvider(final DispatchingService dispatchingService, final BundleHandler bundleHandler, final ServerContext serverContext, final ValidationService validationService) {
        super(serverContext, validationService);
        this.dispatchingService = Validate.notNull(dispatchingService, "dispatchingService must not be null");
        this.bundleHandler = Validate.notNull(bundleHandler, "bundleHandler must not be null");
    }

    @Override
    public Class<Bundle> getResourceType() {
        return Bundle.class;
    }

    @Read
    public Bundle find(@IdParam final IdType id, final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse, final RequestDetails theRequestDetails) {
        final DispatchContext dispatchContext = createDispatchContext(theServletRequest, theServletResponse, theRequestDetails);
        final Bundle bundle = dispatchingService.dispatchRead(dispatchContext);
        validate(bundle, theRequestDetails.getRestOperationType());
        return bundle;
    }

    @Create
    public MethodOutcome createBundle(@ResourceParam final Bundle bundleToCreate, final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse, final RequestDetails theRequestDetails) {
        final DispatchContext dispatchContext = createDispatchContext(theServletRequest, theServletResponse, theRequestDetails);
        return dispatchingService.dispatchWrite(dispatchContext, bundleToCreate);

    }

    @Search(allowUnknownParams = true)
    public IBaseBundle search(
            final HttpServletRequest theServletRequest,
            final HttpServletResponse theServletResponse,
            final RequestDetails theRequestDetails,

            @Description(shortDefinition = "The type of the Document") @OptionalParam(
                    name = "type") final TokenParam type,

            @Description(shortDefinition = "Study type") @OptionalParam(
                    name = "category") final TokenParam studyType,

            @Description(shortDefinition = "Specialty") @OptionalParam(
                    name = "specialty") final TokenParam specialty,

            @Description(shortDefinition = "Patient business identifier") @OptionalParam(
                    name = "patient") final ReferenceParam patient,

            @Description(shortDefinition = "Date range for the search") @OptionalParam(
                    name = "date") final DateRangeParam dateRange,

            @IncludeParam final
            Set<Include> theIncludes,

            @IncludeParam(reverse = true) final
            Set<Include> theRevIncludes,

            @Sort final
            SortSpec theSort,

            @Count final
            Integer theCount,

            @Offset final
            Integer theOffset,

            final SummaryEnum theSummaryMode,

            final SearchTotalModeEnum theSearchTotalMode,

            final SearchContainedModeEnum theSearchContainedMode) {
        final DispatchContext dispatchContext = createDispatchContext(theServletRequest, theServletResponse, theRequestDetails);
        final Bundle serverResponse = dispatchingService.dispatchSearch(dispatchContext);
        final Bundle handledBundle = bundleHandler.handle(serverResponse, null);

        return handledBundle;
    }
}
