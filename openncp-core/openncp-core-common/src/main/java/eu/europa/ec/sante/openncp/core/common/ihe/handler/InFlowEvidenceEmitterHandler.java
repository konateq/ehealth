package eu.europa.ec.sante.openncp.core.common.ihe.handler;

import eu.europa.ec.sante.openncp.common.audit.EventOutcomeIndicator;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.core.common.ihe.evidence.EvidenceUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.soap.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;

/**
 * InFlowEvidenceEmitterHandler - Generates all NRRs
 * Currently supporting the generation of evidences in the following cases:
 * NCP-B receives from Portal
 * NCP-A receives from NCP-B
 * NCP-B receives from NCP-A (left commented as the Evidence Emitter CP does not mandate generation of evidences on the response)
 *
 * @author jgoncalves
 */
public class InFlowEvidenceEmitterHandler extends AbstractPhaseInterceptor<Message>  {

    private final Logger logger = LoggerFactory.getLogger(InFlowEvidenceEmitterHandler.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");


    public InFlowEvidenceEmitterHandler() {
        super(Phase.RECEIVE); // Or Phase.PRE_INVOKE, or another appropriate phase
    }
    /*@Override
    public InvocationResponse invoke(final MessageContext msgContext) {

        logger.info("[NRR] InFlow Evidence Emitter handler is executing");
        final EvidenceEmitterHandlerUtils evidenceEmitterHandlerUtils = new EvidenceEmitterHandlerUtils();
        final SOAPHeader soapHeader = msgContext.getEnvelope().getHeader();
        final SOAPBody soapBody = msgContext.getEnvelope().getBody();
        debugInflowEvidenceEmitter(msgContext);

        try {
            *//* Canonicalization of the full SOAP message *//*
            final Document canonicalDocument = evidenceEmitterHandlerUtils.canonicalizeAxiomSoapEnvelope(msgContext.getEnvelope());
            final String eventType;
            final String title;
            final String msgUUID;
            final AxisService axisService = msgContext.getServiceContext().getAxisService();
            final boolean isClientSide = axisService.isClientSide();
            logger.debug("[NRR] AxisService name: '{}' - isClientSide: '{}'", axisService.getName(), isClientSide);
            if (isClientSide) {

                logger.info("[NRR] Evidence Emitter - Response");
                //  This will stay commented as the Evidence Emitter CP doesn't mandate the generation of evidences in the response.
                // NCP-B receives from NCP-A, e.g.: NRR title = "NCPB_XCPD_RES" eventType = ihe event

//                eventType = this.evidenceEmitterHandlerUtils.getEventTypeFromMessage(soapBody);
//                title = "NCPB_" + this.evidenceEmitterHandlerUtils.getTransactionNameFromMessage(soapBody);
//                //msgUUID = null; It stays as null because it's fetched from soap msg
//                logger.debug("eventType: '{}'", eventType);
//                logger.debug("title: '{}'", title);
//
//                EvidenceUtils.createEvidenceREMNRR(envCanonicalized,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                            tr.com.srdc.epsos.util.Constants.SP_KEYSTORE_PATH,
//                            tr.com.srdc.epsos.util.Constants.SP_KEYSTORE_PASSWORD,
//                            tr.com.srdc.epsos.util.Constants.SP_PRIVATEKEY_ALIAS,
//                            tr.com.srdc.epsos.util.Constants.SC_KEYSTORE_PATH,
//                            tr.com.srdc.epsos.util.Constants.SC_KEYSTORE_PASSWORD,
//                            tr.com.srdc.epsos.util.Constants.SC_PRIVATEKEY_ALIAS,
//                            eventType,
//                            new DateTime(),
//                            EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                            title);
            } else {
                *//* NCP-B receives from Portal, e.g.:
                    NRR
                    title = "PORTAL_PD_REQ_RECEIVED"
                    eventType = "PORTAL_PD_REQ"
                    msguuid = IdA ID + datetime
                NCP-A receives from NCP-B, e.g.:
                    NRR
                    title = "NCPA_XCPD_REQ"
                    eventType = ihe event
                *//*
                eventType = evidenceEmitterHandlerUtils.getEventTypeFromMessage(soapBody);
                title = evidenceEmitterHandlerUtils.getServerSideTitle(soapBody);
                msgUUID = evidenceEmitterHandlerUtils.getMsgUUID(soapHeader, soapBody);
                logger.debug("eventType: '{}' - title: '{}'", eventType, title);

                if (msgUUID != null) {
                    logger.info("[NRR] Evidence Emitter - Portal NCP-B");
                    // this is a Portal-NCPB interaction: msgUUID comes from IdA or TRCA or is random
                    EvidenceUtils.createEvidenceREMNRR(canonicalDocument, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                            Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                            Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD,
                            Constants.SC_PRIVATEKEY_ALIAS, eventType, new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                            title, msgUUID);
                } else {
                    logger.info("[NRR] Evidence Emitter - NCP A/B");
                    // this isn't a Portal-NCPB interaction (it's NCPB-NCPA), so msgUUID is retrieved from the SOAP header
                    EvidenceUtils.createEvidenceREMNRR(canonicalDocument, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                            Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD, Constants.SC_PRIVATEKEY_ALIAS,
                            Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD, Constants.SP_PRIVATEKEY_ALIAS, eventType,
                            new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), title);
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return InvocationResponse.CONTINUE;
    }*/

    private void debugInflowEvidenceEmitter(final Message message) throws SOAPException{} /*{

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
           *//* SOAPMessage soapMessage = message.get(SOAPMessage.class);
            final SOAPHeader soapHeader = soapMessage.getSOAPHeader();
            if (soapHeader != null) {
                final Iterator<?> blocks = soapHeader.examineAllHeaderBlocks();
                loggerClinical.debug("Iterating over SOAP headers");
                while (blocks.hasNext()) {
                    loggerClinical.debug("Processing header");
                    final SOAPHeaderBlock block = (SOAPHeaderBlock) blocks.next();
                    if (loggerClinical.isDebugEnabled()) {
                        loggerClinical.debug(block.toString());
                    }
                    block.setProcessed();
                }
            }*//*
            try {
                SOAPMessage soapMessage = message.get(SOAPMessage.class);
                if (soapMessage != null) {
                    SOAPHeader soapHeader = soapMessage.getSOAPHeader();

                    if (soapHeader != null) {
                        loggerClinical.debug("Iterating over SOAP headers");

                        // Get all header blocks (for SOAP 1.1)
                        NodeList headerBlocks = soapHeader.getChildNodes(); // Or soapHeader.getElementsByTagNameNS("*", "*") for all namespaces

                        for (int i = 0; i < headerBlocks.getLength(); i++) {
                            Node node = (Node) headerBlocks.item(i);
                            if (node instanceof Element) { // Check if it's an Element (header block)
                                Element block = (Element) node;

                                loggerClinical.debug("Processing header");
                                if (loggerClinical.isDebugEnabled()) {

                                StringWriter writer = new StringWriter();
                                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                                transformer.transform(new DOMSource(block), new StreamResult(writer));
                                String headerString = writer.getBuffer().toString();
                                loggerClinical.debug(headerString);
                                    loggerClinical.debug(block.toString()); // Direct toString()
                                }


                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error processing SOAP headers: " + e.getMessage(), e); // Handle it
            }


            loggerClinical.debug("MessageContext properties: '{}'", message.getContextualPropertyKeys());
            loggerClinical.debug("MessageContext messageID: '{}'", message.getId());

            final SessionContext sessionCtx = msgContext.getSessionContext();
            if (sessionCtx != null) {
                loggerClinical.debug("SessionContext CookieID: '{}'", sessionCtx.getCookieID());
            } else {
                loggerClinical.debug("SessionContext is null!");
            }

            final OperationContext operationCtx = msgContext.getOperationContext();
            if (operationCtx != null) {
                loggerClinical.debug("OperationContext operationName: '{}'", operationCtx.getOperationName());
                loggerClinical.debug("OperationContext serviceGroupName: '{}'", operationCtx.getServiceGroupName());
                loggerClinical.debug("OperationContext serviceName: '{}'", operationCtx.getServiceName());
                loggerClinical.debug("OperationContext isComplete: '{}'", operationCtx.isComplete());
            } else {
                loggerClinical.debug("OperationContext is null!");
            }

            final ServiceGroupContext serviceGroupCtx = msgContext.getServiceGroupContext();
            if (serviceGroupCtx != null) {
                loggerClinical.debug("ServiceGroupContext ID: '{}'", serviceGroupCtx.getId());
                final AxisServiceGroup axisServiceGroup = serviceGroupCtx.getDescription();
                final Iterator<AxisService> itAxisService = axisServiceGroup.getServices();
                while (itAxisService.hasNext()) {
                    final AxisService axisService = itAxisService.next();
                    loggerClinical.debug("AxisService BindingName: '{}'", axisService.getBindingName());
                    loggerClinical.debug("AxisService CustomSchemaNamePrefix: '{}'", axisService.getCustomSchemaNamePrefix());
                    loggerClinical.debug("AxisService CustomSchemaNameSuffix: '{}'", axisService.getCustomSchemaNameSuffix());
                    loggerClinical.debug("AxisService endpointName: '{}'", axisService.getEndpointName());
                    final Map<String, AxisEndpoint> axisEndpoints = axisService.getEndpoints();
                    for (final String key : axisEndpoints.keySet()) {
                        final AxisEndpoint axisEndpoint = axisEndpoints.get(key);
                        loggerClinical.debug("AxisEndpoint calculatedEndpointURL: '{}'", axisEndpoint.calculateEndpointURL());
                        loggerClinical.debug("AxisEndpoint alias: '{}'", axisEndpoint.getAlias());
                        loggerClinical.debug("AxisEndpoint endpointURL: '{}'", axisEndpoint.getEndpointURL());
                        loggerClinical.debug("AxisEndpoint active: '{}'", axisEndpoint.isActive());
                    }
                    loggerClinical.debug("AxisService EPRs: '{}'", Arrays.toString(axisService.getEPRs()));
                    loggerClinical.debug("AxisService name: '{}'", axisService.getName());
                    loggerClinical.debug("AxisService isClientSide: '{}'", axisService.isClientSide());
                }
            } else {
                loggerClinical.debug("ServiceGroupContext is null!");
            }

            final ConfigurationContext configCtx = msgContext.getRootContext();
            if (configCtx != null) {
                loggerClinical.debug("ConfigurationContext contextRoot: '{}'", configCtx.getContextRoot());
                loggerClinical.debug("ConfigurationContext serviceGroupContextIDs: '{}'", Arrays.toString(configCtx.getServiceGroupContextIDs()));
                loggerClinical.debug("ConfigurationContext servicePath: '{}'", configCtx.getServicePath());
            } else {
                loggerClinical.debug("ConfigurationContext is null!");
            }
        }
    }*/

    @Override
    public void handleMessage(Message message) throws Fault{} /*{
        {

            logger.info("[NRR] InFlow Evidence Emitter handler is executing");
            final EvidenceEmitterHandlerUtils evidenceEmitterHandlerUtils = new EvidenceEmitterHandlerUtils();

            SOAPMessage soapMessage = message.get(SOAPMessage.class);
            if (soapMessage == null) {
                throw new RuntimeException("SOAPMessage is null");
            }
            SOAPHeader header = null;
            try {
                header = soapMessage.getSOAPHeader();
                SOAPEnvelope sourceEnvelope = soapMessage.getSOAPPart().getEnvelope();
                final SOAPBody soapBody = sourceEnvelope.getBody();
                debugInflowEvidenceEmitter(message);
            } catch (SOAPException e) {
                throw new RuntimeException(e);
            }
            if (header == null) {
                throw new RuntimeException("SOAPHeader is null");
            }

            try {
                *//* Canonicalization of the full SOAP message *//*
                final Document canonicalDocument = evidenceEmitterHandlerUtils.canonicalizeAxiomSoapEnvelope(msgContext.getEnvelope());
                final String eventType;
                final String title;
                final String msgUUID;
                final AxisService axisService = msgContext.getServiceContext().getAxisService();
                final boolean isClientSide = axisService.isClientSide();
                logger.debug("[NRR] AxisService name: '{}' - isClientSide: '{}'", axisService.getName(), isClientSide);
                if (isClientSide) {

                    logger.info("[NRR] Evidence Emitter - Response");
                } else {
                *//* NCP-B receives from Portal, e.g.:
                    NRR
                    title = "PORTAL_PD_REQ_RECEIVED"
                    eventType = "PORTAL_PD_REQ"
                    msguuid = IdA ID + datetime
                NCP-A receives from NCP-B, e.g.:
                    NRR
                    title = "NCPA_XCPD_REQ"
                    eventType = ihe event
                *//*
                    eventType = evidenceEmitterHandlerUtils.getEventTypeFromMessage(soapBody);
                    title = evidenceEmitterHandlerUtils.getServerSideTitle(soapBody);
                    msgUUID = evidenceEmitterHandlerUtils.getMsgUUID(soapHeader, soapBody);
                    logger.debug("eventType: '{}' - title: '{}'", eventType, title);

                    if (msgUUID != null) {
                        logger.info("[NRR] Evidence Emitter - Portal NCP-B");
                        // this is a Portal-NCPB interaction: msgUUID comes from IdA or TRCA or is random
                        EvidenceUtils.createEvidenceREMNRR(canonicalDocument, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                                Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                                Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD,
                                Constants.SC_PRIVATEKEY_ALIAS, eventType, new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                                title, msgUUID);
                    } else {
                        logger.info("[NRR] Evidence Emitter - NCP A/B");
                        // this isn't a Portal-NCPB interaction (it's NCPB-NCPA), so msgUUID is retrieved from the SOAP header
                        EvidenceUtils.createEvidenceREMNRR(canonicalDocument, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                                Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD, Constants.SC_PRIVATEKEY_ALIAS,
                                Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD, Constants.SP_PRIVATEKEY_ALIAS, eventType,
                                new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), title);
                    }
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

            return InvocationResponse.CONTINUE;
        }

    }*/
}
