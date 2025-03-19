package eu.europa.ec.sante.openncp.application.client.connector.assertion;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

@Service
public class AssertionServiceImpl implements AssertionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionServiceImpl.class);

    private static final String SAML20_TOKEN_URN = "urn:oasis:names:tc:SAML:2.0:assertion";
    private final DocumentBuilder documentBuilder;
    private final KeyStoreManager keyStoreManager;
    private final ConfigurationManager configurationManager;

    public AssertionServiceImpl(KeyStoreManager keyStoreManager, ConfigurationManager configurationManager) throws STSClientException {
        this.keyStoreManager = Validate.notNull(keyStoreManager);
        this.configurationManager = Validate.notNull(configurationManager);

        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setXIncludeAware(false);
            documentBuilderFactory.setNamespaceAware(true);
            this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (final ParserConfigurationException ex) {
            throw new STSClientException("Unable to create create document builder", ex);
        }
    }

    @Override
    public Assertion request(final AssertionRequest assertionRequest) throws STSClientException {
        Validate.notNull(assertionRequest);

        try {
            LOGGER.info("TRC-STS request for assertion [{}]", assertionRequest);
            HttpURLConnection httpConnection = (HttpURLConnection) assertionRequest.getLocation().openConnection();
            //Set headers
            httpConnection.setRequestProperty("Content-Type", "application/soap+xml");
            httpConnection.setRequestProperty("SOAPAction", "");
            httpConnection.setRequestMethod("POST");
            httpConnection.setDoInput(true);
            httpConnection.setDoOutput(true);

            LOGGER.info("Checking SSL Hostname Verifier: '{}'", assertionRequest.checkForHostname());
            if (httpConnection instanceof HttpsURLConnection) {  // Going SSL
                ((HttpsURLConnection) httpConnection).setSSLSocketFactory(getSSLSocketFactory());
                if (!assertionRequest.checkForHostname()) {
                    ((HttpsURLConnection) httpConnection).setHostnameVerifier((hostname, sslSession) ->
                        // do add some logic to verify the hostname
                        hostname != null && !hostname.isEmpty()
                    );
                }
            }

            String sslKeyAlias = System.getProperty("javax.net.ssl.key.alias");

            //  Write and send the SOAP message
            LOGGER.info("Sending SOAP request - Default Key Alias: '{}'", StringUtils.isNotBlank(sslKeyAlias) ? sslKeyAlias : "N/A");
            LOGGER.info("SOAP Message : '{}'", assertionRequest.getSoapMessage());

            SOAPMessage soapMessage = assertionRequest.getSoapMessage();
            soapMessage.writeTo(httpConnection.getOutputStream());
            InputStream inputStream = httpConnection.getInputStream();

            LOGGER.info("httpConnection.getInputStream() : '{}'", inputStream);
            SOAPMessage response = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage(new MimeHeaders(), inputStream);

            LOGGER.info("Receiving SOAP response");
            if (response.getSOAPBody().hasFault()) {

                SOAPFault newFault = response.getSOAPBody().getFault();
                String code = newFault.getFaultCode();
                var string = newFault.getFaultString();
                throw new SOAPException("Code:" + code + ", Error String:" + string);
            }
            var assertion = extractAssertion(response);
            LOGGER.info("TRC Assertion: '{}'", assertion != null ? assertionRequest.getId() : "TRC Assertion is NULL");

            if (assertionRequest.validationEnabled()) {
                assertionRequest.validate(assertion);
            }

            return assertion;

        } catch (SOAPException | IOException ex) {
            throw new STSClientException("SOAP Exception: " + ex.getMessage(), ex);
        } catch (UnsupportedOperationException ex) {
            throw new STSClientException("Unsupported Operation: " + ex.getMessage(), ex);
        } catch (Exception e) {
            throw new STSClientException(e.getMessage(), e);
        }
    }

    private Assertion extractAssertion(SOAPMessage response) throws STSClientException {
        try {
            LOGGER.info("[TRC-STS Client] Extract assertion from soap message");
            var body = response.getSOAPBody();
            if (body.getElementsByTagNameNS(SAML20_TOKEN_URN, "Assertion").getLength() != 1) {
                throw new STSClientException("TRC Assertion is missing from the RSTRC body");
            }
            SOAPElement assertion = (SOAPElement) body.getElementsByTagNameNS(SAML20_TOKEN_URN, "Assertion").item(0);
            Document assertDoc = documentBuilder.newDocument();

            Node dupBody = assertDoc.importNode(assertion, true);
            assertDoc.appendChild(dupBody);
            if (assertion == null) {
                return null;
            }

            var unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
            var unmarshaller = unmarshallerFactory.getUnmarshaller(assertion);
            var trcAssertion = (Assertion) unmarshaller.unmarshall(assertDoc.getDocumentElement());
            if (GazelleValidation.isValidationEnable()) {
                GazelleValidation.validateTRCAssertion(trcAssertion, NcpSide.NCP_B);
            }
            return trcAssertion;

        } catch (Exception ex) {
            throw new STSClientException("Error while trying to extract the SAML TRC Assertion from RSTRC Body: " + ex.getMessage());
        }
    }

    private SSLSocketFactory getSSLSocketFactory() {

        SSLContext sslContext;
        try {
            String sigKeystorePassword = configurationManager.getProperty(Constant.NCP_SIG_KEYSTORE_PASSWORD);

            sslContext = SSLContext.getInstance("TLSv1.2");

            var keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStoreManager.getKeyStore(), sigKeystorePassword.toCharArray());

            var trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStoreManager.getTrustStore());

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext.getSocketFactory();

        } catch (KeyManagementException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            return null;
        }
    }
}
