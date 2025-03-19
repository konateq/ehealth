package eu.europa.ec.sante.openncp.sts;

import com.sun.xml.ws.api.security.trust.WSTrustException;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.common.security.issuer.SamlIssuerHelper;
import eu.europa.ec.sante.openncp.common.security.issuer.SamlNextOfKinIssuer;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTimeZone;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ServiceMode(value = Service.Mode.MESSAGE)
@WebServiceProvider(targetNamespace = "https://ehdsi.eu/", serviceName = "NextOfKinTokenService", portName = "NextOfKinTokenService_Port")
@BindingType(value = "http://java.sun.com/xml/ns/jaxws/2003/05/soap/bindings/HTTP/")
@org.springframework.stereotype.Service
public class NextOfKinService extends SecurityTokenServiceWS implements Provider<SOAPMessage> {

    private final Logger logger = LoggerFactory.getLogger(NextOfKinService.class);

    private final SamlNextOfKinIssuer samlNextOfKinIssuer;
    private final ConfigurationManager configurationManager;

    public NextOfKinService(final SamlNextOfKinIssuer samlNextOfKinIssuer, final SignatureManager signatureManager, final ConfigurationManager configurationManager) {
        super(signatureManager);
        this.samlNextOfKinIssuer = Validate.notNull(samlNextOfKinIssuer);
        this.configurationManager = Validate.notNull(configurationManager);
    }

    @Override
    public SOAPMessage invoke(final SOAPMessage soapMessage) {

        logger.info("[NextOfKin WS] Invoke method");
        log(soapMessage);

        final SOAPBody body;
        final SOAPHeader header;
        try {
            body = soapMessage.getSOAPBody();
            header = soapMessage.getSOAPHeader();
        } catch (final SOAPException ex) {
            throw new WebServiceException("Cannot get Soap Message Parts", ex);
        }

        try {
            if (!SUPPORTED_ACTION_URI.equals(getRSTAction(body))) {
                throw new WebServiceException("Only ISSUE action is supported");
            }
            if (!SAML20_TOKEN_URN.equals(getRequestedToken(body))) {
                throw new WebServiceException("Only SAML2.0 Tokens are Issued");
            }
        } catch (final WSTrustException ex) {
            throw new WebServiceException(ex);
        }

        try {
            final var nextOfKinDetail = STSUtils.getNextOfKinDetails(body);
            final String purposeOfUse = STSUtils.getPurposeOfUse(body, STSUtils.NOK_NS, "NoKParameters");
            final String messageId = getMessageIdFromHeader(header);

            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setXIncludeAware(false);
            documentBuilderFactory.setNamespaceAware(true);
            final DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();

            // The response TRC Assertion Issuer.
            final var hcpIdAssertion = getIdAssertionFromHeader(header);
            if (hcpIdAssertion != null) {
                logger.info("hcpIdAssertion: '{}'", hcpIdAssertion.getID());
                if (hcpIdAssertion.getIssueInstant() != null) {
                    logger.info("hcpIdAssertion Issue Instant: '{}'", hcpIdAssertion.getIssueInstant());
                }
            }
            final List<Attribute> attributeList = buildNextOfKinAttributes(nextOfKinDetail);
            final Assertion nextOfKinAssertion = samlNextOfKinIssuer.issueNextOfKinToken(hcpIdAssertion, nextOfKinDetail.getPatientId(), purposeOfUse, hcpIdAssertion.getID(), attributeList);
            logger.info("HCP Assertion Date: '{}' NOK Assertion Date: '{}' -- '{}'", hcpIdAssertion.getIssueInstant().withZone(DateTimeZone.UTC),
                        nextOfKinAssertion.getIssueInstant().withZone(DateTimeZone.UTC), nextOfKinAssertion.getAuthnStatements().isEmpty());
            final Document signedDoc = builder.newDocument();
            final var marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
            marshallerFactory.getMarshaller(nextOfKinAssertion).marshall(nextOfKinAssertion, signedDoc);

            final SOAPMessage response = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
            response.getSOAPBody().addDocument(STSUtils.createRSTRC(signedDoc));
            createResponseHeader(response.getSOAPHeader(), messageId);

            final var strRespHeader = STSUtils.domElementToString(response.getSOAPHeader());
            final var strReqHeader = STSUtils.domElementToString(header);

            String sslCommonName;
            if (context.getUserPrincipal() != null) {

                sslCommonName = context.getUserPrincipal().getName();
                logger.info("WebServiceContext JAX-WS User: '{}'", sslCommonName);
            } else {
                logger.info("WebServiceContext JAX-WS - No User authenticated");
            }

            sslCommonName = HttpUtil.getSubjectDN(false);
            sendNOKAuditMessage(samlNextOfKinIssuer.getPointOfCare(), samlNextOfKinIssuer.getHumanRequestorNameId(),
                                samlNextOfKinIssuer.getHumanRequestorSubjectId(), samlNextOfKinIssuer.getFunctionalRole(),
                    nextOfKinDetail.getLivingSubjectId(), samlNextOfKinIssuer.getFacilityType(), nextOfKinAssertion.getID(),
                                sslCommonName, messageId, strReqHeader.getBytes(StandardCharsets.UTF_8),
                                getMessageIdFromHeader(response.getSOAPHeader()), strRespHeader.getBytes(StandardCharsets.UTF_8));

            log(response);
            return response;
        } catch (final ParserConfigurationException | WSTrustException | SMgrException | SOAPException | MarshallingException | ParseException e) {
            throw new WebServiceException(e);
        }
    }

    private void sendNOKAuditMessage(final String pointOfCareID, final String humanRequestorNameID, final String humanRequestorSubjectID, final String humanRequestorRole,
                                     final String nokID, final String facilityType, final String assertionId, final String certificateCommonName, final String reqMid,
                                     final byte[] reqSecHeader, final String resMid, final byte[] resSecHeader) {

        final var auditService = AuditServiceFactory.getInstance();
        final XMLGregorianCalendar date = DateUtil.getDateAsXMLGregorian(new Date());
        final String trcCommonName = HttpUtil.getTlsCertificateCommonName(
                configurationManager.getProperty("secman.nextOfKin.url"));
        final String sourceGateway = getClientIP();
        logger.info("STS Client IP: '{}'", sourceGateway);
        final var messageContext = context.getMessageContext();
        final HttpServletRequest servletRequest = (HttpServletRequest) messageContext.get(MessageContext.SERVLET_REQUEST);
        final String serverName = servletRequest.getServerName();

        //TODO: Review Audit Trail specification - Identifying SC and SP as value of CN from TLS certificate.
        final EventLog eventLogNOKA = EventLog.createEventLogNOKA(TransactionName.NOK_ASSERTION, EventActionCode.EXECUTE, date,
                                                                  EventOutcomeIndicator.FULL_SUCCESS, pointOfCareID, facilityType, humanRequestorNameID,
                                                                  humanRequestorRole, humanRequestorSubjectID, certificateCommonName, trcCommonName,
                configurationManager.getProperty("COUNTRY_PRINCIPAL_SUBDIVISION"), nokID,
                                                            Constants.UUID_PREFIX + assertionId, reqMid, reqSecHeader, resMid, resSecHeader,
                                                            IPUtil.isLocalLoopbackIp(sourceGateway) ? serverName : sourceGateway,
                                                                  STSUtils.getSTSServerIP(), NcpSide.NCP_B);

        eventLogNOKA.setEventType(EventType.NOK_ASSERTION);
        auditService.write(eventLogNOKA, "13", "2");
    }

    private List<Attribute> buildNextOfKinAttributes(final NextOfKinDetail nextOfKinDetail) {

        final List<Attribute> attributeList = new ArrayList<>();
        if (StringUtils.isNotBlank(nextOfKinDetail.getLivingSubjectId())) {
            attributeList.add(SamlIssuerHelper.createAttribute(nextOfKinDetail.getLivingSubjectId(), "NextOfKinId", Attribute.URI_REFERENCE,
                                                               "urn:ehdsi:names:subject:nextofkin:id"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getFirstName())) {
            attributeList.add(SamlIssuerHelper.createAttribute(nextOfKinDetail.getFirstName(), "NextOfKinFirstName", Attribute.URI_REFERENCE,
                                                               "urn:ehdsi:names:subject:nextofkin:firstname"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getFamilyName())) {
            attributeList.add(SamlIssuerHelper.createAttribute(nextOfKinDetail.getFamilyName(), "NextOfKinFamilyName", Attribute.URI_REFERENCE,
                                                               "urn:ehdsi:names:subject:nextofkin:familyname"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getGender())) {
            attributeList.add(SamlIssuerHelper.createAttribute(nextOfKinDetail.getGender(), "NextOfKinGender", Attribute.URI_REFERENCE,
                                                               "urn:ehdsi:names:subject:nextofkin:gender"));
        }
        if (nextOfKinDetail.getBirthDate() != null) {
            attributeList.add(
                    SamlIssuerHelper.createAttribute(nextOfKinDetail.getBirthDate().toString(), "NextOfKinBirthDate", Attribute.URI_REFERENCE,
                                                     "urn:ehdsi:names:subject:nextofkin:birthdate"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getAddressStreet())) {
            attributeList.add(SamlIssuerHelper.createAttribute(nextOfKinDetail.getAddressStreet(), "NextOfKinAddressStreet", Attribute.URI_REFERENCE,
                                                               "urn:ehdsi:names:subject:nextofkin:address:street"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getAddressCity())) {
            attributeList.add(SamlIssuerHelper.createAttribute(nextOfKinDetail.getAddressCity(), "NextOfKinAddressCity", Attribute.URI_REFERENCE,
                                                               "urn:ehdsi:names:subject:nextofkin:address:city"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getAddressPostalCode())) {
            attributeList.add(SamlIssuerHelper.createAttribute(nextOfKinDetail.getAddressPostalCode(), "NextOfKinPostalCode", Attribute.URI_REFERENCE,
                                                               "urn:ehdsi:names:subject:nextofkin:address:postalcode"));
        }
        if (StringUtils.isNotBlank(nextOfKinDetail.getAddressCountry())) {
            attributeList.add(
                    SamlIssuerHelper.createAttribute(nextOfKinDetail.getAddressCountry(), "NextOfKinAddressCountry", Attribute.URI_REFERENCE,
                                                     "urn:ehdsi:names:subject:nextofkin:address:country"));
        }
        return attributeList;
    }
}
