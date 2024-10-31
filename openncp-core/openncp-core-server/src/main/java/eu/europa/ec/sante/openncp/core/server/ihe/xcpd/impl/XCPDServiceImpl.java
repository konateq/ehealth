package eu.europa.ec.sante.openncp.core.server.ihe.xcpd.impl;

import eu.europa.ec.sante.openncp.common.audit.*;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.configuration.util.OpenNCPConstants;
import eu.europa.ec.sante.openncp.common.configuration.util.ServerMode;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.util.DateUtil;
import eu.europa.ec.sante.openncp.common.util.HttpUtil;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.OpenNCPErrorCodeException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML2Validator;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.*;
import eu.europa.ec.sante.openncp.core.common.ihe.evidence.EvidenceUtils;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCPDErrorCode;
import eu.europa.ec.sante.openncp.core.common.util.SoapElementHelper;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.PatientSearchInterface;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.PatientSearchInterfaceWithDemographics;
import eu.europa.ec.sante.openncp.core.server.api.ihe.xcpd.XCPDNIException;
import eu.europa.ec.sante.openncp.core.server.ihe.xcpd.XCPDServiceInterface;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class XCPDServiceImpl implements XCPDServiceInterface {

    private static final DatatypeFactory DATATYPE_FACTORY;
    private static final String INSTANCE = "INSTANCE";

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (final DatatypeConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(XCPDServiceImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final PatientSearchInterface patientSearchService;
    private final SAML2Validator saml2Validator;

    public XCPDServiceImpl(final PatientSearchInterface patientSearchService, final SAML2Validator saml2Validator) {
        this.patientSearchService = Validate.notNull(patientSearchService);
        this.saml2Validator = Validate.notNull(saml2Validator);
    }

    private String getParticipantObjectID(final II id) {
        return id.getExtension() + "^^^&" + id.getRoot() + "&ISO";
    }

    public void prepareEventLog(final EventLog eventLog, final PRPAIN201305UV02 inputMessage, final PRPAIN201306UV02 outputMessage, final Element soapHeader) {

        logger.info("[XCPD Service] Preparing Event Log: '{}'", eventLog.getEventType());
        eventLog.setEventType(EventType.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS);
        eventLog.setEI_TransactionName(TransactionName.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS);
        eventLog.setEI_EventActionCode(EventActionCode.EXECUTE);
        eventLog.setEI_EventDateTime(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()));
        final String userIdAlias = SoapElementHelper.getAssertionsSPProvidedId(soapHeader);
        eventLog.setHR_UserID(StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "" + "<" + SoapElementHelper.getUserID(soapHeader)
                + "@" + SoapElementHelper.getAssertionsIssuer(soapHeader) + ">");
        eventLog.setHR_AlternativeUserID(SoapElementHelper.getAlternateUserID(soapHeader));
        eventLog.setHR_RoleID(SoapElementHelper.getRoleID(soapHeader));
        // Add point of care to the event log for assertion purposes
        eventLog.setPC_UserID(SoapElementHelper.getPointOfCareUserId(soapHeader));
        eventLog.setPC_RoleID(SoapElementHelper.getPC_RoleID(soapHeader));
        eventLog.setSP_UserID(HttpUtil.getSubjectDN(true));

        // Update audit with Patient ID returned
        final ArrayList<String> requestParticipantObjectIds = new ArrayList<>();
        for (final PRPAMT201306UV02LivingSubjectId livingSubjectId : inputMessage.getControlActProcess().getQueryByParameter().getValue().getParameterList().getLivingSubjectId()) {
            requestParticipantObjectIds.add(getParticipantObjectID(livingSubjectId.getValue().get(0)));
        }
        final ArrayList<String> responseParticipantObjectIds = new ArrayList<>();
        for (final PRPAIN201306UV02MFMIMT700711UV01Subject1 subject1 : outputMessage.getControlActProcess().getSubject()) {
            responseParticipantObjectIds.add(getParticipantObjectID(subject1.getRegistrationEvent().getSubject1().getPatient().getId().get(0)));
        }
        eventLog.setPT_ParticipantObjectIDs(CollectionUtils.isNotEmpty(responseParticipantObjectIds) ? responseParticipantObjectIds : requestParticipantObjectIds);

        // Check if patient id mapping has occurred, prepare event log for patient audit mapping in this case
        if (!CollectionUtils.isEqualCollection(responseParticipantObjectIds, requestParticipantObjectIds)) {
            logger.warn("Patient Source and Target are different: Identifier has been mapped, Patient Mapping audit scheme might be used");
        }
        eventLog.setAS_AuditSourceId(Constants.COUNTRY_PRINCIPAL_SUBDIVISION);
        if (!outputMessage.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()) {
            final String detail = outputMessage.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent();
            final String errorCode = outputMessage.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getCode().getCode();

            if(errorCode.equals(OpenNCPErrorCode.ERROR_PI_NO_MATCH.getCode())) {
                eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.TEMPORAL_FAILURE);
            }else{
                eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.PERMANENT_FAILURE);
            }
            eventLog.setEM_ParticipantObjectID(errorCode);
            eventLog.setEM_ParticipantObjectDetail(detail.getBytes());
        } else {
            eventLog.setEI_EventOutcomeIndicator(EventOutcomeIndicator.FULL_SUCCESS);
        }
    }

    public PRPAIN201306UV02 queryPatient(final PRPAIN201305UV02 request, final SOAPHeader soapHeader, final EventLog eventLog) throws Exception {

        final var response = objectFactory.createPRPAIN201306UV02();
        pRPAIN201306UV02Builder(request, response, soapHeader, eventLog);
        return response;
    }

    private PRPAIN201306UV02MFMIMT700711UV01Subject1 getSubjectByPatientDemographic(final PatientDemographics patientDemographics) {

        final var response = objectFactory.createPRPAIN201306UV02MFMIMT700711UV01Subject1();
        response.getTypeCode().add("SUBJ");

        // Set registrationEvent
        response.setRegistrationEvent(objectFactory.createPRPAIN201306UV02MFMIMT700711UV01RegistrationEvent());
        response.getRegistrationEvent().getClassCode().add("REG");
        response.getRegistrationEvent().getMoodCode().add("EVN");

        // Set registrationEvent/id
        response.getRegistrationEvent().getId().add(objectFactory.createII());
        response.getRegistrationEvent().getId().get(0).getNullFlavor().add("NA");

        // Set registrationEvent/statusCode
        response.getRegistrationEvent().setStatusCode(objectFactory.createCS());
        response.getRegistrationEvent().getStatusCode().setCode("active");

        // Create registrationEvent/Subject
        response.getRegistrationEvent().setSubject1(objectFactory.createPRPAIN201306UV02MFMIMT700711UV01Subject2());
        response.getRegistrationEvent().getSubject1().setTypeCode(ParticipationTargetSubject.SBJ);

        // Create registrationEvent/Subject/Patient
        response.getRegistrationEvent().getSubject1().setPatient(objectFactory.createPRPAMT201310UV02Patient());
        response.getRegistrationEvent().getSubject1().getPatient().getClassCode().add("PAT");

        // Set registrationEvent/Subject/Patient/id
        response.getRegistrationEvent().getSubject1().getPatient().getId().add(objectFactory.createII());
        response.getRegistrationEvent().getSubject1().getPatient().getId().get(0).setRoot(patientDemographics.getIdList().get(0).getRoot());
        response.getRegistrationEvent().getSubject1().getPatient().getId().get(0).setExtension(patientDemographics.getIdList().get(0).getExtension());

        // Set registrationEvent/Subject/Patient/statusCode
        response.getRegistrationEvent().getSubject1().getPatient().setStatusCode(objectFactory.createCS());
        response.getRegistrationEvent().getSubject1().getPatient().getStatusCode().setCode("active");

        // Set registrationEvent/Subject/Patient/patientPerson
        final var prpamt201310UV02Person = objectFactory.createPRPAMT201310UV02Person();
        prpamt201310UV02Person.getClassCode().add("PSN");
        prpamt201310UV02Person.setDeterminerCode(INSTANCE);
        prpamt201310UV02Person.setAdministrativeGenderCode(getAdministrativeCode(patientDemographics));
        prpamt201310UV02Person.setBirthTime(getBirthTime(patientDemographics));
        prpamt201310UV02Person.getName().add(getName(patientDemographics));
        prpamt201310UV02Person.getAddr().add(getAddress(patientDemographics));
        response.getRegistrationEvent().getSubject1().getPatient().setPatientPerson(
                objectFactory.createPRPAMT201310UV02PatientPatientPerson(prpamt201310UV02Person));

        // Set registrationEvent/Subject/Patient/subjectOf1
        response.getRegistrationEvent().getSubject1().getPatient().getSubjectOf1().add(
                objectFactory.createPRPAMT201310UV02Subject());

        // Set registrationEvent/Subject/Patient/subjectOf1/queryMatchObservation
        response.getRegistrationEvent().getSubject1().getPatient().getSubjectOf1().get(0)
                .setQueryMatchObservation(objectFactory.createPRPAMT201310UV02QueryMatchObservation());
        response.getRegistrationEvent().getSubject1().getPatient().getSubjectOf1().get(0).getQueryMatchObservation()
                .getClassCode().add("OBS");
        response.getRegistrationEvent().getSubject1().getPatient().getSubjectOf1().get(0).getQueryMatchObservation()
                .getMoodCode().add("EVN");
        response.getRegistrationEvent().getSubject1().getPatient().getSubjectOf1().get(0).getQueryMatchObservation()
                .setCode(objectFactory.createCD());
        response.getRegistrationEvent().getSubject1().getPatient().getSubjectOf1().get(0).getQueryMatchObservation()
                .getCode().setCode("IHE_PDQ");
        response.getRegistrationEvent().getSubject1().getPatient().getSubjectOf1().get(0).getQueryMatchObservation()
                .getCode().setCodeSystem("2.16.840.1.113883.1.11.19914");

        final var matchInt = objectFactory.createINT();
        matchInt.setValue(BigInteger.valueOf(100));
        response.getRegistrationEvent().getSubject1().getPatient().getSubjectOf1().get(0).getQueryMatchObservation()
                .setValue(matchInt);

        // Set registrationEvent/custodian
        response.getRegistrationEvent().setCustodian(objectFactory.createMFMIMT700711UV01Custodian());
        response.getRegistrationEvent().getCustodian().getTypeCode().add("CST");

        // Set registrationEvent/custodian/assignedEntity
        response.getRegistrationEvent().getCustodian().setAssignedEntity(objectFactory.createCOCTMT090003UV01AssignedEntity());
        response.getRegistrationEvent().getCustodian().getAssignedEntity().setClassCode("ASSIGNED");

        // Set registrationEvent/custodian/assignedEntity/id
        response.getRegistrationEvent().getCustodian().getAssignedEntity().getId().add(objectFactory.createII());
        response.getRegistrationEvent().getCustodian().getAssignedEntity().getId().get(0).setRoot(Constants.HOME_COMM_ID);

        // Set registrationEvent/custodian/assignedEntity/code
        response.getRegistrationEvent().getCustodian().getAssignedEntity().setCode(objectFactory.createCE());
        response.getRegistrationEvent().getCustodian().getAssignedEntity().getCode().setCode("NotHealthDataLocator");
        response.getRegistrationEvent().getCustodian().getAssignedEntity().getCode().setCodeSystem("1.3.6.1.4.1.19376.1.2.27.2");

        return response;
    }

    private CE getAdministrativeCode(final PatientDemographics pd) {
        final var result = objectFactory.createCE();
        result.setCode(pd.getAdministrativeGender().toString());
        return result;
    }

    private TS getBirthTime(final PatientDemographics pd) {
        final var result = objectFactory.createTS();
        final var date = pd.getBirthDate();
        final var dateFormat = new SimpleDateFormat("yyyyMMdd");
        result.setValue(dateFormat.format(date));
        return result;
    }

    private PN getName(final PatientDemographics pd) {
        final var result = objectFactory.createPN();
        final var enFamily = objectFactory.createEnFamily();
        enFamily.setContent(pd.getFamilyName());
        result.getContent().add(objectFactory.createENFamily(enFamily));

        final var enGiven = objectFactory.createEnGiven();
        enGiven.setContent(pd.getGivenName());
        result.getContent().add(objectFactory.createENGiven(enGiven));
        return result;
    }

    private AD getAddress(final PatientDemographics patientDemographics) {

        final var result = objectFactory.createAD();
        // Adding the city
        if (StringUtils.isNotBlank(patientDemographics.getCity())) {
            final var city = objectFactory.createAdxpCity();
            city.setContent(StringUtils.strip(patientDemographics.getCity()));
            result.getContent().add(objectFactory.createADCity(city));
        }

        // Adding the postal code
        if (StringUtils.isNotBlank(patientDemographics.getPostalCode())) {
            final var postal = objectFactory.createAdxpPostalCode();
            postal.setContent(StringUtils.strip(patientDemographics.getPostalCode()));
            result.getContent().add(objectFactory.createADPostalCode(postal));
        }

        // Adding the address street line
        if (StringUtils.isNotBlank(patientDemographics.getStreetAddress())) {
            final var street = objectFactory.createAdxpStreetAddressLine();
            street.setContent(StringUtils.strip(patientDemographics.getStreetAddress()));
            result.getContent().add(objectFactory.createADStreetAddressLine(street));
        }

        // Adding the country
        final var country = objectFactory.createAdxpCountry();
        country.setContent(patientDemographics.getCountry());
        result.getContent().add(objectFactory.createADCountry(country));

        return result;
    }

    /**
     * Prepares a reasonOf element according to error type
     */
    private MFMIMT700711UV01Reason getReasonOfElement(final XCPDErrorCode xcpdErrorCode) {

        final var mfmimt700711UV01Reason = objectFactory.createMFMIMT700711UV01Reason();
        mfmimt700711UV01Reason.setTypeCode("RSON");

        // Set detectedIssueEvent
        mfmimt700711UV01Reason.setDetectedIssueEvent(objectFactory.createMCAIMT900001UV01DetectedIssueEvent());
        mfmimt700711UV01Reason.getDetectedIssueEvent().getClassCode().add("ALRT");
        mfmimt700711UV01Reason.getDetectedIssueEvent().getMoodCode().add("EVN");

        // Set detectedIssueEvent/code
        mfmimt700711UV01Reason.getDetectedIssueEvent().setCode(objectFactory.createCD());
        mfmimt700711UV01Reason.getDetectedIssueEvent().getCode().setCode("ActAdministrativeDetectedIssueCode");
        mfmimt700711UV01Reason.getDetectedIssueEvent().getCode().setCodeSystem("2.16.840.1.113883.5.4");

        if (xcpdErrorCode == XCPDErrorCode.DemographicsQueryNotAllowed) {
            // Set detectedIssueEvent/triggerFor
            final MCAIMT900001UV01Requires mcaimt900001UV01Requires = objectFactory.createMCAIMT900001UV01Requires();
            mfmimt700711UV01Reason.getDetectedIssueEvent().getTriggerFor().add(mcaimt900001UV01Requires);
            mcaimt900001UV01Requires.getTypeCode().add("TRIG");

            // Set detectedIssueEvent/triggerFor/actOrRequired
            final MCAIMT900001UV01ActOrderRequired mcaimt900001UV01ActOrderRequired = objectFactory.createMCAIMT900001UV01ActOrderRequired();

            mcaimt900001UV01Requires.setActOrderRequired(mcaimt900001UV01ActOrderRequired);
            mcaimt900001UV01ActOrderRequired.getClassCode().add("ACT");
            mcaimt900001UV01ActOrderRequired.getMoodCode().add("RQO");

            // Set detectedIssueEvent/triggerFor/actOrRequired/code
            final CE ce = objectFactory.createCE();
            mcaimt900001UV01ActOrderRequired.setCode(ce);
            ce.setCode(xcpdErrorCode.getCode());
            ce.setCodeSystem(xcpdErrorCode.getCodeSystem());
        } else if (xcpdErrorCode == XCPDErrorCode.InsufficientRights
                || xcpdErrorCode == XCPDErrorCode.AnswerNotAvailable) {
            // Set detectedIssueEvent/mitigatedBy
            final MCAIMT900001UV01SourceOf mcaimt900001UV01SourceOf = objectFactory.createMCAIMT900001UV01SourceOf();
            mfmimt700711UV01Reason.getDetectedIssueEvent().getMitigatedBy().add(mcaimt900001UV01SourceOf);
            mcaimt900001UV01SourceOf.setTypeCode(ActRelationshipMitigates.MITGT);

            // Set detectedIssueEvent/mitigatedBy/detectedIssueManagement
            final MCAIMT900001UV01DetectedIssueManagement mcaimt900001UV01DetectedIssueManagement = objectFactory.createMCAIMT900001UV01DetectedIssueManagement();
            mcaimt900001UV01SourceOf.setDetectedIssueManagement(mcaimt900001UV01DetectedIssueManagement);
            mcaimt900001UV01DetectedIssueManagement.getClassCode().add("ACT");
            mcaimt900001UV01DetectedIssueManagement.setMoodCode(XActMoodDefEvn.EVN);

            // Set detectedIssueEvent/mitigatedBy/detectedIssueManagement/code
            final CD cd = objectFactory.createCD();
            cd.setCode(xcpdErrorCode.getCode());
            cd.setCodeSystem(xcpdErrorCode.getCodeSystem());
            mcaimt900001UV01DetectedIssueManagement.setCode(cd);
        }
        return mfmimt700711UV01Reason;
    }

    private void fillOutputMessage(final PRPAIN201306UV02 outputMessage, final XCPDErrorCode xcpdErrorCode, final OpenNCPErrorCode openncpErrorCode, final String context) {
        fillOutputMessage(outputMessage, xcpdErrorCode, openncpErrorCode, context, "AE", "");
    }

    private void fillOutputMessage(final PRPAIN201306UV02 outputMessage, final XCPDErrorCode xcpdErrorCode, final OpenNCPErrorCode openncpErrorCode, final String context, final String location) {
        fillOutputMessage(outputMessage, xcpdErrorCode, openncpErrorCode, context, "AE", location);
    }

    private void fillOutputMessage(final PRPAIN201306UV02 outputMessage, final XCPDErrorCode xcpdErrorCode, final OpenNCPErrorCode openncpErrorCode, final String context, final String code, final String locationText) {

        // Set queryAck/statusCode and queryAck/queryResponseCode
        outputMessage.getControlActProcess().getQueryAck().setStatusCode(objectFactory.createCS());
        outputMessage.getControlActProcess().getQueryAck().getStatusCode().setCode(code);
        outputMessage.getControlActProcess().getQueryAck().setQueryResponseCode(objectFactory.createCS());
        outputMessage.getControlActProcess().getQueryAck().getQueryResponseCode().setCode(code);

        if (xcpdErrorCode != null) {
            outputMessage.getControlActProcess().getReasonOf().add(getReasonOfElement(xcpdErrorCode));
        }

        if (openncpErrorCode != null) {
            logger.error(context);
            // Set acknowledgement/acknowledgementDetail
            final MCCIMT000300UV01Acknowledgement acknowledgement = outputMessage.getAcknowledgement().get(0);
            final MCCIMT000300UV01AcknowledgementDetail acknowledgementDetail = objectFactory.createMCCIMT000300UV01AcknowledgementDetail();

            final CE codeCE = objectFactory.createCE();
            codeCE.setCode(openncpErrorCode.getCode());
            acknowledgementDetail.setCode(codeCE);

            acknowledgementDetail.setText(objectFactory.createED());
            acknowledgementDetail.getText().setContent(context);

            final ST location = objectFactory.createST();
            location.setContent(locationText);
            acknowledgementDetail.getLocation().add(location);

            acknowledgement.getAcknowledgementDetail().add(acknowledgementDetail);
            acknowledgement.getTypeCode().setCode("AE");
        }
    }

    private PatientDemographics parsePRPAIN201305UV02toPatientDemographics(final PRPAIN201305UV02 inputMessage) {

        final var patientDemographics = new PatientDemographics();
        final PRPAIN201305UV02QUQIMT021001UV01ControlActProcess cap = inputMessage.getControlActProcess();

        if (cap != null) {
            final PRPAMT201306UV02QueryByParameter queryByParameter = cap.getQueryByParameter().getValue();
            if (queryByParameter != null) {
                final PRPAMT201306UV02ParameterList pl = queryByParameter.getParameterList();

                // Administrative gender
                try {
                    final List<PRPAMT201306UV02LivingSubjectAdministrativeGender> genders = pl.getLivingSubjectAdministrativeGender();
                    if (genders != null && !genders.isEmpty()) {
                        final PRPAMT201306UV02LivingSubjectAdministrativeGender gender = genders.get(0);
                        patientDemographics.setAdministrativeGender(PatientDemographics.Gender.parseGender(gender.getValue().get(0).getCode()));
                    }
                } catch (final Exception e) {
                    logger.warn("Unable to parse administrative gender", e);
                }

                // BirthDate
                try {
                    final List<PRPAMT201306UV02LivingSubjectBirthTime> bds = pl.getLivingSubjectBirthTime();
                    if (bds != null && !bds.isEmpty()) {
                        final PRPAMT201306UV02LivingSubjectBirthTime bd = bds.get(0);
                        final String sbd = bd.getValue().get(0).getValue();
                        patientDemographics.setBirthDate(DateUtil.parseDateFromString(sbd, "yyyyMMdd"));
                    }
                } catch (final Exception e) {
                    logger.warn("Unable to parse birthDate", e);
                }

                // City, street name, country, postal code
                try {
                    final List<PRPAMT201306UV02PatientAddress> pas = pl.getPatientAddress();
                    if (pas != null && !pas.isEmpty()) {
                        final PRPAMT201306UV02PatientAddress pa = pas.get(0);
                        final List<Serializable> content = pa.getValue().get(0).getContent();
                        for (final Serializable s : content) {
                            if (s instanceof JAXBElement) {
                                final JAXBElement element = (JAXBElement) s;
                                final String eName = element.getName().getLocalPart();
                                if (StringUtils.equals("city", eName)) {
                                    final AdxpCity ac = (AdxpCity) element.getValue();
                                    patientDemographics.setCity(StringUtils.strip(ac.getContent()));
                                } else if (StringUtils.equals("streetName", eName)) {
                                    final AdxpStreetName asn = (AdxpStreetName) element.getValue();
                                    patientDemographics.setStreetAddress(StringUtils.strip(asn.getContent()));
                                } else if (StringUtils.equals("country", eName)) {
                                    final AdxpCountry ac = (AdxpCountry) element.getValue();
                                    patientDemographics.setCountry(StringUtils.strip(ac.getContent()));
                                } else if (StringUtils.equals("postalCode", eName)) {
                                    final AdxpPostalCode apc = (AdxpPostalCode) element.getValue();
                                    patientDemographics.setPostalCode(StringUtils.strip(apc.getContent()));
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    logger.warn("Unable to parse city, street name, country or postal code", e);
                }

                // Given name, family name
                try {
                    final List<PRPAMT201306UV02LivingSubjectName> sns = pl.getLivingSubjectName();
                    if (sns != null && !sns.isEmpty()) {
                        final PRPAMT201306UV02LivingSubjectName sn = sns.get(0);
                        final List<Serializable> content = sn.getValue().get(0).getContent();
                        for (final Serializable s : content) {
                            if (s instanceof JAXBElement) {
                                final JAXBElement element = (JAXBElement) s;
                                final String eName = element.getName().getLocalPart();
                                if (StringUtils.equals("given", eName)) {
                                    final EnGiven eg = (EnGiven) element.getValue();
                                    patientDemographics.setGivenName(eg.getContent());
                                } else if (StringUtils.equals("family", eName)) {
                                    final EnFamily ef = (EnFamily) element.getValue();
                                    patientDemographics.setFamilyName(ef.getContent());
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    logger.warn("Unable to parse given name or family name", e);
                }

                // Id
                try {
                    final List<PRPAMT201306UV02LivingSubjectId> livingSubjectIdList = pl.getLivingSubjectId();
                    if (livingSubjectIdList != null && !livingSubjectIdList.isEmpty()) {
                        final PRPAMT201306UV02LivingSubjectId id = livingSubjectIdList.get(0);
                        patientDemographics.setId(id.getValue().get(0).getExtension());
                    }
                } catch (final Exception e) {
                    logger.warn("Unable to parse patient id", e);
                }
            }
        }

        return patientDemographics;
    }

    private void pRPAIN201306UV02Builder(final PRPAIN201305UV02 inputMessage, final PRPAIN201306UV02 outputMessage, final SOAPHeader soapHeader,
                                         final EventLog eventLog) throws Exception {

        final String sigCountryCode;

        if (patientSearchService instanceof PatientSearchInterfaceWithDemographics) {
            final var patientSearchInterfaceWithDemographics = (PatientSearchInterfaceWithDemographics) patientSearchService;
            final var patientDemographics = parsePRPAIN201305UV02toPatientDemographics(inputMessage);
            patientSearchInterfaceWithDemographics.setPatientDemographics(patientDemographics);
        }

        // Set id of the message
        outputMessage.setId(objectFactory.createII());
        outputMessage.getId().setRoot(UUID.randomUUID().toString());

        // Generate and Set random extension
        outputMessage.getId().setExtension(RandomStringUtils.randomNumeric(10));

        // Set creation time
        outputMessage.setCreationTime(objectFactory.createTS());
        outputMessage.getCreationTime().setValue(DateUtil.getCurrentTimeUTC());

        // Set ITSVersion element
        outputMessage.setITSVersion("XML_1.0");

        // Set version element
        outputMessage.setVersionCode(inputMessage.getVersionCode());

        // Set interaction id
        outputMessage.setInteractionId(objectFactory.createII());

        outputMessage.getInteractionId().setRoot(inputMessage.getInteractionId().getRoot());
        outputMessage.getInteractionId().setExtension("PRPA_IN201306UV02");

        // Set Processing code
        outputMessage.setProcessingCode(objectFactory.createCS());
        outputMessage.getProcessingCode().setCode("P");

        // Set Processing mode code
        outputMessage.setProcessingModeCode(objectFactory.createCS());
        outputMessage.getProcessingModeCode().setCode("T");

        // Set Accept ACK code
        outputMessage.setAcceptAckCode(objectFactory.createCS());
        outputMessage.getAcceptAckCode().setCode("NE");

        // Create acknowledgement
        outputMessage.getAcknowledgement().add(objectFactory.createMCCIMT000300UV01Acknowledgement());

        // Create acknowledgement/targetMessage
        outputMessage.getAcknowledgement().get(0).setTargetMessage(objectFactory.createMCCIMT000300UV01TargetMessage());

        // Set acknowledgement/targetMessage/id
        outputMessage.getAcknowledgement().get(0).getTargetMessage().setId(inputMessage.getId());

        // Set acknowledgement/typeCode
        outputMessage.getAcknowledgement().get(0).setTypeCode(objectFactory.createCS());
        outputMessage.getAcknowledgement().get(0).getTypeCode().setCode("AA");

        final PRPAMT201306UV02QueryByParameter inputQBP = inputMessage.getControlActProcess().getQueryByParameter().getValue();

        // Create controlActProcess
        outputMessage.setControlActProcess(objectFactory.createPRPAIN201306UV02MFMIMT700711UV01ControlActProcess());
        outputMessage.getControlActProcess().setClassCode(ActClassControlAct.CACT);
        outputMessage.getControlActProcess().setMoodCode(XActMoodIntentEvent.EVN);

        // Create controlActProcess/code
        outputMessage.getControlActProcess().setCode(objectFactory.createCD());
        outputMessage.getControlActProcess().getCode().setCode("PRPA_TE201306UV02");

        // Create controlActProcess/queryAck
        outputMessage.getControlActProcess().setQueryAck(objectFactory.createMFMIMT700711UV01QueryAck());

        // Set controlActProcess/queryAck/queryId
        outputMessage.getControlActProcess().getQueryAck().setQueryId(objectFactory.createII());
        outputMessage.getControlActProcess().getQueryAck().getQueryId().setRoot(inputQBP.getQueryId().getRoot());
        outputMessage.getControlActProcess().getQueryAck().getQueryId().setExtension(inputQBP.getQueryId().getExtension());

        final Element shElement;
        try {
            shElement = XMLUtils.toDOM(soapHeader);
        } catch (final Exception e) {
            logger.error("SOAP header jaxb to dom failed.", e);
            throw e;
        }
        patientSearchService.setSOAPHeader(shElement);

        try {
            sigCountryCode = saml2Validator.validateXCPDHeader(shElement);

            final String senderHomeCommID = inputMessage.getSender().getDevice().getId().get(0).getRoot();
            final String receiverHomeCommID = inputMessage.getReceiver().get(0).getDevice().getId().get(0).getRoot();
            logger.info("Sender Home Community ID.... '{}'", senderHomeCommID);
            logger.info("Receiver Home Community ID.. '{}'", receiverHomeCommID);
            logger.info("Constants.HOME_COMM_ID...... '{}'", Constants.HOME_COMM_ID);

            final List<PRPAMT201306UV02LivingSubjectId> livingSubjectIds = inputQBP.getParameterList().getLivingSubjectId();
            if (!receiverHomeCommID.equals(Constants.HOME_COMM_ID)) {
                fillOutputMessage(outputMessage, XCPDErrorCode.AnswerNotAvailable, OpenNCPErrorCode.ERROR_PI_GENERIC, "Receiver has wrong Home Community ID.");
            } else if (!livingSubjectIds.isEmpty()) {
                final var stringBuilderNRO = new StringBuilder();
                final List<PatientId> patientIdList = new ArrayList<>();
                stringBuilderNRO.append("<patient>");
                for (final PRPAMT201306UV02LivingSubjectId livingSubjectId : livingSubjectIds) {
                    final var patientId = new PatientId();
                    patientId.setRoot(livingSubjectId.getValue().get(0).getRoot());
                    patientId.setExtension(livingSubjectId.getValue().get(0).getExtension());
                    stringBuilderNRO.append("<livingSubjectId>");
                    stringBuilderNRO.append("<id>").append(patientId.getRoot()).append("</id>");
                    stringBuilderNRO.append("<extension>").append(patientId.getExtension()).append("</extension>");
                    stringBuilderNRO.append("</livingSubjectId>");
                    patientIdList.add(patientId);
                }
                final List<PRPAMT201306UV02LivingSubjectAdministrativeGender> administrativeGenders =
                        inputQBP.getParameterList().getLivingSubjectAdministrativeGender();
                if (!CollectionUtils.isEmpty(administrativeGenders)) {
                    stringBuilderNRO.append("<livingSubjectAdministrativeGender>").append(administrativeGenders
                            .get(0).getValue().get(0).getCode()).append("</livingSubjectAdministrativeGender>");
                }

                final List<PRPAMT201306UV02LivingSubjectBirthTime> subjectBirthTimes =
                        inputQBP.getParameterList().getLivingSubjectBirthTime();
                if (!CollectionUtils.isEmpty(subjectBirthTimes)) {
                    stringBuilderNRO.append("<livingSubjectBirthTime>").append(subjectBirthTimes.get(0).getValue()
                            .get(0).getValue()).append("</livingSubjectBirthTime>");
                }

                final List<PRPAMT201306UV02LivingSubjectName> livingSubjectNames =
                        inputQBP.getParameterList().getLivingSubjectName();
                if (!CollectionUtils.isEmpty(livingSubjectNames)) {
                    // Implementation to be finalized.
                    logger.info("Patient Names must be added to the NRO message");
                }

                final List<PRPAMT201306UV02PatientAddress> patientAddresses = inputQBP.getParameterList().getPatientAddress();
                if (!CollectionUtils.isEmpty(patientAddresses)) {
                    // Implementation to be finalized.
                    logger.info("Patient Addresses must be added to the NRO message");
                }
                stringBuilderNRO.append("</patient>");
                if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
                    loggerClinical.info("Patient Identifier:\n'{}'", stringBuilderNRO);
                }

                // Joao: we have an adhoc XML document, so we can generate this evidence correctly
                try {
                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    //factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    factory.setXIncludeAware(false);
                    factory.setNamespaceAware(true);
                    final DocumentBuilder builder = factory.newDocumentBuilder();
                    final Document doc = builder.parse(new InputSource(new StringReader(stringBuilderNRO.toString())));
                    EvidenceUtils.createEvidenceREMNRO(doc, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                            Constants.NCP_SIG_PRIVATEKEY_ALIAS, Constants.SP_KEYSTORE_PATH, Constants.SP_KEYSTORE_PASSWORD,
                            Constants.SP_PRIVATEKEY_ALIAS, Constants.NCP_SIG_KEYSTORE_PATH, Constants.NCP_SIG_KEYSTORE_PASSWORD,
                            Constants.NCP_SIG_PRIVATEKEY_ALIAS, EventType.IDENTIFICATION_SERVICE_FIND_IDENTITY_BY_TRAITS.getIheCode(),
                            new DateTime(), EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(), "NI_XCPD_REQ",
                            SoapElementHelper.getHCPAssertion(shElement).getID() + "__" + DateUtil.getCurrentTimeGMT());
                } catch (final Exception e) {
                    logger.error(ExceptionUtils.getStackTrace(e));
                }

                // call to NI
                final List<PatientDemographics> demographicsList = patientSearchService.getPatientDemographics(patientIdList);

                // Joao: the NRR is being generated based on the request data, not on the response. This NRR is optional as per the CP, so it's left commented
//                try {
//                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//                    factory.setNamespaceAware(true);
//                    DocumentBuilder builder = factory.newDocumentBuilder();
//                    Document doc = builder.parse(new InputSource(new StringReader(sb.toString())));
//                    EvidenceUtils.createEvidenceREMNRR(doc,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PATH,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_KEYSTORE_PASSWORD,
//                            tr.com.srdc.epsos.util.Constants.NCP_SIG_PRIVATEKEY_ALIAS,
//                            IHEEventType.epsosIdentificationServiceFindIdentityByTraits.getCode(),
//                            new DateTime(),
//                            EventOutcomeIndicator.FULL_SUCCESS.getCode().toString(),
//                            "NI_XCPD_RES",
//                            Helper.getHCPAssertion(shElement).getID() + "__" + DateUtil.getCurrentTimeGMT());
//                } catch (Exception e) {
//                    logger.error(ExceptionUtils.getStackTrace(e));
//                }
                if (demographicsList.isEmpty()) {
                    // Preparing answer not available error

                    fillOutputMessage(outputMessage, XCPDErrorCode.AnswerNotAvailable, OpenNCPErrorCode.ERROR_PI_NO_MATCH,
                            "No patient found.", "tr.com.srdc.epsos.ws.server.xcpd.impl.XCPDServiceImpl.pRPAIN201306UV02Builder(XCPDServiceImpl.java:" + new Throwable().getStackTrace()[0].getLineNumber() +")");
                    outputMessage.getAcknowledgement().get(0).getTypeCode().setCode("AA");
                } else {
                    var countryCode = "";
                    final var distinguishName = eventLog.getSC_UserID();
                    final int cIndex = distinguishName.indexOf("C=");

                    if (cIndex > 0) {
                        countryCode = distinguishName.substring(cIndex + 2, cIndex + 4);
                    }
                    // Might be necessary to remove later, although it does no harm in reality!
                    // This part is added for handling consents when the call is not HTTPS.
                    // In this case, we check the country code of the signature certificate that ships within the HCP assertion
                    else {
                        logger.info("Could not get client country code from the service consumer certificate. " +
                                "The reason can be that the call was not via HTTPS. Will check the country code from the signature certificate now.");
                        if (sigCountryCode != null) {
                            logger.info("Found the client country code via the signature certificate.");
                            countryCode = sigCountryCode;
                        }
                    }
                    logger.info("The client country code to be used by the PDP: '{}'", countryCode);

                    /*
                     *  Then, it is the Policy Decision Point (PDP) that decides according to the consents of the patients
                     *  Uncomment when PDP works. You may also need to pass the whole PatientID
                     *  (both the root and extension) to PDP, if required by PDP procedures.
                     */
                    for (var i = 0; i < demographicsList.size(); i++) {
                        if (!saml2Validator.isConsentGiven(demographicsList.get(i).getIdList().get(0).getExtension(), countryCode)) {
                            // This patient data cannot be sent to Country B
                            demographicsList.remove(i);
                            i--;
                        } else {
                            outputMessage.getControlActProcess().getSubject().add(getSubjectByPatientDemographic(demographicsList.get(i)));
                        }
                    }
                    if (!demographicsList.isEmpty()) {
                        // There are patient data to be sent, OK
                        fillOutputMessage(outputMessage, null, null, null, "OK");
                    } else {
                        // No patient data can be sent to Country B.
                        fillOutputMessage(outputMessage,
                                XCPDErrorCode.InsufficientRights,
                                OpenNCPErrorCode.WARNING_PI_NO_CONSENT,
                                OpenNCPErrorCode.WARNING_PI_NO_CONSENT.getDescription());
                        outputMessage.getAcknowledgement().get(0).getTypeCode().setCode("AE");
                    }
                }
            } else {
                // Preparing demographic query not allowed error
                fillOutputMessage(outputMessage, XCPDErrorCode.DemographicsQueryNotAllowed, OpenNCPErrorCode.ERROR_PI_GENERIC, "Queries are only available with patient identifiers");
            }
        } catch (final MissingFieldException missingFieldException) {
            logger.error(missingFieldException.getMessage(), missingFieldException);
            fillOutputMessage(outputMessage, XCPDErrorCode.InternalError, OpenNCPErrorCode.ERROR_PI_MISSING_REQUIRED_FIELDS, missingFieldException.getMessage());
        } catch (final InvalidFieldException invalidFieldException) {
            logger.error(invalidFieldException.getMessage(), invalidFieldException);
            fillOutputMessage(outputMessage, XCPDErrorCode.InternalError, OpenNCPErrorCode.ERROR_PI_INCORRECT_FORMATTING, invalidFieldException.getMessage());
        } catch (final OpenNCPErrorCodeException e) {
            logger.error(e.getMessage(), e);
            fillOutputMessage(outputMessage, XCPDErrorCode.InsufficientRights, e.getErrorCode(), e.getMessage());
        } catch (final XCPDNIException e) {
            logger.error(e.getMessage(), e);
            final var codeContext = e.getOpenncpErrorCode().getDescription() + "^" + e.getMessage();
            fillOutputMessage(outputMessage, e.getXcpdErrorCode(), e.getOpenncpErrorCode(), codeContext, Arrays.stream(ExceptionUtils.getRootCauseStackTrace(e)).findFirst().orElse(StringUtils.EMPTY));
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            fillOutputMessage(outputMessage, XCPDErrorCode.InternalError, OpenNCPErrorCode.ERROR_PI_GENERIC, e.getMessage());
        }
        // Set queryByParameter
        final var prpamt201306UV02QueryByParameter = objectFactory.createPRPAMT201306UV02QueryByParameter();
        prpamt201306UV02QueryByParameter.setQueryId(inputQBP.getQueryId());
        prpamt201306UV02QueryByParameter.setStatusCode(inputQBP.getStatusCode());
        prpamt201306UV02QueryByParameter.setParameterList(inputQBP.getParameterList());
        outputMessage.getControlActProcess().setQueryByParameter(
                objectFactory.createPRPAIN201306UV02MFMIMT700711UV01ControlActProcessQueryByParameter(prpamt201306UV02QueryByParameter));

        // Set sender of the input to receiver of the output
        final var mccimt000300UV01Receiver = objectFactory.createMCCIMT000300UV01Receiver();
        mccimt000300UV01Receiver.setTypeCode(CommunicationFunctionType.RCV);
        mccimt000300UV01Receiver.setDevice(objectFactory.createMCCIMT000300UV01Device());
        mccimt000300UV01Receiver.getDevice().setDeterminerCode(INSTANCE);
        mccimt000300UV01Receiver.getDevice().setClassCode(EntityClassDevice.DEV);
        mccimt000300UV01Receiver.getDevice().getId().add(inputMessage.getSender().getDevice().getId().get(0));
        // Set asAgent
        final var mccimt000300UV01Agent = objectFactory.createMCCIMT000300UV01Agent();
        mccimt000300UV01Agent.getClassCode().add("AGNT");

        final var mccimt000300UV01Organization = objectFactory.createMCCIMT000300UV01Organization();
        final MCCIMT000100UV01Organization inpOrganization = inputMessage.getSender().getDevice().getAsAgent().getValue().getRepresentedOrganization().getValue();
        mccimt000300UV01Organization.setClassCode(inpOrganization.getClassCode());
        mccimt000300UV01Organization.setDeterminerCode(inpOrganization.getDeterminerCode());
        mccimt000300UV01Organization.getId().add(inpOrganization.getId().get(0));

        mccimt000300UV01Agent.setRepresentedOrganization(objectFactory.createMCCIMT000300UV01AgentRepresentedOrganization(mccimt000300UV01Organization));
        mccimt000300UV01Receiver.getDevice().setAsAgent(objectFactory.createMCCIMT000300UV01DeviceAsAgent(mccimt000300UV01Agent));
        outputMessage.getReceiver().add(mccimt000300UV01Receiver);

        // Set receiver of the input to sender of the output
        final var mccimt000300UV01Sender = objectFactory.createMCCIMT000300UV01Sender();
        mccimt000300UV01Sender.setTypeCode(CommunicationFunctionType.SND);
        mccimt000300UV01Sender.setDevice(objectFactory.createMCCIMT000300UV01Device());
        mccimt000300UV01Sender.getDevice().setDeterminerCode(INSTANCE);
        mccimt000300UV01Sender.getDevice().setClassCode(EntityClassDevice.DEV);
        mccimt000300UV01Sender.getDevice().getId().add(objectFactory.createII());
        mccimt000300UV01Sender.getDevice().getId().get(0).setRoot(Constants.HOME_COMM_ID);
        outputMessage.setSender(mccimt000300UV01Sender);

        // Prepare Audit Log
        try {
            prepareEventLog(eventLog, inputMessage, outputMessage, shElement);
            logger.info("Preparing Event Log: '{}' - SC UserId: '{}' - SP UserId: '{}'", eventLog.getEventType(),
                    eventLog.getSC_UserID(), eventLog.getSP_UserID());

        } catch (final Exception ex) {
            logger.error("Prepare Audit log failed.", ex);
            // Is it fatal, if Audit log cannot be created?
        }
    }
}
