package eu.europa.ec.sante.openncp.core.server.nc.mock.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import eu.europa.ec.sante.openncp.core.common.fhir.FhirDispatchingClient;
import eu.europa.ec.sante.openncp.core.common.fhir.HapiWebClientFactory;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.services.DispatchingService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FhirMockDispatchingService implements DispatchingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FhirMockDispatchingService.class);

    private final FhirContext fhirContext;

    private final HapiWebClientFactory webClientFactory;


    public FhirMockDispatchingService(final FhirContext fhirContext, final HapiWebClientFactory webClientFactory) {
        this.fhirContext = Validate.notNull(fhirContext, "FhirContext cannot be null");
        this.webClientFactory = Validate.notNull(webClientFactory, "WebClientFactory cannot be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T dispatchSearch(final DispatchContext dispatchContext) {
        Validate.notNull(dispatchContext, "The dispatchContext cannot be null");

        final FhirDispatchingClient hapiWebClient = webClientFactory.createClient("https://sandbox.hl7europe.eu/laboratory/fhir/");
        final Bundle result = hapiWebClient.dispatchSearch(dispatchContext);

        return (T) result;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T dispatchRead(final DispatchContext dispatchContext) {
        Validate.notNull(dispatchContext, "The dispatchContext cannot be null");
        final FhirDispatchingClient hapiWebClient = webClientFactory.createClient("https://sandbox.hl7europe.eu/laboratory/fhir/");
        final Bundle result = hapiWebClient.dispatchRead(dispatchContext);

        return (T) result;
    }

    @Override
    public MethodOutcome dispatchWrite(final DispatchContext dispatchContext, final IBaseResource resourceToCreate) {
        Validate.notNull(dispatchContext, "The dispatchContext cannot be null");
        final FhirDispatchingClient hapiWebClient = webClientFactory.createClient("https://sandbox.hl7europe.eu/laboratory/fhir/");
        return hapiWebClient.dispatchWrite(dispatchContext, resourceToCreate);
    }
}
