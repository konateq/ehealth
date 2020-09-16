package epsos.ccd.netsmart.securitymanager.sts;

import com.sun.xml.ws.api.security.trust.WSTrustException;
import epsos.ccd.gnomon.auditmanager.*;
import epsos.ccd.netsmart.securitymanager.SamlTRCIssuer;
import epsos.ccd.netsmart.securitymanager.SignatureManager;
import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import epsos.ccd.netsmart.securitymanager.sts.util.STSUtils;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditService;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditServiceFactory;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.*;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sun.security.x509.X500Name;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.http.HTTPUtil;
import tr.com.srdc.epsos.util.http.IPUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.ws.*;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.handler.MessageContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

@ServiceMode(value = Mode.MESSAGE)
@WebServiceProvider(targetNamespace = "http://epsos.eu/", serviceName = "SecurityTokenService", portName = "ISecurityTokenService_Port")
@BindingType(value = "http://java.sun.com/xml/ns/jaxws/2003/05/soap/bindings/HTTP/")
public class STSService implements Provider<SOAPMessage> {

    private static final String MESSAGE_ID = "MessageID";
    private static final QName Messaging_To = new QName("http://www.w3.org/2005/08/addressing", "To");
    private static final String SAML20_TOKEN_URN = "urn:oasis:names:tc:SAML:2.0:assertion"; // What
    // can be only requested from the STS
    private static final String SUPPORTED_ACTION_URI = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue"; // Only
    // Issuance is supported
    private static final String TRC_NS = "http://epsos.eu/trc"; // TRC
    // Parameters Namespace
    private static final String WS_TRUST_NS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512"; // WS-Trust
    // Namespace
    private static final String ADDRESSING_NS = "http://www.w3.org/2005/08/addressing"; // WSA
    // Namespace
    private static final String WS_SEC_UTIL_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String WS_SEC_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    static {
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            // SAML Framework cannot be initialized correctly.
        }
    }

    private final Logger logger = LoggerFactory.getLogger(STSService.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    @Resource
    private WebServiceContext context;

    @Override
    public SOAPMessage invoke(SOAPMessage source) {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.debug("Incoming SOAP Message request: '{}'", source);
            log(source);
        }

        SOAPBody body;
        SOAPHeader header;
        try {
            body = source.getSOAPBody();
            header = source.getSOAPHeader();
        } catch (SOAPException ex) {
            throw new WebServiceException("Cannot get Soap Message Parts", ex);
        }

        try {
            if (!SUPPORTED_ACTION_URI.equals(getRSTAction(body))) {
                throw new WebServiceException("Only ISSUE action is supported");
            }
            if (!SAML20_TOKEN_URN.equals(getRequestedToken(body))) {
                throw new WebServiceException("Only SAML2.0 Tokens are Issued");
            }
        } catch (WSTrustException ex) {
            throw new WebServiceException(ex);
        }

        try {
            // these calls are both getters and checkers of message.
            // So we call them first
            String purposeOfUse = STSUtils.getPurposeOfUse(body);
            String patientID = getPatientID(body);
            String mid = getMessageIdFromHeader(header);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();

            // The response TRC Assertion Issuer.
            SamlTRCIssuer samlTRCIssuer = new SamlTRCIssuer();
            Assertion hcpIdAssertion = getIdAssertionFromHeader(header);
            if (hcpIdAssertion != null) {
                logger.info("hcpIdAssertion: '{}'", hcpIdAssertion.getID());
                if (hcpIdAssertion.getIssueInstant() != null) {
                    logger.info("hcpIdAssertion Issue Instant: '{}'", hcpIdAssertion.getIssueInstant());
                }
            }
            Assertion trc = samlTRCIssuer.issueTrcToken(hcpIdAssertion, patientID, purposeOfUse, null);
            if (hcpIdAssertion != null) {
                logger.info("HCP Assertion Date: '{}' TRC Assertion Date: '{}' -- '{}'",
                        hcpIdAssertion.getIssueInstant().withZone(DateTimeZone.UTC),
                        trc.getIssueInstant().withZone(DateTimeZone.UTC), trc.getAuthnStatements().isEmpty());
            }

            Document signedDoc = builder.newDocument();
            MarshallerFactory marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
            marshallerFactory.getMarshaller(trc).marshall(trc, signedDoc);

            SOAPMessage response = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
            response.getSOAPBody().addDocument(STSUtils.createRSTRC(signedDoc));
            createResponseHeader(response.getSOAPHeader(), mid);

            String strRespHeader = STSUtils.domElementToString(response.getSOAPHeader());
            String strReqHeader = STSUtils.domElementToString(header);


            String sslCommonName;

            if (context.getUserPrincipal() != null) {

                sslCommonName = context.getUserPrincipal().getName();
                logger.info("WebServiceContext JAX-WS User: '{}'", sslCommonName);
            } else {
                logger.info("WebServiceContext JAX-WS - No User authenticated");
            }

            sslCommonName = HTTPUtil.getSubjectDN(false);
            sendTRCAuditMessage(samlTRCIssuer.getPointofCare(), samlTRCIssuer.getHumanRequestorNameId(),
                    samlTRCIssuer.getHumanRequestorSubjectId(), samlTRCIssuer.getFunctionalRole(), patientID,
                    samlTRCIssuer.getFacilityType(), trc.getID(), sslCommonName, mid,
                    strReqHeader.getBytes(StandardCharsets.UTF_8), getMessageIdFromHeader(response.getSOAPHeader()),
                    strRespHeader.getBytes(StandardCharsets.UTF_8));

            if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                loggerClinical.debug("Outgoing SOAP Message response: '{}'", response);
                log(response);
            }
            return response;

        } catch (SOAPException | WSTrustException | MarshallingException | SMgrException | ParserConfigurationException ex) {
            throw new WebServiceException(ex);
        }
    }

    private String getCertificateCommonName(Assertion hcpIdAssertion) {

        //TODO: Test Certificate CN retrieve
        KeyInfo hcpSignature = hcpIdAssertion.getSignature().getKeyInfo();
        for (X509Data x509Data : hcpSignature.getX509Datas()) {
            for (X509Certificate x509Certificate : x509Data.getX509Certificates()) {
                logger.info("[SAML] Signature certificate:\n'{}' ", x509Certificate.getValue());

                byte[] encodedCert = Base64.getDecoder().decode(removeDisplayCharacter(x509Certificate.getValue()));
                InputStream inputStream = new ByteArrayInputStream(encodedCert);

                CertificateFactory certFactory;
                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                    java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) certFactory
                            .generateCertificate(inputStream);
                    logger.info(((X500Name) cert.getSubjectDN()).getCommonName());
                    return ((X500Name) cert.getSubjectDN()).getCommonName();
                } catch (CertificateException e) {
                    logger.error("CertificateException: '{}'", e.getMessage());
                } catch (IOException e) {
                    logger.error("IOException: '{}'", e.getMessage());
                }
            }
        }
        return STSUtils.NO_CLIENT_CERTIFICATE;
    }

    private String removeDisplayCharacter(String certificateValue) {

        String certificatePEM = StringUtils.removeAll(certificateValue, "-----BEGIN CERTIFICATE-----");
        certificatePEM = StringUtils.removeAll(certificatePEM, "-----END CERTIFICATE-----");
        certificatePEM = StringUtils.removeAll(certificatePEM, StringUtils.LF);
        certificatePEM = StringUtils.removeAll(certificatePEM, StringUtils.CR);
        return certificatePEM;
    }

    private String getPatientID(SOAPElement body) {

        if (body.getElementsByTagNameNS(TRC_NS, "TRCParameters").getLength() < 1) {
            throw new WebServiceException("No TRC Parameters in RST");
        }

        SOAPElement trcDetails = (SOAPElement) body.getElementsByTagNameNS(TRC_NS, "TRCParameters").item(0);
        if (trcDetails.getElementsByTagNameNS(TRC_NS, "PatientId").item(0) == null) {
            // Cannot be null!
            throw new WebServiceException("Patient ID is Missing from the RST");
        }
        return trcDetails.getElementsByTagNameNS(TRC_NS, "PatientId").item(0).getTextContent();
    }

    private Assertion getIdAssertionFromHeader(SOAPHeader header) throws WSTrustException {

        try {
            // First, find the assertion from the header.
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            if (header.getElementsByTagNameNS(SAML20_TOKEN_URN, "Assertion").getLength() != 1) {
                throw new WSTrustException("SAML Identity Assertion is missing from the Security Header");
            }
            SOAPElement assertion = (SOAPElement) header.getElementsByTagNameNS(SAML20_TOKEN_URN, "Assertion").item(0);
            Document assertDoc = builder.newDocument();

            Node dupBody = assertDoc.importNode(assertion, true);
            assertDoc.appendChild(dupBody);
            if (assertion == null) {
                return null;
            } else {
                assertDoc.getDocumentElement().setIdAttribute("ID", true);
                SignatureManager sm = new SignatureManager();
                try {
                    sm.verifyEnvelopedSignature(assertDoc);
                } catch (SMgrException ex) {
                    throw new WSTrustException("Error validating SAML Assertion signature", ex);
                }
            }
            UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(assertion);
            return (Assertion) unmarshaller.unmarshall(assertDoc.getDocumentElement());

        } catch (ParserConfigurationException | UnmarshallingException ex) {
            throw new WSTrustException("Error Parsing SAML Assertion in Message Header", ex);
        }
    }

    private String getMessageIdFromHeader(SOAPHeader header) {

        if (header.getElementsByTagNameNS(ADDRESSING_NS, MESSAGE_ID).getLength() < 1) {
            throw new WebServiceException("Message ID not found in Header");
        }
        String messageID = header.getElementsByTagNameNS(ADDRESSING_NS, MESSAGE_ID).item(0).getTextContent();
        if (messageID.startsWith("uuid"))
            messageID = "urn:" + messageID;
        return messageID;
    }

    private void createResponseHeader(SOAPHeader header, String messageId) {

        try {
            DateTime now = new DateTime();

            SOAPFactory fac = SOAPFactory.newInstance();
            SOAPElement messageIdElem = header.addHeaderElement(new QName(ADDRESSING_NS, MESSAGE_ID, "wsa"));
            messageIdElem.setTextContent("uuid:" + UUID.randomUUID().toString());
            SOAPElement securityHeaderElem = header.addHeaderElement(new QName(WS_SEC_NS, "Security", "wsse"));

            SOAPElement timeStampElem = fac.createElement("Timestamp", "wsu", WS_SEC_UTIL_NS);
            SOAPElement ltCreated = fac.createElement("Created", "wsu", WS_SEC_UTIL_NS);
            ltCreated.setTextContent(now.toDateTime(DateTimeZone.UTC).toString());

            SOAPElement ltExpires = fac.createElement("Expires", "wsu", WS_SEC_UTIL_NS);
            ltExpires.setTextContent(now.plusHours(2).toDateTime(DateTimeZone.UTC).toString());

            timeStampElem.addChildElement(ltCreated);
            timeStampElem.addChildElement(ltExpires);

            securityHeaderElem.addChildElement(timeStampElem);

            SOAPElement actionElem = header.addHeaderElement(new QName(ADDRESSING_NS, "Action", "wsa"));
            actionElem.setTextContent("urn:IssueTokenResponse");

            SOAPElement relatesToElem = header.addHeaderElement(new QName(ADDRESSING_NS, "RelatesTo", "wsa"));
            relatesToElem.setTextContent(messageId);

        } catch (SOAPException ex) {
            logger.error(null, ex);
            throw new WebServiceException("Could not create Response Header");
        }
    }

    private String getRSTAction(SOAPBody body) throws WSTrustException {

        if (body.getElementsByTagNameNS(WS_TRUST_NS, "RequestType").getLength() < 1) {
            throw new WSTrustException("No Request Type is Specified.");
        }

        return body.getElementsByTagNameNS(WS_TRUST_NS, "RequestType").item(0).getTextContent();
    }

    private String getRequestedToken(SOAPBody body) throws WSTrustException {

        if (body.getElementsByTagNameNS(WS_TRUST_NS, "TokenType").getLength() < 1) {
            throw new WSTrustException("No Token Type is Specified.");
        }

        return body.getElementsByTagNameNS(WS_TRUST_NS, "TokenType").item(0).getTextContent();
    }

    /**
     * @param pointOfCareID
     * @param humanRequestorNameID
     * @param humanRequestorSubjectID
     * @param humanRequestorRole
     * @param patientID
     * @param facilityType
     * @param assertionId
     * @param certificateCommonName
     * @param reqMid
     * @param reqSecHeader
     * @param resMid
     * @param resSecHeader
     */
    private void sendTRCAuditMessage(String pointOfCareID, String humanRequestorNameID, String humanRequestorSubjectID,
                                     String humanRequestorRole, String patientID, String facilityType, String assertionId,
                                     String certificateCommonName, String reqMid, byte[] reqSecHeader, String resMid, byte[] resSecHeader) {

        AuditService auditService = AuditServiceFactory.getInstance();
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        XMLGregorianCalendar date2 = null;
        try {
            date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException ex) {
            logger.error("DatatypeConfigurationException: '{}'", ex.getMessage(), ex);
        }
        String trcCommonName = HTTPUtil.getTlsCertificateCommonName(ConfigurationManagerFactory.getConfigurationManager().getProperty("secman.sts.url"));
        String sourceGateway = getClientIP();
        logger.info("STS Client IP: '{}'", sourceGateway);
        MessageContext messageContext = context.getMessageContext();
        HttpServletRequest servletRequest = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
        String serverName = servletRequest.getServerName();

        //TODO: Review Audit Trail specification - Identifying SC and SP as value of CN from TLS certificate.
        EventLog eventLogTRCA = EventLog.createEventLogTRCA(TransactionName.TRC_ASSERTION, EventActionCode.EXECUTE,
                date2, EventOutcomeIndicator.FULL_SUCCESS, pointOfCareID, facilityType, humanRequestorNameID, humanRequestorRole,
                humanRequestorSubjectID, certificateCommonName, trcCommonName, ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_PRINCIPAL_SUBDIVISION"),
                patientID, Constants.UUID_PREFIX + assertionId, reqMid, reqSecHeader, resMid, resSecHeader,
                IPUtil.isLocalLoopbackIp(sourceGateway) ? serverName : sourceGateway, STSUtils.getSTSServerIP(), NcpSide.NCP_B);

        eventLogTRCA.setEventType(EventType.TRC_ASSERTION);
        auditService.write(eventLogTRCA, "13", "2");
    }

    /**
     * Returns IP address from the Web Service MessageContext.
     *
     * @return String IP address of the client requesting the NCP-B-STS token.
     */
    public String getClientIP() {
        try {
            MessageContext messageContext = context.getMessageContext();
            HttpServletRequest httpServletRequest = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
            String clientIpAddressAsString = httpServletRequest.getRemoteAddr();
            logger.info("clientIpAddress: '{}'", clientIpAddressAsString);
            InetAddress clientIpAddress = InetAddress.getByName(clientIpAddressAsString);
            if (!clientIpAddress.isLinkLocalAddress() && !clientIpAddress.isLoopbackAddress()) {
                return clientIpAddressAsString;
            } else {
                return STSUtils.getSTSServerIP();
            }
        } catch (UnknownHostException ex) {
            logger.error("UnknownHostException: '{}'", ex.getMessage());
        }
        return "Could not get client IP address!";
    }

    /**
     * Logging SOAP messages processed by TRC server.
     *
     * @param message - Incoming SOAP message.
     */
    private void log(SOAPMessage message) {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                message.writeTo(out);
                loggerClinical.info("SOAPMessage:\n{}", out.toString());
            } catch (IOException | SOAPException e) {
                loggerClinical.error("Exception: '{}'", e.getMessage(), e);
            }
        }
    }
}
