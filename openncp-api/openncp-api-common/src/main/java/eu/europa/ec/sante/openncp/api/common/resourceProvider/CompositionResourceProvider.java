package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.*;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import eu.europa.ec.sante.openncp.api.common.handler.BundleHandler;
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import eu.europa.ec.sante.openncp.core.common.fhir.context.ImmutableEuRequestDetails;
import eu.europa.ec.sante.openncp.common.fhir.context.r4.resources.CompositionLabReportMyHealthEu;
import eu.europa.ec.sante.openncp.core.common.fhir.services.DispatchingService;
import eu.europa.ec.sante.openncp.core.common.fhir.services.ValidationService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

@Component
public class CompositionResourceProvider extends AbstractResourceProvider implements IResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositionResourceProvider.class);

    private final DispatchingService dispatchingService;
    private final BundleHandler bundleHandler;

    public CompositionResourceProvider(final DispatchingService dispatchingService, final BundleHandler bundleHandler, final ValidationService validationService) {
        super(validationService);
        this.dispatchingService = Validate.notNull(dispatchingService);
        this.bundleHandler = bundleHandler;
    }

    @Override
    public Class<Composition> getResourceType() {
        return Composition.class;
    }


    @Read
    public CompositionLabReportMyHealthEu find(@IdParam final IdType id, final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse,
                                       final RequestDetails theRequestDetails) {
        final String JWTToken = getJwtFromRequest(theServletRequest);
        final CompositionLabReportMyHealthEu handledCompositionLabReportEu = dispatchingService.dispatchRead(EuRequestDetails.of(theRequestDetails), JWTToken);
        validate(handledCompositionLabReportEu, theRequestDetails.getRestOperationType());
        return handledCompositionLabReportEu;
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

            final SearchContainedModeEnum theSearchContainedMode

    ) {
        final String JWTToken = getJwtFromRequest(theServletRequest);
        final Bundle serverResponse = dispatchingService.dispatchSearch(ImmutableEuRequestDetails.of(theRequestDetails), JWTToken);
        final Bundle handledBundle = bundleHandler.handle(serverResponse);

        return handledBundle;
    }

    @Operation(name = "$document", idempotent = true, bundleType = BundleTypeEnum.DOCUMENT)
    public IBaseBundle getDocumentForComposition(
            final HttpServletRequest theServletRequest,
            @IdParam final IIdType theId,
            @Description(
                    formalDefinition =
                            "Results from this method are returned across multiple pages. This parameter controls the size of those pages.")
            @OperationParam(name = Constants.PARAM_COUNT, typeName = "unsignedInt") final
            IPrimitiveType<Integer> theCount,
            @Description(
                    formalDefinition =
                            "Results from this method are returned across multiple pages. This parameter controls the offset when fetching a page.")
            @OperationParam(name = Constants.PARAM_OFFSET, typeName = "unsignedInt") final
            IPrimitiveType<Integer> theOffset,
            @Description(
                    shortDefinition =
                            "Only return resources which were last updated as specified by the given range")
            @OperationParam(name = Constants.PARAM_LASTUPDATED, min = 0, max = 1) final
            DateRangeParam theLastUpdated,
            @Sort final SortSpec theSortSpec,
            final RequestDetails theRequestDetails) {

        final String JWTToken = getJwtFromRequest(theServletRequest);

        final Bundle handledCompositionLabReportEu = dispatchingService.dispatchRead(ImmutableEuRequestDetails.of(theRequestDetails), JWTToken);

        return handledCompositionLabReportEu;
    }
}
