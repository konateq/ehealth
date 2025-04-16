package eu.europa.ec.sante.openncp.core.server;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xcpd.II;
import eu.europa.ec.sante.openncp.core.server.api.ihe.generated.xds.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.jetbrains.annotations.NotNull;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Common part for client and server logging
// TODO A.R. Should be moved into openncp-util later to avoid duplication
public class EventLogUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogUtil.class);

    private EventLogUtil() {
    }

    @NotNull
    private static List<String> getDocumentIds(final AdhocQueryResponse response) {
        final List<String> documentIds = new ArrayList<>();
        final List<JAXBElement<? extends IdentifiableType>> registryObjectList = response.getRegistryObjectList().getIdentifiables();
        for (final JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {

            if (!(identifiable.getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType eot = (ExtrinsicObjectType) identifiable.getValue();
            for (final ExternalIdentifierType eit : eot.getExternalIdentifiers()) {

                if (eit.getIdentificationScheme().equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME)) {
                    documentIds.add(eit.getValue());
                }
            }
        }
        return documentIds;
    }

    public static void setDocumentType(final EventLog eventLog, final AdhocQueryRequest request) {
        for (final Slot slotType1 : request.getAdhocQuery().getSlots()) {
            if (StringUtils.equals(slotType1.getName(), "$XDSDocumentEntryClassCode")) {
                String documentType = slotType1.getValueList().getValues().get(0);
                documentType = StringUtils.remove(documentType, "('");
                documentType = StringUtils.remove(documentType, "')");
                eventLog.getEventTargetParticipantObjectIds().add(documentType);
            }
        }
    }

    /**
     * @param eventLog
     * @param request
     * @param registryErrorList
     */
    public static void prepareXDRCommonLog(final EventLog eventLog, final ProvideAndRegisterDocumentSetRequest request, final RegistryErrorList registryErrorList) {

        String classCode = null;
        String eventCode = null;
        String countryCode = null;
        String patientId = null;
        String documentUniqueId = "N/A";
        String discardId = "N/A";

        final List<JAXBElement<? extends IdentifiableType>> registryObjectList = request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiables();
        if (registryObjectList != null) {
            for (final JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {

                if (identifiable.getValue() instanceof ExtrinsicObjectType) {

                    for (final ExternalIdentifierType identifierType : ((ExtrinsicObjectType) identifiable.getValue()).getExternalIdentifiers()) {

                        if (StringUtils.equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME, identifierType.getIdentificationScheme())) {

                            documentUniqueId = identifierType.getValue();
                        }
                    }
                } else if (identifiable.getValue() instanceof RegistryPackageType) {
                    final RegistryPackageType registryPackageType = (RegistryPackageType) identifiable.getValue();
                    for (final ExternalIdentifierType externalIdentifier : registryPackageType.getExternalIdentifiers()) {
                        if (StringUtils.equals(externalIdentifier.getIdentificationScheme(), "urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8")) {
                            discardId = externalIdentifier.getValue();
                        }
                    }
                    continue;
                } else if (!(identifiable.getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                final ExtrinsicObjectType eot = (ExtrinsicObjectType) identifiable.getValue();
                for (final ClassificationType classif : eot.getClassifications()) {
                    switch (classif.getClassificationScheme()) {
                        case XDRConstants.EXTRINSIC_OBJECT.CLASS_CODE_SCHEME:
                            classCode = classif.getNodeRepresentation();
                            break;
                        case "urn:uuid:2c6b8cb7-8b2a-4051-b291-b1ae6a575ef4":
                            eventCode = classif.getNodeRepresentation();
                            break;
                        case "urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1":
                            countryCode = classif.getNodeRepresentation();
                            break;
                    }
                }
                for (final ExternalIdentifierType externalIdentifier : eot.getExternalIdentifiers()) {
                    if (externalIdentifier.getIdentificationScheme().equals("urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427")) {
                        patientId = externalIdentifier.getValue();
                    }
                }
            }
        }
        LOGGER.info("EventLogUtil: '{}'", classCode);
        if (StringUtils.equals(classCode, ClassCode.ED_CLASSCODE.getCode())) {
            eventLog.setEventType(EventType.XDR_SERVICE_NCP_A);
            eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_INITIALIZE);
            eventLog.setEI_EventActionCode(EventActionCode.READ);

        } else if (StringUtils.equals(classCode, ClassCode.EDD_CLASSCODE.getCode())) {
            eventLog.setEventType(EventType.XDR_SERVICE_NCP_A);
            eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_DISCARD);
            eventLog.setEI_EventActionCode(EventActionCode.READ);
            eventLog.getEventTargetParticipantObjectIds().add(discardId);
        }

        //  TODO: support dispensation revoke operation
        //  TODO: Audit - Event Target
        eventLog.getEventTargetParticipantObjectIds().add(documentUniqueId);

        // Set Event status of the operation
        if (registryErrorList == null || registryErrorList.getRegistryErrors() == null || registryErrorList.getRegistryErrors().isEmpty()) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            final RegistryError registryError = registryErrorList.getRegistryErrors().get(0);
            eventLog.setEM_ParticipantObjectID(registryError.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(registryError.getCodeContext().getBytes());
        }
    }

    /**
     * @param envelope
     * @return
     */
    public static String getMessageID(final SOAPEnvelope envelope) throws SOAPException {

        final Iterator<Node> it = envelope.getHeader().getChildElements(new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getTextContent();
        } else {
            return "NA";
        }
    }

    public static String getMessageID(final SOAPHeader header) {
        final Iterator<Node> it = header.getChildElements(new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getTextContent();
        } else {
            return "NA";
        }
    }

    public static String getAttributeValue(final Attribute attribute) {

        String attributeValue = null;
        if (!attribute.getAttributeValues().isEmpty()) {
            attributeValue = attribute.getAttributeValues().get(0).getDOM().getTextContent();
        }
        return attributeValue;
    }

    /**
     * Extracts the XDS patient IDs from the XCA query.
     *
     * @param request
     * @return
     */
    public static List<String> getDocumentEntryPatientId(final AdhocQueryRequest request) {

        final List<String> patientIds = new ArrayList<>();
        for (final Slot slotType1 : request.getAdhocQuery().getSlots()) {
            if (slotType1.getName().equals("$XDSDocumentEntryPatientId")) {
                String patientId = slotType1.getValueList().getValues().get(0);
                patientId = patientId.substring(1, patientId.length() - 1);
                patientIds.add(patientId);
            }
        }
        return patientIds;
    }

    public static String getSourceGatewayIdentifier(final SoapMessage soapMessage) {
        // Get HTTP headers as a map (key: header name, value: list of header values)
        final Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>) soapMessage.get(Message.PROTOCOL_HEADERS));
        String headerClientIp = null;
        if (headers != null) {
            final List<String> forwardedForList = headers.get("X-Forwarded-For");
            if (forwardedForList != null && !forwardedForList.isEmpty()) {
                LOGGER.debug("--> X-Forwarded-For Address: '{}'", headerClientIp);
                headerClientIp = forwardedForList.get(0);
            }
        }

        if (StringUtils.isNotBlank(headerClientIp)) {
            // If multiple IPs are present, take the first one
            if (headerClientIp.contains(",")) {
                return headerClientIp.split(",")[0].trim();
            } else {
                return headerClientIp.trim();
            }
        }

        final HttpServletRequest httpServletRequest = (HttpServletRequest) soapMessage.get(AbstractHTTPDestination.HTTP_REQUEST);
        if (httpServletRequest != null) {
            final String clientIp = httpServletRequest.getRemoteAddr();
            if (IPUtil.isLocalLoopbackIp(clientIp)) {
                LOGGER.debug("Client Server Name: '{}'", httpServletRequest.getServerName());
                return httpServletRequest.getServerName();
            } else {
                LOGGER.debug("Client IP: '{}'", clientIp);
                return clientIp;
            }
        }

        return StringUtils.EMPTY;
    }

    public static String getTargetGatewayIdentifier() {
        return IPUtil.getPrivateServerIp();
    }

    public static String getClientCommonName(final HttpServletRequest httpServletRequest) {
        return HttpUtil.getClientCertificate(httpServletRequest);
    }


    private static String getParticipantObjectID(final II id) {
        return id.getExtension() + "^^^&" + id.getRoot() + "&ISO";
    }
}
