package eu.europa.ec.sante.openncp.api.common.resourceProvider;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.SearchContainedModeEnum;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import eu.europa.ec.sante.openncp.api.common.handler.BundleHandler;
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import eu.europa.ec.sante.openncp.core.common.fhir.services.DispatchingService;
import eu.europa.ec.sante.openncp.core.common.fhir.services.ValidationService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PatientResourceProvider extends AbstractResourceProvider implements IResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientResourceProvider.class);

    private final DispatchingService dispatchingService;
    private final BundleHandler bundleHandler;

    public PatientResourceProvider(final DispatchingService dispatchingService, final BundleHandler bundleHandler, final ValidationService validationService) {
        super(validationService);
        this.dispatchingService = Validate.notNull(dispatchingService);
        this.bundleHandler = Validate.notNull(bundleHandler);
    }

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

    @Search(allowUnknownParams = false)
    public IBaseBundle search(final HttpServletRequest theServletRequest, final HttpServletResponse theServletResponse,
                              final RequestDetails theRequestDetails,

                              @Description(shortDefinition = "The patient's date of birth") @OptionalParam(
                                      name = "birthdate") final DateRangeParam theBirthdate,

                              @Description(shortDefinition = "A patient identifier") @RequiredParam(
                                      name = "identifier") final TokenAndListParam theIdentifier,

                              @Description(shortDefinition = "Only return resources which were last updated as specified by the given range") @OptionalParam(
                                      name = "_lastUpdated") final DateRangeParam theLastUpdated,

                              @IncludeParam final Set<Include> theIncludes,

                              @IncludeParam(reverse = true) final Set<Include> theRevIncludes,

                              @Sort final SortSpec theSort,

                              @Count final Integer theCount,

                              @Offset final Integer theOffset,

                              final SummaryEnum theSummaryMode,

                              final SearchTotalModeEnum theSearchTotalMode,

                              final SearchContainedModeEnum theSearchContainedMode,

                              @RawParam final Map<String, List<String>> theAdditionalRawParams) {

        final String jwt = getJwtFromRequest(theServletRequest);

        final Bundle serverResponse = dispatchingService.dispatchSearch(EuRequestDetails.of(theRequestDetails), jwt);
        final Bundle handledBundle = bundleHandler.handle(serverResponse);
        validate(handledBundle, theRequestDetails.getRestOperationType());
        return handledBundle;
    }
}
