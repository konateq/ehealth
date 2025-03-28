package eu.europa.ec.sante.openncp.core.server;

import eu.europa.ec.sante.openncp.common.audit.AuditServiceFactory;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;

public class EventLogClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogClientUtil.class);
    private static final String ERROR_UNKNOWN_HOST = "UNKNOWN_HOST";

    private EventLogClientUtil() {
    }

//    public static void createDummyMustUnderstandHandler(final Stub stub) {
//
//        final var description = new HandlerDescription("DummyMustUnderstandHandler");
//        description.setHandler(new DummyMustUnderstandHandler());
//        final var axisConfiguration = stub._getServiceClient().getServiceContext().getConfigurationContext()
//                .getAxisConfiguration();
//        final List<Phase> phasesList = axisConfiguration.getInFlowPhases();
//        final var myPhase = new Phase("MyPhase");
//        try {
//            myPhase.addHandler(description);
//        } catch (final PhaseException ex) {
//            throw new RuntimeException(ex);
//        }
//        phasesList.add(0, myPhase);
//        axisConfiguration.setInFaultPhases(phasesList);
//    }

    /**
     * Returns the local private IP of the machine executing the method.
     *
     * @return First IP v4 or v6 value retrieved which is not a loopback or local IP address.
     */
    public static String getSourceGatewayIdentifier() {

        return IPUtil.getPrivateServerIp();
    }

    /**
     * Returns the IP address or a remote server.
     *
     * @param endpointReference - client endpoint reference value extracted from the SOAP ServiceClient.
     * @return IP address of the client retrieved by InetAddress or ERROR_UNKNOWN_HOST.
     */
    public static String getTargetGatewayIdentifier(final String endpointReference) {

        try {
            final var uri = new URI(endpointReference);
            final var inetAddress = InetAddress.getByName(uri.getHost());
            if (!inetAddress.isLinkLocalAddress() && !inetAddress.isLoopbackAddress()
                    && (inetAddress instanceof Inet4Address)) {
                return inetAddress.getHostAddress();
            } else {
                return uri.getHost();
            }
        } catch (final Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            return ERROR_UNKNOWN_HOST;
        }
    }

//    public static EventLog prepareEventLog(final MessageContext msgContext, final SOAPEnvelope soapEnvelope, final String endpointReference, final String dstHomeCommunityId) {
//
//        final var eventLog = new EventLog();
//        eventLog.setEI_EventDateTime(DateUtil.getDateAsXMLGregorian(new Date()));
//
//        // Set Active Participant Identification: Service Consumer NCP
//        eventLog.setSC_UserID(HttpUtil.getSubjectDN(false));
//        eventLog.setSP_UserID(HttpUtil.getServerCertificate(endpointReference));
//
//        // Set Audit Source
//        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);
//
//        // Set Source Ip
//        eventLog.setSourceip(getSourceGatewayIdentifier());
//
//        // Set Target Ip
//        eventLog.setTargetip(getTargetGatewayIdentifier(endpointReference));
//
//        // Set Participant Object: Request Message
//        final String reqMessageId = appendUrnUuid(EventLogUtil.getMessageID(msgContext.getEnvelope()));
//        eventLog.setReqM_ParticipantObjectID(reqMessageId);
//        eventLog.setReqM_ParticipantObjectDetail(msgContext.getEnvelope().getHeader().toString().getBytes());
//
//        // Set Participant Object: ResponseMessage
//        final String rspMessageId = appendUrnUuid(eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogUtil.getMessageID(soapEnvelope));
//        eventLog.setResM_ParticipantObjectID(rspMessageId);
//        eventLog.setResM_ParticipantObjectDetail(soapEnvelope.getHeader().toString().getBytes());
//
//        eventLog.setHciIdentifier(dstHomeCommunityId);
//
//        return eventLog;
//    }

    public static void logIdAssertion(final EventLog eventLog, final Assertion idAssertion) {

        final String spProvidedID = idAssertion.getSubject().getNameID().getSPProvidedID();
        final String humanReqUserId = StringUtils.isNotBlank(spProvidedID) ? spProvidedID : "" + "<" + idAssertion.getSubject().getNameID().getValue()
                + "@" + idAssertion.getIssuer().getValue() + ">";
        eventLog.setHR_UserID(humanReqUserId);
        var isOrganizationProvided = false;

        for (final AttributeStatement attributeStatement : idAssertion.getAttributeStatements()) {
            for (final Attribute attribute : attributeStatement.getAttributes()) {
                if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:oasis:names:tc:xspa:1.0:subject:subject-id")) {
                    eventLog.setHR_AlternativeUserID(EventLogUtil.getAttributeValue(attribute));
                } else if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:oasis:names:tc:xacml:2.0:subject:role")) {
                    eventLog.setHR_RoleID(EventLogUtil.getAttributeValue(attribute));
                } else if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:ehdsi:names:subject:healthcare-facility-type")) {
                    eventLog.setPC_RoleID(EventLogUtil.getAttributeValue(attribute));
                } else if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:oasis:names:tc:xspa:1.0:subject:organization")) {
                    eventLog.setPC_UserID(EventLogUtil.getAttributeValue(attribute));
                    isOrganizationProvided = true;
                } else if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:oasis:names:tc:xspa:1.0:environment:locality") && !isOrganizationProvided) {
                    eventLog.setPC_UserID(EventLogUtil.getAttributeValue(attribute));
                }
            }
        }
    }

    public static void logTrcAssertion(final EventLog eventLog, final Assertion idAssertion) {

        for (final AttributeStatement attributeStatement : idAssertion.getAttributeStatements()) {
            for (final Attribute attribute : attributeStatement.getAttributes()) {
                if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:oasis:names:tc:xspa:1.0:subject:subject-id")) {
                    eventLog.setPT_ParticipantObjectIDs(List.of(EventLogUtil.getAttributeValue(attribute)));
                    break;
                }
            }
        }
    }

    public static void sendEventLog(final EventLog eventLog) {
        AuditServiceFactory.getInstance().write(eventLog, "", "1");
    }

    public static String appendUrnUuid(final String uuid) {

        if (uuid == null || uuid.isEmpty() || uuid.startsWith(Constants.UUID_PREFIX)) {
            return uuid;
        } else return Constants.UUID_PREFIX + uuid;
    }
}
