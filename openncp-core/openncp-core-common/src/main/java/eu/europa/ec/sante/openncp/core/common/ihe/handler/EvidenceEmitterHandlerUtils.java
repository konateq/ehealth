package eu.europa.ec.sante.openncp.core.common.ihe.handler;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import java.util.*;

/**
 * Ancillary methods to the EvidenceEmitter class supporting In-Out flows in the Portal
 *
 * @author jgoncalves
 */
public class EvidenceEmitterHandlerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvidenceEmitterHandlerUtils.class);
    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");

    private static final String CLIENT_CONNECTOR_SUBMIT_DOCUMENT_REQUEST = "ProvideAndRegisterDocumentSetRequest";
    private static final String CLIENT_CONNECTOR_SUBMIT_DOCUMENT_RESPONSE = "RegistryResponse";
    private static final String CLIENT_CONNECTOR_QUERY_PATIENT_REQUEST = "PRPA_IN201305UV02";
    private static final String CLIENT_CONNECTOR_QUERY_PATIENT_RESPONSE = "PRPA_IN201306UV02";
    private static final String CLIENT_CONNECTOR_QUERY_DOCUMENTS_REQUEST = "AdhocQueryRequest";
    private static final String CLIENT_CONNECTOR_QUERY_DOCUMENTS_RESPONSE = "AdhocQueryResponse";
    private static final String CLIENT_CONNECTOR_RETRIEVE_DOCUMENT_REQUEST = "RetrieveDocumentSetRequest";
    private static final String CLIENT_CONNECTOR_RETRIEVE_DOCUMENT_RESPONSE = "RetrieveDocumentSetResponse";

    private static final List<String> clientConnectorOperations;
    // maps the message type to its related ad-hoc event
    private static final Map<String, String> events;
    // maps the message type to the ad-hoc transaction name to be placed in the evidence filename
    private static final Map<String, String> transactionNames;

    static {

        final List<String> list = new ArrayList<>();
        list.add(CLIENT_CONNECTOR_SUBMIT_DOCUMENT_REQUEST);
        list.add(CLIENT_CONNECTOR_SUBMIT_DOCUMENT_RESPONSE);
        list.add(CLIENT_CONNECTOR_QUERY_PATIENT_REQUEST);
        list.add(CLIENT_CONNECTOR_QUERY_PATIENT_RESPONSE);
        list.add(CLIENT_CONNECTOR_QUERY_DOCUMENTS_REQUEST);
        list.add(CLIENT_CONNECTOR_QUERY_DOCUMENTS_RESPONSE);
        list.add(CLIENT_CONNECTOR_RETRIEVE_DOCUMENT_REQUEST);
        list.add(CLIENT_CONNECTOR_RETRIEVE_DOCUMENT_RESPONSE);
        clientConnectorOperations = Collections.unmodifiableList(list);
    }

    static {

        final Map<String, String> map = new HashMap<>();
        // Portal-NCP interactions
        map.put(CLIENT_CONNECTOR_SUBMIT_DOCUMENT_REQUEST, "NI_XDR_REQ");
        map.put(CLIENT_CONNECTOR_SUBMIT_DOCUMENT_RESPONSE, "NI_XDR_RES");
        map.put(CLIENT_CONNECTOR_QUERY_PATIENT_REQUEST, "NI_PD_REQ");
        map.put(CLIENT_CONNECTOR_QUERY_PATIENT_RESPONSE, "NI_PD_RES");
        map.put(CLIENT_CONNECTOR_QUERY_DOCUMENTS_REQUEST, "NI_DQ_REQ");
        map.put(CLIENT_CONNECTOR_QUERY_DOCUMENTS_RESPONSE, "NI_DQ_RES");
        map.put(CLIENT_CONNECTOR_RETRIEVE_DOCUMENT_REQUEST, "NI_DR_REQ");
        map.put(CLIENT_CONNECTOR_RETRIEVE_DOCUMENT_RESPONSE, "NI_DR_RES");
        events = Collections.unmodifiableMap(map);
    }

    static {

        final Map<String, String> map = new HashMap<>();
        // Portal-NCP interactions
        map.put(CLIENT_CONNECTOR_SUBMIT_DOCUMENT_REQUEST, "NI_XDR_REQ_SENT");
        map.put(CLIENT_CONNECTOR_SUBMIT_DOCUMENT_RESPONSE, "NI_XDR_RES_RECEIVED");
        map.put(CLIENT_CONNECTOR_QUERY_PATIENT_REQUEST, "NI_PD_REQ_SENT");
        map.put(CLIENT_CONNECTOR_QUERY_PATIENT_RESPONSE, "NI_PD_RES_RECEIVED");
        map.put(CLIENT_CONNECTOR_QUERY_DOCUMENTS_REQUEST, "NI_DQ_REQ_SENT");
        map.put(CLIENT_CONNECTOR_QUERY_DOCUMENTS_RESPONSE, "NI_DQ_RES_RECEIVED");
        map.put(CLIENT_CONNECTOR_RETRIEVE_DOCUMENT_REQUEST, "NI_DR_REQ_SENT");
        map.put(CLIENT_CONNECTOR_RETRIEVE_DOCUMENT_RESPONSE, "NI_DR_RES_RECEIVED");
        transactionNames = Collections.unmodifiableMap(map);
    }

    public EvidenceEmitterHandlerUtils() {
    }

    public String getEventTypeFromMessage(final SOAPBody soapBody) {


        Element firstChild = (Element) soapBody.getFirstChild();

        if (firstChild != null) {
            final String messageElement =  firstChild.getLocalName();
            LOGGER.debug("Message body element: '{}'", messageElement);
            return events.get(messageElement);
        } else {
            return null;
        }


    }

    public String getTransactionNameFromMessage(final SOAPBody soapBody) {

        Element firstChild = (Element) soapBody.getFirstChild();

        if (firstChild != null) {
            final String messageElement =  firstChild.getLocalName();
            LOGGER.debug("Message body element: '{}'", messageElement);
            return transactionNames.get(messageElement);
        } else {
            return null;
        }
    }

    private boolean isClientConnectorOperation(final String operation) {
        return clientConnectorOperations.contains(operation);
    }

    public String getServerSideTitle(final SOAPBody soapBody) {


        Element firstChild = (Element) soapBody.getFirstChild();

        if (firstChild != null) {
            final String messageElement =  firstChild.getLocalName();
            String title = transactionNames.get(messageElement);
            if (!this.isClientConnectorOperation(messageElement)) {
                title = "NCPA_" + title;
            }
            return title;
        } else {
            return null;
        }


    }

    public String getMsgUUID(final SOAPHeader soapHeader, final SOAPBody soapBody) throws Exception {

        String msguuid = null;
        final Element elemSoapHeader = XMLUtils.toDOM(soapHeader);
       // final String operation = soapBody.getFirstElementLocalName();
        Element firstChild = (Element) soapBody.getFirstChild();
        final String operation =  firstChild.getLocalName();
        if (isClientConnectorOperation(operation)) {
            // we're in a Portal-NCPB interaction
            final Assertion identityAssertion = SoapElementHelper.getHCPAssertion(elemSoapHeader);
            final Assertion trca = SoapElementHelper.getTRCAssertion(elemSoapHeader);
            if (identityAssertion != null && trca == null) {
                // this is a XCPD request from Portal to NCP-B, we don't yet have the TRCA
                msguuid = identityAssertion.getID();
            } else if (identityAssertion != null && trca != null) {
                // this is a XCA Query or Retrieve from Portal to NCP-B, we already have the TRCA
                msguuid = trca.getID();
            } else {
                //response to Portal doesn't have IdA, only the request from the Portal has it
                // we don't have the IdA nor SOAP message ID, so we generate one UUID
                msguuid = Constants.UUID_PREFIX + UUID.randomUUID().toString();
            }
        }
        return msguuid;
    }

    public Document canonicalizeAxiomSoapEnvelope(final SOAPEnvelope env) throws Exception {

        final Element envAsDom =
        final Document envCanonicalized = XMLUtil.canonicalize(envAsDom.getOwnerDocument());
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("Pretty printing canonicalized:\n{}", XMLUtil.prettyPrint(envCanonicalized));
        }
        return envCanonicalized;
    }
}
