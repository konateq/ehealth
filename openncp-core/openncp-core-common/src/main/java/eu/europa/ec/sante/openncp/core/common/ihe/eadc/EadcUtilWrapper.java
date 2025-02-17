package eu.europa.ec.sante.openncp.core.common.ihe.eadc;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.util.XMLUtil;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.datamodel.ObjectFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.datamodel.TransactionInfo;
import eu.europa.ec.sante.openncp.core.common.ihe.util.EventLogClientUtil;
import eu.europa.ec.sante.openncp.core.common.util.OidUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.cxf.headers.Header;


/**
 * This class wraps the EADC invocation. As it gathers several aspects required to its proper usage, such as
 * the compilation and preparation of transaction details.
 *
 * @author Marcelo Fonseca<code> - marcelo.fonseca@iuz.pt</code>
 */
public class EadcUtilWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EadcUtilWrapper.class);

    private EadcUtilWrapper() {
    }

    /**
     * Main EADC Wrapper operation. It receives as input all the required information to successfully fill a transaction object.
     *
     * @param reqMessage  the request Servlet Message Context
     * @param rspMessage the response Servlet Message Context
     * @param serviceClient  the Axis2 Service Client
     * @param cda            the (optional) CDA document*
     * @param startTime      the transaction start time
     * @param endTime        the transaction end time
     * @param receivingIso   the country A ISO Code
     * @param dsType         the JDBC Datasource corresponding to the IHE operation
     * @param direction      the Operation type: INBOUND or OUTBOUND
     * @param serviceType    the Service Type representing the action executed to prevent processing of personal data
     */
    public static void invokeEadc(final Message reqMessage, final Message rspMessage, final Client serviceClient, final Document cda,
                                  final Date startTime, final Date endTime, final String receivingIso, final EadcEntry.DsTypes dsType, final EadcUtil.Direction direction,
                                  final ServiceType serviceType) {

        new Thread(() -> {
            final var watch = new StopWatch();
            watch.start();
            try {
                EadcUtil.invokeEadc(reqMessage, rspMessage, cda,
                                    buildTransactionInfo(reqMessage, rspMessage, serviceClient, direction, startTime, endTime, receivingIso,
                                                         serviceType), dsType);
            } catch (final Exception e) {
                LOGGER.error("[EADC] Invocation Failed - Exception: '{}'", e.getMessage(), e);
            } finally {
                watch.stop();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("EADC invocation executed in: '{}ms'", watch.getTime());
                }
            }
        }).start();
    }

    /**
     * Main EADC Wrapper operation. It receives as input all the required information to successfully fill a transaction object.
     *
     * @param requestMsgCtx  the request Servlet Message Context
     * @param responseMsgCtx the response Servlet Message Context
     * @param serviceClient  the Axis2 Service Client
     * @param cda            the (optional) CDA document*
     * @param startTime      the transaction start time
     * @param endTime        the transaction end time
     * @param receivingIso   the country A ISO Code
     * @param dsType         the JDBC Datasource corresponding to the IHE operation
     * @param direction      the Operation type: INBOUND or OUTBOUND
     * @param serviceType    the Service Type representing the action executed to prevent processing of personal data
     */
    public static void invokeEadcFailure(final Message requestMsgCtx, final Message responseMsgCtx, final Client serviceClient, final Document cda,
                                         final Date startTime, final Date endTime, final String receivingIso, final EadcEntry.DsTypes dsType, final EadcUtil.Direction direction,
                                         final ServiceType serviceType, final String errorDescription) {

        new Thread(() -> {
            final var watch = new StopWatch();
            watch.start();
            try {
                EadcUtil.invokeEadcFailure(requestMsgCtx, responseMsgCtx, cda,
                        buildTransactionInfo(requestMsgCtx, responseMsgCtx, serviceClient, direction, startTime, Objects.requireNonNullElseGet(endTime, Date::new),
                                receivingIso, serviceType), dsType, errorDescription);
            } catch (final Exception e) {
                LOGGER.error("[EADC Failure] Invocation Failed - Exception: '{}'", e.getMessage(), e);
            } finally {
                watch.stop();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("EADC Failure invocation executed in: '{}ms'", watch.getTime());
                }
            }
        }).start();
    }

    /*public static boolean hasTransactionErrors(final SOAPEnvelope envelope) {
        if (envelope != null) {
            Iterator<OMElement> it = envelope.getBody().getChildElements();
            while (it.hasNext()) {
                final OMElement elementDocSet = it.next();

                if (StringUtils.equals(elementDocSet.getLocalName(), "RegistryError")) {
                    final String severity = elementDocSet.getAttributeValue(QName.valueOf("severity"));
                    if (StringUtils.equals(severity, "urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error")) {
                        return true;
                    }
                }
                it = elementDocSet.getChildElements();
            }
        }

        return false;
    }*/


    public static boolean hasTransactionErrors(final Message message) {

        if (!(message instanceof SoapMessage)) {
            LOGGER.warn("Message is not a SOAP message. Cannot check for transaction errors.");
            return false;
        }

        SoapMessage soapMessage = (SoapMessage) message;

        try {
            org.w3c.dom.Document document = message.getContent(org.w3c.dom.Document.class);
            if (document == null) { // Handle the case where the document may not have been parsed yet.
                try {
                    javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    dbFactory.setNamespaceAware(true);
                    javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    document = dBuilder.parse(message.getContent(java.io.InputStream.class));
                    message.setContent(org.w3c.dom.Document.class, document); // Put it back into the message
                } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException | java.io.IOException e) {
                    LOGGER.error("Error parsing SOAP message: " + e.getMessage());
                    return false; // Or throw an exception
                }
            }


            // Use XPath (recommended for robustness):
            javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xpathFactory.newXPath();

            // Define namespace context if needed (if "rr" isn't already bound in the message)
            xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    if ("rr".equals(prefix)) {
                        return "urn:oasis:names:tc:ebxml-regrep:xsd:registry-2.0"; // Or your registry namespace
                    }
                    return null;
                }

                public String getPrefix(String namespaceURI) {
                    return null;
                }

                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });


            String expression = "//rr:RegistryError[@severity='urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error']";  // Use your prefix
            javax.xml.xpath.XPathExpression compiledExpression = xpath.compile(expression);
            NodeList errorNodes = (NodeList) compiledExpression.evaluate(document, javax.xml.xpath.XPathConstants.NODESET);

            return errorNodes.getLength() > 0; // If any matching nodes are found, there are errors


        } catch (javax.xml.xpath.XPathExpressionException e) {
            LOGGER.error("Error evaluating XPath: " + e.getMessage());
            return false; // Or throw an exception
        }

    }

/*    public static String getTransactionErrorDescription(final SOAPEnvelope envelope) {
        String errorDescription = "unknown";

        if (envelope != null) {
            Iterator<OMElement> it = envelope.getBody().getChildElements();
            while (it.hasNext()) {
                final OMElement elementDocSet = it.next();

            *//* example element
                <RegistryError
                        xmlns="urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0"
                        codeContext="The requested encoding cannot be provided due to a transcoding error."
                        errorCode="4203"
                        severity="urn:oasis:names:tc:ebxml-regrep:ErrorSeverityType:Error"
                        location="urn:oid:2.16.17.710.823.1000.990.1"/>
            *//*
                if (StringUtils.equals(elementDocSet.getLocalName(), "RegistryError")) {
                    final String err = elementDocSet.getAttributeValue(QName.valueOf("errorCode"));
                    final String cod = elementDocSet.getAttributeValue(QName.valueOf("codeContext"));
                    errorDescription = cod + " [" + err + "]";
                    break;
                }
                it = elementDocSet.getChildElements();
            }
        } else {
            errorDescription = "envelope is null!";
        }
        return errorDescription;
    }*/

    public static String getTransactionErrorDescription(final Message message) {
        String errorDescription = "unknown";

        if (!(message instanceof SoapMessage)) {
            LOGGER.warn("Message is not a SOAP message. Cannot get error description.");
            return errorDescription;
        }

        SoapMessage soapMessage = (SoapMessage) message;

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
                    return "Error parsing message"; // Or throw an exception
                }
            }

            javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xpathFactory.newXPath();

            // Namespace context (Important - if namespace prefix is not defined in the message):
            xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    if ("rr".equals(prefix)) {
                        return "urn:oasis:names:tc:ebxml-regrep:xsd:rs:3.0"; // Or your registry namespace
                    }
                    return null;
                }

                public String getPrefix(String namespaceURI) {
                    return null;
                }

                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });

            // XPath expression (more robust):
            String expression = "//rr:RegistryError"; // Select all RegistryError elements
            XPathExpression compiledExpression = xpath.compile(expression);
            NodeList errorNodes = (NodeList) compiledExpression.evaluate(document, XPathConstants.NODESET);

            if (errorNodes.getLength() > 0) {
                Element errorElement = (Element) errorNodes.item(0); // Get the first error element

                String errorCode = errorElement.getAttribute("errorCode");
                String codeContext = errorElement.getAttribute("codeContext");

                if (codeContext != null && errorCode != null) {
                    errorDescription = codeContext + " [" + errorCode + "]";
                } else if (codeContext != null) {
                    errorDescription = codeContext;
                } else if (errorCode != null) {
                    errorDescription =  "[" + errorCode + "]";
                } else {
                    errorDescription = "RegistryError found, but no errorCode or codeContext attributes.";
                }
            } else {
                errorDescription = "No RegistryError element found.";
            }


        } catch (javax.xml.xpath.XPathExpressionException e) {
            LOGGER.error("Error evaluating XPath: " + e.getMessage());
            errorDescription = "Error evaluating XPath: " + e.getMessage(); // Or throw an exception
        }

        return errorDescription;
    }

    /**
     * Builds a Transaction Info object based on a set of information.
     *
     * @param reqMsgContext the request Servlet Message Context
     * @param rspMsgContext the response Servlet Message Context
     * @param serviceClient the Axis2 Service Client
     * @param direction     the request direction, INBOUND or OUTBOUND
     * @param startTime     the transaction start time
     * @param endTime       the transaction end time
     * @param countryCodeA  the country A ISO Code
     * @param serviceType   the service type related to the IHE transaction
     * @return the filled Transaction Info object
     */
    private static TransactionInfo buildTransactionInfo(final Message reqMsgContext, final Message rspMsgContext, final Client serviceClient,
                                                        final EadcUtil.Direction direction, final Date startTime, final Date endTime, final String countryCodeA,
                                                        final ServiceType serviceType) throws Exception {

        final var transactionInfo = new ObjectFactory().createComplexTypeTransactionInfo();
        transactionInfo.setAuthenticationLevel(reqMsgContext != null ? extractAuthenticationMethodFromAssertion(getAssertion(reqMsgContext)) : null);
        transactionInfo.setDirection(direction != null ? direction.toString() : null);
        transactionInfo.setStartTime(startTime != null ? getDateAsRFC822String(startTime) : null);
        transactionInfo.setEndTime(endTime != null ? getDateAsRFC822String(endTime) : null);
        transactionInfo.setDuration(endTime != null && startTime != null ? String.valueOf(endTime.getTime() - startTime.getTime()) : null);
        transactionInfo.setHomeAddress(EventLogClientUtil.getSourceGatewayIdentifier());
        final String sndIso = reqMsgContext != null ? extractSendingCountryIsoFromAssertion(getAssertion(reqMsgContext)) : null;
        transactionInfo.setSndISO(StringUtils.upperCase(sndIso));
        transactionInfo.setSndNCPOID(sndIso != null ? OidUtil.getHomeCommunityId(sndIso.toLowerCase()) : null);


        if (reqMsgContext != null) {
            // CXF uses WS-Addressing for the From address.
            AddressingProperties addrProps = reqMsgContext.get(AddressingProperties.class);
            if (addrProps != null && addrProps.getFrom() != null && addrProps.getFrom().getAddress() != null) {
                transactionInfo.setHomeHost(addrProps.getFrom().getAddress().getValue()); // Get the address value
            }
        }

        /*
            (EHNCP-1141) We cannot get the MessageID from the reqMsgContext, it returns a wrong one.
            Probably related to how the Axis2 engine sets the MessageID, similar issues were faced during the Evidence
            Emitter refactoring. Plus, for the XCA Retrieve request messages, when comparing this MessageID with the one
            from the message itself, be sure to compare it with the correct WSA headers, there are duplicated ones,
            although belonging to different namespaces (the correct one is xmlns = http://www.w3.org/2005/08/addressing)
        */
        transactionInfo.setSndMsgID(reqMsgContext != null ? getMessageID(reqMsgContext) : null);
        transactionInfo.setHomeHCID("");
        transactionInfo.setHomeISO(Constants.COUNTRY_CODE.toUpperCase());
        transactionInfo.setHomeNCPOID(Constants.HOME_COMM_ID);

        //  TODO: Clarify values for this field according specifications and GDPR, current value set to "N/A GDPR"
        transactionInfo.setHumanRequestor("N/A GDPR");
        transactionInfo.setUserId("N/A GDPR");
        transactionInfo.setPOC(
                reqMsgContext != null ? extractAssertionInfo(getAssertion(reqMsgContext), "urn:oasis:names:tc:xspa:1.0:environment:locality") + " (" +
                                        extractAssertionInfo(getAssertion(reqMsgContext), "urn:ehdsi:names:subject:healthcare-facility-type") + ")"
                                      : null);
        transactionInfo.setPOCID(
                reqMsgContext != null ? extractAssertionInfo(getAssertion(reqMsgContext), "urn:oasis:names:tc:xspa:1.0:subject:organization-id")
                                      : null);
        transactionInfo.setReceivingISO(countryCodeA != null ? StringUtils.upperCase(countryCodeA) : null);
        transactionInfo.setReceivingNCPOID(countryCodeA != null ? OidUtil.getHomeCommunityId(countryCodeA.toLowerCase()) : null);


        if (serviceClient != null && serviceClient.getEndpoint() != null && serviceClient.getEndpoint().getEndpointInfo() != null && serviceClient.getEndpoint().getEndpointInfo().getAddress() != null) {
            transactionInfo.setReceivingHost(serviceClient.getEndpoint().getEndpointInfo().getAddress());
            transactionInfo.setReceivingAddr(EventLogClientUtil.getTargetGatewayIdentifier(serviceClient.getEndpoint().getEndpointInfo().getAddress()));
        }


        if (reqMsgContext != null && reqMsgContext.get(Message.REQUEST_URI) != null) { // For Action
            transactionInfo.setRequestAction((String) reqMsgContext.get(Message.REQUEST_URI));
        }

        if (rspMsgContext != null && rspMsgContext.get(Message.REQUEST_URI) != null) { // For Action
            transactionInfo.setResponseAction((String) rspMsgContext.get(Message.REQUEST_URI));
        }
        if (reqMsgContext != null && reqMsgContext.getExchange() != null) {
            BindingOperationInfo bindingOperationInfo = reqMsgContext.getExchange().getBindingOperationInfo();
            if (bindingOperationInfo != null) {
                OperationInfo operationInfo = bindingOperationInfo.getOperationInfo();
                if (operationInfo != null) {
                        QName serviceName = operationInfo.getName(); // Get the QName of the service
                        if (serviceName != null) { // Check if the QName is not null
                            transactionInfo.setServiceName(serviceName.getLocalPart());

                    }
                }
            }
        }



        if (rspMsgContext != null) {
            // CXF uses WS-Addressing for message IDs.
            AddressingProperties addrProps = rspMsgContext.get(AddressingProperties.class);

            if (addrProps != null && addrProps.getMessageID() != null) {
                transactionInfo.setReceivingMsgID(addrProps.getMessageID().getValue()); // Get the message ID value
            } else {
                transactionInfo.setReceivingMsgID(null); // Explicitly set to null if not present (optional)
            }
        } else {
            transactionInfo.setReceivingMsgID(null); // Explicitly set to null if message context is null (optional)
        }
        transactionInfo.setServiceType(serviceType.getDescription());
        transactionInfo.setTransactionCounter("");
        transactionInfo.setTransactionPK(UUID.randomUUID().toString());

        return transactionInfo;
    }

    private static String getCXFMessageID(final Message message) {
        if (!(message instanceof SoapMessage)) {
            LOGGER.warn("Message is not a SOAP message. Cannot extract MessageID.");
            return null; // Or return a default value or throw an exception
        }

        SoapMessage soapMessage = (SoapMessage) message;
        List<Header> headers = soapMessage.getHeaders();

        if (headers != null) {
            for (Header header : headers) {
                if (header.getObject() instanceof Element) {
                    Element headerElement = (Element) header.getObject();

                    try {
                        javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
                        javax.xml.xpath.XPath xpath = xpathFactory.newXPath();
                        xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
                            public String getNamespaceURI(String prefix) {
                                if ("wsa".equals(prefix)) {
                                    return "http://www.w3.org/2005/08/addressing";
                                }
                                return null;
                            }

                            public String getPrefix(String namespaceURI) {
                                return null;
                            }

                            public Iterator getPrefixes(String namespaceURI) {
                                return null;
                            }
                        });

                        String expression = "//wsa:MessageID"; // Use your namespace prefix
                        javax.xml.xpath.XPathExpression compiledExpression = xpath.compile(expression);
                        String messageID = compiledExpression.evaluate(headerElement);

                        if (messageID != null && !messageID.isEmpty()) {
                            return messageID;
                        }


                    } catch (javax.xml.xpath.XPathExpressionException e) {
                        LOGGER.error("Error evaluating XPath: " + e.getMessage());
                        // Fallback to DOM traversal (less robust):
                        QName messageIDQName = new QName("http://www.w3.org/2005/08/addressing", "MessageID");
                        org.w3c.dom.NodeList nodeList = headerElement.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "MessageID");
                        if (nodeList.getLength() > 0) {
                            return nodeList.item(0).getTextContent();
                        }
                    }
                }
            }
        }

        return null; // MessageID not found.
    }



    /**
     * Extracts and assertion from a given message context
     *
     * @param requestMessageContext
     * @return
     * @throws Exception
     */
    /*private static Assertion getAssertion(final Message requestMessageContext) throws Exception {

        final var soapHeader = requestMessageContext.getEnvelope().getHeader();
        final Element soapHeaderElement = XMLUtils.toDOM(soapHeader);
        return SoapElementHelper.getHCPAssertion(soapHeaderElement);
    }*/

    private static Assertion getAssertion(final Message requestMessageContext) throws SOAPException, TransformerException {

        if (!(requestMessageContext instanceof SoapMessage)) {
            LOGGER.warn("Message is not a SOAP message. Cannot extract header.");
            return null; // Or throw an exception
        }

        SoapMessage soapMessage = (SoapMessage) requestMessageContext;

        List<Header> headers = soapMessage.getHeaders();
        if (headers == null || headers.isEmpty()) {
            LOGGER.warn("No SOAP Headers found in the message.");
            return null; // Or throw an exception
        }


        for (Header header : headers) {
            if (header.getObject() instanceof Element) { // Check if it's a DOM Element
                Element soapHeaderElement = (Element) header.getObject();
                return SoapElementHelper.getHCPAssertion(soapHeaderElement); // Assuming only one header contains the assertion
            }
        }

        LOGGER.warn("No SOAP Header element found containing the assertion.");
        return null; // Or throw an exception

    }

    /**
     * Assertion utility method. Will extract information of a specific assertion, based on a given expression.
     *
     * @param idAssertion the Identity Assertion
     * @param expression  the expression to evaluate
     * @return a string representing the information presented on the specified node
     */
    private static String extractAssertionInfo(final Assertion idAssertion, final String expression) {
        if (idAssertion == null) {
            return null;
        }
        for (final AttributeStatement attributeStatement : idAssertion.getAttributeStatements()) {
            for (final Attribute attribute : attributeStatement.getAttributes()) {
                if (attribute.getName().equals(expression)) {
                    return getAttributeValue(attribute);
                }
            }
        }
        return null;
    }

    /**
     * Extracts information from a given Assertion attribute.
     *
     * @param attribute the Assertion attribute
     * @return a string containing the value of the attribute
     */
    private static String getAttributeValue(final Attribute attribute) {

        String attributeValue = null;
        if (!attribute.getAttributeValues().isEmpty()) {
            attributeValue = attribute.getAttributeValues().get(0).getDOM().getTextContent();
        }
        return attributeValue;
    }

    /**
     * Utility method to convert a specific date to the RFC-2822 format.
     *
     * @param date the date object to be converted
     * @return the RFC 2822 string representation of the date
     */
    private static String getDateAsRFC822String(final Date date) {

        final var timeZone = TimeZone.getTimeZone("UTC");
        final var dateFormat = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z");
        dateFormat.setTimeZone(timeZone);

        return dateFormat.format(date);
    }

    /**
     * Extracts a CDA document from a RetrieveDocumentSetResponseType
     *
     * @param retrieveDocumentSetResponseType
     * @return
     */
    public static Document getCDA(final RetrieveDocumentSetResponseType retrieveDocumentSetResponseType) {

        final RetrieveDocumentSetResponseType.DocumentResponse documentResponse;

        if (retrieveDocumentSetResponseType != null && retrieveDocumentSetResponseType.getDocumentResponse() != null &&
            !retrieveDocumentSetResponseType.getDocumentResponse().isEmpty()) {

            documentResponse = retrieveDocumentSetResponseType.getDocumentResponse().get(0);
            final byte[] documentData = documentResponse.getDocument();
            return toXmlDocument(documentData);
        }
        return null;
    }

    /**
     * Extracts the HP Authentication Method from the given Assertion.
     * All AuthN methods start with "urn:oasis:names:tc:SAML:2.0:ac:classes", e.g.
     * "urn:oasis:names:tc:SAML:2.0:ac:classes:Password", so we just extract the last portion.
     *
     * @param idAssertion the Identity Assertion
     * @return a string containing the authentication method
     */
    private static String extractAuthenticationMethodFromAssertion(final Assertion idAssertion) {

        if (idAssertion == null) {
            return null;
        }
        if (!idAssertion.getAuthnStatements().isEmpty()) {
            final var authnStatement = idAssertion.getAuthnStatements().get(0);
            final String authnContextClassRef = authnStatement.getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef();
            return authnContextClassRef.substring(authnContextClassRef.lastIndexOf(':') + 1);
        } else {
            return null;
        }
    }

    /**
     * Extracts the Subject NameID from the given Assertion.
     *
     * @param idAssertion the Identity Assertion
     * @return string containing the assertion's Subject NameID
     */
    private static String extractNameIdFromAssertion(final Assertion idAssertion) {
        return idAssertion.getSubject().getNameID().getValue();
    }

    /**
     * Extracts the sending country ISO code from Issuer of the given Assertion.
     * E.g., for this issuer:
     * <saml2:Issuer NameQualifier="urn:ehdsi:assertions:hcp">urn:idp:PT:countryB</saml2:Issuer> it will extract "PT"
     *
     * @param idAssertion
     * @return String containing the assertion issuer's ISO country code
     */
    private static String extractSendingCountryIsoFromAssertion(final Assertion idAssertion) {
        if (idAssertion == null) {
            return null;
        }
        return idAssertion.getIssuer().getValue().toUpperCase().split(":")[2];
    }


    /*private static String getMessageID(final SOAPEnvelope envelope) {

        final Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getText();
        } else {
            // [Mustafa: May 8, 2012]: Should not be empty string, sch. gives error.
            return Constants.UUID_PREFIX;
        }
    }*/


    private static String getMessageID(final Message message) {

        if (!(message instanceof SoapMessage)) {
            LOGGER.warn("Message is not a SOAP message. Cannot extract MessageID.");
            return Constants.UUID_PREFIX; // Or throw an exception if appropriate
        }

        SoapMessage soapMessage = (SoapMessage) message;
        List<Header> headers = soapMessage.getHeaders();

        if (headers != null) {
            for (Header header : headers) {
                if (header.getObject() instanceof Element) {
                    Element headerElement = (Element) header.getObject();

                    // More robust approach using XPath (recommended):
                    try {
                        javax.xml.xpath.XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
                        javax.xml.xpath.XPath xpath = xpathFactory.newXPath();
                        String expression = "//wsa:MessageID"; // wsa prefix for WS-Addressing namespace
                        javax.xml.xpath.XPathExpression compiledExpression = xpath.compile(expression);

                        String messageID = compiledExpression.evaluate(headerElement);
                        if (messageID != null && !messageID.isEmpty()) {
                            return messageID;
                        }

                    } catch (javax.xml.xpath.XPathExpressionException e) {
                        LOGGER.error("Error evaluating XPath expression: " + e.getMessage());
                        // Fallback to DOM traversal (less robust):
                        QName messageIDQName = new QName("http://www.w3.org/2005/08/addressing", "MessageID");
                        org.w3c.dom.NodeList nodeList = headerElement.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "MessageID");
                        if (nodeList.getLength() > 0) {
                            return nodeList.item(0).getTextContent();
                        }
                    }

                }
            }
        }

        return Constants.UUID_PREFIX; // MessageID not found.
    }

    public static Document toXmlDocument(final byte[] content) {

        if (ArrayUtils.isEmpty(content)) {
            return null;
        }
        try {
            return XMLUtil.parseContent(content);
        } catch (final ParserConfigurationException | SAXException | IOException e) {
            return null;
        }
    }
}
