package com.spirit.epsos.cc.adc.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Utility-class for reading a file into a DOM-structure This class uses the singleton pattern for ensuring,
 * that cached utilized Objects are only initialized once.
 */
public class XmlFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlFileReader.class);
    // Singleton-object
    private static XmlFileReader singletonInstance = null;
    private final DocumentBuilder objDocBuilder;

    /**
     * This constructor is private to ensure, that it is only called by the getInstance() method.
     * It initializes the static DocumentBuilderFactory object and the static DocumentBuilderFactory object.
     *
     * @throws Exception
     */
    private XmlFileReader() throws Exception {

        LOGGER.info("Trying to initialize the XmlFileReader singleton object");
        DocumentBuilderFactory objDocBuilderFactory = DocumentBuilderFactory.newInstance();
        objDocBuilderFactory.setNamespaceAware(true);
        objDocBuilderFactory.setValidating(false);
        try {
            this.objDocBuilder = objDocBuilderFactory.newDocumentBuilder();
            LOGGER.info("XmlFileReader singleton object initialized successfully");
        } catch (ParserConfigurationException e) {
            throw new Exception("An Error occurred during setting up an XML DocumentBuilder Object", e);
        }
    }

    /**
     * Getter-method for retrieving the singleton-object
     *
     * @return
     * @throws Exception
     */
    public static synchronized XmlFileReader getInstance() throws Exception {

        if (XmlFileReader.singletonInstance == null) {
            try {
                XmlFileReader.singletonInstance = new XmlFileReader();
            } catch (Exception exception) {
                throw new Exception("Unable to initialize the singleton for class " + XmlFileReader.class.getCanonicalName(), exception);
            }
        }
        LOGGER.debug("Instance of XmlFileReader successfully retrieved");
        return XmlFileReader.singletonInstance;
    }

    /**
     * Reds an xml-file and returns the content as a DOM-structure
     *
     * @param strFilePath The path to the file to be read
     * @return The xml-file's content as DOM-structure
     * @throws Exception
     */
    public Document readXmlDocumentFromFile(String strFilePath) throws Exception {

        LOGGER.debug("Trying to read the following File into a DOM structure: '{}'", strFilePath);
        // Preparing the FileInputStream
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(new File(strFilePath));
        } catch (FileNotFoundException fileNotFoundException) {
            throw new Exception("The XML file to parse was not found or can not be opened for reading: " + strFilePath, fileNotFoundException);
        }
        LOGGER.debug("FileInputStream initialized successfully");
        // Preparing the xmlDocument
        Document xmlDocument = null;
        try {
            xmlDocument = this.objDocBuilder.parse(fileInputStream);
        } catch (SAXException saxException) {
            throw new Exception("Error, when parsing the following XML document: " + strFilePath, saxException);
        } catch (IOException e) {
            LOGGER.warn("IOException: '{}'", e.getMessage(), e);
        }
        // Closing the fileInputStream
        try {
            fileInputStream.close();
        } catch (IOException ioException) {
            LOGGER.warn("Unable to close the fileInputStream for file:" + strFilePath, ioException);
        }
        // Post-checking the result's validity
        if (xmlDocument == null) {
            throw new Exception("The resulting XML Document from reading " + strFilePath + " was null");
        }
        LOGGER.debug("XML Document successfully parsed from File");
        return xmlDocument;
    }
}
