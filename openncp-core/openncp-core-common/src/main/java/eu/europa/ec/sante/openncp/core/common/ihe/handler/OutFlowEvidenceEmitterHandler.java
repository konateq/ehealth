package eu.europa.ec.sante.openncp.core.common.ihe.handler;


import eu.europa.ec.sante.openncp.common.audit.EventOutcomeIndicator;
import eu.europa.ec.sante.openncp.common.audit.EventType;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.evidence.EvidenceUtils;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.handlers.AbstractHandler;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;


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

            final X509Certificate issuerCert = EvidenceUtils.getCertificate(Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                    Constants.NCP_SIG_PRIVATEKEY_ALIAS);
            final X509Certificate recipientCert = EvidenceUtils.getCertificate(Constants.SC_KEYSTORE_PATH, Constants.SC_KEYSTORE_PASSWORD, Constants.SC_PRIVATEKEY_ALIAS);
            final X509Certificate senderCert = EvidenceUtils.getCertificate(Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD, Constants.SP_PRIVATEKEY_ALIAS);

            final PrivateKey key = EvidenceUtils.getSigningKey(Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                    Constants.NCP_SIG_PRIVATEKEY_ALIAS);

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
                EvidenceUtils.createEvidenceREMNRO(
                        envCanonicalized,
                        issuerCert,
                        issuerCert,
                        senderCert,
                        key,
                        eventType,
                        new DateTime(),
                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
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

                EvidenceUtils.createEvidenceREMNRO(
                        envCanonicalized,
                        issuerCert,
                        senderCert,
                        issuerCert,
                        key,
                        eventType,
                        new DateTime(),
                        EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
                        title,
                        msgUUID);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return InvocationResponse.CONTINUE;
    }
}
