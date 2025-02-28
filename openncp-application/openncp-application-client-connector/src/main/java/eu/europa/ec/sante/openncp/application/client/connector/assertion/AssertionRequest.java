package eu.europa.ec.sante.openncp.application.client.connector.assertion;

import eu.europa.ec.sante.openncp.common.security.util.AssertionUtil;
import org.immutables.value.Value;
import org.opensaml.saml.saml2.core.Assertion;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import java.net.URL;
import java.util.UUID;

public interface AssertionRequest {
    String ADDRESSING_NS = "http://www.w3.org/2005/08/addressing";
    String WS_TRUST_NS = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    String ACTION_URI = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue";
    String SAML20_TOKEN_URN = "urn:oasis:names:tc:SAML:2.0:assertion";
    String WS_SEC_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    @Value.Derived
    default String getId() {
        return "urn:uuid:" + UUID.randomUUID();
    }

    URL getLocation();

    @Value.Auxiliary
    Assertion getAssertion();

    boolean checkForHostname();

    boolean validationEnabled();

    void validate(Assertion assertion);

    @Value.Auxiliary
    void getSoapBody(SOAPBody body);

    @Value.Auxiliary
    default DocumentBuilder getDocumentBuilder() {
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setXIncludeAware(false);
            documentBuilderFactory.setNamespaceAware(true);
            return documentBuilderFactory.newDocumentBuilder();
        } catch (final ParserConfigurationException ex) {
            throw new STSClientException("Unable to create RST Message");
        }
    }

    @Value.Auxiliary
    default SOAPMessage getSoapMessage() {
        try {
            final SOAPMessage message = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();
            final SOAPHeader soapHeader = message.getSOAPHeader();
            final SOAPHeaderElement messageIdElem = soapHeader.addHeaderElement(new QName(ADDRESSING_NS, "MessageID", "wsa"));
            messageIdElem.setTextContent(getId());

            final SOAPHeaderElement securityHeaderElem = soapHeader.addHeaderElement(new QName(WS_SEC_NS, "Security", "wsse"));
            securityHeaderElem.setMustUnderstand(false);

            final Element assertionElement = AssertionUtil.toElement(getAssertion(), getDocumentBuilder().newDocument()).orElseThrow(() -> new STSClientException("Could not convert the assertion to element"));
            securityHeaderElem.appendChild(soapHeader.getOwnerDocument().importNode(assertionElement, true));

            final SOAPBody soapBody = message.getSOAPBody();
            getSoapBody(soapBody);

            return message;
        } catch (final SOAPException e) {
            throw new STSClientException("Could not create the soap message", e);
        }
    }
}
