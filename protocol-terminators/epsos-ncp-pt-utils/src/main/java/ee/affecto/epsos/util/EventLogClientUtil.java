package ee.affecto.epsos.util;

import ee.affecto.epsos.ws.handler.DummyMustUnderstandHandler;
import epsos.ccd.gnomon.auditmanager.EventLog;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditServiceFactory;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.phaseresolver.PhaseException;
import org.apache.commons.lang.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.DateUtil;
import tr.com.srdc.epsos.util.http.HTTPUtil;
import tr.com.srdc.epsos.util.http.IPUtil;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.Date;
import java.util.List;

public class EventLogClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogClientUtil.class);
    private static final String ERROR_UNKNOWN_HOST = "UNKNOWN_HOST";

    private EventLogClientUtil() {
    }

    public static void createDummyMustUnderstandHandler(Stub stub) {

        var description = new HandlerDescription("DummyMustUnderstandHandler");
        description.setHandler(new DummyMustUnderstandHandler());
        var axisConfiguration = stub._getServiceClient().getServiceContext().getConfigurationContext()
                .getAxisConfiguration();
        List<Phase> phasesList = axisConfiguration.getInFlowPhases();
        var myPhase = new Phase("MyPhase");
        try {
            myPhase.addHandler(description);
        } catch (PhaseException ex) {
            throw new RuntimeException(ex);
        }
        phasesList.add(0, myPhase);
        axisConfiguration.setInFaultPhases(phasesList);
    }

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
    public static String getTargetGatewayIdentifier(String endpointReference) {

        try {
            var uri = new URI(endpointReference);
            var inetAddress = InetAddress.getByName(uri.getHost());
            if (!inetAddress.isLinkLocalAddress() && !inetAddress.isLoopbackAddress()
                    && (inetAddress instanceof Inet4Address)) {
                return inetAddress.getHostAddress();
            } else {
                return uri.getHost();
            }
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            return ERROR_UNKNOWN_HOST;
        }
    }

    public static EventLog prepareEventLog(MessageContext msgContext, SOAPEnvelope soapEnvelope, String endpointReference) {

        var eventLog = new EventLog();
        eventLog.setEI_EventDateTime(DateUtil.getDateAsXMLGregorian(new Date()));

        // Set Active Participant Identification: Service Consumer NCP
        eventLog.setSC_UserID(HTTPUtil.getSubjectDN(false));
        eventLog.setSP_UserID(HTTPUtil.getServerCertificate(endpointReference));

        // Set Audit Source
        eventLog.setAS_AuditSourceId(ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_PRINCIPAL_SUBDIVISION"));

        // Set Source Ip
        eventLog.setSourceip(getSourceGatewayIdentifier());

        // Set Target Ip
        eventLog.setTargetip(getTargetGatewayIdentifier(endpointReference));

        // Set Participant Object: Request Message
        String reqMessageId = appendUrnUuid(EventLogUtil.getMessageID(msgContext.getEnvelope()));
        eventLog.setReqM_ParticipantObjectID(reqMessageId);
        eventLog.setReqM_ParticipantObjectDetail(msgContext.getEnvelope().getHeader().toString().getBytes());

        // Set Participant Object: ResponseMessage
        String rspMessageId = appendUrnUuid(EventLogUtil.getMessageID(soapEnvelope));
        eventLog.setResM_ParticipantObjectID(rspMessageId);
        eventLog.setResM_ParticipantObjectDetail(soapEnvelope.getHeader().toString().getBytes());

        return eventLog;
    }

    public static void logIdAssertion(EventLog eventLog, Assertion idAssertion) {

        String spProvidedID = idAssertion.getSubject().getNameID().getSPProvidedID();
        String humanReqUserId = StringUtils.isNotBlank(spProvidedID) ? spProvidedID : "" + "<" + idAssertion.getSubject().getNameID().getValue()
                + "@" + idAssertion.getIssuer().getValue() + ">";
        eventLog.setHR_UserID(humanReqUserId);
        var isOrganizationProvided = false;

        for (AttributeStatement attributeStatement : idAssertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
                if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:oasis:names:tc:xspa:1.0:subject:subject-id")) {
                    eventLog.setHR_AlternativeUserID(EventLogUtil.getAttributeValue(attribute));
                } else if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:oasis:names:tc:xacml:2.0:subject:role")) {
                    eventLog.setHR_RoleID(EventLogUtil.getRoleId(attribute));
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

    public static void logTrcAssertion(EventLog eventLog, Assertion idAssertion) {

        for (AttributeStatement attributeStatement : idAssertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
                if (StringUtils.equalsIgnoreCase(attribute.getName(), "urn:oasis:names:tc:xacml:1.0:resource:resource-id")) {
                    eventLog.setPT_ParticipantObjectID(EventLogUtil.getAttributeValue(attribute));
                    break;
                }
            }
        }
    }

    public static void sendEventLog(EventLog eventLog) {

        AuditServiceFactory.getInstance().write(eventLog, "", "1");
    }

    public static String appendUrnUuid(String uuid) {

        if (uuid == null || uuid.isEmpty() || uuid.startsWith(Constants.UUID_PREFIX)) {
            return uuid;
        } else return Constants.UUID_PREFIX + uuid;
    }
}
