package com.spirit.epsos.cc.adc.extractor;

import eu.epsos.pt.eadc.util.EadcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

/**
 * Wrapper-utility for setting up and transforming an xml-document by using a provided xslt.
 * By initializing an object of this class a transformer object is being cached for improving performance.
 */
public class EasyXsltTransformer {

    private static final TransformerFactory objTransformerFact = TransformerFactory.newInstance();
    private final Logger logger = LoggerFactory.getLogger(EasyXsltTransformer.class);
    private Document xsltDocument = null;
    private Transformer transformer = null;

    /**
     * Hide the default-constructor to force using the parameterized constructor.
     */
    private EasyXsltTransformer() {
    }

    /**
     * Constructor for creating an EasyXsltTransformer-object.
     *
     * @param xsltDocument
     * @throws Exception
     */
    public EasyXsltTransformer(Document xsltDocument) throws Exception {

        this();
        if (xsltDocument == null) {
            logger.error("The supplied XSLT-Document was null");
            throw new Exception("The supplied XSLT-Document was null");
        }
        DOMSource xsltDomSource = new DOMSource(xsltDocument);
        try {
            this.xsltDocument = xsltDocument;
            this.transformer = EasyXsltTransformer.objTransformerFact.newTransformer(xsltDomSource);
            logger.info("An XSLT-Transformer object was initialized successfully");
        } catch (TransformerConfigurationException transformerConfigurationException) {
            logger.error("TransformerConfigurationException: Used XSLT Document:\n'{}'", EadcUtil.convertXMLDocumentToString(xsltDocument));
            throw new Exception("An error occurred, when loading the XSLT-Document", transformerConfigurationException);
        }
    }

    /**
     * SYNCHRONIZED: The transformer object MUST NOT be used by multiple threads at the same time (javax.xml.transform.Transformer)
     *
     * @param xmlSourceNode
     * @return
     * @throws Exception
     */
    public synchronized Document transform(Node xmlSourceNode) throws Exception {

        logger.debug("Entering synchronized part");
        DOMResult objDomResult = new DOMResult(null);
        try {
            this.transformer.transform(new DOMSource(xmlSourceNode), objDomResult);
        } catch (TransformerException transformerException) {
            logger.error("TransformerException : Documents leading to that exception are:\nUsed XML-Document:\n'{}'\nUsed XSLT-Document:\n'{}'",
                    EadcUtil.convertXMLDocumentToString((Document) xmlSourceNode),
                    EadcUtil.convertXMLDocumentToString(this.xsltDocument));
            logger.debug("Leaving synchronous part");
            throw new Exception("An error occurred during an XSLT-Transformation", transformerException);
        }
        logger.debug("Leaving synchronized part");
        return (Document) objDomResult.getNode();
    }
}
