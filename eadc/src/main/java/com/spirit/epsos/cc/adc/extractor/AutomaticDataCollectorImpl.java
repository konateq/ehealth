package com.spirit.epsos.cc.adc.extractor;

import com.ibatis.common.jdbc.ScriptRunner;
import com.spirit.epsos.cc.adc.db.EadcDbConnect;
import eu.epsos.pt.eadc.util.EadcFactory;
import eu.epsos.pt.eadc.util.EadcUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.StringReader;
import java.util.TreeMap;

/**
 * Utility for extracting data from a transaction-xml-structure and inserting the results into an sql-database.
 * A detailed behavior of the extraction-process can be configured within the config.xml.
 * The supported structure for config.xml as well as the supported structure for the transaction-xml are specified by
 * xml-schemas. All files are located within the EADC_resources directory.
 * This directory must be placed within the current working directory.
 */
public class AutomaticDataCollectorImpl implements AutomaticDataCollector {

    // constant for the cda-namespace
    public static final String CDA_NAMESPACE = "urn:hl7-org:v3";

    // Path to the factory.xslt
    private static final String PATH_XSLT_FACTORY = new File(EadcUtil.getDefaultDsPath()).getAbsolutePath() + File.separator
            + "EADC_resources" + File.separator + "xslt" + File.separator + "factory.xslt";
    // Path to the config.xml
    private static final String PATH_XML_CONFIG = new File(EadcUtil.getDefaultDsPath()).getAbsolutePath() + File.separator
            + "EADC_resources" + File.separator + "config" + File.separator + "config.xml";
    private static final String SERVER_EHEALTH_MODE = "server.ehealth.mode";
    private static AutomaticDataCollectorImpl INSTANCE = null;
    private final Logger logger = LoggerFactory.getLogger(AutomaticDataCollectorImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    // Map with one intermediateTransformer per CDA-classCode
    private final TreeMap<String, EasyXsltTransformer> intermediateTransformerList;
    // DOM structure for caching the factory.xslt
    private final Document factoryXslt;
    // DOM structure for caching the config.xml
    private final Document configXml;

    /**
     * Private constructor initializing a new AutomaticDataCollector (Implementation hidden to use Singleton getInstance().
     *
     * @throws IllegalArgumentException Will be thrown, when a required resource can not be initialized
     */
    private AutomaticDataCollectorImpl() {

        try {
            System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
            intermediateTransformerList = new TreeMap<>();
            this.factoryXslt = XmlFileReader.getInstance().readXmlDocumentFromFile(AutomaticDataCollectorImpl.PATH_XSLT_FACTORY);
            this.configXml = XmlFileReader.getInstance().readXmlDocumentFromFile(AutomaticDataCollectorImpl.PATH_XML_CONFIG);

        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while creating an Instance of AutomaticDataCollector: " + e.getMessage(), e);
        }
    }

    /**
     * Initializer of class AutomaticDataCollectorImpl.
     *
     * @return an Instance of AutomaticDataCollectorImpl initialized.
     */
    public static AutomaticDataCollectorImpl getInstance() {

        if (INSTANCE == null)
            INSTANCE = new AutomaticDataCollectorImpl();

        return INSTANCE;
    }

    /**
     * Processes a transaction, extracts data according to config.xml and stores it into the database
     *
     * @param transaction    The transaction-xml-structure as specified by the XML-Schema
     * @param dataSourceName The dataSourceName of the Database
     * @throws Exception
     */
    public void processTransaction(String dataSourceName, Document transaction) throws Exception {

        logger.debug("Processing a Transaction Object as Document");
        String sqlInsertStatementList = this.extractDataAndCreateAccordingSqlInserts(transaction);
        if (!StringUtils.equals(System.getProperty(SERVER_EHEALTH_MODE), "PRODUCTION")) {
            loggerClinical.info("Insert the following sql-queries:\n'{}'", sqlInsertStatementList);
        }
        this.runSqlScript(dataSourceName, sqlInsertStatementList);
    }

    /**
     * Extracts data from a transaction and creates the according sql-insert-statements
     *
     * @param transaction The transaction xml-structure as specified by the XML-Schema
     * @return A list of sql-insert-statements
     * @throws Exception
     */
    private String extractDataAndCreateAccordingSqlInserts(Document transaction) throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("--> method extractDataAndCreateAccordingSqlInserts({})", transaction);
        }
        String processedDocumentCode;
        String processedDocumentCodeSystem;
        String processedDocumentCodeAndCodeSystemCombination;

        if (!StringUtils.equals(System.getProperty(SERVER_EHEALTH_MODE), "PRODUCTION") && loggerClinical.isDebugEnabled()) {
            loggerClinical.debug("XML Document:\n'{}'", EadcUtil.convertXMLDocumentToString(transaction));
        }

        NodeList clinicalDocumentNodeList = transaction.getElementsByTagNameNS(CDA_NAMESPACE, "ClinicalDocument");
        int numberOfCdaDocuments = clinicalDocumentNodeList.getLength();
        // Test, if the currently processed comes without a CDA-document
        if (numberOfCdaDocuments < 1) {
            processedDocumentCode = "N/A";
            processedDocumentCodeSystem = "N/A";
        } else {
            // Check the validity of the supplied CDA-content
            // And extract the classCode for using the correct classCode-customized xslt
            if (numberOfCdaDocuments > 1) {
                logger.error("Multiple CDA Documents were found within the Transaction");
                throw new Exception("Multiple CDA Documents were found within the Transaction");
            }
            Node clinicalDocumentNode = clinicalDocumentNodeList.item(0);
            if (clinicalDocumentNode.getNodeType() != Element.ELEMENT_NODE) {
                logger.error("The ClinicalDocument Node being found was not of type org.w3c.dom.Element");
                throw new Exception("The ClinicalDocument Node being found was not of type org.w3c.dom.Element");
            }
            Element clinicalDocumentElement = (Element) clinicalDocumentNode;
            NodeList codeNodeList = clinicalDocumentElement.getElementsByTagNameNS(CDA_NAMESPACE, "code");
            Node codeNode = codeNodeList.item(0);
            if (codeNode.getParentNode() != clinicalDocumentElement) {
                logger.error("The first codeNode found was not a child of ClinicalDocument");
                throw new Exception("The first codeNode found was not a child of ClinicalDocument");
            }
            if (codeNode.getNodeType() != Element.ELEMENT_NODE) {
                logger.error("The code node being found was not of type org.w3c.dom.Element");
                throw new Exception("The code node being found was not of type org.w3c.dom.Element");
            }

            Element codeElement = (Element) codeNode;
            // Extracting the document's code
            processedDocumentCode = codeElement.getAttribute("code");
            if (processedDocumentCode == null) {
                logger.error("Unable to read the code Attribute");
                throw new Exception("Unable to read the code Attribute");
            }
            if (processedDocumentCode.length() == 0) {
                logger.error("The code Attribute was either not specified or it was the empty string");
                throw new Exception("The code Attribute was either not specified or it was the empty string");
            }
            logger.debug("code: '{}", processedDocumentCode);
            // Extracting the document's codeSystem
            processedDocumentCodeSystem = codeElement.getAttribute("codeSystem");
            if (processedDocumentCodeSystem == null) {
                logger.error("Unable to read the codeSystem Attribute");
                throw new Exception("Unable to read the codeSystem Attribute");
            }
            if (processedDocumentCodeSystem.length() == 0) {
                logger.error("The codeSystemAttribute was either not specified or it was the empty string");
                throw new Exception("The codeSystemAttribute was either not specified or it was the empty string");
            }
        }
        processedDocumentCodeAndCodeSystemCombination = processedDocumentCode + "\"" + processedDocumentCodeSystem;
        // Guarantee, that the cachedIntermediaTransformerList is being initialized only once.
        // And ensure, that a cachedIntermediaTransformer is only created/added once to the cachedIntermediaTransformerList
        EasyXsltTransformer currentTransformer = this.intermediateTransformerList.get(processedDocumentCodeAndCodeSystemCombination);
        Document result;
        if (currentTransformer == null) {
            try {
                synchronized (this.intermediateTransformerList) {
                    currentTransformer = this.intermediateTransformerList.get(processedDocumentCodeAndCodeSystemCombination);
                    if (currentTransformer == null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Creating the XSLT-Transformer for code: '{}' and codeSystem: '{}'",
                                    processedDocumentCode, processedDocumentCodeSystem);
                        }
                        ((Element) this.factoryXslt.getElementsByTagNameNS("http://www.w3.org/1999/XSL/Transform",
                                "variable").item(0)).setAttribute("select", "'" + processedDocumentCode + "'");
                        ((Element) this.factoryXslt.getElementsByTagNameNS("http://www.w3.org/1999/XSL/Transform",
                                "variable").item(1)).setAttribute("select", "'" + processedDocumentCodeSystem + "'");

                        currentTransformer = new EasyXsltTransformer(new EasyXsltTransformer(this.factoryXslt).transform(this.configXml));
                        this.intermediateTransformerList.put(processedDocumentCodeAndCodeSystemCombination, currentTransformer);
                    }
                }
            } catch (Exception exception) {
                throw new Exception("Unable to initialize the customized XSLT for processedDocumentCode:" + processedDocumentCode
                        + " and processedDocumentCodeSystem:" + processedDocumentCodeSystem, exception);
            }
            logger.debug("Current intermediaTransformer retrieved successfully");
        }
        // Perform the data-extraction
        try {
            result = currentTransformer.transform(transaction);
        } catch (Exception exception) {
            throw new Exception("Error when transforming a document", exception);
        }
        // As the XSLT returns plain text, the content is found within the result's root-node which is a text-node.
        return result.getFirstChild().getTextContent();
    }

    /**
     * Run the provided SQL-script by using the provided dataSourceName
     *
     * @param sqlScript      The sql-script being executed. Must be a list of sql-statements being terminated with ";".
     * @param dataSourceName The dataSource Identifier being used to connect to the database. This string usually refers
     *                       to a database-specification in a datasource xml-file.
     * @throws Exception
     */
    private void runSqlScript(String dataSourceName, String sqlScript) throws Exception {

        StopWatch watch = new StopWatch();
        watch.start();
        EadcDbConnect sqlConnection = null;

        try (StringReader stringReader = new StringReader(sqlScript)) {

            sqlConnection = EadcFactory.INSTANCE.createEadcDbConnect(dataSourceName);
            ScriptRunner objScriptRunner = new ScriptRunner(sqlConnection.getConnection(), false, true);
            objScriptRunner.setLogWriter(null);
            objScriptRunner.setErrorLogWriter(null);
            objScriptRunner.runScript(stringReader);

        } catch (Exception exception) {
            throw new Exception("The following error occurred during an SQL operation:", exception);
        } finally {
            if (sqlConnection != null) {
                sqlConnection.closeConnection();
            }
            watch.stop();
            if (logger.isDebugEnabled()) {
                logger.debug("[EADC] SQL script executed in: '{}ms'", watch.getTime());
            }
        }
    }
}
