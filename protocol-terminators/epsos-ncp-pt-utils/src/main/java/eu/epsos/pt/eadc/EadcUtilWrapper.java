package eu.epsos.pt.eadc;

import com.spirit.epsos.cc.adc.EadcEntry;
import ee.affecto.epsos.util.EventLogClientUtil;
import eu.epsos.pt.eadc.datamodel.ObjectFactory;
import eu.epsos.pt.eadc.datamodel.TransactionInfo;
import eu.epsos.pt.eadc.util.EadcUtil;
import eu.epsos.pt.eadc.util.EadcUtil.Direction;
import eu.europa.ec.sante.ehdsi.eadc.ServiceType;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.util.XMLUtils;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthnStatement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import tr.com.srdc.epsos.securityman.helper.Helper;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.OidUtil;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.UUID;

/**
 * This class wraps the EADC invocation. As it gathers several aspects required to its proper usage, such as
 * the compilation and preparation of transaction details.
 *
 * @author Marcelo Fonseca<code> - marcelo.fonseca@iuz.pt</code>
 */
public class EadcUtilWrapper {

    private EadcUtilWrapper() {
    }

    /**
     * Main EADC Wrapper operation. It receives as input all the required information to successfully fill a transaction object.
     *
     * @param reqMsgCtx   the request Servlet Message Context
     * @param respMsgCtx  the response Servlet Message Context
     * @param CDA         the (optional) CDA document
     * @param idAssertion the Identity Assertion
     * @param startTime   the transaction start time
     * @param endTime     the transaction end time
     * @param rcvgIso     the country A ISO Code
     * @throws Exception
     */
    public static void invokeEadc(MessageContext reqMsgCtx, MessageContext respMsgCtx, ServiceClient serviceClient,
                                  Document cda, Date startTime, Date endTime, String rcvgIso, EadcEntry.DsTypes dsType,
                                  Direction direction, ServiceType serviceType) throws Exception {

        EadcUtil.invokeEadc(reqMsgCtx, respMsgCtx, cda, buildTransactionInfo(reqMsgCtx, respMsgCtx, serviceClient,
                direction, startTime, endTime, rcvgIso, serviceType), dsType);
    }

    /**
     * Builds a Transaction Info object based on a set of information.
     *
     * @param reqMsgContext the request Servlet Message Context
     * @param rspMsgContext the response Servlet Message Context
     * @param direction     the request direction, INBOUND or OUTBOUND
     * @param idAssertion   the Identity Assertion
     * @param startTime     the transaction start time
     * @param endTime       the transaction end time
     * @param countryAcode  the country A ISO Code
     * @return the filled Transaction Info object
     */
    private static TransactionInfo buildTransactionInfo(MessageContext reqMsgContext, MessageContext rspMsgContext,
                                                        ServiceClient serviceClient, Direction direction, Date startTime,
                                                        Date endTime, String countryAcode, ServiceType serviceType) throws Exception {

        TransactionInfo result = new ObjectFactory().createComplexTypeTransactionInfo();
        result.setAuthenticationLevel(reqMsgContext != null ? extractAuthenticationMethodFromAssertion(getAssertion(reqMsgContext)) : null);
        result.setDirection(direction != null ? direction.toString() : null);
        result.setStartTime(startTime != null ? getDateAsRFC822String(startTime) : null);
        result.setEndTime(endTime != null ? getDateAsRFC822String(endTime) : null);
        result.setDuration(endTime != null && startTime != null ? String.valueOf(endTime.getTime() - startTime.getTime()) : null);

        result.setHomeAddress(EventLogClientUtil.getLocalIpAddress());
        String sndIso = reqMsgContext != null ? extractSendingCountryIsoFromAssertion(getAssertion(reqMsgContext)) : null;
        result.setSndISO(sndIso);
        result.setSndNCPOID(sndIso != null ? OidUtil.getHomeCommunityId(sndIso.toLowerCase()) : null);

        if (reqMsgContext != null && reqMsgContext.getOptions() != null && reqMsgContext.getOptions().getFrom() != null && reqMsgContext.getOptions().getFrom().getAddress() != null) {
            result.setHomeHost(reqMsgContext.getOptions().getFrom().getAddress());
        }

        /* we cannot get the MessageID from the reqMsgContext, it returns a wrong one. 
        Probably related to how the Axis2 engine sets the MessageID, similar issues 
        were faced during the Evidence Emitter refactoring. Plus, for the XCA Retrieve request messages, 
        when comparing this MessageID with the one from the message itself, be sure to compare it with
        the correct WSA headers, there are duplicated ones, although belonging to different
        namespaces (the correct one is xmlns = http://www.w3.org/2005/08/addressing) (EHNCP-1141)*/
        result.setSndMsgID(reqMsgContext != null ? getMessageID(reqMsgContext.getEnvelope()) : null);
        result.setHomeHCID("");
        result.setHomeISO(Constants.COUNTRY_CODE);
        result.setHomeNCPOID(Constants.HOME_COMM_ID);

        //  TODO: Clarify values for this field according specifications and GDPR, current value set to "N/A GDPR"
        result.setHumanRequestor("N/A GDPR");
        result.setUserId("N/A GDPR");
        //result.setHumanRequestor(reqMsgContext != null ? extractNameIdFromAssertion(getAssertion(reqMsgContext)) : null);
        //result.setUserId(reqMsgContext != null ? extractAssertionInfo(getAssertion(reqMsgContext), "urn:oasis:names:tc:xacml:1.0:subject:subject-id") : null);

        result.setPOC(reqMsgContext != null ?
                extractAssertionInfo(getAssertion(reqMsgContext), "urn:oasis:names:tc:xspa:1.0:environment:locality") + " (" +
                        extractAssertionInfo(getAssertion(reqMsgContext), "urn:epsos:names:wp3.4:subject:healthcare-facility-type") + ")" : null);
        result.setPOCID(reqMsgContext != null ? extractAssertionInfo(getAssertion(reqMsgContext), "urn:oasis:names:tc:xspa:1.0:subject:organization-id") : null);

        result.setReceivingISO(countryAcode);
        result.setReceivingNCPOID(countryAcode != null ? OidUtil.getHomeCommunityId(countryAcode.toLowerCase()) : null);
        if (serviceClient != null && serviceClient.getOptions() != null && serviceClient.getOptions().getTo() != null && serviceClient.getOptions().getTo().getAddress() != null) {
            result.setReceivingHost(serviceClient.getOptions().getTo().getAddress());
            result.setReceivingAddr(EventLogClientUtil.getServerIpAddress(serviceClient.getOptions().getTo().getAddress()));
        }
        if (reqMsgContext != null && reqMsgContext.getOptions() != null && reqMsgContext.getOptions().getAction() != null) {
            result.setRequestAction(reqMsgContext.getOptions().getAction());
        }
        if (rspMsgContext != null && rspMsgContext.getOptions() != null && rspMsgContext.getOptions().getAction() != null) {
            result.setResponseAction(rspMsgContext.getOptions().getAction());
        }
        if (reqMsgContext != null && reqMsgContext.getOperationContext() != null && reqMsgContext.getOperationContext().getServiceName() != null) {
            result.setServiceName(reqMsgContext.getOperationContext().getServiceName());
        }
        result.setReceivingMsgID(rspMsgContext != null ? rspMsgContext.getOptions().getMessageId() : null);
        result.setServiceType(serviceType.getDescription());
        result.setTransactionCounter("");
        result.setTransactionPK(UUID.randomUUID().toString());
        return result;
    }

    /**
     * Extracts and assertion from a given message context
     *
     * @param requestMessageContext
     * @return
     * @throws Exception
     */
    private static Assertion getAssertion(MessageContext requestMessageContext) throws Exception {

        SOAPHeader soapHeader = requestMessageContext.getEnvelope().getHeader();
        Element soapHeaderElement = XMLUtils.toDOM(soapHeader);
        return Helper.getHCPAssertion(soapHeaderElement);
    }

    /**
     * Assertion utility method. Will extract information of a specific assertion, based on a given expression.
     *
     * @param idAssertion the Identity Assertion
     * @param expression  the expression to evaluate
     * @return a string representing the information presented on the specified node
     */
    private static String extractAssertionInfo(Assertion idAssertion, String expression) {

        for (AttributeStatement attributeStatement : idAssertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
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
    private static String getAttributeValue(Attribute attribute) {

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
    private static String getDateAsRFC822String(Date date) {

        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z");
        dateFormat.setTimeZone(timeZone);

        // Date format according to RFC-2822 specifications.
        return dateFormat.format(date);
    }

    /**
     * Extracts a CDA document from a RetrieveDocumentSetResponseType
     *
     * @param retrieveDocumentSetResponseType
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static Document getCDA(ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType retrieveDocumentSetResponseType) throws ParserConfigurationException, SAXException, IOException {

        ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType.DocumentResponse documentResponse;

        if (retrieveDocumentSetResponseType != null && retrieveDocumentSetResponseType.getDocumentResponse() != null && !retrieveDocumentSetResponseType.getDocumentResponse().isEmpty()) {
            documentResponse = retrieveDocumentSetResponseType.getDocumentResponse().get(0);

            byte[] documentData = documentResponse.getDocument();
            return convertToDomDocument(documentData);
        }
        return null;
    }

    /**
     * Converts a set of bytes into a Document
     *
     * @param documentData
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static Document convertToDomDocument(byte[] documentData) throws ParserConfigurationException, SAXException, IOException {

        Document xmlDocument;
        String xmlStr = new String(documentData, StandardCharsets.UTF_8);
        xmlDocument = XMLUtil.parseContent(xmlStr);
        return xmlDocument;
    }

    /**
     * Extracts the HP Authentication Method from the given Assertion.
     * All AuthN methods start with "urn:oasis:names:tc:SAML:2.0:ac:classes", e.g.
     * "urn:oasis:names:tc:SAML:2.0:ac:classes:Password", so we just extract the last portion.
     *
     * @param idAssertion the Identity Assertion
     * @return a string containing the authentication method
     */
    private static String extractAuthenticationMethodFromAssertion(Assertion idAssertion) {

        if (!idAssertion.getAuthnStatements().isEmpty()) {
            AuthnStatement authnStatement = idAssertion.getAuthnStatements().get(0);
            String authnContextClassRef = authnStatement.getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef();
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
    private static String extractNameIdFromAssertion(Assertion idAssertion) {
        return idAssertion.getSubject().getNameID().getValue();
    }

    /**
     * Extracts the sending country ISO code from Issuer of the given Assertion.
     * E.g., for this issuer:
     * <saml2:Issuer NameQualifier="urn:epsos:wp34:assertions">urn:idp:PT:countryB</saml2:Issuer>
     * it will extract "PT"
     * *
     *
     * @param idAssertion
     * @return string containing the assertion issuer's ISO country code
     */
    private static String extractSendingCountryIsoFromAssertion(Assertion idAssertion) {
        return idAssertion.getIssuer().getValue().split(":")[2];
    }

    /**
     * Copied from *_ServiceMessageReceiverInOut.java
     * It returns the MessageID directly from the SOAP Envelope.
     *
     * @param envelope The SOAP envelope
     * @return The message ID
     */
    private static String getMessageID(SOAPEnvelope envelope) {

        Iterator<OMElement> it = envelope.getHeader().getChildrenWithName(
                new QName("http://www.w3.org/2005/08/addressing", "MessageID"));
        if (it.hasNext()) {
            return it.next().getText();
        } else {
            // [Mustafa: May 8, 2012]: Should not be empty string, sch. giveserror.
            return Constants.UUID_PREFIX;
        }
    }
}
