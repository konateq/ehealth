package eu.europa.ec.sante.openncp.core.common.ihe.transformation.util;

import eu.europa.ec.sante.openncp.core.common.ihe.transformation.config.TMConfiguration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static eu.europa.ec.sante.openncp.common.configuration.util.Constants.EPSOS_PROPS_PATH;

@Component
public class CodedElementList implements TMConstants {

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
    private static final CodedElementList instance = null;
    private static final HashMap<String, Collection<CodedElementListItem>> hmDocAndLists = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(CodedElementList.class);
    private final TMConfiguration tmConfiguration;
    private boolean isInitialized = false;


    public CodedElementList(final TMConfiguration tmConfiguration) {
        this.tmConfiguration = Validate.notNull(tmConfiguration, "tmConfiguration cannot be null");
    }

    public boolean isCodedElementListOverride() {
        return this.tmConfiguration.isCodedElementListOverride();
    }

    public boolean isConfigurableElementIdentification() {
        return this.tmConfiguration.isConfigurableElementIdentification();
    }

    public String getCodedElementListPath() {
        return this.tmConfiguration.getCodedElementListPath();
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
    @PostConstruct
    public void afterPropertiesSet() throws Exception {

        // Read xml file (coded_element_list_ehdsi.xml)
        if (isConfigurableElementIdentification() && !isInitialized) {
            logger.info("[TM] Configurable Coded Element List used - Override enabled: '{}'", isCodedElementListOverride());
            final Document doc;
            // If the default coded element configuration is overridden, trying to load the national configuration file.
            if (isCodedElementListOverride()) {
                var path = Paths.get(System.getenv("EPSOS_PROPS_PATH"), getCodedElementListPath());
                doc = XmlUtil.getDocument(path.toFile(), true);
            } else {
                // Otherwise the default eHDSI Coded Element List is used.
                final InputStream inputStream = CodedElementList.class.getClassLoader().getResourceAsStream(XML_CODED_ELEMENT_LIST);
                doc = XmlUtil.getDocument(inputStream, true);
            }

            final Element root = doc.getDocumentElement();
            final NodeList codedElements = root.getElementsByTagName(CODED_ELEMENT);
            logger.info("[TM] '{}' Coded Elements found", codedElements.getLength());

            // Starting to fill collections.
            Element codedElement;
            Element usageElement;
            Element docTypeElement;
            for (int i = 0; i < codedElements.getLength(); i++) {
                codedElement = (Element) codedElements.item(i);

                final NodeList usageNL = codedElement.getElementsByTagName(USAGE);
                // exactly 1 element usage expected
                usageElement = (Element) usageNL.item(0);

                final NodeList docTypesUsageNL = usageElement.getChildNodes();
                for (int j = 0; j < docTypesUsageNL.getLength(); j++) {
                    final Node node = docTypesUsageNL.item(j);
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
                                logger.warn("[TM] Clinical Document type not supported by eHDSI : {}", docTypeElement.getNodeName());
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

    private Collection<CodedElementListItem> addItem(final Element docTypeElement, final Element codedElement, Collection<CodedElementListItem> collection) {

        final String usage = docTypeElement.getTextContent();
        if (collection == null) {
            collection = new ArrayList<>();
        }
        try {
            if (isNeeded(usage)) {
                final CodedElementListItem item = new CodedElementListItem();

                final String xPath = codedElement.getElementsByTagName(ELEMENT_PATH).item(0).getTextContent();
                final String valueSet = codedElement.getElementsByTagName(VALUE_SET).item(0).getTextContent();
                final String valueSetVersion = codedElement.getElementsByTagName(VALUE_SET_VERSION).item(0).getTextContent();
                final String targetLanguageCode = codedElement.getElementsByTagName(TARGET_LANGUAGE_CODE).item(0).getTextContent();

                item.setxPath(xPath);
                item.setUsage(usage);
                item.setValueSet(valueSet);
                item.setValueSetVersion(valueSetVersion);
                item.setTargetLanguageCode(targetLanguageCode);
                if (!collection.contains(item)) {
                    collection.add(item);
                }
            }
        } catch (final Exception e) {
            logger.error("Exception: '{}'", e.getMessage(), e);
        }
        return collection;
    }

    private boolean isNeeded(final String usage) {
        return (usage != null && !usage.isEmpty() && (usage.equals(R) || usage.equals(RNFA) || usage.equals(O)));
    }

    /**
     * For input cda Document type returns correct List of CodedElementListItems.
     *
     * @param cdaDocumentType - Type af Clinical Document supported by eHDSI.
     * @return List of CodedElementListItems.
     */
    public Collection<CodedElementListItem> getList(final String cdaDocumentType) {
        return hmDocAndLists.get(cdaDocumentType);
    }
}
