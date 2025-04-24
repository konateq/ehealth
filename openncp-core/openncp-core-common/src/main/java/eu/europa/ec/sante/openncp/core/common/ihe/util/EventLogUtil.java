package eu.europa.ec.sante.openncp.core.common.ihe.util;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.audit.EventLog;
import eu.europa.ec.sante.openncp.common.audit.EventOutcomeIndicator;
import eu.europa.ec.sante.openncp.common.audit.EventType;
import eu.europa.ec.sante.openncp.common.audit.TransactionName;
import eu.europa.ec.sante.openncp.common.configuration.util.http.IPUtil;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.constants.xdr.XDRConstants;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.*;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetRequestType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryRequest;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.query._3.AdhocQueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rim._3.*;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryError;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.rs._3.RegistryErrorList;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.kernel.http.HTTPConstants;
import org.apache.axis2.transport.http.TransportHeaders;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.opensaml.saml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    public static void prepareXCPDCommonLog(final EventLog eventLog, final MessageContext msgContext, final PRPAIN201305UV02 request, final PRPAIN201306UV02 response) {

        // Set Event Identification
        eventLog.setEventType(EventType.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS);
        eventLog.setEI_TransactionName(TransactionName.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS);

        if (!response.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()) {

            final String detail = response.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent();
            final String errorCode = response.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getCode().getCode();
            if(errorCode.equals(OpenNCPErrorCode.ERROR_PI_NO_MATCH.getCode())) {
                eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
            }else{
                eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            }
            eventLog.setEM_ParticipantObjectID(errorCode);
            eventLog.setEM_ParticipantObjectDetail(detail.getBytes());
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        }
        // Set Participant Object: Patient Source
        final List<String> sourcePatientIds = new ArrayList<>();
        for (final PRPAMT201306UV02LivingSubjectId livingSubjectId : request.getControlActProcess().getQueryByParameter().getValue().getParameterList().getLivingSubjectId()) {
            sourcePatientIds.add(getParticipantObjectID(livingSubjectId.getValue().get(0)));
        }
        eventLog.setPS_ParticipantObjectIDs(sourcePatientIds);

        // Set Participant Object: Patient Target
        final List<String> targetPatientIds = new ArrayList<>();
        if (!response.getControlActProcess().getSubject().isEmpty()) {
            for (final PRPAIN201306UV02MFMIMT700711UV01Subject1 subject1 : response.getControlActProcess().getSubject()) {
                targetPatientIds.add(getParticipantObjectID(subject1.getRegistrationEvent().getSubject1().getPatient().getId().get(0)));
            }
        } else {
            for (final PRPAMT201306UV02LivingSubjectId livingSubjectId : response.getControlActProcess().getQueryByParameter().getValue().getParameterList().getLivingSubjectId()) {
                targetPatientIds.add(getParticipantObjectID(livingSubjectId.getValue().get(0)));
            }
        }
        eventLog.setPT_ParticipantObjectIDs(targetPatientIds);

        // Set Participant Object: Error Message
        if (!response.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()) {

            final String errorCode = response.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getCode().getCode();
            final String error = response.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent();
            eventLog.setEM_ParticipantObjectID(errorCode);
            eventLog.setEM_ParticipantObjectDetail(error.getBytes());
        }

        extractXcpdQueryByParamFromHeader(eventLog, msgContext, "PRPA_IN201305UV02", "controlActProcess", "queryByParameter");
        extractHCIIdentifierFromHeader(eventLog, msgContext);

    }

    /**
     * @param eventLog
     * @param msgContext
     * @param request
     * @param response
     * @param eventType
     * @param transactionName
     */
    public static void prepareXCACommonLogQuery(final EventLog eventLog,
                                                final MessageContext msgContext,
                                                final AdhocQueryRequest request,
                                                final AdhocQueryResponse response,
                                                final EventType eventType,
                                                final TransactionName transactionName) {
        eventLog.setEventType(eventType);
        eventLog.setEI_TransactionName(transactionName);
        eventLog.setPT_ParticipantObjectIDs(getDocumentEntryPatientId(request));

        if (response.getRegistryObjectList() != null) {

            final List<String> documentIds = getDocumentIds(response);
            //TODO: Audit - Event Target
            eventLog.setEventTargetParticipantObjectIds(documentIds);
        }

        // Set Audit Operation status
        if (response.getRegistryObjectList() == null) {

            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            setDocumentType(eventLog, request);
        } else if (response.getRegistryErrorList() == null) {

            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        } else {

            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
            setDocumentType(eventLog, request);
        }

        if (response.getRegistryErrorList() != null) {

            final RegistryError re = response.getRegistryErrorList().getRegistryError().get(0);
            eventLog.setEM_ParticipantObjectID(re.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(re.getCodeContext() == null ? null : re.getCodeContext().getBytes());
        }

        extractQueryByParamFromHeader(eventLog, msgContext, "AdhocQueryRequest", "AdhocQuery", "id");
        extractHCIIdentifierFromHeader(eventLog, msgContext);
    }

    @NotNull
    private static List<String> getDocumentIds(AdhocQueryResponse response) {
        final List<String> documentIds = new ArrayList<>();
        final List<JAXBElement<? extends IdentifiableType>> registryObjectList = response.getRegistryObjectList().getIdentifiable();
        for (final JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {

            if (!(identifiable.getValue() instanceof ExtrinsicObjectType)) {
                continue;
            }
            final ExtrinsicObjectType eot = (ExtrinsicObjectType) identifiable.getValue();
            for (final ExternalIdentifierType eit : eot.getExternalIdentifier()) {

                if (eit.getIdentificationScheme().equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME)) {
                    documentIds.add(eit.getValue());
                }
            }
        }
        return documentIds;
    }

    public static void setDocumentType(EventLog eventLog, AdhocQueryRequest request) {
        for (final SlotType1 slotType1 : request.getAdhocQuery().getSlot()) {
            if (StringUtils.equals(slotType1.getName(), "$XDSDocumentEntryClassCode")) {
                String documentType = slotType1.getValueList().getValue().get(0);
                documentType = StringUtils.remove(documentType, "('");
                documentType = StringUtils.remove(documentType, "')");
                eventLog.getEventTargetParticipantObjectIds().add(documentType);
            }
        }
    }

    /**
     * @param eventLog
     * @param msgContext
     * @param request
     * @param response
     * @param eventType
     * @param transactionName
     */
    public static void prepareXCACommonLogRetrieve(final EventLog eventLog,
                                                   final MessageContext msgContext,
                                                   final RetrieveDocumentSetRequestType request,
                                                   final RetrieveDocumentSetResponseType response,
                                                   final EventType eventType,
                                                   final TransactionName transactionName) {

        eventLog.setEventType(eventType);
        eventLog.setEI_TransactionName(transactionName);

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

            final RegistryError re = response.getRegistryResponse().getRegistryErrorList().getRegistryError().get(0);
            // TODO A.R. on TSAM errors currently errorCode=null, codeContext=null - maybe faulty XCA server implementation?
            // What exactly we should log on partial success? Originally was codeContext, but is value OK?
            eventLog.setEM_ParticipantObjectID(re.getErrorCode());
            if (re.getCodeContext() != null) {
                eventLog.setEM_ParticipantObjectDetail(re.getCodeContext().getBytes());
            } else if (re.getValue() != null) {
                eventLog.setEM_ParticipantObjectDetail(re.getValue().getBytes());
            }
        }

        extractQueryByParamFromHeader(eventLog, msgContext, "RetrieveDocumentSetRequest", "DocumentRequest", "HomeCommunityId");
        extractHCIIdentifierFromHeader(eventLog, msgContext);
    }

    public static void extractXcpdQueryByParamFromHeader(final EventLog eventLog, final MessageContext msgContext, final String elem1, final String elem2, final String elem3) {
        if(msgContext.getEnvelope().getBody().getChildrenWithLocalName(elem1).hasNext()) {
            final OMElement elem_PRPA_IN201305UV02 = msgContext.getEnvelope().getBody().getChildrenWithLocalName(elem1).next();
            if(elem_PRPA_IN201305UV02.getChildrenWithLocalName(elem2).hasNext()){
                final OMElement elem_controlActProcess = elem_PRPA_IN201305UV02.getChildrenWithLocalName(elem2).next();
                if(elem_controlActProcess.getChildrenWithLocalName(elem3).hasNext()) {
                    final OMElement elem_qBP = elem_controlActProcess.getChildrenWithLocalName(elem3).next();
                    eventLog.setQueryByParameter(elem_qBP.toString());
                }
            }
        }
    }

    public static void extractQueryByParamFromHeader(final EventLog eventLog, final MessageContext msgContext, final String elem1, final String elem2, final String elem3) {
        if(msgContext.getEnvelope().getBody().getChildrenWithLocalName("AdhocQueryRequest").hasNext()) {
            final OMElement elem_AdhocQueryRequest = msgContext.getEnvelope().getBody().getChildrenWithLocalName("AdhocQueryRequest").next();
            if(elem_AdhocQueryRequest.getChildrenWithLocalName("AdhocQuery").hasNext()){
                final OMElement elem_AdhocQuery = elem_AdhocQueryRequest.getChildrenWithLocalName("AdhocQuery").next();
                elem_AdhocQuery.getAttributeValue(QName.valueOf("id"));
                eventLog.setQueryByParameter(elem_AdhocQuery.toString());
            }
        }
    }

    public static void extractHCIIdentifierFromHeader(final EventLog eventLog, final MessageContext msgContext) {
        if(msgContext.getEnvelope().getHeader().getChildrenWithLocalName("Security").hasNext()) {
            final OMElement elemSecurity = msgContext.getEnvelope().getHeader().getChildrenWithLocalName("Security").next();
            for (final Iterator<OMElement> itSecurity = elemSecurity.getChildElements(); itSecurity.hasNext(); ) {
                final OMElement elemAssertion = itSecurity.next();
                for (final Iterator<OMElement> it = elemAssertion.getChildElements(); it.hasNext(); ) {
                    final OMElement elem = it.next();
                    if("AttributeStatement".equals(elem.getLocalName())) {
                        for (final Iterator<OMElement> itAttribute = elem.getChildElements(); itAttribute.hasNext(); ) {
                            final OMElement elemAttribute = itAttribute.next();
                            final String attrib = elemAttribute.getAttributeValue(new QName("FriendlyName"));
                            if("HCI Identifier".equals(attrib)) {
                                final Iterator<OMElement> elemAttributeValue = elemAttribute.getChildrenWithLocalName("AttributeValue");
                                final OMElement elemAttributeValueText = elemAttributeValue.next();
                                eventLog.setHciIdentifier(elemAttributeValueText.getText());
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param eventLog
     * @param request
     * @param registryErrorList
     */
    public static void prepareXDRCommonLog(final EventLog eventLog, final ProvideAndRegisterDocumentSetRequestType request, final RegistryErrorList registryErrorList) {

        String classCode = null;
        String eventCode = null;
        String countryCode = null;
        String patientId = null;
        String documentUniqueId = "N/A";
        String discardId = "N/A";

        final List<JAXBElement<? extends IdentifiableType>> registryObjectList = request.getSubmitObjectsRequest().getRegistryObjectList().getIdentifiable();
        if (registryObjectList != null) {
            for (final JAXBElement<? extends IdentifiableType> identifiable : registryObjectList) {
                if (identifiable.getValue() instanceof ExtrinsicObjectType) {
                    for (final ExternalIdentifierType identifierType : ((ExtrinsicObjectType) identifiable.getValue()).getExternalIdentifier()) {
                        if (StringUtils.equals(XDRConstants.EXTRINSIC_OBJECT.XDSDOC_UNIQUEID_SCHEME, identifierType.getIdentificationScheme())) {
                            documentUniqueId = identifierType.getValue();
                        }
                    }
                } else if (identifiable.getValue() instanceof RegistryPackageType) {
                    final RegistryPackageType registryPackageType = (RegistryPackageType) identifiable.getValue();
                    for (final ExternalIdentifierType externalIdentifier : registryPackageType.getExternalIdentifier()) {
                        if (StringUtils.equals(externalIdentifier.getIdentificationScheme(), "urn:uuid:96fdda7c-d067-4183-912e-bf5ee74998a8")) {
                            discardId = externalIdentifier.getValue();
                        }
                    }
                    continue;
                } else if (!(identifiable.getValue() instanceof ExtrinsicObjectType)) {
                    continue;
                }
                final ExtrinsicObjectType eot = (ExtrinsicObjectType) identifiable.getValue();
                for (final ClassificationType classif : eot.getClassification()) {
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
                for (final ExternalIdentifierType externalIdentifier : eot.getExternalIdentifier()) {
                    if (externalIdentifier.getIdentificationScheme().equals("urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427")) {
                        patientId = externalIdentifier.getValue();
                    }
                }
            }
        }
        LOGGER.info("EventLogUtil: '{}'", classCode);
        eventLog.setEventType(eventLog.getNcpSide()==NcpSide.NCP_A ? EventType.XDR_SERVICE_NCP_A : EventType.XDR_SERVICE_NCP_B);
        if (StringUtils.equals(classCode, ClassCode.ED_CLASSCODE.getCode())) {
            eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_INITIALIZE);
        } else if (StringUtils.equals(classCode, ClassCode.EDD_CLASSCODE.getCode())) {
            eventLog.setEI_TransactionName(TransactionName.DISPENSATION_SERVICE_DISCARD);
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
            final RegistryError registryError = registryErrorList.getRegistryError().get(0);
            eventLog.setEM_ParticipantObjectID(registryError.getErrorCode());
            eventLog.setEM_ParticipantObjectDetail(registryError.getCodeContext().getBytes());
        }
    }

    /**
     * @param envelope
     * @return
     */
    public static String getMessageID(final SOAPEnvelope envelope) {

        final Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getText();
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
        for (final SlotType1 slotType1 : request.getAdhocQuery().getSlot()) {
            if (slotType1.getName().equals("$XDSDocumentEntryPatientId")) {
                String patientId = slotType1.getValueList().getValue().get(0);
                patientId = patientId.substring(1, patientId.length() - 1);
                patientIds.add(patientId);
            }
        }
        return patientIds;
    }


    /**
     * @param messageContext - JAXWS Axis2 MessageContext used by the request.
     * @return
     */
    public static String getSourceGatewayIdentifier(final MessageContext messageContext) {

        final TransportHeaders headers = (TransportHeaders) messageContext.getProperty(MessageContext.TRANSPORT_HEADERS);
        final String headerClientIp = headers.get("X-Forwarded-For");
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
        final String clientIp = (String) messageContext.getProperty(MessageContext.REMOTE_ADDR);
        if (IPUtil.isLocalLoopbackIp(clientIp)) {
            final HttpServletRequest servletRequest = (HttpServletRequest) messageContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
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

    public static String getClientCommonName(final MessageContext messageContext) {

        final HttpServletRequest servletRequest = (HttpServletRequest) messageContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        return HttpUtil.getClientCertificate(servletRequest);
    }

    private static String getParticipantObjectID(final II id) {
        return id.getExtension() + "^^^&" + id.getRoot() + "&ISO";
    }
}
