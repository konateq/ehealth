package eu.europa.ec.sante.ehdsi.openncp.tm.util;

import eu.europa.ec.sante.ehdsi.openncp.tm.config.TMConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Schematron validator. Based on ISO schematron implementation.
 *
 * @author Frantisek Rudik
 * @author Organization: Posam
 * @author mail:frantisek.rudik@posam.sk
 * @version 1.4, 2010, 20 October
 */
@Service
public class SchematronValidator implements TMConstants {

    private static final Logger log = LoggerFactory.getLogger(SchematronValidator.class);

    private static final String phase1Template = "iso_dsdl_include.xsl";
    private static final String phase2Template = "iso_abstract_expand.xsl";
    private static final String phase3Template = "iso_svrl_for_xslt2.xsl";

    private static final String SVRL_FAILED_ASSERT_XPATH = "//svrl:failed-assert";
    private static final String SCH_TEMP_DIR = System.getenv("EPSOS_PROPS_PATH") + "validation" +
            File.separatorChar + "schematron";

    private static SchematronValidator instance = null;
    private String xslDirectoryPath;
    private HashMap<String, String> friendlyType;
    private HashMap<String, String> pivotType;

    @Autowired
    private TMConfiguration config;

    private SchematronValidator() {
    }

    public static SchematronValidator getInstance() {

        if (instance == null) {
            instance = new SchematronValidator();
        }
        return instance;
    }

    public static SchematronResult validate(File inputSchemeFile, Document inputXmlDocument) {

        return getInstance().doValidate(inputSchemeFile, inputXmlDocument);
    }

    private SchematronResult doValidate(File inputSchemeFile, Document inputXmlDocument) {

        SchematronResult result = new SchematronResult();
        try {
            log.info("...Schematron validation BEGIN");
            log.info("Schematron file: '{}'", inputSchemeFile.getAbsolutePath());
            ensureTempExist();
            System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer;
            Source in;
            Result out;

            String inputSchemeName = inputSchemeFile.getName().trim();
            long time = inputSchemeFile.lastModified();

            String phase1Out = inputSchemeName + "." + time + ".phase1.xml";
            String phase2Out = inputSchemeName + "." + time + ".phase2.xml";
            String phase3Out = inputSchemeName + "." + time + ".phase3.xml";

            File phase1OutFile = getSchema(phase1Out);
            File phase2OutFile = getSchema(phase2Out);
            File phase3OutFile = getSchema(phase3Out);

            if (!phase3OutFile.exists()) {
                // if final Schematron xsl does not exists delete all old files
                File[] files = new File(SCH_TEMP_DIR).listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().startsWith(inputSchemeName)) {
                            boolean deleted = file.delete();
                            if (log.isDebugEnabled()) {
                                log.debug("File: '{}' has been deleted: '{}'", file.getName(), deleted);
                            }
                        }
                    }
                }
            }

            //phase 1
            if (!phase1OutFile.exists()) {
                transformer = factory.newTransformer(new StreamSource(getXsl(phase1Template)));
                in = new StreamSource(inputSchemeFile);
                out = new StreamResult(phase1OutFile);
                transformer.transform(in, out);
            }

            //phase2
            if (!phase2OutFile.exists()) {
                transformer = factory.newTransformer(new StreamSource(getXsl(phase2Template)));
                in = new StreamSource(phase1OutFile);
                out = new StreamResult(phase2OutFile);
                transformer.transform(in, out);
            }

            //phase3
            if (!phase3OutFile.exists()) {
                transformer = factory.newTransformer(new StreamSource(getXsl(phase3Template)));
                in = new StreamSource(phase2OutFile);
                out = new StreamResult(phase3OutFile);
                transformer.transform(in, out);
            }
            //phase4
            transformer = factory.newTransformer(new StreamSource(phase3OutFile));
            in = new DOMSource(inputXmlDocument);
            out = new DOMResult();
            transformer.transform(in, out);

            DOMResult domOut = (DOMResult) out;
            NodeList errorNodes = evaluateResult(domOut);
            result.setErrors(errorNodes);
            result.setValid(errorNodes.getLength() == 0);
            log.info("...Schematron validation END - result is valid ? '{}'", result.isValid());
        } catch (XPathException e) {
            log.error("XPathException: '{}'", e.getMessage(), e);
        } catch (TransformerFactoryConfigurationError e) {
            log.error("TransformerFactoryConfigurationError: '{}'", e.getMessage(), e);
        } catch (TransformerException e) {
            log.error("TransformerException: '{}'", e.getMessage(), e);
        }
        return result;
    }

    private void ensureTempExist() {

        File schemaTemp = new File(SCH_TEMP_DIR);
        if (!schemaTemp.exists()) {
            boolean created = schemaTemp.mkdirs();
            if (log.isDebugEnabled()) {
                log.debug("Directory: '{}' has been created: '{}'", SCH_TEMP_DIR, created);
            }
        }
    }

    private NodeList evaluateResult(final DOMResult domOut) throws XPathExpressionException {

        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }

            public String getPrefix(String namespaceURI) {
                return domOut.getNode().lookupPrefix(namespaceURI);
            }

            public String getNamespaceURI(String prefix) {
                return domOut.getNode().lookupNamespaceURI(prefix);
            }
        });
        XPathExpression exp = xpath.compile(SVRL_FAILED_ASSERT_XPATH);
        return (NodeList) exp.evaluate((domOut).getNode(), XPathConstants.NODESET);
    }

    private File getXsl(String name) {
        return new File(getXslDirectoryPath() + File.separator + name);
    }

    private File getSchema(String name) {
        return new File(SCH_TEMP_DIR + File.separator + name);
    }

    public String getXslDirectoryPath() {
        return xslDirectoryPath;
    }

    public void setXslDirectoryPath(String xslDirectoryPath) {
        this.xslDirectoryPath = xslDirectoryPath;
    }

    public Map<String, String> getFriendlyType() {
        return friendlyType;
    }

    public Map<String, String> getPivotType() {
        return pivotType;
    }

    public void setConfig(TMConfiguration config) {
        this.config = config;
    }

    public void afterPropertiesSet() throws Exception {

        friendlyType = new HashMap<>();
        friendlyType.put(PATIENT_SUMMARY3, config.getPatientSummarySchematronFriendlyPath());
        friendlyType.put(EPRESCRIPTION3, config.getePrescriptionSchematronFriendlyPath());
        friendlyType.put(EDISPENSATION3, config.geteDispensationSchematronFriendlyPath());
        friendlyType.put(HCER3, config.getHcerSchematronFriendlyPath());
        friendlyType.put(MRO3, config.getMroSchematronFriendlyPath());
        friendlyType.put(SCANNED1, config.getScannedDocFriendlyPath());

        pivotType = new HashMap<>();
        pivotType.put(PATIENT_SUMMARY3, config.getPatientSummarySchematronPivotPath());
        pivotType.put(EPRESCRIPTION3, config.getePrescriptionSchematronPivotPath());
        pivotType.put(EDISPENSATION3, config.geteDispensationSchematronPivotPath());
        pivotType.put(HCER3, config.getHcerSchematronPivotPath());
        pivotType.put(MRO3, config.getMroSchematronPivotPath());
        pivotType.put(SCANNED3, config.getScannedDocPivotPath());
    }
}
