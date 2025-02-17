package eu.europa.ec.sante.openncp.core.common.ihe.handler;

import eu.europa.ec.sante.openncp.core.common.ihe.eadc.EadcUtilWrapper;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPEnvelope; // Import SOAPEnvelope
import javax.xml.soap.SOAPHeader; // Import SOAPHeader
import javax.xml.soap.SOAPMessage;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DummyMustUnderstandHandler implements PhaseInterceptor<Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EadcUtilWrapper.class);

    @Override
    public void handleMessage(Message message) {
        if (!(message instanceof SoapMessage)) {
            LOGGER.info("Message is not a SOAP message. Cannot check for MustUnderstand headers.");
            return;
        }

        SOAPMessage soapMessage = message.get(SOAPMessage.class);

        try {
            Document document = message.getContent(Document.class);
            if (document == null) { // Handle the case where the document may not have been parsed yet.
                try {
                    javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    dbFactory.setNamespaceAware(true);
                    javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    document = dBuilder.parse(message.getContent(java.io.InputStream.class));
                    message.setContent(Document.class, document); // Put it back into the message
                } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException e) {
                    LOGGER.error("Error parsing SOAP message: " + e.getMessage());
                    throw new RuntimeException("Error parsing SOAP message", e);
                }
            }


            // XPath for MustUnderstand headers (replace with your actual namespace and header name)
            javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xpathFactory.newXPath();

            // Example: Check for a header named "YourMustUnderstandHeader" in the namespace "http://your-namespace"
            String expression = "//soapenv:Header/ns:YourMustUnderstandHeader[@mustUnderstand='1']"; // Adjust namespace prefixes
            xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if ("soapenv".equals(prefix)) {
                        return "http://schemas.xmlsoap.org/soap/envelope/";
                    } else if ("ns".equals(prefix)) {
                        return "http://your-namespace"; // Replace with your namespace
                    }
                    return null;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }

                @Override
                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });

            XPathExpression compiledExpression = xpath.compile(expression);

            NodeList mustUnderstandHeaders = (NodeList) compiledExpression.evaluate(document, XPathConstants.NODESET);

            if (mustUnderstandHeaders.getLength() > 0) {
                // MustUnderstand header found! Mark it as processed and REMOVE it.
                LOGGER.info("MustUnderstand header 'YourMustUnderstandHeader' found and REMOVED.");

                SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
                SOAPHeader soapHeader = envelope.getHeader();
                if (soapHeader != null) {
                    for (int i = 0; i < mustUnderstandHeaders.getLength(); i++) {
                        org.w3c.dom.Node nodeToRemove = mustUnderstandHeaders.item(i);
                        soapHeader.removeChild(nodeToRemove);
                    }
                }
            }

        } catch (Exception e) { // Catch SOAPException, TransformerException, RuntimeException, etc.
            LOGGER.error("Error processing message: " + e.getMessage());
            if (!(e instanceof org.apache.cxf.binding.soap.SoapFault)) { // If it's not already a SoapFault
                throw new RuntimeException("Error processing message", e); // Re-throw as RuntimeException or your custom exception
            }
        }
    }

    @Override
    public void handleFault(Message message) {

    }

    @Override
    public Set<String> getAfter() {
        return Set.of();
    }

    @Override
    public Set<String> getBefore() {
        return Set.of();
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getPhase() {
        return "";
    }

    @Override
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return List.of();
    }

    // ... (rest of the code remains the same)
}
