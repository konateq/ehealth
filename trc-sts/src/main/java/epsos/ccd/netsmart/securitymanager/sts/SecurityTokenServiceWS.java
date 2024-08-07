package epsos.ccd.netsmart.securitymanager.sts;

import com.sun.xml.ws.api.security.trust.WSTrustException;
import epsos.ccd.netsmart.securitymanager.SignatureManager;
import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import epsos.ccd.netsmart.securitymanager.sts.util.STSUtils;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptacular.util.CertUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.xmlsec.signature.X509Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class SecurityTokenServiceWS {

    public static final String MESSAGE_ID = "MessageID";
    public static final QName Messaging_To = new QName("http://www.w3.org/2005/08/addressing", "To");
    public static final String SAML20_TOKEN_URN = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String SUPPORTED_ACTION_URI = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue";
    public static final String NOK_NS = "https://ehdsi.eu/assertion/nok";
    public static final String TRC_NS = "https://ehdsi.eu/assertion/trc";
    public static final String WS_TRUST_NS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    public static final String ADDRESSING_NS = "http://www.w3.org/2005/08/addressing";
    public static final String WS_SEC_UTIL_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static final String WS_SEC_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    static {
        try {
            InitializationService.initialize();
        } catch (final InitializationException e) {
            // SAML Framework cannot be initialized correctly.
        }
    }

    private final Logger logger = LoggerFactory.getLogger(SecurityTokenServiceWS.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    @Resource
    public WebServiceContext context;

    public SecurityTokenServiceWS() {
        // Default empty constructor.
    }

    public void createResponseHeader(final SOAPHeader header, final String messageId) {

        try {
            final var now = new DateTime();

            final var soapFactory = SOAPFactory.newInstance();
            final SOAPElement messageIdElem = header.addHeaderElement(new QName(ADDRESSING_NS, MESSAGE_ID, "wsa"));
            messageIdElem.setTextContent("uuid:" + UUID.randomUUID());
            final SOAPElement securityHeaderElem = header.addHeaderElement(new QName(WS_SEC_NS, "Security", "wsse"));

            final SOAPElement timeStampElem = soapFactory.createElement("Timestamp", "wsu", WS_SEC_UTIL_NS);
            final SOAPElement ltCreated = soapFactory.createElement("Created", "wsu", WS_SEC_UTIL_NS);
            ltCreated.setTextContent(now.toDateTime(DateTimeZone.UTC).toString());

            final SOAPElement ltExpires = soapFactory.createElement("Expires", "wsu", WS_SEC_UTIL_NS);
            ltExpires.setTextContent(now.plusHours(2).toDateTime(DateTimeZone.UTC).toString());

            timeStampElem.addChildElement(ltCreated);
            timeStampElem.addChildElement(ltExpires);

            securityHeaderElem.addChildElement(timeStampElem);

            final SOAPElement actionElem = header.addHeaderElement(new QName(ADDRESSING_NS, "Action", "wsa"));
            actionElem.setTextContent("urn:IssueTokenResponse");

            final SOAPElement relatesToElem = header.addHeaderElement(new QName(ADDRESSING_NS, "RelatesTo", "wsa"));
            relatesToElem.setTextContent(messageId);

        } catch (final SOAPException ex) {
            logger.error(null, ex);
            throw new WebServiceException("Could not create Response Header");
        }
    }

    public String getCertificateCommonName(final Assertion hcpIdAssertion) {

        //TODO: Test Certificate CN retrieve
        final var keyInfo = hcpIdAssertion.getSignature().getKeyInfo();
        for (final X509Data x509Data : keyInfo.getX509Datas()) {
            for (final org.opensaml.xmlsec.signature.X509Certificate x509Certificate : x509Data.getX509Certificates()) {
                logger.info("[SAML] Signature certificate:\n'{}' ", x509Certificate.getValue());

                final byte[] encodedCert = Base64.getDecoder().decode(removeDisplayCharacter(x509Certificate.getValue()));
                final InputStream inputStream = new ByteArrayInputStream(encodedCert);

                final CertificateFactory certFactory;
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                    final java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) certFactory
                            .generateCertificate(inputStream);
                    logger.info(getCommonName(cert));
                    return getCommonName(cert);
                } catch (final CertificateException e) {
                    logger.error("CertificateException: '{}'", e.getMessage());
                }
            }
        }
        return STSUtils.NO_CLIENT_CERTIFICATE;
    }

    public String getClientIP() {

        try {
            final var messageContext = context.getMessageContext();
            final var httpServletRequest = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
            final var clientIpAddressAsString = httpServletRequest.getRemoteAddr();
            logger.debug("clientIpAddress: '{}'", clientIpAddressAsString);
            final var clientIpAddress = InetAddress.getByName(clientIpAddressAsString);
            if (!clientIpAddress.isLinkLocalAddress() && !clientIpAddress.isLoopbackAddress()) {
                return clientIpAddressAsString;
            } else {
                return STSUtils.getSTSServerIP();
            }
        } catch (final UnknownHostException ex) {
            logger.error("UnknownHostException: '{}'", ex.getMessage());
        }
        return "Could not get client IP address!";
    }

    public String getCommonName(final X509Certificate certificate) {
        return CertUtil.subjectCN(certificate);
    }

    public Assertion getIdAssertionFromHeader(final SOAPHeader header) throws WSTrustException {

        try {
            // First, find the assertion from the header.
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            if (header.getElementsByTagNameNS(SAML20_TOKEN_URN, "Assertion").getLength() != 1) {
                throw new WSTrustException("SAML Identity Assertion is missing from the Security Header");
            }
            final SOAPElement assertion = (SOAPElement) header.getElementsByTagNameNS(SAML20_TOKEN_URN, "Assertion").item(0);
            final Document assertDoc = builder.newDocument();

            final Node dupBody = assertDoc.importNode(assertion, true);
            assertDoc.appendChild(dupBody);
            if (assertion == null) {
                return null;
            } else {
                assertDoc.getDocumentElement().setIdAttribute("ID", true);
                final var signatureManager = new SignatureManager();
                signatureManager.verifyEnvelopedSignature(assertDoc);
            }
            final var unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
            final var unmarshaller = unmarshallerFactory.getUnmarshaller(assertion);
            return (Assertion) unmarshaller.unmarshall(assertDoc.getDocumentElement());

        } catch (final SMgrException ex) {
            throw new WSTrustException("Error validating SAML Assertion signature", ex);
        } catch (final ParserConfigurationException | UnmarshallingException ex) {
            throw new WSTrustException("Error Parsing SAML Assertion in Message Header", ex);
        }
    }

    public String getMessageIdFromHeader(final SOAPHeader header) {

        if (header.getElementsByTagNameNS(ADDRESSING_NS, MESSAGE_ID).getLength() < 1) {
            throw new WebServiceException("Message ID not found in Header");
        }
        String messageID = header.getElementsByTagNameNS(ADDRESSING_NS, MESSAGE_ID).item(0).getTextContent();
        if (messageID.startsWith("uuid"))
            messageID = "urn:" + messageID;
        return messageID;
    }

    public List<String> getPatientIDs(final SOAPElement body) {

        if (body.getElementsByTagNameNS(TRC_NS, "TRCParameters").getLength() < 1) {
            throw new WebServiceException("No TRC Parameters in RST");
        }

        final SOAPElement trcDetails = (SOAPElement) body.getElementsByTagNameNS(TRC_NS, "TRCParameters").item(0);
        final NodeList patientIdElements = trcDetails.getElementsByTagNameNS(TRC_NS, "PatientId");
        if (patientIdElements.getLength() == 0 || patientIdElements.item(0) == null) {
            throw new WebServiceException("Patient ID is Missing from the RST");
        }
        final List<String> patientIds = new ArrayList<>();
        for (int i = 0; i < patientIdElements.getLength(); i++) {
            patientIds.add(patientIdElements.item(i).getTextContent());
        }
        return patientIds;
    }

    public String getRSTAction(final SOAPBody body) throws WSTrustException {

        if (body.getElementsByTagNameNS(WS_TRUST_NS, "RequestType").getLength() < 1) {
            throw new WSTrustException("No Request Type is Specified.");
        }

        return body.getElementsByTagNameNS(WS_TRUST_NS, "RequestType").item(0).getTextContent();
    }

    public String getRequestedToken(final SOAPBody body) throws WSTrustException {

        if (body.getElementsByTagNameNS(WS_TRUST_NS, "TokenType").getLength() < 1) {
            throw new WSTrustException("No Token Type is Specified.");
        }

        return body.getElementsByTagNameNS(WS_TRUST_NS, "TokenType").item(0).getTextContent();
    }

    public void log(final SOAPMessage message) {
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            try (final var out = new ByteArrayOutputStream()) {
                message.writeTo(out);
                loggerClinical.debug("SOAPMessage:\n{}", out);
            } catch (final IOException | SOAPException e) {
                loggerClinical.error("Exception: '{}'", e.getMessage(), e);
            }
        }
    }

    private String removeDisplayCharacter(final String certificateValue) {

        String certificatePEM = RegExUtils.removeAll(certificateValue, "-----BEGIN CERTIFICATE-----");
        certificatePEM = RegExUtils.removeAll(certificatePEM, "-----END CERTIFICATE-----");
        certificatePEM = RegExUtils.removeAll(certificatePEM, StringUtils.LF);
        certificatePEM = RegExUtils.removeAll(certificatePEM, StringUtils.CR);
        return certificatePEM;
    }
}
