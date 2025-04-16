package eu.europa.ec.sante.openncp.application.client.connector;

import eu.europa.ec.sante.openncp.application.client.connector.fhir.RestApiClientService;
import eu.europa.ec.sante.openncp.application.client.connector.fhir.security.JwtTokenGenerator;
import eu.europa.ec.sante.openncp.application.client.connector.interceptor.SamlAssertionInterceptor;
import eu.europa.ec.sante.openncp.application.client.connector.interceptor.TransportTokenInInterceptor;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.context.ServerContext;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

/*
 *  Client Service class providing access to the MyHealth@EU workflows (Patient Summary, ePrescription, OrCD etc.).
 */
@Service
public class DefaultClientConnectorService implements ClientConnectorService {

    private final Logger logger = LoggerFactory.getLogger(DefaultClientConnectorService.class);

    private final ConfigurationManager configurationManager;


    private final RestApiClientService restApiClientService;

    private final JwtTokenGenerator jwtTokenGenerator;

    private static final String DCCS_SC_KEYSTORE_PASSWORD = "SC_KEYSTORE_PASSWORD";

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final ClientConnectorServicePortTypeWrapper clientConnectorService;

    public LoggingFeature loggingFeature(ServerContext serverContext) {
        final LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        if (serverContext.isProduction()) {
            loggingFeature.addSensitiveElementNames(new HashSet<>(Arrays.asList("password", "administrativeGender", "birthDate", "city", "country", "familyName", "givenName", "postalCode", "streetAddress", "patientId", "nextOfKinId", "AttributeStatement", "creationDate", "person", "description", "base64Binary")));
            loggingFeature.addSensitiveProtocolHeaderNames(new HashSet<>(Arrays.asList("Server", "Accept", "host", "Date")));
            loggingFeature.setVerbose(false);
        } else {
            loggingFeature.setVerbose(true);
        }
        return loggingFeature;
    }

    public DefaultClientConnectorService(final ConfigurationManager configurationManager,
                                         final RestApiClientService restApiClientService,
                                         final JwtTokenGenerator jwtTokenGenerator,
                                         ServerContext serverContext) {
        this.configurationManager = Validate.notNull(configurationManager, "configurationManager must not be null");
        // URL of the targeted NCP-B - ClientService.wsdl
        final String endpointReference = Validate.notBlank(configurationManager.getProperty("PORTAL_CLIENT_CONNECTOR_URL"));
        this.restApiClientService = Validate.notNull(restApiClientService, "restApiClientService must not be null");
        this.jwtTokenGenerator = Validate.notNull(jwtTokenGenerator, "jwtTokenGenerator must not be null");

        final ClientService ss = new ClientService();
        clientConnectorService = new ClientConnectorServicePortTypeWrapper(ss.getClientServicePort());
        clientConnectorService.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointReference);

        final Client client = ClientProxy.getClient(clientConnectorService.getClientConnectorServicePortType());
        client.getBus().getFeatures().add(loggingFeature(serverContext));
        client.getBus().getFeatures().add(new WSAddressingFeature());

        client.getOutInterceptors().add(new SamlAssertionInterceptor());
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        client.getInInterceptors().add(new TransportTokenInInterceptor());

        final HTTPConduit conduit = (HTTPConduit) client.getConduit();

        final TLSClientParameters tlsClientParameters = new TLSClientParameters();
        // This should be configurable, you don't want to disable the CN check in production!!
        tlsClientParameters.setDisableCNCheck(true);
        tlsClientParameters.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        tlsClientParameters.setSSLSocketFactory(getSSLSocketFactory());

        conduit.setTlsClientParameters(tlsClientParameters);
    }


    private SSLSocketFactory getSSLSocketFactory() {
        // Load the Keystore
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("JKS");
        } catch (final KeyStoreException e) {
            throw new RuntimeException("Error creating the keystore instance", e);
        }
        InputStream keystoreStream;
        try {
            keystoreStream = new FileInputStream(configurationManager.getProperty("SC_KEYSTORE_PATH"));
        } catch (final FileNotFoundException e) {
            throw new RuntimeException("Could not find the keystore", e);
        }
        try {
            keyStore.load(keystoreStream, configurationManager.getProperty(DCCS_SC_KEYSTORE_PASSWORD).toCharArray());
        } catch (final IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Error loading the keystore", e);
        }

        // Add Keystore to KeyManager
        KeyManagerFactory keyManagerFactory;
        try {
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not create the key manager factory", e);
        }
        try {
            keyManagerFactory.init(keyStore, configurationManager.getProperty(DCCS_SC_KEYSTORE_PASSWORD).toCharArray());
        } catch (final KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException("Could not initialize the keystore", e);
        }

        final TrustManagerFactory trustManagerFactory;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not create the trust manager factory", e);
        }

        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance("JKS");
        } catch (final KeyStoreException e) {
            throw new RuntimeException("Error creating the truststore instance", e);
        }
        final InputStream trustStoreStream;
        try {
            trustStoreStream = new FileInputStream(configurationManager.getProperty("TRUSTSTORE_PATH"));
        } catch (final FileNotFoundException e) {
            throw new RuntimeException("Could not find the truststore", e);
        }
        try {
            trustStore.load(trustStoreStream, configurationManager.getProperty("TRUSTSTORE_PASSWORD").toCharArray());
        } catch (final IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Error loading the truststore", e);
        }

        try {
            trustManagerFactory.init(trustStore);
        } catch (final KeyStoreException e) {
            throw new RuntimeException("Could not initialize the truststore", e);
        }

        // Create SSLContext with KeyManager and TrustManager
        SSLContext context;
        try {
            context = SSLContext.getInstance("TLS");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not get the ssl context instance", e);
        }
        try {
            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        } catch (final KeyManagementException e) {
            throw new RuntimeException("Could not initialize the SSL context", e);
        }
        return context.getSocketFactory();
    }


    /**
     * Returns a list of clinical documents related to the patient demographics provided.
     *
     * @param assertions   - Map of assertions required by the transaction (HCP, TRC and NoK optional).
     * @param countryCode  - ISO Country code of the patient country of origin.
     * @param patientId    - Unique Patient Identifier retrieved from NCP-A.
     * @param classCodes   - Class Codes of the documents to retrieve.
     * @param filterParams - Extra parameters for search filtering.
     * @return List of clinical documents and metadata searched by the clinician.
     */
    @Override
    public List<EpsosDocument> queryDocuments(final Map<AssertionType, Assertion> assertions, final String countryCode, final PatientId patientId,
                                              final List<GenericDocumentCode> classCodes, final FilterParams filterParams)
            throws ClientConnectorException {

        logger.info("[National Connector] queryDocuments(countryCode:'{}')", countryCode);
        try {
            final var queryDocumentRequest = objectFactory.createQueryDocumentRequest();
            classCodes.forEach(genericDocumentCode -> queryDocumentRequest.getClassCode().add(genericDocumentCode));
            queryDocumentRequest.setPatientId(patientId);
            queryDocumentRequest.setCountryCode(countryCode);
            queryDocumentRequest.setFilterParams(filterParams);

            clientConnectorService.setAssertions(assertions);

            final List<EpsosDocument> epsosDocuments = clientConnectorService.getClientConnectorServicePortType().queryDocuments(queryDocumentRequest);
            logger.info("epsosDocuments : {}", epsosDocuments);
            return epsosDocuments;
        } catch (Exception e) {
            throw createClientConnectorException(e);
        }
    }

    /**
     * Returns demographics of the patient corresponding to the identity traits provided.
     *
     * @param assertions          - Map of assertions required by the transaction (HCP, TRC and NoK optional)
     * @param countryCode         - ISO Country code of the patient country of origin.
     * @param patientDemographics - Identifiers of the requested patient
     * @return List of patients found (only 1 patient is expected in MyHealth@EU)
     */
    public List<PatientDemographics> queryPatient(final Map<AssertionType, Assertion> assertions, final String countryCode,
                                                  final PatientDemographics patientDemographics) throws ClientConnectorException {

        logger.info("[National Connector] queryPatient(countryCode:'{}')", countryCode);
        try {
            final var queryPatientRequest = objectFactory.createQueryPatientRequest();
            queryPatientRequest.setPatientDemographics(patientDemographics);
            queryPatientRequest.setCountryCode(countryCode);

            clientConnectorService.setAssertions(assertions);

            return clientConnectorService.getClientConnectorServicePortType().queryPatient(queryPatientRequest);
        } catch (Throwable e) {
            throw createClientConnectorException(e);
        }
    }

    /**
     * Default Webservice test method available mainly for configuration and testing purpose.
     *
     * @param assertions - Map of assertions required by the transaction (HCP, TRC and NoK optional).
     * @param name       - Token sent for testing.
     * @return Hello message concatenated with the token passed as parameter.
     */
    public String sayHello(final Map<AssertionType, Assertion> assertions, final String name) {

        logger.info("[National Connector] sayHello(name:'{}')", name);

        return clientConnectorService.getClientConnectorServicePortType().sayHello(name);
    }

    /**
     * @param assertions   - Map of assertions required by the transaction (HCP, NoK optional).
     * @param countryCode  - ISO Country code of the patient country of origin.
     * @param searchParams - Search parameters to uniquely define the patient.
     * @return List of PatientMyHealthEu resources
     * @throws ClientConnectorException - Checked exception that passes the exception thrown by OpenNCP
     */
    @Override
    public ResponseEntity<String> queryPatientFhir(final Map<AssertionType, Assertion> assertions, final String countryCode, final Map<String, String> searchParams) throws ClientConnectorException {
        try {
            final String jwtToken = jwtTokenGenerator.generate(assertions);
            return restApiClientService.search(countryCode, jwtToken, searchParams, "Patient");
        } catch (final Exception e) {
            throw new ClientConnectorException(e.getMessage(), e);
        }
    }

    /**
     * @param assertions   - Map of assertions required by the transaction (HCP, TRC, NoK optional).
     * @param countryCode  - ISO Country code of the patient country of origin.
     * @param searchParams - Search parameters to match the DocumentReferences.
     * @return List of DocumentReference Resources
     * @throws ClientConnectorException - Checked exception that passes the exception thrown by OpenNCP
     */
    @Override
    public ResponseEntity<String> queryDocumentReferenceFhir(final Map<AssertionType, Assertion> assertions, final String countryCode, final Map<String, String> searchParams) throws ClientConnectorException {
        try {
            final String jwtToken = jwtTokenGenerator.generate(assertions);
            return restApiClientService.search(countryCode, jwtToken, searchParams, "DocumentReference");
        } catch (final Exception e) {
            throw new ClientConnectorException(e.getMessage(), e);
        }
    }

    /**
     * @param assertions   - Map of assertions required by the transaction (HCP, TRC, NoK optional).
     * @param countryCode  - ISO Country code of the patient country of origin.
     * @param searchParams - Search parameters to identify the Bundle.
     * @return List of Bundle resources
     * @throws ClientConnectorException - Checked exception that passes the exception thrown by OpenNCP
     */
    @Override
    public ResponseEntity<String> queryBundleFhir(final Map<AssertionType, Assertion> assertions, final String countryCode, final Map<String, String> searchParams) throws ClientConnectorException {
        try {
            final String jwtToken = jwtTokenGenerator.generate(assertions);
            return restApiClientService.search(countryCode, jwtToken, searchParams, "Bundle");
        } catch (final Exception e) {
            throw new ClientConnectorException(e.getMessage(), e);
        }
    }

    /**
     * @param assertions  - Map of assertions required by the transaction (HCP, TRC, NoK optional).
     * @param countryCode - ISO Country code of the patient country of origin.
     * @param id          - Identifier of the bundle
     * @return List of Bundle resources
     * @throws ClientConnectorException - Checked exception that passes the exception thrown by OpenNCP
     */
    @Override
    public ResponseEntity<String> queryBundleFhirById(final Map<AssertionType, Assertion> assertions, final String countryCode, final String id) throws ClientConnectorException {
        try {
            final String jwtToken = jwtTokenGenerator.generate(assertions);
            return restApiClientService.search(countryCode, jwtToken, new HashMap<>(), "Bundle/" + id);
        } catch (final Exception e) {
            throw new ClientConnectorException(e.getMessage(), e);
        }
    }


    /**
     * Retrieves the clinical document of an identified patient (prescription, patient summary or original clinical document).
     *
     * @param assertions      - Map of assertions required by the transaction (HCP, TRC and NoK optional).
     * @param countryCode     - ISO Country code of the patient country of origin.
     * @param documentId      - Unique identifier of the CDA document.
     * @param homeCommunityId - HL7 Home Community ID of the country of origin.
     * @param classCode       - HL7 ClassCode of the document type to be retrieved.
     * @param targetLanguage  - Expected target language of the CDA translation.
     * @return Clinical Document and metadata returned by the Country of Origin.
     */
    public EpsosDocument retrieveDocument(final Map<AssertionType, Assertion> assertions, final String countryCode, final DocumentId documentId,
                                          final String homeCommunityId, final GenericDocumentCode classCode, final String targetLanguage) throws ClientConnectorException {

        logger.info("[National Connector] retrieveDocument(countryCode:'{}', homeCommunityId:'{}', targetLanguage:'{}')", countryCode,
                homeCommunityId, targetLanguage);
        try {
            final var retrieveDocumentRequest = objectFactory.createRetrieveDocumentRequest();
            retrieveDocumentRequest.setDocumentId(documentId);
            retrieveDocumentRequest.setClassCode(classCode);
            retrieveDocumentRequest.setCountryCode(countryCode);
            retrieveDocumentRequest.setHomeCommunityId(homeCommunityId);
            retrieveDocumentRequest.setTargetLanguage(targetLanguage);

            clientConnectorService.setAssertions(assertions);

            return clientConnectorService.getClientConnectorServicePortType().retrieveDocument(retrieveDocumentRequest);
        } catch (Exception e) {
            throw createClientConnectorException(e);
        }
    }

    /**
     * Submits Clinical Document to the patient country of origin (dispense and discard).
     *
     * @param assertions          - Map of assertions required by the transaction (HCP, TRC and NoK optional).
     * @param countryCode         - ISO Country code of the patient country of origin.
     * @param document            - Clinical document and metadata to be submitted to the patient country of origin.
     * @param patientDemographics - Demographics of the patient linked to the document submission.
     * @return Acknowledge and status of the document submission.
     */
    public SubmitDocumentResponse submitDocument(final Map<AssertionType, Assertion> assertions, final String countryCode, final EpsosDocument document,
                                                 final PatientDemographics patientDemographics) throws ClientConnectorException {

        logger.info("[National Connector] submitDocument(countryCode:'{}')", countryCode);
        try {
            final var submitDocumentRequest = objectFactory.createSubmitDocumentRequest();
            submitDocumentRequest.setDocument(document);
            submitDocumentRequest.setCountryCode(countryCode);
            submitDocumentRequest.setPatientDemographics(patientDemographics);

            clientConnectorService.setAssertions(assertions);

            final SubmitDocumentResponse submitDocumentResponse = objectFactory.createSubmitDocumentResponse();
            submitDocumentResponse.setResponseStatus(clientConnectorService.getClientConnectorServicePortType().submitDocument(submitDocumentRequest));
            return submitDocumentResponse;
        } catch (Exception e) {
            throw createClientConnectorException(e);
        }
    }

    private ClientConnectorException createClientConnectorException(Throwable e) {
        if (e instanceof SOAPFaultException) {
            final SOAPFaultException soapFaultException = (SOAPFaultException) e;
            final String niExceptionMessage = extractNiExceptionMessage(soapFaultException.getMessage());
            final String soapFaultSubCode = soapFaultException.getFault().getFaultSubcodes().next().getLocalPart();
            final OpenNCPErrorCode openNCPErrorCode = OpenNCPErrorCode.getErrorCode(soapFaultSubCode);
            return (openNCPErrorCode != null) ? new ClientConnectorException(openNCPErrorCode, soapFaultException.getMessage(), niExceptionMessage, e) :
                    new ClientConnectorException(soapFaultException.getMessage(), soapFaultException);
        } else {
            return new ClientConnectorException(e.getMessage(), e);
        }
    }

    public String extractNiExceptionMessage(final String soapFaultExceptionMessage) {
        String[] split = soapFaultExceptionMessage.split("\\^");
        if (split.length > 1) {
            return Optional.of(String.join(StringUtils.SPACE, Arrays.asList(split).subList(1, split.length)))
                    .filter(joinedMessage -> !joinedMessage.isBlank())
                    .orElse(null);
        }
        return null;
    }
}
