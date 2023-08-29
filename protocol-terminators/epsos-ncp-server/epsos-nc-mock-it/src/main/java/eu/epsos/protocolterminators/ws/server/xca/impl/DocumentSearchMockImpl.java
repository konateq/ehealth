package eu.epsos.protocolterminators.ws.server.xca.impl;

import eu.epsos.protocolterminators.ws.server.common.NationalConnectorGateway;
import eu.epsos.protocolterminators.ws.server.common.ResourceList;
import eu.epsos.protocolterminators.ws.server.common.ResourceLoader;
import eu.epsos.protocolterminators.ws.server.exception.NIException;
import eu.epsos.protocolterminators.ws.server.exception.NoMatchException;
import eu.epsos.protocolterminators.ws.server.exception.OriginalDataMissingException;
import eu.epsos.protocolterminators.ws.server.exception.ProcessingDeferredException;
import eu.epsos.protocolterminators.ws.server.util.NationalConnectorUtil;
import eu.epsos.protocolterminators.ws.server.xca.DocumentSearchInterface;
import eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode;
import eu.europa.ec.sante.ehdsi.openncp.mock.util.CdaUtils;
import fi.kela.se.epsos.data.model.*;
import fi.kela.se.epsos.data.model.SearchCriteria.Criteria;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import tr.com.srdc.epsos.data.model.PatientDemographics;
import tr.com.srdc.epsos.data.model.SimpleConfidentialityEnum;
import tr.com.srdc.epsos.data.model.SubstitutionCodeEnum;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.XMLUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static eu.europa.ec.sante.ehdsi.constant.error.OpenNCPErrorCode.ERROR_PS_PDF_FORMAT_NOT_PROVIDED;

/**
 * @author konstantin.hypponen@kela.fi Note this is a very dirty implementation
 */
public class DocumentSearchMockImpl extends NationalConnectorGateway implements DocumentSearchInterface {

    private static final String PATTERN_EP = "epstore.+\\.xml";
    private static final String PATTERN_PS = "psstore.+\\.xml";
    private static final String PATTERN_MRO = "mrostore.+\\.xml";
    private static final String PATTERN_ORCD_LABORATORY_RESULTS = "orcdstore.11502-2.+\\.xml";
    private static final String PATTERN_ORCD_HOSPITAL_DISCHARGE_REPORTS = "orcdstore.34105-7.+\\.xml";
    private static final String PATTERN_ORCD_MEDICAL_IMAGING_REPORTS = "orcdstore.18748-4.+\\.xml";
    private static final String PATTERN_ORCD_MEDICAL_IMAGES = "orcdstore.x-clinical-image.+\\.xml";
    private static final String CONSTANT_EXTENSION = "extension";
    private static final String EHDSI_HL7_NAMESPACE = "urn:hl7-org:v3";
    private static final String EHDSI_EPSOS_MEDICATION_NAMESPACE = "urn:epsos-org:ep:medication";
    private static final String EHDSI_PS_L3_TEMPLATE_ID = "1.3.6.1.4.1.12559.11.10.1.3.1.1.3";
    private static final String EHDSI_PS_L1_TEMPLATE_ID = "1.3.6.1.4.1.12559.11.10.1.3.1.1.7";
    private static final String EHDSI_EP_L1_TEMPLATE_ID = "1.3.6.1.4.1.12559.11.10.1.3.1.1.6";
    private final Logger logger = LoggerFactory.getLogger(DocumentSearchMockImpl.class);
    private final List<DocumentAssociation<EPDocumentMetaData>> epDocumentMetaDatas = new ArrayList<>();
    private final List<DocumentAssociation<PSDocumentMetaData>> psDocumentMetaDatas = new ArrayList<>();
    private final List<DocumentAssociation<MroDocumentMetaData>> mroDocumentMetaDatas = new ArrayList<>();
    private final List<OrCDDocumentMetaData> orCDDocumentLaboratoryResultsMetaDatas = new ArrayList<>();
    private final List<OrCDDocumentMetaData> orCDDocumentHospitalDischargeReportsMetaDatas = new ArrayList<>();
    private final List<OrCDDocumentMetaData> orCDDocumentMedicalImagingReportsMetaDatas = new ArrayList<>();
    private final List<OrCDDocumentMetaData> orCDDocumentMedicalImagesMetaDatas = new ArrayList<>();
    private final List<EPSOSDocument> documents = new ArrayList<>();

    public DocumentSearchMockImpl() {

        Collection<String> documentlist = ResourceList.getResources(Pattern.compile(PATTERN_EP));
        var resourceLoader = new ResourceLoader();

        // Mocked ePrescription fill up
        for (String xmlFilename : documentlist) {

            logger.debug("Reading file '{}'", xmlFilename);
            String pdfFilename = xmlFilename.substring(0, xmlFilename.length() - 4) + ".pdf";

            try {
                var xmlDocString = resourceLoader.getResource(xmlFilename);
                Document xmlDoc = XMLUtil.parseContent(xmlDocString);
                var size = getFileSize(xmlFilename);
                var hash = getHash(xmlFilename);
                addFormatToOID(xmlDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML);

                PatientDemographics pd = CdaUtils.getPatientDemographicsFromXMLDocument(xmlDoc);
                String productCode = null;
                String productName = null;
                Element element = getProductFromPrescription(xmlDoc);
                if (element != null) {
                    productCode = element.getAttribute("code");
                    productName = element.getAttribute("displayName");
                }

                String description = getDescriptionFromDocument(xmlDoc);
                String atcCode = getAtcCode(xmlDoc);
                String atcName = getAtcName(xmlDoc);
                String doseFormCode = getDoseFormCode(xmlDoc);
                String doseFormName = getDoseFormName(xmlDoc);
                String strength = getStrength(xmlDoc);
                EPDocumentMetaData.SubstitutionMetaData substitution = getSubstitution(xmlDoc);
                boolean dispensable = getDispensable(xmlDoc);

                var epListParam = new EpListParam(dispensable, atcCode, atcName, doseFormCode, doseFormName, strength, substitution);
                EPDocumentMetaData epdXml;
                logger.info("Document ID: '{}' parsed for Patient ID: '{}'", getOIDFromDocument(xmlDoc), pd.getId());
                if (StringUtils.contains(pd.getId(), "-1234-W6")) {
                    epdXml = DocumentFactory.createEPDocumentXML(getOIDFromDocument(xmlDoc), pd.getId(), new Date(),
                            Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc), getClinicalDocumentAuthor(xmlDoc),
                            description, productCode, productName, epListParam, getClinicalDocumentConfidentialityEnum(xmlDoc),
                            this.getClinicalDocumentLanguage(xmlDoc));
                } else {
                    epdXml = DocumentFactory.createEPDocumentXML(getOIDFromDocument(xmlDoc), pd.getId(), new Date(),
                            Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc), getClinicalDocumentAuthor(xmlDoc),
                            description, productCode, productName, epListParam, getClinicalDocumentConfidentialityEnum(xmlDoc),
                            this.getClinicalDocumentLanguage(xmlDoc), size, hash);
                }

                logger.debug("Placed XML doc id='{}' HomeCommId='{}', Patient Id: '{}' into eP repository",
                        epdXml.getId(), Constants.HOME_COMM_ID, pd.getId());
                documents.add(DocumentFactory.createEPSOSDocument(epdXml.getPatientId(), epdXml.getClassCode(), xmlDoc));

                if (!StringUtils.endsWith(pdfFilename, "-NO-PDF.pdf")) {
                    EPDocumentMetaData epdPdf = null;
                    try {
                        Document pdfDoc = XMLUtil.parseContent(xmlDocString);
                        size = getFileSize(pdfFilename);
                        byte[] pdfcontents = resourceLoader.getResourceAsByteArray(pdfFilename);
                        wrapPDFinCDA(pdfcontents, pdfDoc);
                        addFormatToOID(pdfDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_PDF);
                        if (StringUtils.contains(pd.getId(), "-1234-W6")) {
                            epdPdf = DocumentFactory.createEPDocumentPDF(getOIDFromDocument(pdfDoc), pd.getId(),
                                    new Date(), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc),
                                    getClinicalDocumentAuthor(xmlDoc), description, productCode, productName, epListParam,
                                    getClinicalDocumentConfidentialityEnum(xmlDoc), this.getClinicalDocumentLanguage(xmlDoc));
                        } else {
                            epdPdf = DocumentFactory.createEPDocumentPDF(getOIDFromDocument(pdfDoc), pd.getId(),
                                    new Date(), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc),
                                    getClinicalDocumentAuthor(xmlDoc), description, productCode, productName, epListParam,
                                    getClinicalDocumentConfidentialityEnum(xmlDoc), this.getClinicalDocumentLanguage(xmlDoc)
                                    , size, hash);
                        }
                        logger.debug("Placed PDF doc id='{}' into eP repository", epdPdf.getId());
                        documents.add(DocumentFactory.createEPSOSDocument(epdPdf.getPatientId(), epdPdf.getClassCode(), pdfDoc));
                    } catch (Exception e) {
                        logger.warn("Could not read file at" + pdfFilename, e);
                    }
                    epDocumentMetaDatas.add(DocumentFactory.createDocumentAssociation(epdXml, epdPdf));
                }
            } catch (Exception e) {
                logger.warn("Could not read file at '{}'", xmlFilename, e);
            }
        }

        // Mocked Patient Summaries fill up
        documentlist = ResourceList.getResources(Pattern.compile(PATTERN_PS));

        for (String xmlFilename : documentlist) {
            logger.debug("Reading file '{}", xmlFilename);

            String pdfFilename = xmlFilename.substring(0, xmlFilename.length() - 4) + ".pdf";

            try {
                var xmlDocString = resourceLoader.getResource(xmlFilename);
                Document xmlDoc = XMLUtil.parseContent(xmlDocString);
                var size = getFileSize(xmlFilename);
                var hash = getHash(xmlFilename);
                addFormatToOID(xmlDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML);
                logger.debug("Parsing PS patient demographics");
                PatientDemographics pd = CdaUtils.getPatientDemographicsFromXMLDocument(xmlDoc);
                PSDocumentMetaData psdXml;
                if (StringUtils.contains(pd.getId(), "-1234-W6")) {
                    psdXml = DocumentFactory.createPSDocumentXML(getOIDFromDocument(xmlDoc), pd.getId(),
                            new Date(), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc),
                            getClinicalDocumentAuthor(xmlDoc), this.getClinicalDocumentConfidentialityEnum(xmlDoc),
                            this.getClinicalDocumentLanguage(xmlDoc));
                } else {
                    psdXml = DocumentFactory.createPSDocumentXML(getOIDFromDocument(xmlDoc), pd.getId(),
                            new Date(), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc),
                            getClinicalDocumentAuthor(xmlDoc), this.getClinicalDocumentConfidentialityEnum(xmlDoc),
                            this.getClinicalDocumentLanguage(xmlDoc), size, hash);
                }
                documents.add(DocumentFactory.createEPSOSDocument(psdXml.getPatientId(), psdXml.getClassCode(), xmlDoc));

                if (!StringUtils.endsWith(pdfFilename, "-NO-PDF.pdf")) {
                    PSDocumentMetaData psdPdf = null;

                    try {
                        Document pdfDoc = XMLUtil.parseContent(xmlDocString);
                        size = getFileSize(pdfFilename);
                        byte[] pdfcontents = resourceLoader.getResourceAsByteArray(pdfFilename);
                        wrapPDFinCDA(pdfcontents, pdfDoc);
                        logger.debug("Adding format to the document's OID");
                        addFormatToOID(pdfDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_PDF);
                        if (StringUtils.contains(pd.getId(), "-1234-W6")) {
                            psdPdf = DocumentFactory.createPSDocumentPDF(getOIDFromDocument(pdfDoc), pd.getId(),
                                    new Date(), Constants.HOME_COMM_ID, getTitleFromDocument(pdfDoc),
                                    getClinicalDocumentAuthor(xmlDoc), this.getClinicalDocumentConfidentialityEnum(pdfDoc),
                                    this.getClinicalDocumentLanguage(pdfDoc));
                        } else {
                            psdPdf = DocumentFactory.createPSDocumentPDF(getOIDFromDocument(pdfDoc), pd.getId(),
                                    new Date(), Constants.HOME_COMM_ID, getTitleFromDocument(pdfDoc),
                                    getClinicalDocumentAuthor(xmlDoc), this.getClinicalDocumentConfidentialityEnum(pdfDoc),
                                    this.getClinicalDocumentLanguage(pdfDoc), size, hash);
                        }
                        documents.add(DocumentFactory.createEPSOSDocument(psdPdf.getPatientId(), psdPdf.getClassCode(), pdfDoc));
                    } catch (Exception e) {
                        logger.warn("Could not read file at '{}'", pdfFilename, e);
                    }
                    psDocumentMetaDatas.add(DocumentFactory.createDocumentAssociation(psdXml, psdPdf));
                }
            } catch (Exception e) {
                logger.warn("Could not read file at '{}'", xmlFilename, e);
            }
        }

        // Mocked OrCDs fill up
        var author = new OrCDDocumentMetaData.Author();
        author.setAuthorPerson("AuthorPerson OrCD Test");
        author.setAuthorSpeciality(Arrays.asList("Speciality 1", "Speciality 2", "Speciality 3"));

        List<OrCDDocumentMetaData.Author> authors = new ArrayList<>();
        authors.add(author);

        var reasonOfHospitalisation = new OrCDDocumentMetaData.ReasonOfHospitalisation("K56.2", "1.3.6.1.4.1.12559.11.10.1.3.1.44.2", "Volvulus");

        /* Hospital Discharge Reports */
        documentlist = ResourceList.getResources(Pattern.compile(PATTERN_ORCD_HOSPITAL_DISCHARGE_REPORTS));
        for (String xmlFilename : documentlist) {
            logger.debug("Reading file '{}", xmlFilename);
            try {
                var xmlDocString = resourceLoader.getResource(xmlFilename);
                Document xmlDoc = XMLUtil.parseContent(xmlDocString);
                var size = getFileSize(xmlFilename);
                var hash = getHash(xmlFilename);
                addFormatToOID(xmlDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML);
                logger.debug("Parsing OrCD patient demographics");
                PatientDemographics pd = CdaUtils.getPatientDemographicsFromXMLDocument(xmlDoc);

                OrCDDocumentMetaData orcddXml = DocumentFactory.createOrCDHospitalDischargeReportsDocument(getOIDFromDocument(xmlDoc), pd.getId(),
                        getCreationDateFromDocument(xmlDoc), getServiceStartTimeFromDocument(xmlDoc), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc), getClinicalDocumentAuthor(xmlDoc),
                        this.getClinicalDocumentConfidentialityEnum(xmlDoc), this.getClinicalDocumentLanguage(xmlDoc), size, authors, reasonOfHospitalisation, hash);
                documents.add(DocumentFactory.createEPSOSDocument(orcddXml.getPatientId(), orcddXml.getClassCode(), xmlDoc));
                orCDDocumentHospitalDischargeReportsMetaDatas.add(orcddXml);
                logger.debug("Placed XML doc id= '{}' into OrCD repository", orcddXml.getId());

            } catch (Exception e) {
                logger.warn("Could not read file at '{}'", xmlFilename, e);
            }
        }

        /* Laboratory Results */
        documentlist = ResourceList.getResources(Pattern.compile(PATTERN_ORCD_LABORATORY_RESULTS));
        for (String xmlFilename : documentlist) {
            logger.debug("Reading file '{}", xmlFilename);
            try {
                var xmlDocString = resourceLoader.getResource(xmlFilename);
                Document xmlDoc = XMLUtil.parseContent(xmlDocString);
                var size = getFileSize(xmlFilename);
                var hash = getHash(xmlFilename);
                addFormatToOID(xmlDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML);
                logger.debug("Parsing OrCD patient demographics");
                PatientDemographics pd = CdaUtils.getPatientDemographicsFromXMLDocument(xmlDoc);

                OrCDDocumentMetaData orcddXml = DocumentFactory.createOrCDLaboratoryResultsDocument(getOIDFromDocument(xmlDoc), pd.getId(),
                        getCreationDateFromDocument(xmlDoc), getServiceStartTimeFromDocument(xmlDoc), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc), getClinicalDocumentAuthor(xmlDoc),
                        this.getClinicalDocumentConfidentialityEnum(xmlDoc), this.getClinicalDocumentLanguage(xmlDoc), size, authors, hash);
                documents.add(DocumentFactory.createEPSOSDocument(orcddXml.getPatientId(), orcddXml.getClassCode(), xmlDoc));
                orCDDocumentLaboratoryResultsMetaDatas.add(orcddXml);
                logger.debug("Placed XML doc id= '{}' into OrCD repository", orcddXml.getId());
            } catch (Exception e) {
                logger.warn("Could not read file at '{}'", xmlFilename, e);
            }
        }

        /* Medical Imaging Reports */
        documentlist = ResourceList.getResources(Pattern.compile(PATTERN_ORCD_MEDICAL_IMAGING_REPORTS));
        for (String xmlFilename : documentlist) {
            logger.debug("Reading file '{}", xmlFilename);
            try {
                var xmlDocString = resourceLoader.getResource(xmlFilename);
                Document xmlDoc = XMLUtil.parseContent(xmlDocString);
                var size = getFileSize(xmlFilename);
                var hash = getHash(xmlFilename);
                addFormatToOID(xmlDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML);
                logger.debug("Parsing OrCD patient demographics");
                PatientDemographics pd = CdaUtils.getPatientDemographicsFromXMLDocument(xmlDoc);

                OrCDDocumentMetaData orcddXml = DocumentFactory.createOrCDMedicalImagingReportsDocument(getOIDFromDocument(xmlDoc), pd.getId(),
                        getCreationDateFromDocument(xmlDoc), getServiceStartTimeFromDocument(xmlDoc), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc), getClinicalDocumentAuthor(xmlDoc),
                        this.getClinicalDocumentConfidentialityEnum(xmlDoc), this.getClinicalDocumentLanguage(xmlDoc), size, authors, reasonOfHospitalisation, hash);
                documents.add(DocumentFactory.createEPSOSDocument(orcddXml.getPatientId(), orcddXml.getClassCode(), xmlDoc));

                orCDDocumentMedicalImagingReportsMetaDatas.add(orcddXml);
                logger.debug("Placed XML doc id= '{}' into OrCD repository", orcddXml.getId());
            } catch (Exception e) {
                logger.warn("Could not read file at '{}'", xmlFilename, e);
            }
        }

        /* Medical Images */
        documentlist = ResourceList.getResources(Pattern.compile(PATTERN_ORCD_MEDICAL_IMAGES));
        for (String xmlFilename : documentlist) {
            logger.debug("Reading file '{}'", xmlFilename);
            try {
                var xmlDocString = resourceLoader.getResource(xmlFilename);
                Document xmlDoc = XMLUtil.parseContent(xmlDocString);
                var size = getFileSize(xmlFilename);
                var hash = getHash(xmlFilename);
                addFormatToOID(xmlDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML);
                logger.debug("Parsing OrCD patient demographics");
                PatientDemographics pd = CdaUtils.getPatientDemographicsFromXMLDocument(xmlDoc);

                OrCDDocumentMetaData orcddXml = DocumentFactory.createOrCDMedicalImagesDocument(getOIDFromDocument(xmlDoc), pd.getId(),
                        getCreationDateFromDocument(xmlDoc), getServiceStartTimeFromDocument(xmlDoc), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc), getClinicalDocumentAuthor(xmlDoc),
                        this.getClinicalDocumentConfidentialityEnum(xmlDoc), this.getClinicalDocumentLanguage(xmlDoc), OrCDDocumentMetaData.DocumentFileType.PNG, size, authors, reasonOfHospitalisation, hash);
                documents.add(DocumentFactory.createEPSOSDocument(orcddXml.getPatientId(), orcddXml.getClassCode(), xmlDoc));

                orCDDocumentMedicalImagesMetaDatas.add(orcddXml);
                logger.debug("Placed XML doc id= '{}' into OrCD repository", orcddXml.getId());
            } catch (Exception e) {
                logger.warn("Could not read file at '{}'", xmlFilename, e);
            }
        }

        // Mocked MROs fill up
        documentlist = ResourceList.getResources(Pattern.compile(PATTERN_MRO));

        for (String xmlFilename : documentlist) {
            logger.debug("reading file '{}'", xmlFilename);
            // make sure there is a pdf version of the document in the repository
            String pdfFilename = xmlFilename.substring(0, xmlFilename.length() - 4) + ".pdf";
            if (StringUtils.equals(resourceLoader.getResource(pdfFilename), "")) {
                continue;
            }

            try {
                var xmlDocString = resourceLoader.getResource(xmlFilename);
                Document xmlDoc = loadCDADocument(xmlDocString);
                var size = getFileSize(xmlFilename);
                var hash = getHash(xmlFilename);
                addFormatToOID(xmlDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_XML);
                Document pdfDoc = loadCDADocument(xmlDocString);
                byte[] pdfcontents = resourceLoader.getResourceAsByteArray(pdfFilename);
                wrapPDFinCDA(pdfcontents, pdfDoc);
                logger.debug("Adding format to the document's OID");
                addFormatToOID(pdfDoc, EPSOSDocumentMetaData.EPSOSDOCUMENT_FORMAT_PDF);
                logger.debug("Parsing MRO patient demographics");
                PatientDemographics pd = CdaUtils.getPatientDemographicsFromXMLDocument(xmlDoc);

                MroDocumentMetaData psdPdf = DocumentFactory.createMroDocumentPDF(getOIDFromDocument(pdfDoc), pd.getId(),
                        new Date(), Constants.HOME_COMM_ID, getTitleFromDocument(pdfDoc), getClinicalDocumentAuthor(xmlDoc),
                        this.getClinicalDocumentConfidentialityEnum(pdfDoc), this.getClinicalDocumentLanguage(pdfDoc), size, hash);
                documents.add(DocumentFactory.createEPSOSDocument(psdPdf.getPatientId(), psdPdf.getClassCode(), pdfDoc));
                MroDocumentMetaData psdXml = DocumentFactory.createMroDocumentXML(getOIDFromDocument(xmlDoc), pd.getId(),
                        new Date(), Constants.HOME_COMM_ID, getTitleFromDocument(xmlDoc), getClinicalDocumentAuthor(xmlDoc),
                        this.getClinicalDocumentConfidentialityEnum(xmlDoc), this.getClinicalDocumentLanguage(xmlDoc), size, hash);
                documents.add(DocumentFactory.createEPSOSDocument(psdXml.getPatientId(), psdPdf.getClassCode(), xmlDoc));
                logger.debug("Placed PDF doc id='{}' into MRO repository", psdPdf.getId());
                logger.debug("Placed XML doc id='{}' into MRO repository", psdXml.getId());

                mroDocumentMetaDatas.add(DocumentFactory.createDocumentAssociation(psdXml, psdPdf));
            } catch (Exception e) {
                logger.warn("Could not read file at '{}'", xmlFilename, e);
            }
        }
    }

    private long getFileSize(String xmlFilename) throws IOException {
        // Temporarily return a fixed size, max size of biggest resource.
        // In the future instead of accessing the war resources it would be better to
        // access the mock files from an external folder in openncp-configuration
        return 512 * 1024;

        /*
        var classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream(xmlFilename);
        var tempFile = new File("temp.xml");
        FileUtils.copyInputStreamToFile(is, tempFile);
        long bytes = tempFile.length();
        tempFile.delete();
        return bytes;
        */
    }

    private String getHash(String xmlFilename) throws IOException {
        var classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream(xmlFilename);
        return DigestUtils.sha1Hex(is);
    }

    private Document loadCDADocument(String content) throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        InputSource inputSource = new InputSource();
        inputSource.setCharacterStream(new StringReader(content));
        return docBuilder.parse(inputSource);
    }

    private String getClinicalDocumentAuthor(Document doc) {

        List<Node> nodeList = XMLUtil.getNodeList(doc, "ClinicalDocument/author/assignedAuthor/assignedPerson/name");
        var author = "";
        for (Node node : nodeList) {

            NodeList nodeList1 = node.getChildNodes();
            if (nodeList1 != null) {
                var prefix = new StringBuilder();
                var suffix = new StringBuilder();
                var given = new StringBuilder();
                var givenAdditional = new StringBuilder();
                var family = new StringBuilder();
                for (var i = 0; i < nodeList1.getLength(); i++) {
                    Node node1 = nodeList1.item(i);

                    if (node1.getNodeType() == Node.ELEMENT_NODE) {
                        logger.debug("Node: '{}'", node1.getLocalName());
                        switch (node1.getLocalName()) {
                            case "prefix":
                                if (prefix.length() > 0) {
                                    prefix.append(" ");
                                }
                                prefix.append(node1.getTextContent());
                                break;
                            case "suffix":
                                if (suffix.length() > 0) {
                                    suffix.append(" ");
                                }
                                suffix.append(node1.getTextContent());
                                break;
                            case "given":
                                if (given.length() <= 0) {
                                    given.append(node1.getTextContent());
                                } else {
                                    if (givenAdditional.length() > 0) {
                                        givenAdditional.append(" ");
                                    }
                                    givenAdditional.append(node1.getTextContent());
                                }
                                break;
                            case "family":
                                if (family.length() > 0) {
                                    family.append(" ");
                                }
                                family.append(node1.getTextContent());
                                break;
                            default:
                                logger.warn("No Author information to append...");
                                break;
                        }
                    }
                }
                /*
                    From (https://hl7-definition.caristix.com/v2/HL7v2.5/DataTypes/XCN)
                    XCN.1 - Id Number
                    XCN.2 - Family Name
                    XCN.3 - Given Name (FirstName)
                    XCN.4 - Second And Further Given Names Or Initials
                    XCN.5 - Suffix (e.g., Jr Or Iii)
                    XCN.6 - Prefix (e.g., Dr)
                    ...
                */
                String id = "";
                author = String.format("%s^%s^%s^%s^%s^%s", id, family, given, givenAdditional, suffix, prefix);
            }
        }
        return StringUtils.trim(author);
    }

    private Date getCreationDateFromDocument(Document doc) throws ParseException {
        List<Node> nodeList = XMLUtil.getNodeList(doc, "ClinicalDocument/effectiveTime");
        Date creationDate = null;
        for (Node node : nodeList) {
            if (node.getAttributes().getNamedItem("value") != null) {
                var simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssZ");
                creationDate = simpleDateFormat.parse(node.getAttributes().getNamedItem("value").getTextContent());
                logger.debug("creationDate: '{}'", creationDate);
            }
        }
        return creationDate;
    }

    private Date getServiceStartTimeFromDocument(Document doc) throws ParseException {
        List<Node> nodeList = XMLUtil.getNodeList(doc, "ClinicalDocument/documentationOf/serviceEvent/effectiveTime/high");
        Date serviceStartTime = null;
        for (Node node : nodeList) {
            if (node.getAttributes().getNamedItem("value") != null) {
                var simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
                serviceStartTime = simpleDateFormat.parse(node.getAttributes().getNamedItem("value").getTextContent());
                logger.debug("serviceStartTime: '{}'", serviceStartTime);
            }
        }
        return serviceStartTime;
    }

    private SimpleConfidentialityEnum getClinicalDocumentConfidentialityEnum(Document doc) {
        SimpleConfidentialityEnum confidentiality = SimpleConfidentialityEnum.N;
        List<Node> nodeList = XMLUtil.getNodeList(doc, "ClinicalDocument/confidentialityCode");
        var code = "";
        for (Node node : nodeList) {
            if (node.getAttributes().getNamedItem("code") != null) {
                code = node.getAttributes().getNamedItem("code").getTextContent();
                if (StringUtils.equals(code, SimpleConfidentialityEnum.N.name())) {
                    confidentiality = SimpleConfidentialityEnum.N;
                } else if (StringUtils.equals(code, SimpleConfidentialityEnum.R.name())) {
                    confidentiality = SimpleConfidentialityEnum.R;
                } else if (StringUtils.equals(code, SimpleConfidentialityEnum.V.name())) {
                    confidentiality = SimpleConfidentialityEnum.V;
                }
                logger.debug("confidentiality code: '{}'", code);
            }
        }
        return confidentiality;
    }

//    private String getClinicalDocumentConfidentialityCode(Document doc) {
//        List<Node> nodeList = XMLUtil.getNodeList(doc, "ClinicalDocument/confidentialityCode");
//        var code = "";
//        for (Node node : nodeList) {
//            if (node.getAttributes().getNamedItem("code") != null) {
//                code = node.getAttributes().getNamedItem("code").getTextContent();
//                logger.debug("confidentiality code: '{}'", code);
//            }
//        }
//        return StringUtils.trim(code);
//    }

//    private String getClinicalDocumentConfidentialityDisplay(Document doc) {
//        List<Node> nodeList = XMLUtil.getNodeList(doc, "ClinicalDocument/confidentialityCode");
//        var display = "";
//        for (Node node : nodeList) {
//            if (node.getAttributes().getNamedItem("displayName") != null) {
//                display = node.getAttributes().getNamedItem("displayName").getTextContent();
//                logger.debug("confidentiality displayName: '{}'", display);
//            }
//        }
//        return StringUtils.trim(display);
//    }

    private String getClinicalDocumentLanguage(Document doc) {
        List<Node> nodeList = XMLUtil.getNodeList(doc, "ClinicalDocument/languageCode");
        var documentLanguage = "";
        for (Node node : nodeList) {
            if (node.getAttributes().getNamedItem("code") != null) {
                documentLanguage = node.getAttributes().getNamedItem("code").getTextContent();
                logger.debug("clinical Document language: '{}'", documentLanguage);
            }
        }
        return StringUtils.trim(documentLanguage);
    }

    @Override
    public DocumentAssociation<PSDocumentMetaData> getPSDocumentList(SearchCriteria searchCriteria) throws NIException {

        logger.info("[National Infrastructure Mock] Get Patient Summary Document List: '{}'", searchCriteria.toString());
        for (DocumentAssociation<PSDocumentMetaData> documentAssociation : psDocumentMetaDatas) {

            if (documentAssociation.getXMLDocumentMetaData() != null) {
                logger.debug("Patient ID: '{}'", documentAssociation.getXMLDocumentMetaData().getPatientId());
            } else {
                logger.debug("Document Association is null");
            }
            if (documentAssociation.getXMLDocumentMetaData() != null
                    && StringUtils.startsWith(searchCriteria.getCriteriaValue(Criteria.PATIENT_ID), documentAssociation.getXMLDocumentMetaData().getPatientId())) {
                //&& StringUtils.equals(documentAssociation.getXMLDocumentMetaData().getPatientId(), searchCriteria.getCriteriaValue(Criteria.PATIENT_ID))) {
                logger.debug("getPSDocumentList(SearchCriteria searchCriteria): '{}'", documentAssociation);
                if (documentAssociation.getPDFDocumentMetaData() == null) {
                    OriginalDataMissingException ex = new OriginalDataMissingException("[National Infrastructure Mock] No PDF found associated for the CDA");
                    ex.setOpenncpErrorCode(ERROR_PS_PDF_FORMAT_NOT_PROVIDED);
                    //throw new OriginalDataMissingException("[National Infrastructure Mock] No PDF found associated for the CDA");
                    throw ex;
                }
                return documentAssociation;
            }
        }
        NoMatchException noMatchException = new NoMatchException("[National Infrastructure Mock] No PS List Found");
        noMatchException.setOpenncpErrorCode(OpenNCPErrorCode.ERROR_PS_NOT_FOUND);
        throw noMatchException;
    }

    @Override
    public List<DocumentAssociation<EPDocumentMetaData>> getEPDocumentList(SearchCriteria searchCriteria) throws NIException {

        logger.info("[National Infrastructure Mock] Get ePrescription Document List: '{}'", searchCriteria.toString());
        Assertion assertion = NationalConnectorUtil.getTRCAssertionFromSOAPHeader(getSOAPHeader());
        NationalConnectorUtil.logAssertionAsXml(assertion);
        if (StringUtils.equals(searchCriteria.getCriteriaValue(Criteria.PATIENT_ID), "1-9999^^^&2.16.17.710.850.1000.990.1.1000&ISO")) {
            NoMatchException noMatchException = new NoMatchException("[National Infrastructure Mock] No eP List Found");
            noMatchException.setOpenncpErrorCode(OpenNCPErrorCode.ERROR_EP_NOT_FOUND);
            throw noMatchException;
        }
        if (StringUtils.equals(searchCriteria.getCriteriaValue(Criteria.PATIENT_ID), "1-9998^^^&2.16.17.710.850.1000.990.1.1000&ISO")) {
            throw new NIException(OpenNCPErrorCode.ERROR_EP_GENERIC, "Possible temporary unavailability of the service.");
        }

        List<DocumentAssociation<EPDocumentMetaData>> metaDatas = new ArrayList<>();
        for (DocumentAssociation<EPDocumentMetaData> documentAssociation : epDocumentMetaDatas) {
            if (documentAssociation.getXMLDocumentMetaData() != null
                    && StringUtils.startsWith(searchCriteria.getCriteriaValue(Criteria.PATIENT_ID), documentAssociation.getXMLDocumentMetaData().getPatientId())) {
                //&& StringUtils.equals(documentAssociation.getXMLDocumentMetaData().getPatientId(), searchCriteria.getCriteriaValue(Criteria.PATIENT_ID))) {
                metaDatas.add(documentAssociation);
                logger.debug("getEPDocumentList(SearchCriteria searchCriteria): '{}'", documentAssociation);
                if (documentAssociation.getPDFDocumentMetaData() == null) {
                    OriginalDataMissingException ex = new OriginalDataMissingException("[National Infrastructure Mock] No PDF found associated for the CDA");
                    ex.setOpenncpErrorCode(OpenNCPErrorCode.ERROR_EP_PDF_FORMAT_NOT_PROVIDED);
                    //throw new OriginalDataMissingException("[National Infrastructure Mock] No PDF found associated for the CDA");
                    throw ex;
                }
            }
        }

        if (!metaDatas.isEmpty()) {
            return metaDatas;
        }

        NoMatchException noMatchException = new NoMatchException("[National Infrastructure Mock] No eP List Found");
        noMatchException.setOpenncpErrorCode(OpenNCPErrorCode.ERROR_EP_NOT_FOUND);
        throw noMatchException;
    }

    @Override
    public List<OrCDDocumentMetaData> getOrCDHospitalDischargeReportsDocumentList(SearchCriteria searchCriteria) throws NIException {
        logger.info("[National Infrastructure Mock] Get Original Clinical Document List for Hospital Discharge Reports: '{}'", searchCriteria.toString());
        return getOrCDDocumentList(searchCriteria, orCDDocumentHospitalDischargeReportsMetaDatas);
    }

    @Override
    public List<OrCDDocumentMetaData> getOrCDLaboratoryResultsDocumentList(SearchCriteria searchCriteria) throws NIException {
        logger.info("[National Infrastructure Mock] Get Original Clinical Document List for Laboratory results: '{}'", searchCriteria.toString());
        return getOrCDDocumentList(searchCriteria, orCDDocumentLaboratoryResultsMetaDatas);
    }

    @Override
    public List<OrCDDocumentMetaData> getOrCDMedicalImagingReportsDocumentList(SearchCriteria searchCriteria) throws NIException {
        logger.info("[National Infrastructure Mock] Get Original Clinical Document List for Medical Imaging Reports: '{}'", searchCriteria.toString());
        return getOrCDDocumentList(searchCriteria, orCDDocumentMedicalImagingReportsMetaDatas);
    }

    @Override
    public List<OrCDDocumentMetaData> getOrCDMedicalImagesDocumentList(SearchCriteria searchCriteria) throws NIException {
        logger.info("[National Infrastructure Mock] Get Original Clinical Document List for Medical Images: '{}'", searchCriteria.toString());
        return getOrCDDocumentList(searchCriteria, orCDDocumentMedicalImagesMetaDatas);
    }

    private List<OrCDDocumentMetaData> getOrCDDocumentList(SearchCriteria searchCriteria, List<OrCDDocumentMetaData> orCDMetaDataList) throws ProcessingDeferredException {
        List<OrCDDocumentMetaData> metaDatas = new ArrayList<>();

        if (searchCriteria.getCriteriaValue(Criteria.PATIENT_ID).equals("3-ERROR-ORCD-GENERIC")) {
            ProcessingDeferredException exception = new ProcessingDeferredException("[National Infrastructure Mock] Mock error for ORCD scenario");
            throw exception;
        }

        Long maximumSize = null;
        var maximumSizeCriteriaString = searchCriteria.getCriteriaValue(Criteria.MAXIMUM_SIZE);
        if (!StringUtils.isEmpty(maximumSizeCriteriaString)) {
            maximumSize = Long.parseLong(maximumSizeCriteriaString);
        }
        Instant createdAfter = null;
        var createdAfterCriteriaString = searchCriteria.getCriteriaValue(Criteria.CREATED_AFTER);
        if (createdAfterCriteriaString != null) {
            createdAfter = Instant.parse(createdAfterCriteriaString);
        }
        Instant createdBefore = null;
        var createdBeforeCriteriaString = searchCriteria.getCriteriaValue(Criteria.CREATED_BEFORE);
        if (createdBeforeCriteriaString != null) {
            createdBefore = Instant.parse(createdBeforeCriteriaString);
        }

        for (OrCDDocumentMetaData orCDDocumentMetaData : orCDMetaDataList) {
            var creationInstant = orCDDocumentMetaData.getEffectiveTime().toInstant();
            //if (StringUtils.equals(orCDDocumentMetaData.getPatientId(), searchCriteria.getCriteriaValue(Criteria.PATIENT_ID))
            if (StringUtils.startsWith(searchCriteria.getCriteriaValue(Criteria.PATIENT_ID), orCDDocumentMetaData.getPatientId())
                    && (maximumSize == null || orCDDocumentMetaData.getSize() <= maximumSize)
                    && (createdBefore == null || (creationInstant.compareTo(createdBefore) <= 0))
                    && (createdAfter == null || (createdAfter.compareTo(creationInstant) <= 0))) {
                metaDatas.add(orCDDocumentMetaData);
                logger.debug("getOrCDDocumentList(SearchCriteria searchCriteria): '{}'", orCDDocumentMetaData);
            }
        }
        return metaDatas;
    }

    @Override
    public EPSOSDocument getDocument(SearchCriteria searchCriteria) throws NIException {

        logger.info("[National Infrastructure Mock] Retrieve Document: '{}', '{}', '{}'", searchCriteria.getCriteriaValue(Criteria.DOCUMENT_ID),
                searchCriteria.getCriteriaValue(Criteria.PATIENT_ID), searchCriteria.getCriteriaValue(Criteria.REPOSITORY_ID));
        if (StringUtils.equals("2-9999^^^&2.16.17.710.850.1000.990.1.1000&ISO", searchCriteria.getCriteriaValue(Criteria.PATIENT_ID))) {
            throw new NIException(OpenNCPErrorCode.ERROR_GENERIC_DOCUMENT_MISSING, "[National Infrastructure Mock] Error Retrieving Document for Patient: " +
                    searchCriteria.getCriteriaValue(Criteria.PATIENT_ID));
        }
        for (EPSOSDocument epsosDocument : documents) {
            if (epsosDocument.matchesCriteria(searchCriteria)) {
                logger.debug("getDocument(SearchCriteria searchCriteria): '{}'", epsosDocument);
                return epsosDocument;
            }
        }
        throw new NIException(OpenNCPErrorCode.ERROR_GENERIC, "[National Infrastructure Mock] Error Retrieving Document");
    }

    @Override
    public DocumentAssociation<MroDocumentMetaData> getMroDocumentList(SearchCriteria searchCriteria) {

        logger.info("[National Infrastructure Mock] Get Medication Related Overview Document List: '{}'", searchCriteria.toString());
        for (DocumentAssociation<MroDocumentMetaData> documentAssociation : mroDocumentMetaDatas) {

            if (documentAssociation.getXMLDocumentMetaData() != null
                    && StringUtils.startsWith(searchCriteria.getCriteriaValue(Criteria.PATIENT_ID), documentAssociation.getXMLDocumentMetaData().getPatientId())) {
                //&& documentAssociation.getXMLDocumentMetaData().getPatientId().equals(searchCriteria.getCriteriaValue(Criteria.PATIENT_ID))) {
                logger.debug("getMroDocumentList(SearchCriteria searchCriteria): '{}'", documentAssociation);
                return documentAssociation;
            }
        }

        return null;
    }

    private String getOIDFromDocument(Document document) {

        var oid = "";
        if (document.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "id").getLength() > 0) {
            Node id = document.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "id").item(0);
            if (id.getAttributes().getNamedItem("root") != null) {
                oid = oid + id.getAttributes().getNamedItem("root").getTextContent();
            }
            if (id.getAttributes().getNamedItem(CONSTANT_EXTENSION) != null) {
                oid = oid + "^" + id.getAttributes().getNamedItem(CONSTANT_EXTENSION).getTextContent();
            }
        }
        logger.debug("CDA Document ID: '{}'", oid);
        return oid;
    }

    private String getTitleFromDocument(Document doc) {

        NodeList documentNames = doc.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "title");

        if (documentNames != null && documentNames.getLength() > 0) {
            Node titleNode = documentNames.item(0);
            return titleNode.getTextContent();
        }
        logger.debug("Could not locate the title of the prescription");
        return "Document Title Not Available";
    }

    private Element getProductFromPrescription(Document document) {
        NodeList elements = document.getElementsByTagNameNS(EHDSI_EPSOS_MEDICATION_NAMESPACE, "generalizedMedicineClass");
        if (elements.getLength() == 0) {
            return null;
        }
        NodeList children = elements.item(0)
                .getChildNodes();
        for (var i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (Objects.equals(Node.ELEMENT_NODE, node.getNodeType()) &&
                    Objects.equals("code", node.getLocalName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private void wrapPDFinCDA(byte[] pdf, Document doc) {

        logger.debug("NameSpace: '{}', Document URI '{}', XML encoding: '{}', BaseURI: '{}'", doc.getNamespaceURI(),
                doc.getDocumentURI(), doc.getXmlEncoding(), doc.getBaseURI());

        // Remove old component element
        Node oldComponent = doc.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "component").item(0);
        Element newComponent = doc.createElementNS(EHDSI_HL7_NAMESPACE, "component");

        // Replace templateID value
        Node templateId = doc.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "templateId").item(0);
        Node value = templateId.getAttributes().getNamedItem("root");
        String val = value.getNodeValue();
        String updatedValue = StringUtils.equals(val, EHDSI_PS_L3_TEMPLATE_ID) ? EHDSI_PS_L1_TEMPLATE_ID : EHDSI_EP_L1_TEMPLATE_ID;
        value.setNodeValue(updatedValue);

        // Add new component element
        Element nonXMLBody = doc.createElementNS(EHDSI_HL7_NAMESPACE, "nonXMLBody");
        nonXMLBody.setAttribute("classCode", "DOCBODY");
        nonXMLBody.setAttribute("moodCode", "EVN");
        Element text = doc.createElementNS(EHDSI_HL7_NAMESPACE, "text");

        text.setAttribute("mediaType", "application/pdf");
        text.setAttribute("representation", "B64");
        text.setTextContent(new String(Base64.encodeBase64(pdf)));

        nonXMLBody.appendChild(text);
        newComponent.appendChild(nonXMLBody);

        Node rootNode = doc.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "ClinicalDocument").item(0);

        rootNode.replaceChild(newComponent, oldComponent);
        logger.debug("PDF document added.");
    }

    private void addFormatToOID(Document document, int format) {

        if (document.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "id").getLength() > 0) {
            Element id = (Element) document.getElementsByTagNameNS(EHDSI_HL7_NAMESPACE, "id").item(0);
            if (id.hasAttribute(CONSTANT_EXTENSION)) {
                id.setAttribute(CONSTANT_EXTENSION, id.getAttribute(CONSTANT_EXTENSION) + "." + format);
            } else {
                id.setAttribute(CONSTANT_EXTENSION, Integer.toString(format));
            }
        }
    }

    private String getDescriptionFromDocument(Document doc) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        String description = null;

        try {
            description = path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='name']/text()", doc) +
                    ", " + path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='formCode']/@displayName", doc) +
                    ", " + path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='desc']/text()", doc);
        } catch (XPathExpressionException e) {
            logger.error("XPath expression error", e);
        }

        return description;
    }

    private String getAtcCode(Document doc) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        String atcCode = null;

        try {
            atcCode = path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='asSpecializedKind']/*[local-name()='generalizedMedicineClass']/*[local-name()='code']/@code", doc);
        } catch (XPathExpressionException e) {
            logger.error("XPath expression error", e);
        }

        return atcCode;
    }

    private String getAtcName(Document doc) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        String atcName = null;

        try {
            atcName = path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='asSpecializedKind']/*[local-name()='generalizedMedicineClass']/*[local-name()='code']/@displayName", doc);
        } catch (XPathExpressionException e) {
            logger.error("XPath expression error", e);
        }

        return atcName;
    }

    private String getDoseFormCode(Document doc) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        String doseFormCode = null;

        try {
            doseFormCode = path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='asContent']/*[local-name()='containerPackagedMedicine']/*[local-name()='formCode']/@code", doc);
        } catch (XPathExpressionException e) {
            logger.error("XPath expression error", e);
        }

        return doseFormCode;
    }

    private String getDoseFormName(Document doc) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        String doseFormName = null;

        try {
            doseFormName = path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='asContent']/*[local-name()='containerPackagedMedicine']/*[local-name()='formCode']/@displayName", doc);
        } catch (XPathExpressionException e) {
            logger.error("XPath expression error", e);
        }

        return doseFormName;
    }

    private String getStrength(Document doc) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        String strength = null;

        try {
            //strength = path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='asContent']/*[local-name()='containerPackagedMedicine']/*[local-name()='capacityQuantity']/@value", doc);
            strength = path.evaluate("//*[local-name()='manufacturedMaterial']/*[local-name()='desc']/text()", doc);
        } catch (XPathExpressionException e) {
            logger.error("XPath expression error", e);
        }

        return strength;
    }

    private EPDocumentMetaData.SubstitutionMetaData getSubstitution(Document doc) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        String substitution;

        //substitution = path.evaluate("//*[local-name()='entryRelationship']/*[local-name()='observation']/*[local-name()='SubstanceAdminSubstitution']/@value", doc);
        // Substitution is always allowed in Country B unless formally specified SUBST with code=N which means no substitution,
        // all other cases (No SUBST element, code G=Generic or code TE=Therapeutic alternative means substitution is allowed
        List<Node> nodeListCode = XMLUtil.getNodeList(doc, "/ClinicalDocument/component/structuredBody/component/section/entry/substanceAdministration[@classCode = 'SBADM']/entryRelationship[@typeCode = 'SUBJ']/observation[@classCode = 'OBS']/code[@code = 'SUBST']");
        List<Node> nodeListValue = XMLUtil.getNodeList(doc, "/ClinicalDocument/component/structuredBody/component/section/entry/substanceAdministration[@classCode = 'SBADM']/entryRelationship[@typeCode = 'SUBJ']/observation[@classCode = 'OBS']/value[@code = 'N']");

        var substitutionMetadata = new EPDocumentMetaDataImpl.SimpleSubstitutionMetadata(SubstitutionCodeEnum.G);
        if (nodeListCode != null && !nodeListCode.isEmpty()) {
            for (Node node : nodeListValue) {
                String valueAttr = node.getNodeName();
                String codeAttr = node.getAttributes().getNamedItem("code").getNodeValue();
                logger.debug("Value: '{}' - Code: '{}'", valueAttr, codeAttr);
                if (valueAttr.equals("value") && codeAttr.equals("N")) {
                    substitutionMetadata = new EPDocumentMetaDataImpl.SimpleSubstitutionMetadata(SubstitutionCodeEnum.N);
                } else {
                    substitutionMetadata = new EPDocumentMetaDataImpl.SimpleSubstitutionMetadata(SubstitutionCodeEnum.G);
                }
                break;
            }
        }
        return substitutionMetadata;
    }

    private boolean getDispensable(Document xmlDoc) {
        return true;
    }
}
