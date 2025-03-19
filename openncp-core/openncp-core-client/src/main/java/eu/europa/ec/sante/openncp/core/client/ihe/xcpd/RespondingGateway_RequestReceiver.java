package eu.europa.ec.sante.openncp.core.client.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.org.hl7.v3.*;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.OpenNCPException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCPDErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RespondingGateway_RequestReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RespondingGateway_RequestReceiver.class);

    private RespondingGateway_RequestReceiver() {
    }

    /**
     * Extracts a Patient Demographics List from a PRPA_IN201306UV02 HL7
     * message.
     *
     * @param pRPA_IN201306UV02 the XCPD response message.
     * @return a list containing Patient Demographics objects.
     * @throws NoPatientIdDiscoveredException This represents the impossibility to transform the
     *                                        input data.
     * @see PatientDemographics
     * @see PRPAIN201306UV02
     * @see List
     */
    public static List<PatientDemographics> respondingGateway_PRPA_IN201306UV02(final PRPAIN201306UV02 pRPA_IN201306UV02)
            throws OpenNCPException {

        final List<PatientDemographics> patients = new ArrayList<>(1);

        // TODO A.R. How can be pRPA_IN201306UV02  be null when no matches?
        if (pRPA_IN201306UV02 != null && pRPA_IN201306UV02.getControlActProcess() != null
                && pRPA_IN201306UV02.getControlActProcess().getSubject() != null
                && !pRPA_IN201306UV02.getControlActProcess().getSubject().isEmpty()) {

            for (int s = 0; s < pRPA_IN201306UV02.getControlActProcess().getSubject().size(); s++) {
                try {
                    if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(0).getRegistrationEvent() != null) {
                        if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(0).getRegistrationEvent().getSubject1() != null) {
                            PatientDemographics pd = new PatientDemographics();

                            // Set pd.id and pd.homeCommunityId
                            if (!pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getId().isEmpty()) {
                                pd.setId(pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getId().get(0).getExtension());
                                pd.setHomeCommunityId(pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getId().get(0).getRoot());
                            }

                            if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient() != null) {
                                if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson() != null) {
                                    // Set pd.administrativeGender
                                    if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAdministrativeGenderCode() != null) {
                                        String sAdministrativeGender = pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAdministrativeGenderCode().getCode();
                                        if (sAdministrativeGender != null) {
                                            pd.setAdministrativeGender(PatientDemographics.Gender.parseGender(sAdministrativeGender));
                                        }
                                    }

                                    // Set pd.birthDate
                                    if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getBirthTime() != null) {
                                        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                                        Date birthDate;
                                        String sBirthdate;

                                        try {
                                            sBirthdate = pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getBirthTime().getValue().substring(0, 8);
                                            if (sBirthdate != null) {
                                                birthDate = df.parse(sBirthdate);
                                                pd.setBirthDate(birthDate);
                                            }
                                        } catch (ParseException ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    }

                                    // Set pd.familyName and pd.givenName
                                    if (!pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getName().isEmpty()) {
                                        for (int i = 0; i < pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getName().get(0).getContent().size(); i++) {
                                            Object o = pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getName().get(0).getContent().get(i);
                                            if (o instanceof JAXBElement) {
                                                @SuppressWarnings("unchecked")
                                                JAXBElement<Object> temp = (JAXBElement<Object>) o;
                                                if (temp.getValue() instanceof EnFamily) {
                                                    EnFamily family = (EnFamily) temp.getValue();
                                                    pd.setFamilyName(family.getContent());
                                                } else if (temp.getValue() instanceof EnGiven) {
                                                    EnGiven given = (EnGiven) temp.getValue();
                                                    pd.setGivenName(given.getContent());
                                                }
                                            }
                                        }
                                    }

                                    // Set pd.city , pd.country , pd.postalCode and pd.streetAddress
                                    if (!pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAddr().isEmpty()) {
                                        for (int i = 0; i < pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAddr().get(0).getContent().size(); i++) {
                                            Object o = pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAddr().get(0).getContent().get(i);
                                            if (o instanceof JAXBElement) {
                                                @SuppressWarnings("unchecked")
                                                JAXBElement<Object> temp = (JAXBElement<Object>) pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAddr().get(0).getContent().get(i);
                                                if (temp.getValue() instanceof AdxpCity) {
                                                    AdxpCity city = (AdxpCity) temp.getValue();
                                                    pd.setCity(city.getContent());
                                                } else if (temp.getValue() instanceof AdxpCountry) {
                                                    AdxpCountry country = (AdxpCountry) temp.getValue();
                                                    pd.setCountry(country.getContent());
                                                } else if (temp.getValue() instanceof AdxpPostalCode) {
                                                    AdxpPostalCode postalCode = (AdxpPostalCode) temp.getValue();
                                                    pd.setPostalCode(postalCode.getContent());
                                                } else if (temp.getValue() instanceof AdxpStreetName) {
                                                    AdxpStreetName streetName = (AdxpStreetName) temp.getValue();
                                                    pd.setStreetAddress(streetName.getContent());
                                                } else if (temp.getValue() instanceof AdxpStreetAddressLine) {
                                                    AdxpStreetAddressLine streetAddressLine = (AdxpStreetAddressLine) temp.getValue();
                                                    pd.setStreetAddress(streetAddressLine.getContent());
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                            patients.add(pd);
                        }
                    }
                } catch (ParseException pe) {
                    throw new OpenNCPException(OpenNCPErrorCode.ERROR_PI_NO_MATCH, pe);
                }
            }
        } else {

            String errorMsg = null;
            String xcpdErrorCodeValue = null;
            String openncpErrorCodeValue = null;
            String locationValue = null;
            MCAIMT900001UV01DetectedIssueEvent detectedIssueEvent = getDetectedIssueEvent(pRPA_IN201306UV02);

            String acknowledgementDetailText = getAcknowledgementDetailText(pRPA_IN201306UV02);

            // Tries to retrieve DetectedIssueEvent to fill error message
            if (detectedIssueEvent != null) {
                if (detectedIssueEvent.getMitigatedBy() != null && !detectedIssueEvent.getMitigatedBy().isEmpty()) {
                    xcpdErrorCodeValue = detectedIssueEvent.getMitigatedBy().get(0).getDetectedIssueManagement().getCode().getCode();
                } else if (detectedIssueEvent.getTriggerFor() != null && !detectedIssueEvent.getTriggerFor().isEmpty()) {
                    xcpdErrorCodeValue = detectedIssueEvent.getTriggerFor().get(0).getActOrderRequired().getCode().getCode();
                }
                openncpErrorCodeValue = getAcknowledgementDetailCode(pRPA_IN201306UV02);
                locationValue = getAcknowledgementDetailLocation(pRPA_IN201306UV02);
            } else {
                // If DetectedIssueEvent is not present, it tries to get Acknowledgement details.
                errorMsg = "Error: DetectedIssueEvent element or sub-element not present.";
                if (acknowledgementDetailText != null) {
                    errorMsg = acknowledgementDetailText;
                }
            }

            XCPDErrorCode xcpdErrorCode= XCPDErrorCode.getErrorCode(xcpdErrorCodeValue);
            OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.getErrorCode(openncpErrorCodeValue);

            if(xcpdErrorCode == null && openncpErrorCode == null){
                LOGGER.warn("No error code found in the XCPD response : " + errorMsg);
            }

            throw new OpenNCPException(openncpErrorCode, acknowledgementDetailText, locationValue);
        }

        return patients;
    }

    private static String getAcknowledgementDetailText(final PRPAIN201306UV02 pRPA_IN201306UV02) {
        if (pRPA_IN201306UV02 != null
                && !pRPA_IN201306UV02.getAcknowledgement().isEmpty()
                && pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail() != null
                && !pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()
                && pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent() != null) {
            return pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent();
        }
        return null;
    }

    private static String getAcknowledgementDetailCode(final PRPAIN201306UV02 pRPA_IN201306UV02) {
        if (pRPA_IN201306UV02 != null
                && !pRPA_IN201306UV02.getAcknowledgement().isEmpty()
                && pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail() != null
                && !pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()
                && pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getCode() != null) {
            return pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getCode().getCode();
        }
        return null;
    }

    private static String getAcknowledgementDetailLocation(final PRPAIN201306UV02 pRPA_IN201306UV02) {
        if (pRPA_IN201306UV02 != null
                && !pRPA_IN201306UV02.getAcknowledgement().isEmpty()
                && pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail() != null
                && !pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()
                && pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getLocation() != null
                && !pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getLocation().isEmpty() ) {
            return pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getLocation().get(0).getContent();
        }
        return null;
    }

    private static MCAIMT900001UV01DetectedIssueEvent getDetectedIssueEvent(final PRPAIN201306UV02 pRPA_IN201306UV02) {
        if (pRPA_IN201306UV02 != null
                && pRPA_IN201306UV02.getControlActProcess() != null
                && pRPA_IN201306UV02.getControlActProcess().getReasonOf() != null
                && !pRPA_IN201306UV02.getControlActProcess().getReasonOf().isEmpty()
                && pRPA_IN201306UV02.getControlActProcess().getReasonOf().get(0).getDetectedIssueEvent() != null) {

            return pRPA_IN201306UV02.getControlActProcess().getReasonOf().get(0).getDetectedIssueEvent();
        }
        return null;
    }
}
