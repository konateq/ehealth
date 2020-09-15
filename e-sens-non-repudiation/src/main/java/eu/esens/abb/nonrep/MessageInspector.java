package eu.esens.abb.nonrep;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.mail.internet.MimeMessage;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;


public class MessageInspector {

    private static final String SOAP_LOCAL_NAME = "Envelope";
    private static final String WS_ADDRESSING_NS = "http://www.w3.org/2005/08/addressing";
    private static final String IHE_ITI_XCA_RETRIEVE = "urn:ihe:iti:2007:CrossGatewayRetrieve";
    private final Logger logger = LoggerFactory.getLogger(MessageInspector.class);
    private MessageType messageType;
    private String messageUUID;

    public MessageInspector(final MimeMessage incomingMsg) throws MalformedMIMEMessageException {
        throw new MalformedMIMEMessageException("Not yet implemented");
    }

    public MessageInspector(final SOAPMessage incomingMsg) throws MalformedIHESOAPException, SOAPException {
        this(incomingMsg.getSOAPBody().getOwnerDocument());
    }

    /**
     * @param incomingMsg
     * @throws MalformedIHESOAPException
     */
    public MessageInspector(final Document incomingMsg) throws MalformedIHESOAPException {

        if (incomingMsg == null) {
            throw new NullPointerException("No message has been passed");
        }
        logger.debug("MessageInspector, called with a document. Checking headers");
        checkHeaders(incomingMsg);
    }

    private void checkHeaders(Document incomingMsg) throws MalformedIHESOAPException {

        Element docElement = incomingMsg.getDocumentElement();
        logger.info("[Non Repudiation] '{}' - '{}'", docElement.getLocalName(), docElement.getNamespaceURI());
        if (StringUtils.equals(docElement.getLocalName(), SOAP_LOCAL_NAME)
                && StringUtils.equals(docElement.getNamespaceURI(), SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE)) {

            // TODO: Incoming Soap message 1.1 are not considered at this  point of time as a known type of message for
            //  the sake of evidence emitter. As they are no using right now the WS Addressing etc. SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE
            logger.debug("Found a SOAP 1.2 message");
            // The SOAP Message must be well structured to avoid MITM attacks e.g., it must have one SOAP header
            // and one single addressing. No WSSE4j is used here (which doesn't check for it).
            NodeList nodeList = docElement.getElementsByTagNameNS(docElement.getNamespaceURI(), "Header");
            Utilities.checkForNull(nodeList, "Header", logger);

            // get the body
            NodeList nlBody = docElement.getElementsByTagNameNS(docElement.getNamespaceURI(), "Body");
            Utilities.checkForNull(nlBody, "Body", logger);
            Element body = (Element) nlBody.item(0);

            // it can only be an element here, no classcast
            Element header = (Element) nodeList.item(0);

            // header must have one addressing action
            nodeList = header.getElementsByTagNameNS(WS_ADDRESSING_NS, "Action");
            Utilities.checkForNull(nodeList, "WS-Addressing action", logger);


            Element action = (Element) nodeList.item(0);
            String actionText = action.getTextContent();
            if (actionText == null) {
                throw new MalformedIHESOAPException("No action text found");
            }

            if (StringUtils.equals(actionText, IHE_ITI_XCA_RETRIEVE)) {
                logger.debug("Found an IHE ITI XCA RETRIEVE");
                IHEXCARetrieve xcaRetrieve = new IHEXCARetrieve(body);
                this.setMessageType(xcaRetrieve);
            } else {
                logger.warn("Action not recognized: '{}'", actionText);
                //TODO: I differentiate here, since one may do some other guesses, to see if it is a valid message
                UnknownMessageType umt = new UnknownMessageType(incomingMsg);
                this.setMessageType(umt);
            }

            nodeList = header.getElementsByTagNameNS(WS_ADDRESSING_NS, "MessageID");
            Utilities.checkForNull(nodeList, "WS-Addressing MessageID", logger);

            Element uuidEl = (Element) nodeList.item(0);
            String uuidText = uuidEl.getTextContent();
            if (uuidText == null) {
                throw new MalformedIHESOAPException("No UUID can be found in the WS-Addressing header");
            } else {
                this.messageUUID = uuidText;
            }
        } else {
            logger.warn("The document passed is not a SOAP.");
            UnknownMessageType umt = new UnknownMessageType(incomingMsg);
            this.setMessageType(umt);
        }
    }

    public MessageType getMessageType() {
        return messageType;
    }

    private void setMessageType(final MessageType messageType) {
        this.messageType = messageType;
    }

    public String getMessageUUID() {
        return this.messageUUID;
    }
}
