package eu.europa.ec.sante.openncp.core.common.ihe.handler;


import eu.europa.ec.sante.openncp.common.audit.EventOutcomeIndicator;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.core.common.ihe.evidence.EvidenceUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;


/**
 * EvidenceEmitterHandler
 * Generates all NROs for the Portal
 * Currently supporting the generation of evidences in the following cases: Portal sends request to NCP-B
 *
 * @author jgoncalves
 */
public class OutFlowEvidenceEmitterHandler extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutFlowEvidenceEmitterHandler.class);

    public OutFlowEvidenceEmitterHandler() {
        super(Phase.RECEIVE); // Or Phase.PRE_INVOKE, or another appropriate phase
    }

/*    @Override
    public InvocationResponse invoke(final MessageContext msgcontext) {

        LOGGER.info("OutFlow Evidence Emitter handler is executing");
        final EvidenceEmitterHandlerUtils evidenceEmitterHandlerUtils = new EvidenceEmitterHandlerUtils();

        *//* I'll leave this here as it might be useful in the future *//*

//        SOAPHeader soapHeader = msgcontext.getEnvelope().getHeader();
//        if (soapHeader != null) {
//            Iterator<?> blocks = soapHeader.examineAllHeaderBlocks();
//            LOGGER.debug("Iterating over soap headers");
//            while (blocks.hasNext()) {
//                LOGGER.debug("Processing header");
//                SOAPHeaderBlock block = (SOAPHeaderBlock)blocks.next();
//                LOGGER.debug(block.toString());
//                block.setProcessed();
//            }
//        }

//        LOGGER.debug("LOGGING TEST VALUES");
//        LOGGER.debug("MessageContext properties: " + msgcontext.getProperties());
//        LOGGER.debug("MessageContext messageID: " + msgcontext.getMessageID());
//        
//        SessionContext sessionCtx = msgcontext.getSessionContext();
//        if (sessionCtx != null) {
//            LOGGER.debug("SessionContext CookieID: " + sessionCtx.getCookieID());
//        } else {
//            LOGGER.debug("SessionContext is null!");
//        }

//        OperationContext operationCtx = msgcontext.getOperationContext();
//        if (operationCtx != null) {
//            LOGGER.debug("OperationContext operationName: " + operationCtx.getOperationName());
//            LOGGER.debug("OperationContext serviceGroupName: " + operationCtx.getServiceGroupName());
//            LOGGER.debug("OperationContext serviceName; " + operationCtx.getServiceName());
//            LOGGER.debug("OperationContext isComplete: " + operationCtx.isComplete());
//        } else {
//            LOGGER.debug("OperationContext is null!");
//        }

//        ServiceGroupContext serviceGroupCtx = msgcontext.getServiceGroupContext();
//        if (serviceGroupCtx != null) {
//            LOGGER.debug("ServiceGroupContext ID: " + serviceGroupCtx.getId());
//            AxisServiceGroup axisServiceGroup = serviceGroupCtx.getDescription();
//            Iterator<AxisService> itAxisService = axisServiceGroup.getServices();
//            while (itAxisService.hasNext()) {
//                AxisService axisService = itAxisService.next();
//                LOGGER.debug("AxisService BindingName: " + axisService.getBindingName());
//                LOGGER.debug("AxisService CustomSchemaNamePrefix: " + axisService.getCustomSchemaNamePrefix());
//                LOGGER.debug("AxisService CustomSchemaNameSuffix: " + axisService.getCustomSchemaNameSuffix());
//                LOGGER.debug("AxisService endpointName: " + axisService.getEndpointName());
//                Map<String,AxisEndpoint> axisEndpoints = axisService.getEndpoints();
//                for (String key : axisEndpoints.keySet()) {
//                    AxisEndpoint axisEndpoint = axisEndpoints.get(key);
//                    LOGGER.debug("AxisEndpoint calculatedEndpointURL: " + axisEndpoint.calculateEndpointURL());
//                    LOGGER.debug("AxisEndpoint alias: " + axisEndpoint.getAlias());
//                    LOGGER.debug("AxisEndpoint endpointURL: " + axisEndpoint.getEndpointURL());
//                    LOGGER.debug("AxisEndpoint active: " + axisEndpoint.isActive());
//                }
//                LOGGER.debug("AxisService EPRs: " + Arrays.toString((String[]) axisService.getEPRs()));
//                LOGGER.debug("AxisService name: " + axisService.getName());
//                LOGGER.debug("AxisService isClientSide: " + axisService.isClientSide());
//            } 
//        } else {
//            LOGGER.debug("ServiceGroupContext is null!");
//        }

//        ConfigurationContext configCtx = msgcontext.getRootContext();
//        if (configCtx != null) {
//            LOGGER.debug("ConfigurationContext contextRoot: " + configCtx.getContextRoot());
//            LOGGER.debug("ConfigurationContext serviceGroupContextIDs: " + Arrays.toString((String[])configCtx.getServiceGroupContextIDs()));
//            LOGGER.debug("ConfigurationContext servicePath: " + configCtx.getServicePath());
//        } else {
//            LOGGER.debug("ConfigurationContext is null!");
//        }

        try {
            *//* Canonicalize the full SOAP message *//*
            final Document envCanonicalized = evidenceEmitterHandlerUtils.canonicalizeAxiomSoapEnvelope(msgcontext.getEnvelope());

            final SOAPHeader soapHeader = msgcontext.getEnvelope().getHeader();
            final SOAPBody soapBody = msgcontext.getEnvelope().getBody();
            final String eventType = evidenceEmitterHandlerUtils.getEventTypeFromMessage(soapBody);
            final String title = evidenceEmitterHandlerUtils.getTransactionNameFromMessage(soapBody);
            final String msgUUID = evidenceEmitterHandlerUtils.getMsgUUID(soapHeader, soapBody);
            LOGGER.debug("eventType: '{}'", eventType);
            LOGGER.debug("title: '{}'", title);
            LOGGER.debug("msgUUID: '{}", msgUUID);

            *//* Portal sends request to NCP-B*//*
            EvidenceUtils.createEvidenceREMNRO(envCanonicalized, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                    Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                    Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD,
                    Constants.SC_PRIVATEKEY_ALIAS, eventType, new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                    title, msgUUID);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return InvocationResponse.CONTINUE;
    }*/

    @Override
    public void handleMessage(Message message) throws Fault {
        {

            LOGGER.info("OutFlow Evidence Emitter handler is executing");
            final EvidenceEmitterHandlerUtils evidenceEmitterHandlerUtils = new EvidenceEmitterHandlerUtils();

            /* I'll leave this here as it might be useful in the future */

//        SOAPHeader soapHeader = msgcontext.getEnvelope().getHeader();
//        if (soapHeader != null) {
//            Iterator<?> blocks = soapHeader.examineAllHeaderBlocks();
//            LOGGER.debug("Iterating over soap headers");
//            while (blocks.hasNext()) {
//                LOGGER.debug("Processing header");
//                SOAPHeaderBlock block = (SOAPHeaderBlock)blocks.next();
//                LOGGER.debug(block.toString());
//                block.setProcessed();
//            }
//        }

//        LOGGER.debug("LOGGING TEST VALUES");
//        LOGGER.debug("MessageContext properties: " + msgcontext.getProperties());
//        LOGGER.debug("MessageContext messageID: " + msgcontext.getMessageID());
//
//        SessionContext sessionCtx = msgcontext.getSessionContext();
//        if (sessionCtx != null) {
//            LOGGER.debug("SessionContext CookieID: " + sessionCtx.getCookieID());
//        } else {
//            LOGGER.debug("SessionContext is null!");
//        }

//        OperationContext operationCtx = msgcontext.getOperationContext();
//        if (operationCtx != null) {
//            LOGGER.debug("OperationContext operationName: " + operationCtx.getOperationName());
//            LOGGER.debug("OperationContext serviceGroupName: " + operationCtx.getServiceGroupName());
//            LOGGER.debug("OperationContext serviceName; " + operationCtx.getServiceName());
//            LOGGER.debug("OperationContext isComplete: " + operationCtx.isComplete());
//        } else {
//            LOGGER.debug("OperationContext is null!");
//        }

//        ServiceGroupContext serviceGroupCtx = msgcontext.getServiceGroupContext();
//        if (serviceGroupCtx != null) {
//            LOGGER.debug("ServiceGroupContext ID: " + serviceGroupCtx.getId());
//            AxisServiceGroup axisServiceGroup = serviceGroupCtx.getDescription();
//            Iterator<AxisService> itAxisService = axisServiceGroup.getServices();
//            while (itAxisService.hasNext()) {
//                AxisService axisService = itAxisService.next();
//                LOGGER.debug("AxisService BindingName: " + axisService.getBindingName());
//                LOGGER.debug("AxisService CustomSchemaNamePrefix: " + axisService.getCustomSchemaNamePrefix());
//                LOGGER.debug("AxisService CustomSchemaNameSuffix: " + axisService.getCustomSchemaNameSuffix());
//                LOGGER.debug("AxisService endpointName: " + axisService.getEndpointName());
//                Map<String,AxisEndpoint> axisEndpoints = axisService.getEndpoints();
//                for (String key : axisEndpoints.keySet()) {
//                    AxisEndpoint axisEndpoint = axisEndpoints.get(key);
//                    LOGGER.debug("AxisEndpoint calculatedEndpointURL: " + axisEndpoint.calculateEndpointURL());
//                    LOGGER.debug("AxisEndpoint alias: " + axisEndpoint.getAlias());
//                    LOGGER.debug("AxisEndpoint endpointURL: " + axisEndpoint.getEndpointURL());
//                    LOGGER.debug("AxisEndpoint active: " + axisEndpoint.isActive());
//                }
//                LOGGER.debug("AxisService EPRs: " + Arrays.toString((String[]) axisService.getEPRs()));
//                LOGGER.debug("AxisService name: " + axisService.getName());
//                LOGGER.debug("AxisService isClientSide: " + axisService.isClientSide());
//            }
//        } else {
//            LOGGER.debug("ServiceGroupContext is null!");
//        }

//        ConfigurationContext configCtx = msgcontext.getRootContext();
//        if (configCtx != null) {
//            LOGGER.debug("ConfigurationContext contextRoot: " + configCtx.getContextRoot());
//            LOGGER.debug("ConfigurationContext serviceGroupContextIDs: " + Arrays.toString((String[])configCtx.getServiceGroupContextIDs()));
//            LOGGER.debug("ConfigurationContext servicePath: " + configCtx.getServicePath());
//        } else {
//            LOGGER.debug("ConfigurationContext is null!");
//        }

            try {
                /* Canonicalize the full SOAP message */

                if (message != null) {
                    SOAPMessage soapMessage = message.get(SOAPMessage.class);
                    if (soapMessage != null) {
                        SOAPEnvelope sourceEnvelope = soapMessage.getSOAPPart().getEnvelope();
                        if (sourceEnvelope != null) {
                            final Document envCanonicalized = evidenceEmitterHandlerUtils.canonicalizeAxiomSoapEnvelope(soapMessage.getSOAPHeader());

                            final SOAPHeader soapHeader = soapMessage.getSOAPHeader();
                            final SOAPBody soapBody = sourceEnvelope.getBody();
                            final String eventType = evidenceEmitterHandlerUtils.getEventTypeFromMessage(soapBody);
                            final String title = evidenceEmitterHandlerUtils.getTransactionNameFromMessage(soapBody);
                            final String msgUUID = evidenceEmitterHandlerUtils.getMsgUUID(soapHeader, soapBody);
                            LOGGER.debug("eventType: '{}'", eventType);
                            LOGGER.debug("title: '{}'", title);
                            LOGGER.debug("msgUUID: '{}", msgUUID);

                            /* Portal sends request to NCP-B*/
                            EvidenceUtils.createEvidenceREMNRO(envCanonicalized, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                                    Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                                    Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD,
                                    Constants.SC_PRIVATEKEY_ALIAS, eventType, new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                                    title, msgUUID);
                        }
                    }
                }

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

           // return InvocationResponse.CONTINUE;
        }

    }
}
