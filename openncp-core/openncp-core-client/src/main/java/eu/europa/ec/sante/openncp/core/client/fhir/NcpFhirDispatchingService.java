package eu.europa.ec.sante.openncp.core.client.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import eu.europa.ec.sante.openncp.core.common.fhir.FhirDispatchingClient;
import eu.europa.ec.sante.openncp.core.common.fhir.HapiWebClientFactory;
import eu.europa.ec.sante.openncp.core.common.fhir.context.DispatchContext;
import eu.europa.ec.sante.openncp.core.common.fhir.services.FhirDispatchingService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class NcpFhirDispatchingService implements FhirDispatchingService {

    private final HapiWebClientFactory hapiWebClientFactory;

    public NcpFhirDispatchingService(final HapiWebClientFactory hapiWebClientFactory) {
        this.hapiWebClientFactory = Validate.notNull(hapiWebClientFactory, "HapiWebClientFactory must not be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T dispatchSearch(final DispatchContext requestDetails) {
        Validate.notNull(requestDetails, "The request details cannot be null");

        final FhirDispatchingClient hapiWebClient = hapiWebClientFactory.createClient(requestDetails);
        final Bundle result = hapiWebClient.dispatchSearch(requestDetails);
        return (T) result;
    }


    @Override
    public <T extends IBaseResource> T dispatchRead(final DispatchContext requestDetails) {
        Validate.notNull(requestDetails, "The request details cannot be null");

        final FhirDispatchingClient hapiWebClient = hapiWebClientFactory.createClient(requestDetails);
        return hapiWebClient.dispatchRead(requestDetails);
    }

    @Override
    public MethodOutcome dispatchWrite(final DispatchContext requestDetails, final IBaseResource resourceToCreate) {
        Validate.notNull(requestDetails, "The request details cannot be null");

        final FhirDispatchingClient hapiWebClient = hapiWebClientFactory.createClient(requestDetails);
        return hapiWebClient.dispatchWrite(requestDetails, resourceToCreate);
    }
}
