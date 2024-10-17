package eu.europa.ec.sante.openncp.core.client.fhir;

import eu.europa.ec.sante.openncp.core.common.fhir.FhirDispatchingClient;
import eu.europa.ec.sante.openncp.core.common.fhir.HapiWebClientFactory;
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import eu.europa.ec.sante.openncp.core.common.fhir.services.DispatchingService;
import eu.europa.ec.sante.openncp.core.common.fhir.services.ValidationService;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FhirDispatchingService implements DispatchingService {

    private final HapiWebClientFactory hapiWebClientFactory;

    public FhirDispatchingService(final HapiWebClientFactory hapiWebClientFactory, final ValidationService validationService) {
        this.hapiWebClientFactory = Validate.notNull(hapiWebClientFactory, "HapiWebClientFactory must not be null");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T dispatchSearch(final EuRequestDetails requestDetails, String JWTToken) {
        Validate.notNull(requestDetails, "The request details cannot be null");

        final FhirDispatchingClient hapiWebClient = hapiWebClientFactory.createClient(requestDetails);
        final Bundle result = hapiWebClient.dispatch(requestDetails, JWTToken);
        return (T) result;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T extends IBaseResource> T dispatchRead(final EuRequestDetails requestDetails, String JWTToken) {
        Validate.notNull(requestDetails, "The request details cannot be null");

        final FhirDispatchingClient hapiWebClient = hapiWebClientFactory.createClient(requestDetails);
        final Bundle result = hapiWebClient.dispatch(requestDetails, JWTToken);
        return (T) result;
    }
}
