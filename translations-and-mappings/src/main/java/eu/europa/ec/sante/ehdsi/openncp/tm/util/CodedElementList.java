package eu.europa.ec.sante.ehdsi.openncp.tm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class CodedElementList implements InitializingBean, TMConstants {

    private static final String XML_CODED_ELEMENT_LIST = "coded_element_list_ehdsi.xml";

    // Collection of Elements contained in Patient Summary CDA document (level 3)
    private static Collection<CodedElementListItem> patientSummaryl3;
    // Collection of Elements contained in header of Patient Summary CDA document (level 1 with pdf)
    private static Collection<CodedElementListItem> patientSummaryl1;
    // Collection of Elements contained in header of ePrescription CDA document (level 1 with pdf)
    private static Collection<CodedElementListItem> ePrescriptionl1;
    // Collection of Elements contained in ePrescription CDA document (level 3)
    private static Collection<CodedElementListItem> ePrescriptionl3;
    // Collection of Elements contained in header of eDispensation CDA document (level 1 with pdf)
    private static Collection<CodedElementListItem> eDispensationl1;
    // Collection of Elements contained in eDispensation CDA document (level 3)
    private static Collection<CodedElementListItem> eDispensationl3;
    // Collections of CodedElementListItem for CDA document types
    private static Collection<CodedElementListItem> hcerl3;
    private static Collection<CodedElementListItem> hcerl1;
    private static Collection<CodedElementListItem> mrol3;
    private static Collection<CodedElementListItem> mrol1;
    private static CodedElementList instance = null;
    private static HashMap<String, Collection<CodedElementListItem>> hmDocAndLists = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(CodedElementList.class);
    private boolean isInitialized = false;
    // Path to xml file with Coded Element List
    private String codedElementListPath;
    // True means configurable Element identification is used; false means no
    private boolean configurableElementIdentification;
    // True means that the coded element list will be overridden by the user.
    private boolean codedElementListOverride = false;

    private CodedElementList() {
    }

    public static CodedElementList getInstance() {
        if (instance == null) {
            instance = new CodedElementList();
        }
        return instance;
    }

    public boolean isCodedElementListOverride() {
        return codedElementListOverride;
    }

    public void setCodedElementListOverride(boolean codedElementListOverride) {
        this.codedElementListOverride = codedElementListOverride;
    }

    public boolean isConfigurableElementIdentification() {
        return configurableElementIdentification;
    }

    public void setConfigurableElementIdentification(
            boolean configurableElementIdentification) {
        this.configurableElementIdentification = configurableElementIdentification;
    }

    public String getCodedElementListPath() {
        return codedElementListPath;
    }

    public void setCodedElementListPath(String codedElementListPath) {
        this.codedElementListPath = codedElementListPath;
    }

    public Collection<CodedElementListItem> getPatientSummaryl3() {
        return patientSummaryl3;
    }

    public Collection<CodedElementListItem> getPatientSummaryl1() {
        return patientSummaryl1;
    }

    public Collection<CodedElementListItem> getePrescriptionl1() {
        return ePrescriptionl1;
    }

    public Collection<CodedElementListItem> getePrescriptionl3() {
        return ePrescriptionl3;
    }

    public Collection<CodedElementListItem> geteDispensationl1() {
        return eDispensationl1;
    }

    public Collection<CodedElementListItem> geteDispensationl3() {
        return eDispensationl3;
    }

    /**
     * Invoked by the containing BeanFactory after it has set all bean properties.
     * This method allows the bean instance to perform validation of its overall configuration and final initialization
     * of the Transformation Manager.
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        // Read xml file (coded_element_list_ehdsi.xml)
        if (configurableElementIdentification && !isInitialized) {
            logger.info("[TM] Configurable Coded Element List used - Override enabled: '{}'", isCodedElementListOverride());
            Document doc;
            // If the default coded element configuration is overridden, trying to load the national configuration file.
            if (isCodedElementListOverride()) {
                doc = XmlUtil.getDocument(new File(codedElementListPath), true);
            } else {
                // Otherwise the default eHDSI Coded Element List is used.
                InputStream inputStream = CodedElementList.class.getClassLoader().getResourceAsStream(XML_CODED_ELEMENT_LIST);
                doc = XmlUtil.getDocument(inputStream, true);
            }

            Element root = doc.getDocumentElement();
            NodeList codedElements = root.getElementsByTagName(CODED_ELEMENT);
            logger.info("[TM] '{}' Coded Elements found", codedElements.getLength());

            // Starting to fill collections.
            Element codedElement;
            Element usageElement;
            Element docTypeElement;
            for (int i = 0; i < codedElements.getLength(); i++) {
                codedElement = (Element) codedElements.item(i);

                NodeList usageNL = codedElement.getElementsByTagName(USAGE);
                // exactly 1 element usage expected
                usageElement = (Element) usageNL.item(0);

                NodeList docTypesUsageNL = usageElement.getChildNodes();
                for (int j = 0; j < docTypesUsageNL.getLength(); j++) {
                    Node node = docTypesUsageNL.item(j);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        docTypeElement = (Element) node;

                        switch (docTypeElement.getNodeName()) {
                            case PATIENT_SUMMARY1:
                                patientSummaryl1 = addItem(docTypeElement, codedElement, patientSummaryl1);
                                break;
                            case PATIENT_SUMMARY3:
                                patientSummaryl3 = addItem(docTypeElement, codedElement, patientSummaryl3);
                                break;
                            case EPRESCRIPTION1:
                                ePrescriptionl1 = addItem(docTypeElement, codedElement, ePrescriptionl1);
                                break;
                            case EPRESCRIPTION3:
                                ePrescriptionl3 = addItem(docTypeElement, codedElement, ePrescriptionl3);
                                break;
                            case EDISPENSATION3:
                                eDispensationl3 = addItem(docTypeElement, codedElement, eDispensationl3);
                                break;
                            case HCER3:
                                hcerl3 = addItem(docTypeElement, codedElement, hcerl3);
                                break;
                            case HCER1:
                                hcerl1 = addItem(docTypeElement, codedElement, hcerl1);
                                break;
                            case MRO3:
                                mrol3 = addItem(docTypeElement, codedElement, mrol3);
                                break;
                            case MRO1:
                                mrol1 = addItem(docTypeElement, codedElement, mrol1);
                                break;
                            default:
                                logger.warn("[TM] Clinical Document type not supported by eHDSI");
                                break;
                        }
                    }
                }
            }

            hmDocAndLists.put(PATIENT_SUMMARY1, patientSummaryl1);
            hmDocAndLists.put(PATIENT_SUMMARY3, patientSummaryl3);
            hmDocAndLists.put(EDISPENSATION3, eDispensationl3);
            hmDocAndLists.put(EPRESCRIPTION1, ePrescriptionl1);
            hmDocAndLists.put(EPRESCRIPTION3, ePrescriptionl3);

            hmDocAndLists.put(HCER1, hcerl1);
            hmDocAndLists.put(HCER3, hcerl3);
            hmDocAndLists.put(MRO1, mrol1);
            hmDocAndLists.put(MRO3, mrol3);

            isInitialized = true;

        } else {
            if(isInitialized) {
                logger.warn("[TM] Configurable Coded Element List already initialized");
            } else {
                logger.warn("[TM] Configurable Coded Element List NOT used");
            }
        }
    }

    private Collection<CodedElementListItem> addItem(Element docTypeElement, Element codedElement, Collection<CodedElementListItem> collection) {

        String usage = docTypeElement.getTextContent();
        if (collection == null) {
            collection = new ArrayList<>();
        }
        try {
            if (isNeeded(usage)) {
                CodedElementListItem item = new CodedElementListItem();

                String xPath = codedElement.getElementsByTagName(ELEMENT_PATH).item(0).getTextContent();
                String valueSet = codedElement.getElementsByTagName(VALUE_SET).item(0).getTextContent();
                String valueSetVersion = codedElement.getElementsByTagName(VALUE_SET_VERSION).item(0).getTextContent();
                String targetLanguageCode = codedElement.getElementsByTagName(TARGET_LANGUAGE_CODE).item(0).getTextContent();

                item.setxPath(xPath);
                item.setUsage(usage);
                item.setValueSet(valueSet);
                item.setValueSetVersion(valueSetVersion);
                item.setTargetLanguageCode(targetLanguageCode);
                if (!collection.contains(item)) {
                    collection.add(item);
                }
            }
        } catch (Exception e) {
            logger.error("Exception: '{}'", e.getMessage(), e);
        }
        return collection;
    }

    private boolean isNeeded(String usage) {
        return (usage != null && usage.length() > 0 && (usage.equals(R) || usage.equals(RNFA) || usage.equals(O)));
    }

    /**
     * For input cda Document type returns correct List of CodedElementListItems.
     *
     * @param cdaDocumentType - Type af Clinical Document supported by eHDSI.
     * @return List of CodedElementListItems.
     */
    public Collection<CodedElementListItem> getList(String cdaDocumentType) {
        return hmDocAndLists.get(cdaDocumentType);
    }
}
