package eu.europa.ec.sante.openncp.api.client;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.*;
import eu.europa.ec.sante.openncp.core.client.ihe.ClientService;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.*;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.feature.Features;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.jws.WebService;
import java.util.List;
import java.util.Map;

@WebService(serviceName = "ClientService", portName = "ClientServicePort",
        targetNamespace = "http://api.client.core.openncp.sante.ec.europa.eu", wsdlLocation = "classpath:ClientService.wsdl",
        endpointInterface = "eu.europa.ec.sante.openncp.core.client.api.ClientServicePortType")
@Service
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class ClientServiceImpl implements ClientServicePortType {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientServiceImpl.class);

    private final ClientService clientService;

    private static final String CLIENT_CONNECTOR_EXCEPTION_MESSAGE = "No assertion context found";

    public ClientServiceImpl(@Qualifier("iheClientService") final ClientService clientService) {
        this.clientService = Validate.notNull(clientService);
    }

    @Override
    public String submitDocument(final SubmitDocumentRequest submitDocumentRequest) {
        final Map<AssertionType, Assertion> assertionMap = AssertionContextProvider.getAssertionContext()
                .orElseThrow(() -> new ClientException(
                        CLIENT_CONNECTOR_EXCEPTION_MESSAGE))
                .getAssertions();
        final SubmitDocumentOperation submitDocumentOperation = ImmutableSubmitDocumentOperation.builder()
                .assertions(assertionMap)
                .request(submitDocumentRequest)
                .build();
        return clientService.submitDocument(submitDocumentOperation);
    }

    @Override
    public List<EpsosDocument> queryDocuments(final QueryDocumentRequest queryDocumentRequest) {
        final Map<AssertionType, Assertion> assertionMap = AssertionContextProvider.getAssertionContext()
                .orElseThrow(() -> new ClientException(
                        CLIENT_CONNECTOR_EXCEPTION_MESSAGE))
                .getAssertions();
        final QueryDocumentOperation queryDocumentOperation = ImmutableQueryDocumentOperation.builder()
                .assertions(assertionMap)
                .request(queryDocumentRequest)
                .build();
        final List<EpsosDocument> epsosDocuments = clientService.queryDocuments(queryDocumentOperation);
        LOGGER.info("epsosDocuments : {}", epsosDocuments);
        return epsosDocuments;
    }

    @Override
    public EpsosDocument retrieveDocument(final RetrieveDocumentRequest retrieveDocumentRequest) {
        final Map<AssertionType, Assertion> assertionMap = AssertionContextProvider.getAssertionContext()
                .orElseThrow(() -> new ClientException(
                        CLIENT_CONNECTOR_EXCEPTION_MESSAGE))
                .getAssertions();
        final RetrieveDocumentOperation retrieveDocumentOperation = ImmutableRetrieveDocumentOperation.builder()
                .assertions(assertionMap)
                .request(retrieveDocumentRequest)
                .build();
        return clientService.retrieveDocument(retrieveDocumentOperation);
    }

    @Override
    public List<PatientDemographics> queryPatient(final QueryPatientRequest queryPatientRequest) {
        final Map<AssertionType, Assertion> assertionMap = AssertionContextProvider.getAssertionContext()
                .orElseThrow(() -> new ClientException(
                        CLIENT_CONNECTOR_EXCEPTION_MESSAGE))
                .getAssertions();
        final QueryPatientOperation queryPatientOperation = ImmutableQueryPatientOperation.builder()
                .assertions(assertionMap)
                .request(queryPatientRequest)
                .build();
        return clientService.queryPatient(queryPatientOperation);
    }

    @Override
    public String sayHello(final String name) {
        final Map<AssertionType, Assertion> assertionMap = AssertionContextProvider.getAssertionContext()
                .orElseThrow(() -> new ClientException(
                        CLIENT_CONNECTOR_EXCEPTION_MESSAGE))
                .getAssertions();
        final AssertionInfoMap assertionInfoMap = PhaseInterceptorChain.getCurrentMessage().get(AssertionInfoMap.class);

        return clientService.sayHello(name);
    }
}
