package eu.europa.ec.sante.openncp.core.common.ihe.eadc;

import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.datamodel.ObjectFactory;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.datamodel.Transaction;
import eu.europa.ec.sante.openncp.core.common.ihe.eadc.datamodel.TransactionInfo;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * The purpose of this class is to help the eADC invocation process, by providing utility methods to invoke the service.
 */
public class EadcUtil {

    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final Logger LOGGER = LoggerFactory.getLogger(EadcUtil.class);
    private static final String SERVER_EHEALTH_MODE = "server.ehealth.mode";
    private static String defaultDsPath = null;

    /**
     * This utility method will invoke eADC for a given transaction. You will need to provide the Axis Request and
     * Response message context, together with optional document, a Transaction Info object containing transaction
     * details and a selected data source.
     *
     * @param reqMsgContext the Servlet request message context
     * @param rspMsgContext the Servlet response message context
     * @param cdaDocument   the optional CDA document, leave as null if not necessary
     * @param transInfo     the Transaction Info object
     * @throws Exception
     */
    public static void invokeEadcFailure(MessageContext reqMsgContext, MessageContext rspMsgContext, Document cdaDocument,
                                         TransactionInfo transInfo, EadcEntry.DsTypes datasource, String errorDescription) throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();
        Document reqEnv;
        Document respEnv;
        Transaction transaction;
        Document transactionDocument;

        reqEnv = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        respEnv = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        if (reqMsgContext != null && reqMsgContext.getEnvelope() != null) {
            reqEnv.adoptNode(XMLUtils.toDOM(reqMsgContext.getEnvelope()));
        }
        if (rspMsgContext != null && rspMsgContext.getEnvelope() != null) {
            respEnv.adoptNode(XMLUtils.toDOM(rspMsgContext.getEnvelope()));
        }

        transaction = buildTransaction(transInfo);

        if (cdaDocument != null) {
            transactionDocument = TransactionHelper.insertCdaInTransaction(transaction, cdaDocument);
        } else {
            transactionDocument = TransactionHelper.convertTransaction(transaction);
        }
        if (!StringUtils.equals(System.getProperty(SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name()) && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("[EADC] XML Transaction:\n'{}'", xmlToString(transactionDocument));
        }
        EadcEntry eadcEntry = EadcFactory.INSTANCE.getEntry(datasource.toString(), transactionDocument, reqEnv, respEnv);
        EadcReceiverImpl eadcReceiver = new EadcReceiverImpl();
        eadcReceiver.processFailure(eadcEntry, errorDescription);

        watch.stop();
        LOGGER.info("[EADC] Transaction Failure Processing executed in: '{}ms'", watch.getTime());
    }

    /**
     * This utility method will invoke eADC for a given transaction. You will need to provide the Axis Request and
     * Response message context, together with optional document, a Transaction Info object containing transaction
     * details and a selected data source.
     *
     * @param reqMsgContext the Servlet request message context
     * @param rspMsgContext the Servlet response message context
     * @param cdaDocument   the optional CDA document, leave as null if not necessary
     * @param transInfo     the Transaction Info object
     * @throws Exception
     */
    public static void invokeEadc(MessageContext reqMsgContext, MessageContext rspMsgContext, Document cdaDocument,
                                  TransactionInfo transInfo, EadcEntry.DsTypes datasource) throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();
        Document reqEnv;
        Document respEnv;
        Transaction transaction;
        Document transactionDocument;

        reqEnv = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        respEnv = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        if (reqMsgContext != null && reqMsgContext.getEnvelope() != null) {
            reqEnv.adoptNode(XMLUtils.toDOM(reqMsgContext.getEnvelope()));
        }
        if (rspMsgContext != null && rspMsgContext.getEnvelope() != null) {
            respEnv.adoptNode(XMLUtils.toDOM(rspMsgContext.getEnvelope()));
        }

        transaction = buildTransaction(transInfo);

        if (cdaDocument != null) {
            transactionDocument = TransactionHelper.insertCdaInTransaction(transaction, cdaDocument);
        } else {
            transactionDocument = TransactionHelper.convertTransaction(transaction);
        }
        if (!StringUtils.equals(System.getProperty(SERVER_EHEALTH_MODE), ServerMode.PRODUCTION.name()) && LOGGER_CLINICAL.isDebugEnabled()) {
            LOGGER_CLINICAL.debug("[EADC] XML Transaction:\n'{}'", xmlToString(transactionDocument));
        }
        EadcEntry eadcEntry = EadcFactory.INSTANCE.getEntry(datasource.toString(), transactionDocument, reqEnv, respEnv);
        EadcReceiverImpl eadcReceiver = new EadcReceiverImpl();
        eadcReceiver.process(eadcEntry);

        watch.stop();
        LOGGER.info("[EADC] Transaction Processing executed in: '{}ms'", watch.getTime());
    }

    /**
     * Fills a Transaction object with a Transaction Info element.
     *
     * @param transactionInfo
     * @return
     */
    private static Transaction buildTransaction(TransactionInfo transactionInfo) {

        Transaction result = new ObjectFactory().createComplexTypeTransaction();
        result.setTransactionInfo(transactionInfo);
        return result;
    }

    /**
     * @return the defaultDsPath
     */
    public static String getDefaultDsPath() {

        if (defaultDsPath == null) {
            defaultDsPath = getApplicationRootPath();
        }
        return defaultDsPath;
    }

    /**
     * Helper method to return the system epSOS Properties Path
     *
     * @return the full OpenNCP Props Path currently used by the system.
     */
    public static String getApplicationRootPath() {

        String path = System.getenv("EPSOS_PROPS_PATH");

        if (path == null) {
            path = System.getProperty("EPSOS_PROPS_PATH");
            if (path == null) {
                LOGGER.error("EPSOS_PROPS_PATH not found!");
                return null;
            }
        }
        return path;
    }

    public static String xmlToString(Node node) {

        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerException e) {
            LOGGER.error("TransformerException: '{}'", e.getMessage());
        }
        return StringUtils.EMPTY;
    }

    /**
     * convertXMLDocumentToString
     *
     * @param xmlDocument
     * @return String
     * @throws Exception
     */
    public static String convertXMLDocumentToString(Document xmlDocument) throws Exception {

        if (xmlDocument == null) {
            LOGGER.warn("XML Document is NULL. Can't convert XML Document to String.");
            return "";
        }
        // set the TransformFactory to use the Saxon TransformerFactoryImpl method
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        StringWriter objStrWriter = new StringWriter();
        StreamResult objStreamResult = new StreamResult(objStrWriter);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer objTransformer = transformerFactory.newTransformer();
        objTransformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        objTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
        objTransformer.setOutputProperty(OutputKeys.METHOD, "xml");
        objTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        objTransformer.transform(new DOMSource(xmlDocument.getDocumentElement()), objStreamResult);
        return objStreamResult.getWriter().toString();
    }

    /**
     * This enum represents the transaction directions
     */
    public enum Direction {

        INBOUND("INBOUND"), OUTBOUND("OUTBOUND");
        private final String value;

        Direction(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
