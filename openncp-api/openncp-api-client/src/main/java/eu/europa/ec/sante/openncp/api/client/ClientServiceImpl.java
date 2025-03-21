package eu.europa.ec.sante.openncp.api.client;

import eu.europa.ec.sante.openncp.core.client.api.*;
import eu.europa.ec.sante.openncp.core.client.ihe.ClientService;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.*;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.feature.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.jws.WebService;
import java.util.List;

@WebService(serviceName = "ClientService", portName = "ClientServicePort",
        targetNamespace = "http://api.client.core.openncp.sante.ec.europa.eu", wsdlLocation = "classpath:ClientService.wsdl",
        endpointInterface = "eu.europa.ec.sante.openncp.core.client.api.ClientServicePortType")
@Service
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class ClientServiceImpl implements ClientServicePortType {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientServiceImpl.class);

    private final ClientService clientService;

    private static final String ASSERTION_CONTEXT_MISSING = "No assertion context found";
    private static final String SAMLDETAILS_MISSING = "No saml details found";

    public ClientServiceImpl(@Qualifier("iheClientService") final ClientService clientService) {
        this.clientService = Validate.notNull(clientService);
    }

    @Override
    public String submitDocument(final SubmitDocumentRequest submitDocumentRequest) {
        final SamlDetails samlDetails = SecurityContextProvider.getSecurityContext()
                .orElseThrow(() -> new ClientException(
                        ASSERTION_CONTEXT_MISSING))
                .getSamlDetails()
                .orElseThrow(() -> new ClientException(SAMLDETAILS_MISSING));

        final SubmitDocumentOperation submitDocumentOperation = ImmutableSubmitDocumentOperation.builder()
                .samlDetails(samlDetails)
                .request(submitDocumentRequest)
                .build();
        return clientService.submitDocument(submitDocumentOperation);
    }

    @Override
    public List<EpsosDocument> queryDocuments(final QueryDocumentRequest queryDocumentRequest) {
        final SamlDetails samlDetails = SecurityContextProvider.getSecurityContext()
                .orElseThrow(() -> new ClientException(
                        ASSERTION_CONTEXT_MISSING))
                .getSamlDetails()
                .orElseThrow(() -> new ClientException(SAMLDETAILS_MISSING));

        final QueryDocumentOperation queryDocumentOperation = ImmutableQueryDocumentOperation.builder()
                .samlDetails(samlDetails)
                .request(queryDocumentRequest)
                .build();
        final List<EpsosDocument> epsosDocuments = clientService.queryDocuments(queryDocumentOperation);
        LOGGER.info("epsosDocuments : {}", epsosDocuments);
        return epsosDocuments;
    }

    @Override
    public EpsosDocument retrieveDocument(final RetrieveDocumentRequest retrieveDocumentRequest) {
        final SamlDetails samlDetails = SecurityContextProvider.getSecurityContext()
                .orElseThrow(() -> new ClientException(
                        ASSERTION_CONTEXT_MISSING))
                .getSamlDetails()
                .orElseThrow(() -> new ClientException(SAMLDETAILS_MISSING));

        final RetrieveDocumentOperation retrieveDocumentOperation = ImmutableRetrieveDocumentOperation.builder()
                .samlDetails(samlDetails)
                .request(retrieveDocumentRequest)
                .build();
        return clientService.retrieveDocument(retrieveDocumentOperation);
    }

    @Override
    public List<PatientDemographics> queryPatient(final QueryPatientRequest queryPatientRequest) {
        final SamlDetails samlDetails = SecurityContextProvider.getSecurityContext()
                .orElseThrow(() -> new ClientException(
                        ASSERTION_CONTEXT_MISSING))
                .getSamlDetails()
                .orElseThrow(() -> new ClientException(SAMLDETAILS_MISSING));

        final QueryPatientOperation queryPatientOperation = ImmutableQueryPatientOperation.builder()
                .samlDetails(samlDetails)
                .request(queryPatientRequest)
                .build();
        return clientService.queryPatient(queryPatientOperation);
    }

    @Override
    public String sayHello(final String name) {
        return clientService.sayHello(name);
    }
}
