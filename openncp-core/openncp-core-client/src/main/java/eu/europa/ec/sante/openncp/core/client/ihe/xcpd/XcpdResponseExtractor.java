package eu.europa.ec.sante.openncp.core.client.ihe.xcpd;

import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.core.client.ihe.xcpd.generated.*;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCPDErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class XcpdResponseExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(XcpdResponseExtractor.class);

    private XcpdResponseExtractor() {
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
    public static List<PatientDemographics> extract(final PRPAIN201306UV02 pRPA_IN201306UV02)
            throws NoPatientIdDiscoveredException {

        final List<PatientDemographics> patients = new ArrayList<>(1);

        // TODO A.R. How can be pRPA_IN201306UV02  be null when no matches?
        if (pRPA_IN201306UV02 != null && pRPA_IN201306UV02.getControlActProcess() != null
                && pRPA_IN201306UV02.getControlActProcess().getSubject() != null
                && !pRPA_IN201306UV02.getControlActProcess().getSubject().isEmpty()) {

            for (int s = 0; s < pRPA_IN201306UV02.getControlActProcess().getSubject().size(); s++) {
                try {
                    if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(0).getRegistrationEvent() != null) {
                        if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(0).getRegistrationEvent().getSubject1() != null) {
                            final PatientDemographics pd = new PatientDemographics();

                            // Set pd.id and pd.homeCommunityId
                            if (!pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getId().isEmpty()) {
                                pd.setId(pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getId().get(0).getExtension());
                                pd.setHomeCommunityId(pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getId().get(0).getRoot());
                            }

                            if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient() != null) {
                                if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson() != null) {
                                    // Set pd.administrativeGender
                                    if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAdministrativeGenderCode() != null) {
                                        final String sAdministrativeGender = pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAdministrativeGenderCode().getCode();
                                        if (sAdministrativeGender != null) {
                                            pd.setAdministrativeGender(PatientDemographics.Gender.parseGender(sAdministrativeGender));
                                        }
                                    }

                                    // Set pd.birthDate
                                    if (pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getBirthTime() != null) {
                                        final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
                                        final Date birthDate;
                                        final String sBirthdate;

                                        try {
                                            sBirthdate = pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getBirthTime().getValue().substring(0, 8);
                                            if (sBirthdate != null) {
                                                birthDate = df.parse(sBirthdate);
                                                pd.setBirthDate(birthDate);
                                            }
                                        } catch (final ParseException ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    }

                                    // Set pd.familyName and pd.givenName
                                    if (!pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getName().isEmpty()) {
                                        for (int i = 0; i < pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getName().get(0).getContent().size(); i++) {
                                            final Object o = pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getName().get(0).getContent().get(i);
                                            if (o instanceof JAXBElement) {
                                                @SuppressWarnings("unchecked") final JAXBElement<Object> temp = (JAXBElement<Object>) o;
                                                if (temp.getValue() instanceof EnFamily) {
                                                    final EnFamily family = (EnFamily) temp.getValue();
                                                    pd.setFamilyName(extractContent(family.getContent()));
                                                } else if (temp.getValue() instanceof EnGiven) {
                                                    final EnGiven given = (EnGiven) temp.getValue();
                                                    pd.setGivenName(extractContent(given.getContent()));
                                                }
                                            }
                                        }
                                    }

                                    // Set pd.city , pd.country , pd.postalCode and pd.streetAddress
                                    if (!pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAddr().isEmpty()) {
                                        for (int i = 0; i < pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAddr().get(0).getContent().size(); i++) {
                                            final Object o = pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAddr().get(0).getContent().get(i);
                                            if (o instanceof JAXBElement) {
                                                @SuppressWarnings("unchecked") final JAXBElement<Object> temp = (JAXBElement<Object>) pRPA_IN201306UV02.getControlActProcess().getSubject().get(s).getRegistrationEvent().getSubject1().getPatient().getPatientPerson().getValue().getAddr().get(0).getContent().get(i);
                                                if (temp.getValue() instanceof AdxpCity) {
                                                    final AdxpCity city = (AdxpCity) temp.getValue();
                                                    pd.setCity(extractContent(city.getContent()));
                                                } else if (temp.getValue() instanceof AdxpCountry) {
                                                    final AdxpCountry country = (AdxpCountry) temp.getValue();
                                                    pd.setCountry(extractContent(country.getContent()));
                                                } else if (temp.getValue() instanceof AdxpPostalCode) {
                                                    final AdxpPostalCode postalCode = (AdxpPostalCode) temp.getValue();
                                                    pd.setPostalCode(extractContent(postalCode.getContent()));
                                                } else if (temp.getValue() instanceof AdxpStreetName) {
                                                    final AdxpStreetName streetName = (AdxpStreetName) temp.getValue();
                                                    pd.setStreetAddress(extractContent(streetName.getContent()));
                                                } else if (temp.getValue() instanceof AdxpStreetAddressLine) {
                                                    final AdxpStreetAddressLine streetAddressLine = (AdxpStreetAddressLine) temp.getValue();
                                                    pd.setStreetAddress(extractContent(streetAddressLine.getContent()));
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                            patients.add(pd);
                        }
                    }
                } catch (final ParseException pe) {
                    throw new NoPatientIdDiscoveredException(OpenNCPErrorCode.ERROR_PI_NO_MATCH, pe);
                }
            }
        } else {

            String errorMsg = null;
            String xcpdErrorCodeValue = null;
            String openncpErrorCodeValue = null;
            String locationValue = null;
            final MCAIMT900001UV01DetectedIssueEvent detectedIssueEvent = getDetectedIssueEvent(pRPA_IN201306UV02);

            final String acknowledgementDetailText = getAcknowledgementDetailText(pRPA_IN201306UV02);

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

            final XCPDErrorCode xcpdErrorCode = XCPDErrorCode.getErrorCode(xcpdErrorCodeValue);
            final OpenNCPErrorCode openncpErrorCode = OpenNCPErrorCode.getErrorCode(openncpErrorCodeValue);

            if(xcpdErrorCode == null && openncpErrorCode == null){
                LOGGER.warn("No error code found in the XCPD response : " + errorMsg);
            }

            throw new NoPatientIdDiscoveredException(xcpdErrorCode, openncpErrorCode, acknowledgementDetailText, locationValue);
        }

        return patients;
    }

    private static String getAcknowledgementDetailText(final PRPAIN201306UV02 pRPA_IN201306UV02) {
        if (pRPA_IN201306UV02 != null
                && !pRPA_IN201306UV02.getAcknowledgement().isEmpty()
                && pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail() != null
                && !pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().isEmpty()
                && pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent() != null) {
            return extractContent(pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getText().getContent());
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
            return extractContent(pRPA_IN201306UV02.getAcknowledgement().get(0).getAcknowledgementDetail().get(0).getLocation().get(0).getContent());
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

    private static String extractContent(final List<Serializable> content) {
        if (content != null && !content.isEmpty()) {
            return (String) content.get(0);
        } else {
            return StringUtils.EMPTY;
        }
    }
}
