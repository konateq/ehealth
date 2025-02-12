package eu.europa.ec.sante.openncp.core.common.ihe.handler;


import eu.europa.ec.sante.openncp.common.audit.EventOutcomeIndicator;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.core.common.ihe.evidence.EvidenceUtils;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.handlers.AbstractHandler;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


/**
 * EvidenceEmitterHandler
 * Generates all NROs for the Portal
 * Currently supporting the generation of evidences in the following cases: Portal sends request to NCP-B
 *
 * @author jgoncalves
 */
public class OutFlowEvidenceEmitterHandler extends AbstractHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutFlowEvidenceEmitterHandler.class);

    @Override
    public InvocationResponse invoke(final MessageContext msgcontext) {

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
            final Document envCanonicalized = evidenceEmitterHandlerUtils.canonicalizeAxiomSoapEnvelope(msgcontext.getEnvelope());

            final SOAPHeader soapHeader = msgcontext.getEnvelope().getHeader();
            final SOAPBody soapBody = msgcontext.getEnvelope().getBody();
            final String eventType = evidenceEmitterHandlerUtils.getEventTypeFromMessage(soapBody);
            final String title = evidenceEmitterHandlerUtils.getTransactionNameFromMessage(soapBody);
            final String msgUUID = evidenceEmitterHandlerUtils.getMsgUUID(soapHeader, soapBody);
            LOGGER.debug("eventType: '{}'", eventType);
            LOGGER.debug("title: '{}'", title);
            LOGGER.debug("msgUUID: '{}", msgUUID);

            AxisService axisService = msgcontext.getServiceContext().getAxisService();
            boolean isClientSide = axisService.isClientSide();
            LOGGER.debug("AxisService name: '{}' - isClientSide: '{}'", axisService.getName(), isClientSide);

            if (isClientSide) {
                LOGGER.info("[NRO] Evidence Emitter - NCP-B");
                /* NCP-B sends to NCP-A, e.g.:
                    NRO
                    title = "NCPB_XCPD_REQ"
                    eventType = ihe event
                */
                //msgUUID = null; It stays as null because it's fetched from soap msg
                LOGGER.debug("Title: '{}' - eventType: '{}'", title, eventType);

                /* Portal sends request to NCP-B*/
                EvidenceUtils.createEvidenceREMNRO(envCanonicalized, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                        Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                        Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD,
                        Constants.SC_PRIVATEKEY_ALIAS, eventType, new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                        "NCPB_" + title, msgUUID);
            } else {
                LOGGER.info("[NRO] Evidence Emitter - NCP-A");
                /* NCP-A replies to NCP-B, e.g.:
                    NRO
                    title = "NCPA_XCPD_RES"
                    eventType = ihe event
                NCP-B replies to Portal, e.g.:
                    NRO
                    title = "NCPB_PD_RES_SENT"
                    eventType = "NCPB_PD_RES"
                    msguuid = random
                */
                /* Joao: as per the CP, evidence generation on the way back is optional,
                so I leave it commented. If in the future it's decided that is mandatory,
                just uncomment.
                */
//                eventType = this.evidenceEmitterHandlerUtils.getEventTypeFromMessage(soapBody);
//                title = this.evidenceEmitterHandlerUtils.getServerSideTitle(soapBody);
//                msgUUID = this.evidenceEmitterHandlerUtils.getMsgUUID(soapHeader, soapBody);
//                logger.debug("eventType: " + eventType);
//                logger.debug("title: " + title);
//                logger.debug("msgUUID: " + msgUUID);
//
//                if (msgUUID != null) {
//                    // this is a Portal-NCPB interaction: msgUUID comes from IdA or is random
//                    EvidenceUtils.createEvidenceREMNRO(envCanonicalized,
//                                Constants.NCP_SIG_KEYSTORE_PATH,
//                                Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                                Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                                Constants.SC_KEYSTORE_PATH,
//                                Constants.SC_KEYSTORE_PASSWORD,
//                                Constants.SC_PRIVATEKEY_ALIAS,
//                                Constants.NCP_SIG_KEYSTORE_PATH,
//                                Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                                Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                                eventType,
//                                new DateTime(),
//                                EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                                title,
//                                msgUUID);
//                } else {
//                    // this isn't a Portal-NCPB interaction (it's NCPB-NCPA), so msgUUID is retrieved from the soap header
//                    EvidenceUtils.createEvidenceREMNRO(envCanonicalized,
//                                Constants.NCP_SIG_KEYSTORE_PATH,
//                                Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                                Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                                Constants.SP_KEYSTORE_PATH,
//                                Constants.SP_KEYSTORE_PASSWORD,
//                                Constants.SP_PRIVATEKEY_ALIAS,
//                                Constants.SC_KEYSTORE_PATH,
//                                Constants.SC_KEYSTORE_PASSWORD,
//                                Constants.SC_PRIVATEKEY_ALIAS,
//                                eventType,
//                                new DateTime(),
//                                EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                                title);
//                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return InvocationResponse.CONTINUE;
    }
}
