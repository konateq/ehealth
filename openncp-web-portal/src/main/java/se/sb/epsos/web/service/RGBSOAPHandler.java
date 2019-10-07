package se.sb.epsos.web.service;

import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.gazelle.validation.OpenNCPValidation;
import org.apache.commons.lang3.BooleanUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.impl.AssertionMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import se.sb.epsos.web.auth.AuthenticatedUser;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class RGBSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    private static final String SECURITY_NAMESPACE_URL = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String SECURITY_NAMESPACE_PREFIX = "wsse";
    private static final String SECURITY_NODENAME = "Security";
    private final Logger logger = LoggerFactory.getLogger(RGBSOAPHandler.class);
    private AssertionHandler assertionHandler = new AssertionHandler();
    private AuthenticatedUser user;

    public RGBSOAPHandler(AuthenticatedUser user) {
        this.user = user;
    }

    /**
     * Convert any SOAP object that implements SOAPMessage into a String
     *
     * @param msg SOAP object
     * @return String
     * @throws SOAPException
     * @throws IOException
     */
    private String getXmlFromSOAPMessage(SOAPMessage msg) throws SOAPException, IOException {

        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        msg.writeTo(byteArrayOS);
        return new String(byteArrayOS.toByteArray());
    }

    @Override
    public void close(MessageContext context) {
        context.entrySet().clear();
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {

        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (BooleanUtils.isTrue(outboundProperty)) {
            SOAPMessage message = context.getMessage();
            try {
                SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
                SOAPHeader header = envelope.getHeader();
                if (header == null) {
                    header = envelope.addHeader();
                }
                Assertion assertion = user.getAssertion();

                // If the user has not received an assertion yet, generate it
                if (assertion == null || assertionHandler.isExpired(assertion)) {
                    assertion = assertionHandler.createSAMLAssertion(user);
                    assertionHandler.signSAMLAssertion(assertion);
                    if (OpenNCPValidation.isValidationEnable()) {
                        OpenNCPValidation.validateHCPAssertion(assertion, NcpSide.NCP_B);
                    }
                    user.setAssertion(assertion);
                }

                AssertionMarshaller marshaller = new AssertionMarshaller();
                SOAPElement security = header.addChildElement(SECURITY_NODENAME, SECURITY_NAMESPACE_PREFIX, SECURITY_NAMESPACE_URL);
                SOAPFactory soapFactory = SOAPFactory.newInstance();

                if (user.getTrcAssertion() != null) {
                    Element trcAssertionElement = marshaller.marshall(user.getTrcAssertion());
                    SOAPElement assertionTrcSOAP = soapFactory.createElement(trcAssertionElement);
                    security.addChildElement(assertionTrcSOAP);
                }

                Element element = marshaller.marshall(assertion);
                SOAPElement assertionSOAP = soapFactory.createElement(element);
                security.addChildElement(assertionSOAP);

                logger.debug("Soap message: '{}'", getXmlFromSOAPMessage(message));
            } catch (Exception e) {
                logger.error("Failed to handling assertion: '{}'", e.getMessage(), e);
            }
        }
        return true;
    }

    @Override
    public Set<QName> getHeaders() {
        return new TreeSet<>();
    }
}
