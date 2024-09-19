package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.*;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import eu.europa.ec.sante.openncp.api.common.handler.BundleHandler;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.context.ImmutableDispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.services.DispatchingService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

@Component
public class BundleResourceProvider extends AbstractResourceProvider implements IResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleResourceProvider.class);

    private final DispatchingService dispatchingService;
    private final BundleHandler bundleHandler;

    public BundleResourceProvider(final DispatchingService dispatchingService, final BundleHandler bundleHandler) {
        this.dispatchingService = Validate.notNull(dispatchingService);
        this.bundleHandler = Validate.notNull(bundleHandler);
    }

    @Override
    public Class<Bundle> getResourceType() {
        return Bundle.class;
    }

    @Read
    public Bundle find(@IdParam final IdType id, final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse, final RequestDetails theRequestDetails) {
        final Bundle bundle = dispatchingService.dispatchRead(ImmutableDispatchContext.of(theRequestDetails, theServletRequest, theServletResponse));
        return bundle;
    }

    @Create
    public MethodOutcome createPatient(@ResourceParam Bundle bundleToCreate, final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse, final RequestDetails theRequestDetails) {
        return dispatchingService.dispatchWrite(ImmutableDispatchContext.of(theRequestDetails, theServletRequest, theServletResponse), bundleToCreate);

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

        final Bundle serverResponse = dispatchingService.dispatchSearch(ImmutableDispatchContext.of(theRequestDetails, theServletRequest, theServletResponse));
        final Bundle handledBundle = bundleHandler.handle(serverResponse);

        return handledBundle;
    }
}
