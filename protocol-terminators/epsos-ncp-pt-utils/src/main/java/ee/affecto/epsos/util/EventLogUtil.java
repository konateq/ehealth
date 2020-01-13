package ee.affecto.epsos.util;

import epsos.ccd.gnomon.auditmanager.*;
import eu.epsos.util.xdr.XDRConstants;
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
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.TransportHeaders;
import org.apache.commons.lang3.StringUtils;
import org.hl7.v3.II;
import org.hl7.v3.PRPAIN201305UV02;
import org.hl7.v3.PRPAIN201306UV02;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        eventLog.setEventType(EventType.epsosIdentificationServiceFindIdentityByTraits);
        eventLog.setEI_TransactionName(TransactionName.epsosIdentificationServiceFindIdentityByTraits);
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
                eventLog.setEM_PatricipantObjectID("0");
                eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            }
            eventLog.setEM_PatricipantObjectDetail(detail.getBytes());
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        }
        // Patient Source is not written, because HCP assurance audit does not include patient mapping information
        // Set Participant Object: Patient Target
        String st2 = "";
        if (!response.getControlActProcess().getQueryByParameter().getValue().getParameterList().getLivingSubjectId().isEmpty()) {

            II instanceIdentifier = response.getControlActProcess().getQueryByParameter().getValue().getParameterList().getLivingSubjectId().get(0).getValue().get(0);
            if (instanceIdentifier.getExtension() != null && instanceIdentifier.getRoot() != null) {
                st2 = instanceIdentifier.getExtension() + "^^^&" + instanceIdentifier.getRoot() + "&ISO";
            }
        }
        eventLog.setPT_PatricipantObjectID(st2);

        // Set Participant Object: Error Message
        if (!response.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()) {

            String error = response.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent();
            eventLog.setEM_PatricipantObjectID(error);
            eventLog.setEM_PatricipantObjectDetail(error.getBytes());
        }
    }

    /**
     * @param eventLog
     * @param request
     * @param response
     * @param classCode
     */
    public static void prepareXCACommonLogQuery(EventLog eventLog, AdhocQueryRequest request, AdhocQueryResponse response, String classCode) {

        switch (classCode) {
            case Constants.PS_CLASSCODE:
                eventLog.setEventType(EventType.epsosPatientServiceList);
                eventLog.setEI_TransactionName(TransactionName.epsosPatientServiceList);
                break;
            case Constants.EP_CLASSCODE:
                eventLog.setEventType(EventType.epsosOrderServiceList);
                eventLog.setEI_TransactionName(TransactionName.epsosOrderServiceList);
                break;
            case Constants.MRO_CLASSCODE:
                eventLog.setEventType(EventType.epsosMroList);
                eventLog.setEI_TransactionName(TransactionName.epsosMroServiceList);
                break;
        }

        eventLog.setPT_PatricipantObjectID(getDocumentEntryPatientId(request));
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
                    documentType = StringUtils.remove(documentType, "('");
                    documentType = StringUtils.remove(documentType, "')");
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
                    documentType = StringUtils.remove(documentType, "('");
                    documentType = StringUtils.remove(documentType, "')");
                    eventLog.getEventTargetParticipantObjectIds().add(documentType);
                }
            }
        }

        if (response.getRegistryErrorList() != null) {

            RegistryError re = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_PatricipantObjectID(re.getErrorCode());
            eventLog.setEM_PatricipantObjectDetail(re.getCodeContext() == null ? null : re.getCodeContext().getBytes());
        }
    }

    /**
     * @param eventLog
     * @param request
     * @param response
     * @param classCode
     */
    public static void prepareXCACommonLogRetrieve(EventLog eventLog, RetrieveDocumentSetRequestType request, RetrieveDocumentSetResponseType response, String classCode) {

        switch (classCode) {
            case Constants.PS_CLASSCODE:
                eventLog.setEventType(EventType.epsosPatientServiceRetrieve);
                eventLog.setEI_TransactionName(TransactionName.epsosPatientServiceRetrieve);
                break;
            case Constants.EP_CLASSCODE:
                eventLog.setEventType(EventType.epsosOrderServiceRetrieve);
                eventLog.setEI_TransactionName(TransactionName.epsosOrderServiceRetrieve);
                break;
            case Constants.MRO_CLASSCODE:
                eventLog.setEventType(EventType.epsosMroRetrieve);
                eventLog.setEI_TransactionName(TransactionName.epsosMroServiceRetrieve);
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
            eventLog.setEM_PatricipantObjectID(re.getErrorCode());
            if (re.getCodeContext() != null) {
                eventLog.setEM_PatricipantObjectDetail(re.getCodeContext().getBytes());
            } else if (re.getValue() != null) {
                eventLog.setEM_PatricipantObjectDetail(re.getValue().getBytes());
            }
        }
    }

    /**
     * @param eventLog
     * @param request
     * @param rel
     */
    public static void prepareXDRCommonLog(EventLog eventLog, ProvideAndRegisterDocumentSetRequestType request, RegistryErrorList rel) {

        String id = null;
        String classCode = null;
        String eventCode = null;
        String countryCode = null;
        String patientId = null;
        String documentUniqueId = "N/A";

        List<JAXBElement<? extends IdentifiableType>> registryObjectList = request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable();
        if (registryObjectList != null) {
            for (JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {

                //EHNCP-1631
                if (identifiable.getValue() instanceof ExtrinsicObjectType) {

                    for (ExternalIdentifierType identifierType : ((ExtrinsicObjectType) identifiable.getValue()).getExternalIdentifier()) {

                        if (StringUtils.equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME, identifierType.getIdentificationScheme())) {

                            documentUniqueId = identifierType.getValue();
                        }
                    }

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
        if (StringUtils.equals(classCode, Constants.CONSENT_CLASSCODE) && eventCode != null) {

            if (eventCode.endsWith(Constants.CONSENT_PUT_SUFFIX)) {

                eventLog.setEventType(EventType.epsosConsentServicePut);
                eventLog.setEI_TransactionName(TransactionName.epsosConsentServicePut);
                eventLog.setEI_EventActionCode(EventActionCode.UPDATE);
            } else if (eventCode.endsWith(Constants.CONSENT_DISCARD_SUFFIX)) {

                eventLog.setEventType(EventType.epsosConsentServiceDiscard);
                eventLog.setEI_TransactionName(TransactionName.epsosConsentServiceDiscard);
                eventLog.setEI_EventActionCode(EventActionCode.DELETE);
            }
        } else if (StringUtils.equals(classCode, Constants.ED_CLASSCODE)) {

            eventLog.setEventType(EventType.epsosDispensationServiceInitialize);
            eventLog.setEI_TransactionName(TransactionName.epsosDispensationServiceInitialize);
            eventLog.setEI_EventActionCode(EventActionCode.UPDATE);
        }

        //  TODO: support dispensation revoke operation
        //  TODO: Audit - Event Target
        eventLog.getEventTargetParticipantObjectIds().add(documentUniqueId);

        // Set Event status of the operation
        if (rel == null || rel.getRegistryError() == null || rel.getRegistryError().isEmpty()) {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            RegistryError re = rel.getRegistryError().get(0);
            eventLog.setEM_PatricipantObjectID(re.getErrorCode());
            eventLog.setEM_PatricipantObjectDetail(re.getCodeContext().getBytes());
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

    /**
     * Extracts the XDS patient ID from the XCA query.
     *
     * @param request
     * @return
     */
    private static String getDocumentEntryPatientId(AdhocQueryRequest request) {

        for (SlotType1 sl : request.getAdhocQuery().getSlot()) {
            if (sl.getName().equals("$XDSDocumentEntryPatientId")) {
                String patientId = sl.getValueList().getValue().get(0);
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
        if (IPUtil.isLocalIp(clientIp)) {
            HttpServletRequest servletRequest = (HttpServletRequest) messageContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
            return servletRequest.getServerName();
        } else {
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
