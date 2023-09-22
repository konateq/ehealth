package eu.europa.ec.sante.ehdsi.openncp.tm.util;

/**
 * Constants for TM module
 *
 * @author Frantisek Rudik
 * @author Organization: Posam
 * @author mail:frantisek.rudik@posam.sk
 * @version 1.10, 2010, 20 October
 */
public interface TMConstants {

    String NEWLINE = System.getProperty("line.separator");

    //transcoding & translation
    String TRANSLATION = "translation";
    String CODE = "code";
    String CODE_SYSTEM = "codeSystem";
    String CODE_SYSTEM_NAME = "codeSystemName";
    String CODE_SYSTEM_VERSION = "codeSystemVersion";
    String DISPLAY_NAME = "displayName";
    String ORIGINAL_TEXT = "originalText";
    String REFERENCE = "reference";

    //responseStructure
    String RESPONSE_ELEMENT = "responseElement";
    String RESPONSE_STATUS = "responseStatus";
    String STATUS = "status";
    String RESULT = "result";
    String STATUS_SUCCESS = "success";
    String STATUS_FAILURE = "failure";
    String ERRORS = "errors";
    String ERROR = "error";
    String WARNINGS = "warnings";
    String WARNING = "warning";
    String DESCRIPTION = "description";

    String DOCUMENT = "Document:";
    String COLON = ":";

    //unstructured CDA
    String COMPONENT = "component";
    String NON_XML_BODY = "nonXMLBody";
    String TEXT = "text";
    String MEDIA_TYPE = "mediaType";
    String APPLICATION_PDF = "application/pdf";
    String REPRESENTATION = "representation";
    String B64 = "B64";

    //Posam test version comment
    String POSAM_COMMENT = "This CDA document was processed (transcoded/translated) by test version of PosAm Transformation Manager component.";

    //Coded Element List
    String CODED_ELEMENT = "codedElement";
    String ELEMENT_PATH = "elementPath";
    String USAGE = "usage";
    String VALUE_SET = "valueSet";
    String VALUE_SET_VERSION = "valueSetVersion";
    String TARGET_LANGUAGE_CODE = "targetLanguageCode";
    //Document types
    String PATIENT_SUMMARY3 = "patientSummaryCDAl3";
    String PATIENT_SUMMARY1 = "patientSummaryCDAl1pdf";
    String EPRESCRIPTION3 = "ePrescriptionCDAl3";
    String EPRESCRIPTION1 = "ePrescriptionCDAl1pdf";
    String EDISPENSATION3 = "eDispensationCDAl3";
    String HCER3 = "HCERDocCDAl3";
    String HCER1 = "HCERDocCDAl1pdf";
    String MRO3 = "MRODocCDAl3";
    String MRO1 = "MRODocCDAl1pdf";
    String SCANNED1 = "scannedDocCDAl1pdf";
    String SCANNED3 = "scannedCDAl1pdf";

    //Usage/Optionality of Coded Elements
    String R = "R";
    String RNFA = "RNFA";
    String O = "O";
    String NA = "NA";

    //Reference values
    String VALUE = "value";
    String HASH = "#";
    String ID = "ID";

    //XPaths called from code
    String XPATH_CLINICALDOCUMENT_CODE = "/ClinicalDocument/code";
    String XPATH_STRUCTUREDBODY = "/ClinicalDocument/component/structuredBody";
    String XPATH_NONXMLBODY = "/ClinicalDocument/component/nonXMLBody";
    String XPATH_ALL_ELEMENTS_WITH_CODE_ATTR = "//*[@code and @codeSystem]";
    String XPATH_ORIGINAL_TEXT_REFERENCE_VALUE = "originalText/reference[@value]";
    String XPATH_ALL_ELEMENTS_WITH_ID_ATTR = "//*/*[@ID]";
    String XPATH_TRANSLATION = "translation";

    String EHDSI_HL7_NAMESPACE = "urn:hl7-org:v3";
    //XmlUtil Constants
    String EMPTY_XMLNS = "xmlns=\"\"";
    String EMPTY_STRING = "";

    //xsi:type check
    String XSI_TYPE = "xsi:type";
    String CE = "CE";
    String CD = "CD";

    //other (log, ...)
    String TRANSCODING = "transcoding";
}
