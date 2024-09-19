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
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import eu.europa.ec.sante.openncp.core.common.fhir.context.ImmutableEuRequestDetails;
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
        final String JWTToken = getJwtFromRequest(theServletRequest);
        final Bundle bundle = dispatchingService.dispatchRead(EuRequestDetails.of(theRequestDetails), JWTToken);
        return bundle;
    }

    @Create
    public MethodOutcome createPatient(@ResourceParam Bundle theBundle, final HttpServletRequest theServletRequest, final RequestDetails theRequestDetails) {

        final String JWTToken = getJwtFromRequest(theServletRequest);
        final MethodOutcome methodOutcome = dispatchingService.dispatchWrite(EuRequestDetails.of(theRequestDetails), JWTToken);

        /*
         * First we might want to do business validation. The UnprocessableEntityException
         * results in an HTTP 422, which is appropriate for business rule failure
         */
        if (theBundle.getIdentifier().isEmpty()) {
            /* It is also possible to pass an OperationOutcome resource
             * to the UnprocessableEntityException if you want to return
             * a custom populated OperationOutcome. Otherwise, a simple one
             * is created using the string supplied below.
             */
            throw new UnprocessableEntityException(Msg.code(636) + "No identifier supplied");
        }

        // Save this patient to the database...
        savePatientToDatabase(thePatient);

        // This method returns a MethodOutcome object which contains
        // the ID (composed of the type Patient, the logical ID 3746, and the
        // version ID 1)
        final MethodOutcome retVal = new MethodOutcome();
        retVal.setId(new IdType("Patient", "3746", "1"));

        // You can also add an OperationOutcome resource to return
        // This part is optional though:
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setDiagnostics("One minor issue detected");
        retVal.setOperationOutcome(outcome);
        return retVal;
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

        final String JWTToken = getJwtFromRequest(theServletRequest);
        final Bundle serverResponse = dispatchingService.dispatchSearch(ImmutableEuRequestDetails.of(theRequestDetails), JWTToken);
        final Bundle handledBundle = bundleHandler.handle(serverResponse);

        return handledBundle;
    }
}
