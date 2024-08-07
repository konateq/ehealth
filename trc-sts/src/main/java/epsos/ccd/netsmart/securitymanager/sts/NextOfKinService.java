package epsos.ccd.netsmart.securitymanager.sts;

import com.sun.xml.ws.api.security.trust.WSTrustException;
import epsos.ccd.gnomon.auditmanager.*;
import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import epsos.ccd.netsmart.securitymanager.sts.util.STSUtils;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditServiceFactory;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.openncp.securitymanager.SamlIssuerHelper;
import eu.europa.ec.sante.openncp.securitymanager.SamlNextOfKinIssuer;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.DateUtil;
import tr.com.srdc.epsos.util.http.HTTPUtil;
import tr.com.srdc.epsos.util.http.IPUtil;

import javax.servlet.http.HttpServletRequest;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.ws.*;
import javax.xml.ws.handler.MessageContext;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ServiceMode(value = Service.Mode.MESSAGE)
@WebServiceProvider(targetNamespace = "https://ehdsi.eu/", serviceName = "NextOfKinTokenService", portName = "NextOfKinTokenService_Port")
@BindingType(value = "http://java.sun.com/xml/ns/jaxws/2003/05/soap/bindings/HTTP/")
public class NextOfKinService extends SecurityTokenServiceWS implements Provider<SOAPMessage> {

    private final Logger logger = LoggerFactory.getLogger(NextOfKinService.class);

    @Override
    public SOAPMessage invoke(SOAPMessage soapMessage) {

        logger.info("[NextOfKin WS] Invoke method");
        log(soapMessage);

        SOAPBody body;
        SOAPHeader header;
        try {
            body = soapMessage.getSOAPBody();
            header = soapMessage.getSOAPHeader();
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
            var nextOfKinDetail = STSUtils.getNextOfKinDetails(body);
            String messageId = getMessageIdFromHeader(header);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();

            // The response TRC Assertion Issuer.
            var samlNextOfKinIssuer = new SamlNextOfKinIssuer();
            var hcpIdAssertion = getIdAssertionFromHeader(header);
            if (hcpIdAssertion != null) {
                logger.info("hcpIdAssertion: '{}'", hcpIdAssertion.getID());
                if (hcpIdAssertion.getIssueInstant() != null) {
                    logger.info("hcpIdAssertion Issue Instant: '{}'", hcpIdAssertion.getIssueInstant());
                }
            }
            List<Attribute> attributeList = buildNextOfKinAttributes(nextOfKinDetail);
            var nextOfKinAssertion = samlNextOfKinIssuer.issueNextOfKinToken(hcpIdAssertion, "doctorId",
                    hcpIdAssertion.getID(), attributeList);
            if (hcpIdAssertion != null) {
                logger.info("HCP Assertion Date: '{}' TRC Assertion Date: '{}' -- '{}'",
                        hcpIdAssertion.getIssueInstant().atZone(ZoneId.of("UTC")),
                        nextOfKinAssertion.getIssueInstant().atZone(ZoneId.of("UTC")), nextOfKinAssertion.getAuthnStatements().isEmpty());
            }
            Document signedDoc = builder.newDocument();
            var marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
            marshallerFactory.getMarshaller(nextOfKinAssertion).marshall(nextOfKinAssertion, signedDoc);

            SOAPMessage response = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
            response.getSOAPBody().addDocument(STSUtils.createRSTRC(signedDoc));
            createResponseHeader(response.getSOAPHeader(), messageId);

            var strRespHeader = STSUtils.domElementToString(response.getSOAPHeader());
            var strReqHeader = STSUtils.domElementToString(header);

            String sslCommonName;
            if (context.getUserPrincipal() != null) {

                sslCommonName = context.getUserPrincipal().getName();
                logger.info("WebServiceContext JAX-WS User: '{}'", sslCommonName);
            } else {
                logger.info("WebServiceContext JAX-WS - No User authenticated");
            }

            sslCommonName = HTTPUtil.getSubjectDN(false);
            sendNOKAuditMessage(samlNextOfKinIssuer.getPointOfCare(), samlNextOfKinIssuer.getHumanRequestorNameId(),
                    samlNextOfKinIssuer.getHumanRequestorSubjectId(), samlNextOfKinIssuer.getFunctionalRole(),
                    nextOfKinDetail.getLivingSubjectIds(), samlNextOfKinIssuer.getFacilityType(), nextOfKinAssertion.getID(), sslCommonName, messageId,
                    strReqHeader.getBytes(StandardCharsets.UTF_8), getMessageIdFromHeader(response.getSOAPHeader()),
                    strRespHeader.getBytes(StandardCharsets.UTF_8));

            log(response);
            return response;
        } catch (ParserConfigurationException | WSTrustException | SMgrException | SOAPException | MarshallingException | ParseException e) {
            throw new WebServiceException(e);
        }
    }

    private void sendNOKAuditMessage(String pointOfCareID, String humanRequestorNameID, String humanRequestorSubjectID,
                                     String humanRequestorRole, final List<String> nokIDs, String facilityType, String assertionId,
                                     String certificateCommonName, String reqMid, byte[] reqSecHeader, String resMid, byte[] resSecHeader) {

        var auditService = AuditServiceFactory.getInstance();
        XMLGregorianCalendar date = DateUtil.getDateAsXMLGregorian(new Date());
        String trcCommonName = HTTPUtil.getTlsCertificateCommonName(ConfigurationManagerFactory.getConfigurationManager().getProperty("secman.nextOfKin.url"));
        String sourceGateway = getClientIP();
        logger.info("STS Client IP: '{}'", sourceGateway);
        var messageContext = context.getMessageContext();
        HttpServletRequest servletRequest = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
        String serverName = servletRequest.getServerName();

        //TODO: Review Audit Trail specification - Identifying SC and SP as value of CN from TLS certificate.
        EventLog eventLogNOKA = EventLog.createEventLogNOKA(TransactionName.NOK_ASSERTION, EventActionCode.EXECUTE,
                date, EventOutcomeIndicator.FULL_SUCCESS, pointOfCareID, facilityType, humanRequestorNameID,
                humanRequestorRole, humanRequestorSubjectID, certificateCommonName, trcCommonName,
                ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_PRINCIPAL_SUBDIVISION"),
                nokIDs, Constants.UUID_PREFIX + assertionId, reqMid, reqSecHeader, resMid, resSecHeader,
                IPUtil.isLocalLoopbackIp(sourceGateway) ? serverName : sourceGateway, STSUtils.getSTSServerIP(), NcpSide.NCP_B);

        eventLogNOKA.setEventType(EventType.NOK_ASSERTION);
        auditService.write(eventLogNOKA, "13", "2");
    }

    private List<Attribute> buildNextOfKinAttributes(NextOfKinDetail nextOfKinDetail) {

        List<Attribute> attributeList = new ArrayList<>();
        if (nextOfKinDetail.getLivingSubjectIds() != null && StringUtils.isNotBlank(nextOfKinDetail.getLivingSubjectIds().get(0))) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getLivingSubjectIds().get(0), "NextOfKinId", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:id"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getFirstName())) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getFirstName(), "NextOfKinFirstName", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:firstname"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getFamilyName())) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getFamilyName(), "NextOfKinFamilyName", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:familyname"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getGender())) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getGender(), "NextOfKinGender", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:gender"));
        }
        if (nextOfKinDetail.getBirthDate() != null) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getBirthDate().toString(), "NextOfKinBirthDate", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:birthdate"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getAddressStreet())) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getAddressStreet(), "NextOfKinAddressStreet", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:address:street"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getAddressCity())) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getAddressCity(), "NextOfKinAddressCity", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:address:city"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getAddressPostalCode())) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getAddressPostalCode(), "NextOfKinPostalCode", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:address:postalcode"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getAddressCountry())) {
            attributeList.add(SamlIssuerHelper
                    .createAttribute(nextOfKinDetail.getAddressCountry(), "NextOfKinAddressCountry", Attribute.URI_REFERENCE,
                            "urn:ehdsi:names:subject:nextofkin:address:country"));
        }
        return attributeList;
    }
}
