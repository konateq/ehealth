package se.sb.epsos.web.service;

import epsos.ccd.netsmart.securitymanager.sts.client.TRCAssertionRequest;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.PropertyNotFoundException;
import hl7OrgV3.ClinicalDocumentDocument1;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.impl.AssertionMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import se.sb.epsos.shelob.ws.client.jaxws.*;
import se.sb.epsos.web.auth.AuthenticatedUser;
import se.sb.epsos.web.model.*;
import se.sb.epsos.web.util.*;
import tr.com.srdc.epsos.util.Constants;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * This class is the service facade to the NCP and handles all the service calls.
 *
 * @author Anders
 */
public class NcpServiceFacadeImpl implements NcpServiceFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcpServiceFacadeImpl.class);
    private static final GraphiteLogger GRAPHITELOGGER = GraphiteLogger.getDefaultLogger();
    private ClientConnectorServiceService service;
    private ClientConnectorService shelobConnector;
    private TrcServiceHandler trcServiceHandler;
    private String sessionId;
    private AssertionHandler assertionHandler = new AssertionHandler();

    public NcpServiceFacadeImpl(ClientConnectorServiceService service, ClientConnectorService shelobConnector,
                                TrcServiceHandler trcServiceHandler) {
        this.service = service;
        this.trcServiceHandler = trcServiceHandler;
        this.shelobConnector = shelobConnector;
    }

    public NcpServiceFacadeImpl() {
        this.service = NcpClientConnector.createClientConnector();
        this.trcServiceHandler = new TrcServiceHandler();
    }

    private static XMLGregorianCalendar getXMLGregorian(GregorianCalendar calendar) {
        XMLGregorianCalendar date2 = null;
        try {
            date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("DatatypeConfigurationException: '{}'", e.getMessage(), e);
        }
        return date2;
    }

    private static XMLGregorianCalendar getDate(String indate, DateTimeFormatter df) {
        DateTime dateTime = df.parseDateTime(indate);
        return getXMLGregorian(dateTime.toGregorianCalendar());
    }

    private static String getUniqueId() {

        String uniqueId;
        String pnoid = ConfigurationManagerFactory.getConfigurationManager().getProperty("HOME_COMM_ID");
        String prop = "pn.uniqueid";
        int pid;

        try {

            pid = Integer.parseInt(ConfigurationManagerFactory.getConfigurationManager().getProperty(prop));
            pid = pid + 1;
            uniqueId = pnoid + "." + pid;
            ConfigurationManagerFactory.getConfigurationManager().setProperty(prop, String.valueOf(pid));
        } catch (PropertyNotFoundException e) {
            ConfigurationManagerFactory.getConfigurationManager().setProperty(prop, "1");
            uniqueId = pnoid + "." + "1";
        }

        return uniqueId;
    }

    @Override
    public String about() {
        return getClass().getSimpleName() + " (online: epSOS-Web)";
    }

    @Override
    public void bindToSession(String sessionId) {
        LOGGER.debug("Binding service facade to session: '{}'", sessionId);
        this.sessionId = sessionId;
    }

    @Override
    public void initUser(final AuthenticatedUser userDetails) {
        /*
         * Creating web service call header with assertion.
         */
        service.setHandlerResolver(portInfo -> {
            List<Handler> handlerList = new ArrayList<>();
            handlerList.add(new RGBSOAPHandler(userDetails));
            return handlerList;
        });

        /*
         * If you want to change this port you have to change it on server side in the axis2.xml
         */
        if (FeatureFlagsManager.check(Feature.ENABLE_SSL)) {
            shelobConnector = service.getPort(new QName("http://cc.pt.epsos.eu", "ClientConnectorServiceHttpsSoap11Endpoint"), ClientConnectorService.class);
        } else {
            shelobConnector = service.getPort(new QName("http://cc.pt.epsos.eu", "ClientConnectorServiceHttpSoap11Endpoint"), ClientConnectorService.class);
        }
    }

    @Override
    public List<Person> queryForPatient(AuthenticatedUser userDetails, List<PatientIdVO> patientList, CountryVO country) throws NcpServiceException {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("queryForPatient called for country: '{}'", country.getId());
            LOGGER.info("Patient ids: '{}", Arrays.toString(patientList.toArray()));
        }

        QueryPatientRequest queryPatientRequest = createQueryPatientRequest(patientList, country);
        List<Person> personList = new ArrayList<>();
        if (shelobConnector != null) {
            try {
                List<PatientDemographics> queryPatient = shelobConnector.queryPatient(queryPatientRequest);
                for (PatientDemographics dem : queryPatient) {
                    Person person = new Person(this.sessionId, dem, country.getId());
                    personList.add(person);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Patient ID: '{}'", person.getId());
                        LOGGER.debug("Patient Country: '{}", person.getCountry());
                        LOGGER.debug("Patient Gender: '{}", person.getGender());
                        LOGGER.debug("Patient Birthdate: '{}", dem.getBirthDate());
                    }
                }
            } catch (Exception e) {
                throw new NcpServiceException(e.getMessage(), e);
            }
        } else {
            throw new NcpServiceException("Webservice is not initialized", new Exception());
        }
        return personList;
    }

    private QueryPatientRequest createQueryPatientRequest(List<PatientIdVO> list, CountryVO country) {

        QueryPatientRequest createQueryPatientRequest = new QueryPatientRequest();
        PatientDemographics dem = new PatientDemographics();
        createQueryPatientRequest.setCountryCode(country.getId());
        dem.setCountry(country.getId());
        for (PatientIdVO id : list) {
            if (id.getDomain() != null && id.getValue() != null) {
                LOGGER.debug("Identifier '{}' domain: '{}'", id.getLabel(), id.getDomain());
                LOGGER.debug("Identifier '{}' value: '{}'", id.getLabel(), id.getValue());
                PatientId patientId = new PatientId();
                patientId.setRoot(id.getDomain());
                patientId.setExtension(id.getValue());
                dem.getPatientId().add(patientId);
            } else if (id.getLabel().contains("address") && id.getValue() != null) {
                LOGGER.debug("address: '{}'", id.getValue());
                dem.setStreetAddress(id.getValue());
            } else if (id.getLabel().contains("city") && id.getValue() != null) {
                LOGGER.debug("city: '{}'", id.getValue());
                dem.setCity(id.getValue());
            } else if (id.getLabel().contains("code") && id.getValue() != null) {
                LOGGER.debug("code: '{}'", id.getValue());
                dem.setPostalCode(id.getValue());
            } else if (id.getLabel().contains("givenname") && id.getValue() != null) {
                LOGGER.debug("givenname: '{}'", id.getValue());
                dem.setGivenName(id.getValue());
            } else if (id.getLabel().contains("surname") && id.getValue() != null) {
                LOGGER.debug("surname: '{}'", id.getValue());
                dem.setFamilyName(id.getValue());
            } else if (id.getLabel().contains("birth.date") && id.getValue() != null) {
                XMLGregorianCalendar g = getDate(id.getValue(), ISODateTimeFormat.basicDate());
                LOGGER.debug("birthdate: '{}' birthdate as xml: '{}'", id.getValue(), g);
                dem.setBirthDate(g);
            } else if (id.getLabel().contains("sex") && id.getValue() != null) {
                LOGGER.debug("sex: '{}'", id.getValue());
                dem.setAdministrativeGender(id.getValue());
            }
            createQueryPatientRequest.setPatientDemographics(dem);
        }
        return createQueryPatientRequest;
    }

    /**
     * Fetches a TRC assertion for the user. The user gets a new HCP assertion every time before generating a TRC assertion
     * Reason: HCP assertion has max 4 hours life time, and might expire during user session time
     */
    @Override
    public void setTRCAssertion(TRC trc, AuthenticatedUser userDetails) throws NcpServiceException {

        LOGGER.info("start::setTRCAssertion()");
        try {
            LOGGER.info("Building TRC request");
            LOGGER.info("Epsos ID: '{}'", trc.getPerson().getEpsosId());
            LOGGER.info("Purpose: '{}'", trc.getPurpose());

            // Generate a new HCP assertion for the user
            Assertion assertion = assertionHandler.createSAMLAssertion(userDetails);
            assertionHandler.signSAMLAssertion(assertion);
            userDetails.setAssertion(assertion);

            AssertionMarshaller marshaller2 = new AssertionMarshaller();
            Element element2 = marshaller2.marshall(userDetails.getAssertion());
            Document document2 = element2.getOwnerDocument();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Assertion1:\n'{}'", new XmlUtil().prettyPrint(document2));
            }
            TRCAssertionRequest trcAssertionRequest = trcServiceHandler.buildTrcRequest(userDetails.getAssertion(), trc.getPerson().getEpsosId(),
                    trc.getPurpose());
            LOGGER.info("Building TRC request done");
            if (trcAssertionRequest != null) {
                LOGGER.info("Making request to TRC-STS:");
                Assertion trcAssertion = trcAssertionRequest.request();
                LOGGER.info("TRC-STS request done");
                AssertionMarshaller marshaller = new AssertionMarshaller();
                Element element = marshaller.marshall(trcAssertion);
                Document document = element.getOwnerDocument();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Assertion2:\n'{}'", new XmlUtil().prettyPrint(document));
                }
                userDetails.setTrc(trc);
                userDetails.setTrcAssertion(trcAssertion);
            } else {
                LOGGER.error("TrcAssertionRequest was null");
                throw new NcpServiceException("Failed to build TRC Request, TRCAssertionRequest was null", new Exception());
            }
        } catch (Exception e) {
            LOGGER.error("TRC webservice call failed", e);
            throw new NcpServiceException("TRC Webservice call failed", e);
        }
        LOGGER.info("stop::setTRCAssertion()");
    }

    @Override
    public List<MetaDocument> queryDocuments(Person person, String doctype, AuthenticatedUser userDetails) throws NcpServiceException {

        PatientDemographics dem = person.getPatientDemographics();
        LOGGER.debug("HomeCommunityId: '{}'", dem.getPatientId().get(0).getRoot());
        QueryDocumentRequest request = new QueryDocumentRequest();
        GenericDocumentCode classCode = new GenericDocumentCode();
        if ("EP".equals(doctype)) {
            classCode.setNodeRepresentation("57833-6");
            classCode.setValue("57833-6");
        } else if ("PS".equals(doctype)) {
            classCode.setNodeRepresentation("60591-5");
            classCode.setValue("60591-5");
        }

        classCode.setSchema("2.16.840.1.113883.6.1");
        request.setClassCode(classCode);
        request.setCountryCode(person.getCountryCode());
        request.setPatientId(person.getPatientId());

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("'{}'", XmlUtil.marshallJaxbObject(new XmlTypeWrapper<>(request)));
            }
        } catch (JAXBException e) {
            LOGGER.warn("Failed to marshall Jaxb object for debug logging.", e);
        }

        List<MetaDocument> docList = new ArrayList<>();
        try {
            List<EpsosDocument> list = shelobConnector.queryDocuments(request);
            for (EpsosDocument doc : list) {
                MetaDocument metaDocument = new MetaDocument(this.sessionId, person.getEpsosId(), doc);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created MetaDocument with key: '{}'", metaDocument.getDtoCacheKey());
                }
                docList.add(metaDocument);
            }
        } catch (Exception e) {
            throw new NcpServiceException(e.getMessage(), e);
        }
        GRAPHITELOGGER.logMetric("epsos-web.service.queryDocuments.success." + person.getCountryCode() + "." + doctype, 1L);
        return docList;
    }

    @Override
    public CdaDocument retrieveDocument(MetaDocument doc) throws NcpServiceException {
        String hcid = doc.getDoc().getHcid();
        if (hcid != null && hcid.startsWith(Constants.OID_PREFIX)) {
            hcid = hcid.substring(8);
        }
        CountryVO country = CountryConfigManager.getCountry(hcid);
        String countryCode = country != null ? country.getId() : "UNKNOWN";
        CdaDocument document = null;

        RetrieveDocumentRequest retrieveDocumentRequest = new RetrieveDocumentRequest();
        retrieveDocumentRequest.setCountryCode(countryCode);
        retrieveDocumentRequest.setClassCode(doc.getDoc().getClassCode());
        DocumentId documentId = new DocumentId();
        documentId.setDocumentUniqueId(doc.getDoc().getUuid());
        documentId.setRepositoryUniqueId(doc.getDoc().getRepositoryId());
        retrieveDocumentRequest.setDocumentId(documentId);
        retrieveDocumentRequest.setHomeCommunityId(addOIDPrefix(doc.getDoc().getHcid()));

        try {
            EpsosDocument epsosDocument = shelobConnector.retrieveDocument(retrieveDocumentRequest);
            LOGGER.debug("Doc: '{}'", epsosDocument);
            byte[] bytes = epsosDocument.getBase64Binary();
            if (doc.getType().equals(DocType.EP)) {
                document = new Prescription(doc, bytes, epsosDocument);
            } else if (doc.getType().equals(DocType.PS)) {
                document = new Patientsummary(doc, bytes, epsosDocument);
            } else if (doc.getType().equals(DocType.PDF)) {
                document = new PdfDocument(doc, bytes, epsosDocument);
            }
        } catch (ParserConfigurationException | SAXException | XmlException | IOException e) {
            GRAPHITELOGGER.logMetric("epsos-web.service.retrieveDocument.failed." + countryCode + "." + doc.getType(), 1L);
            throw new NcpServiceException("Failed to parse retrieved document", e);
        } catch (SOAPFaultException sfe) {
            GRAPHITELOGGER.logMetric("epsos-web.service.retrieveDocument.failed. " + sfe.getMessage(), 1L);
            throw new NcpServiceException(sfe.getMessage(), sfe);
        }

        GRAPHITELOGGER.logMetric("epsos-web.service.retrieveDocument.success." + countryCode + "." + doc.getType(), 1L);
        if (LOGGER.isDebugEnabled() && document != null) {
            LOGGER.debug("Retreived Document:\n{}", new String(document.getBytes()));
        }
        return document;
    }

    @Override
    public byte[] submitDocument(Dispensation dispensation, AuthenticatedUser user, Person person, String eD_PageAsString) throws NcpServiceException {

        byte[] bytes;
        String oidRoot = MasterConfigManager.get("ApplicationConfigManager.xmlDispensationRoot");
        String cdaIdExtension = Long.toString(System.currentTimeMillis());
        String pdfIdExtension = Long.toString(System.currentTimeMillis());
        String dispensationDocumentCacheKey = dispensation.getDoc().getUuid();
        dispensation.getDoc().setUuid(oidRoot + "^" + cdaIdExtension);
        try {
            bytes = getDispensationDocument(dispensation, user, cdaIdExtension, pdfIdExtension);
        } catch (Exception e) {
            LOGGER.error("Exception", e);
            throw new NcpServiceException("Failed to create CDA", e);
        }

        byte[] pdfInBytes;
        byte[] pdfCdaInBytes;
        try {
            pdfInBytes = PdfHandler.convertStringToPdf(eD_PageAsString);
            pdfCdaInBytes = getDispensationAsByteArray(getDispensationDocumentPDF(pdfInBytes, dispensation, user, cdaIdExtension, pdfIdExtension));
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            throw new NcpServiceException("Failed to create PDF", e);
        }
        EpsosDocument doc = dispensation.getDoc();
        doc.setMimeType("text/xml");
        doc.setBase64Binary(bytes);
        doc.setSubmissionSetId(getUniqueId());

        GenericDocumentCode classCode = new GenericDocumentCode();
        classCode.setNodeRepresentation("60593-1");
        classCode.setSchema("2.16.840.1.113883.6.1");
        doc.setClassCode(classCode);
        GenericDocumentCode formatCode = new GenericDocumentCode();
        formatCode.setNodeRepresentation("urn:epSOS:ep:dis:2010");
        doc.setFormatCode(formatCode);

        EpsosDocument docPdf = new EpsosDocument();
        docPdf.setMimeType("text/xml");
        docPdf.setBase64Binary(pdfCdaInBytes);
        GenericDocumentCode classCodePdf = new GenericDocumentCode();
        classCodePdf.setNodeRepresentation("60593-1");
        docPdf.setClassCode(classCodePdf);
        GenericDocumentCode formatCodePdf = new GenericDocumentCode();
        formatCodePdf.setNodeRepresentation("urn:ihe:iti:xds-sd:pdf:2008");
        docPdf.setFormatCode(formatCodePdf);
        doc.getAssociatedDocuments().add(docPdf);

        SubmitDocumentRequest request = new SubmitDocumentRequest();
        request.setCountryCode(person.getCountryCode());
        request.setDocument(doc);
        request.setPatientDemographics(person.getPatientDemographics());

        try {
            shelobConnector.submitDocument(request);
            LOGGER.info("Submitdocument is done.");
            GRAPHITELOGGER.logMetric("epsos-web.service.submitDocument.success." + person.getCountryCode(), 1L);
        } catch (SOAPFaultException sfe) {
            // Revert dispensation uuid, because retry is not possible if the dispensation uuid does not match the uuid in DocumentCache
            dispensation.getDoc().setUuid(dispensationDocumentCacheKey);
            throw new NcpServiceException("Failed to submit document.", sfe);
        } finally {
            StringBuilder buf = new StringBuilder();
            buf.append(new String(doc.getBase64Binary())).append("\n");
            for (EpsosDocument epsDoc : doc.getAssociatedDocuments()) {
                buf.append("Child document: ");
                buf.append(new String(epsDoc.getBase64Binary())).append("\n");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Submitted Documents:\n{}", buf);
            }
        }
        return pdfInBytes;
    }

    private ClinicalDocumentDocument1 getDispensationDocumentPDF(byte[] bytes, Dispensation dispensation,
                                                                 AuthenticatedUser user, String cdaIdExtension,
                                                                 String pdfIdExtension) throws Exception {

        ePtoeDMapper mapper = new ePtoeDMapper();
        return mapper.createDispensation_PDF(bytes, dispensation, user, cdaIdExtension, pdfIdExtension);
    }

    private byte[] getDispensationDocument(Dispensation dispensation, AuthenticatedUser user, String cdaIdExtension,
                                           String pdfIdExtension) throws Exception {

        ePtoeDMapper mapper = new ePtoeDMapper();
        return mapper.createDispensationFromPrescription(dispensation, user, cdaIdExtension, pdfIdExtension);
    }

    private byte[] getDispensationAsByteArray(ClinicalDocumentDocument1 eD_Document) {

        String xml = eD_Document.xmlText(new XmlOptions().setCharacterEncoding(StandardCharsets.UTF_8.name()).setSavePrettyPrint());
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private String addOIDPrefix(String oid) {
        if (oid == null) {
            return null;
        }
        if (oid.startsWith(Constants.OID_PREFIX)) {
            return oid;
        }
        return Constants.OID_PREFIX + oid;
    }
}
