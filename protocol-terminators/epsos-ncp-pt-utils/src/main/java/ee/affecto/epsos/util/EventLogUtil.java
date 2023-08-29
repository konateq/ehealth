package ee.affecto.epsos.util;

import epsos.ccd.gnomon.auditmanager.*;
import eu.epsos.util.xdr.XDRConstants;
import eu.europa.ec.sante.ehdsi.constant.ClassCode;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.AssertionConstants;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.*;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryError;
import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryErrorList;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.transport.http.TransportHeaders;
import org.apache.commons.lang3.StringUtils;
import org.hl7.v3.II;
import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201306UV02;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.http.HTTPUtil;
import tr.com.srdc.epsos.util.http.IPUtil;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

// Common part for client and server logging
// TODO A.R. Should be moved into openncp-util later to avoid duplication
public class EventLogUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogUtil.class);

    private EventLogUtil() {
    }

    /**
     * @param eventLog
     * @param request
     * @param response
     */
    public static void prepareXCPDCommonLog(EventLog eventLog, PRPAIN201305UV02 request, PRPAIN201306UV02 response) {

        // Set Event Identification
        eventLog.setEventType(EventType.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS);
        eventLog.setEI_TransactionName(TransactionName.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS);
        eventLog.setEI_EventActionCode(EventActionCode.EXECUTE);

        if (!response.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()) {

            String detail = response.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent();
            if (detail.startsWith("(")) {
                String code = detail.substring(1, 5);
                if (code.equals("1102")) {
                    eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
                } else {
                    eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
                }
            } else {
                eventLog.setEM_ParticipantObjectID("0");
                eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            }
            eventLog.setEM_ParticipantObjectDetail(detail.getBytes());
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        }
        // Patient Source is not written, because HCP assurance audit does not include patient mapping information
        // Set Participant Object: Patient Target
        String patientId = "";
        if (!response.getControlActProcess().getSubject().isEmpty()) {

            II instanceIdentifier = response.getControlActProcess().getSubject().get(0).getRegistrationEvent()
                    .getSubject1().getPatient().getId().get(0);
            if (instanceIdentifier.getExtension() != null && instanceIdentifier.getRoot() != null) {
                patientId = instanceIdentifier.getExtension() + "^^^&" + instanceIdentifier.getRoot() + "&ISO";
            }
        } else {
            // TODO: To be reviewed - No Patient details return then audit message is reporting Patient search criteria
            II instanceIdentifier = response.getControlActProcess().getQueryByParameter().getValue().getParameterList()
                    .getLivingSubjectId().get(0).getValue().get(0);
            if (instanceIdentifier.getExtension() != null && instanceIdentifier.getRoot() != null) {
                patientId = instanceIdentifier.getExtension() + "^^^&" + instanceIdentifier.getRoot() + "&ISO";
            }
        }
        eventLog.setPT_ParticipantObjectID(patientId);

        // Set Participant Object: Error Message
        if (!response.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()) {

            String error = response.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent();
            eventLog.setEM_ParticipantObjectID(error);
            eventLog.setEM_ParticipantObjectDetail(error.getBytes());
        }
    }

    /**
     * @param eventLog
     * @param request
     * @param response
     * @param classCode
     */
    public static void prepareXCACommonLogQuery(EventLog eventLog, AdhocQueryRequest request, AdhocQueryResponse response, ClassCode classCode) {

        switch (classCode) {
            case PS_CLASSCODE:
                eventLog.setEventType(EventType.PATIENT_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.PATIENT_SERVICE_LIST);
                break;
            case EP_CLASSCODE:
                eventLog.setEventType(EventType.ORDER_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.ORDER_SERVICE_LIST);
                break;
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
                eventLog.setEventType(EventType.ORCD_SERVICE_LIST);
                eventLog.setEI_TransactionName(TransactionName.ORCD_SERVICE_LIST);
                break;
            case MRO_CLASSCODE:
                eventLog.setEventType(EventType.MRO_LIST);
                eventLog.setEI_TransactionName(TransactionName.MRO_SERVICE_LIST);
                break;
        }

        eventLog.setPT_ParticipantObjectID(getDocumentEntryPatientId(request));
        eventLog.setEI_EventActionCode(EventActionCode.READ);

        if (response.getRegistryObjectList() != null) {

            List<String> documentIds = new ArrayList<>();
            List<JAXBElement<? extends IdentifiableType>> registryObjectList = response.getRegistryObjectList().getIdentifiable();
            for (JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {

                if (!(identifiable.getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                ExtrinsicObjectType eot = (ExtrinsicObjectType) identifiable.getValue();
                for (ExternalIdentifierType eit : eot.getExternalIdentifier()) {

                    if (eit.getIdentificationScheme().equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME)) {
                        documentIds.add(eit.getValue());
                    }
                }
            }
            //TODO: Audit - Event Target
            eventLog.setEventTargetParticipantObjectIds(documentIds);
        }

        // Set Audit Operation status
        if (response.getRegistryObjectList() == null) {

            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            for (SlotType1 slotType1 : request.getAdhocQuery().getSlot()) {
                if (org.apache.commons.lang.StringUtils.equals(slotType1.getName(), "$XDSDocumentEntryClassCode")) {
                    String documentType = slotType1.getValueList().getValue().get(0);
                    documentType = org.apache.commons.lang3.StringUtils.remove(documentType, "('");
                    documentType = org.apache.commons.lang3.StringUtils.remove(documentType, "')");
                    eventLog.getEventTargetParticipantObjectIds().add(documentType);
                }
            }
        } else if (response.getRegistryErrorList() == null) {

            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {

            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
            for (SlotType1 slotType1 : request.getAdhocQuery().getSlot()) {
                if (org.apache.commons.lang.StringUtils.equals(slotType1.getName(), "$XDSDocumentEntryClassCode")) {
                    String documentType = slotType1.getValueList().getValue().get(0);
                    documentType = org.apache.commons.lang3.StringUtils.remove(documentType, "('");
                    documentType = org.apache.commons.lang3.StringUtils.remove(documentType, "')");
                    eventLog.getEventTargetParticipantObjectIds().add(documentType);
                }
            }
        }

        if (response.getRegistryErrorList() != null) {

            RegistryError re = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_ParticipantObjectID(re.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(re.getCodeContext() == null ? null : re.getCodeContext().getBytes());
        }
    }

    /**
     * @param eventLog
     * @param request
     * @param response
     * @param classCode
     */
    public static void prepareXCACommonLogRetrieve(EventLog eventLog, RetrieveDocumentSetRequestType request, RetrieveDocumentSetResponseType response, ClassCode classCode) {

        switch (classCode) {
            case PS_CLASSCODE:
                eventLog.setEventType(EventType.PATIENT_SERVICE_RETRIEVE);
                eventLog.setEI_TransactionName(TransactionName.PATIENT_SERVICE_RETRIEVE);
                break;
            case EP_CLASSCODE:
                eventLog.setEventType(EventType.ORDER_SERVICE_RETRIEVE);
                eventLog.setEI_TransactionName(TransactionName.ORDER_SERVICE_RETRIEVE);
                break;
            case ORCD_HOSPITAL_DISCHARGE_REPORTS_CLASSCODE:
            case ORCD_LABORATORY_RESULTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGING_REPORTS_CLASSCODE:
            case ORCD_MEDICAL_IMAGES_CLASSCODE:
                eventLog.setEventType(EventType.ORCD_SERVICE_RETRIEVE);
                eventLog.setEI_TransactionName(TransactionName.ORCD_SERVICE_RETRIEVE);
                break;
            case MRO_CLASSCODE:
                eventLog.setEventType(EventType.MRO_RETRIEVE);
                eventLog.setEI_TransactionName(TransactionName.MRO_SERVICE_RETRIEVE);
                break;
        }

        eventLog.setEI_EventActionCode(EventActionCode.READ);
        //  TODO: Audit - Event Target
        eventLog.getEventTargetParticipantObjectIds().add(request.getDocumentRequest().get(0).getDocumentUniqueId());

        if (response.getDocumentResponse() == null || response.getDocumentResponse().isEmpty() || response.getDocumentResponse().get(0).getDocument() == null) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
        } else if (response.getRegistryResponse().getRegistryErrorList() == null) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
        }

        if (response.getRegistryResponse().getRegistryErrorList() != null && response.getRegistryResponse().getRegistryErrorList().getRegistryError() != null
                && !response.getRegistryResponse().getRegistryErrorList().getRegistryError().isEmpty()) {

            RegistryError re = response.getRegistryResponse().getRegistryErrorList().getRegistryError().get(0);
            // TODO A.R. on TSAM errors currently errorCode=null, codeContext=null - maybe faulty XCA server implementation?
            // What exactly we should log on partial success? Originally was codeContext, but is value OK?
            eventLog.setEM_ParticipantObjectID(re.getErrorCode());
            if (re.getCodeContext() != null) {
                eventLog.setEM_ParticipantObjectDetail(re.getCodeContext().getBytes());
            } else if (re.getValue() != null) {
                eventLog.setEM_ParticipantObjectDetail(re.getValue().getBytes());
            }
        }
    }

    /**
     * @param eventLog
     * @param request
     * @param registryErrorList
     */
    public static void prepareXDRCommonLog(EventLog eventLog, ProvideAndRegisterDocumentSetRequestType request, RegistryErrorList registryErrorList) {

        String id = null;
        String classCode = null;
        String eventCode = null;
        String countryCode = null;
        String patientId = null;
        String documentUniqueId = "N/A";
        String discardId = "N/A";

        List<JAXBElement<? extends IdentifiableType>> registryObjectList = request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable();
        if (registryObjectList != null) {
            for (JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {

                if (identifiable.getValue() instanceof ExtrinsicObjectType) {

                    for (ExternalIdentifierType identifierType : ((ExtrinsicObjectType) identifiable.getValue()).getExternalIdentifier()) {

                        if (StringUtils.equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME, identifierType.getIdentificationScheme())) {

                            documentUniqueId = identifierType.getValue();
                        }
                    }
                } else if (identifiable.getValue() instanceof RegistryPackageType) {
                    RegistryPackageType registryPackageType = (RegistryPackageType) identifiable.getValue();
                    for (ExternalIdentifierType externalIdentifier : registryPackageType.getExternalIdentifier()) {
                        if (StringUtils.equals(externalIdentifier.getIdentificationScheme(), "urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8")) {
                            discardId = externalIdentifier.getValue();
                        }
                    }
                    continue;
                } else if (!(identifiable.getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                ExtrinsicObjectType eot = (ExtrinsicObjectType) identifiable.getValue();
                id = eot.getId();
                for (ClassificationType classif : eot.getClassification()) {
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
                for (ExternalIdentifierType externalIdentifier : eot.getExternalIdentifier()) {
                    if (externalIdentifier.getIdentificationScheme().equals("urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427")) {
                        patientId = externalIdentifier.getValue();
                    }
                }
            }
        }
        LOGGER.info("EventLogUtil: '{}'", classCode);
        if (StringUtils.equals(classCode, ClassCode.CONSENT_CLASSCODE.getCode()) && eventCode != null) {

            if (eventCode.endsWith(Constants.CONSENT_PUT_SUFFIX)) {

                eventLog.setEventType(EventType.CONSENT_SERVICE_PUT);
                eventLog.setEI_TransactionName(TransactionName.CONSENT_SERVICE_PUT);
                eventLog.setEI_EventActionCode(EventActionCode.UPDATE);
            } else if (eventCode.endsWith(Constants.CONSENT_DISCARD_SUFFIX)) {

                eventLog.setEventType(EventType.CONSENT_SERVICE_DISCARD);
                eventLog.setEI_TransactionName(TransactionName.CONSENT_SERVICE_DISCARD);
                eventLog.setEI_EventActionCode(EventActionCode.DELETE);
            }
        } else if (StringUtils.equals(classCode, ClassCode.ED_CLASSCODE.getCode())) {
            eventLog.setEventType(EventType.DISPENSATION_SERVICE_INITIALIZE);
            eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_INITIALIZE);
            eventLog.setEI_EventActionCode(EventActionCode.UPDATE);

        } else if (StringUtils.equals(classCode, ClassCode.EDD_CLASSCODE.getCode())) {
            eventLog.setEventType(EventType.DISPENSATION_SERVICE_DISCARD);
            eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_DISCARD);
            eventLog.setEI_EventActionCode(EventActionCode.UPDATE);
            eventLog.getEventTargetParticipantObjectIds().add(discardId);
        }

        //  TODO: support dispensation revoke operation
        //  TODO: Audit - Event Target
        eventLog.getEventTargetParticipantObjectIds().add(documentUniqueId);

        // Set Event status of the operation
        if (registryErrorList == null || registryErrorList.getRegistryError() == null || registryErrorList.getRegistryError().isEmpty()) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            RegistryError registryError = registryErrorList.getRegistryError().get(0);
            eventLog.setEM_ParticipantObjectID(registryError.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(registryError.getCodeContext().getBytes());
        }
    }

    /**
     * @param envelope
     * @return
     */
    public static String getMessageID(SOAPEnvelope envelope) {

        Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getText();
        } else {
            return "NA";
        }
    }

    public static String getAttributeValue(Attribute attribute) {

        String attributeValue = null;
        if (!attribute.getAttributeValues().isEmpty()) {
            attributeValue = attribute.getAttributeValues().get(0).getDOM().getTextContent();
        }
        return attributeValue;
    }

    public static String getRoleId(Attribute attribute) {
        NodeList nodeList = attribute.getAttributeValues().get(0).getDOM().getChildNodes();
        for (int i=0; i<nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ("Role".equals(node.getNodeName())) {
                NamedNodeMap attributes = node.getAttributes();
                for (int j=0; j<attributes.getLength(); j++) {
                    Node attributeValue = attributes.item(j);
                    if ("displayName".equals(attributeValue.getNodeName())) {
                        return attributeValue.getNodeValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts the XDS patient ID from the XCA query.
     *
     * @param request
     * @return
     */
    private static String getDocumentEntryPatientId(AdhocQueryRequest request) {

        for (SlotType1 slotType1 : request.getAdhocQuery().getSlot()) {
            if (slotType1.getName().equals("$XDSDocumentEntryPatientId")) {
                String patientId = slotType1.getValueList().getValue().get(0);
                patientId = patientId.substring(1, patientId.length() - 1);
                return patientId;
            }
        }
        return "$XDSDocumentEntryPatientId Not Found!";
    }

    /**
     * @param message
     * @return
     */
    private static boolean isUUIDValid(String message) {
        try {
            UUID.fromString(message);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * @param messageContext - JAXWS Axis2 MessageContext used by the request.
     * @return
     */
    public static String getSourceGatewayIdentifier(MessageContext messageContext) {

        TransportHeaders headers = (TransportHeaders) messageContext.getProperty(MessageContext.TRANSPORT_HEADERS);
        String headerClientIp = headers.get("X-Forwarded-For");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("--> X-Forwarded-For Address: '{}'", headerClientIp);
            LOGGER.debug("--> Remote Address: '{}'", messageContext.getProperty(MessageContext.REMOTE_ADDR));
            LOGGER.debug("--> Transport Address: '{}'", messageContext.getProperty(MessageContext.TRANSPORT_ADDR));
        }
        if (StringUtils.isNotBlank(headerClientIp)) {
            if (StringUtils.contains(headerClientIp, ",")) {
                return StringUtils.split(headerClientIp, ",")[0];
            } else {
                return headerClientIp;
            }
        }
        String clientIp = (String) messageContext.getProperty(MessageContext.REMOTE_ADDR);
        if (IPUtil.isLocalLoopbackIp(clientIp)) {
            HttpServletRequest servletRequest = (HttpServletRequest) messageContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
            LOGGER.debug("Client Server Name: '{}'", servletRequest.getServerName());
            return servletRequest.getServerName();
        } else {
            LOGGER.debug("Client IP: '{}'", clientIp);
            return clientIp;
        }
    }

    public static String getTargetGatewayIdentifier() {
        return IPUtil.getPrivateServerIp();
    }

    public static String getClientCommonName(MessageContext messageContext) {

        HttpServletRequest servletRequest = (HttpServletRequest) messageContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        return HTTPUtil.getClientCertificate(servletRequest);
    }
}
