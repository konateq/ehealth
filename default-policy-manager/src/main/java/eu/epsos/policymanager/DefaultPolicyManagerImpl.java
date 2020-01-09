package eu.epsos.policymanager;

import eu.epsos.assertionvalidator.*;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.com.srdc.epsos.securityman.exceptions.InsufficientRightsException;
import tr.com.srdc.epsos.securityman.exceptions.InvalidFieldException;
import tr.com.srdc.epsos.securityman.exceptions.MissingFieldException;
import tr.com.srdc.epsos.util.Constants;

import java.util.List;

import static eu.epsos.assertionvalidator.AssertionHelper.getAttributeFromAssertion;

/**
 * Default Policy Manager implementation conform with IHE profiles and eHDSI specifications.
 */
public class DefaultPolicyManagerImpl implements PolicyManagerInterface {

    private static final String DEPRECATED_EPSOS_DOCTOR_ROLE = "medical doctor";
    private final Logger logger = LoggerFactory.getLogger(DefaultPolicyManagerImpl.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    @Override
    public void HealthcareFacilityValidator(Assertion assertion, String documentClass) throws MissingFieldException,
            InvalidFieldException {

        String healthcareFacilityType = getAttributeFromAssertion(assertion, URN_EPSOS_NAMES_WP3_4_SUBJECT_HEALTHCARE_FACILITY_TYPE);
        if (healthcareFacilityType.equals(HealthcareFacilityType.HOSPITAL.toString())
                || healthcareFacilityType.equals(HealthcareFacilityType.RESIDENT_PHYSICIAN.toString())
                || healthcareFacilityType.equals(HealthcareFacilityType.PHARMACY.toString())
                || healthcareFacilityType.equals(HealthcareFacilityType.OTHER.toString())) {
            logger.debug("HCP Identity Assertion Healthcare Facility Type: '{}'", healthcareFacilityType);
        } else {
            logger.warn("InvalidFieldException: epSOS Healthcare Facility Type 'urn:epsos:names:wp3.4:subject:healthcare-facility-type' attribute in assertion should be one of followings {'Hospital', 'Resident Physician', 'Pharmacy', 'Other'}.");
            throw new InvalidFieldException("epSOS Healthcare Facility Type 'urn:epsos:names:wp3.4:subject:healthcare-facility-type' attribute in assertion should be one of followings {'Hospital', 'Resident Physician', 'Pharmacy', 'Other'}.");
        }
    }

    @Override
    public void OnBehalfOfValidator(Assertion assertion, String documentClass) throws MissingFieldException,
            InvalidFieldException {

        String onBehalfOfRole = getAttributeFromAssertion(assertion, URN_EPSOS_NAMES_WP3_4_SUBJECT_ON_BEHALF_OF);
        if (onBehalfOfRole.equals(OnBehalfOf.DENTIST.toString())
                || onBehalfOfRole.equals(OnBehalfOf.NURSE.toString())
                || onBehalfOfRole.equals(OnBehalfOf.PHARMACIST.toString())
                || onBehalfOfRole.equals(OnBehalfOf.PHYSICIAN.toString())
                || onBehalfOfRole.equals(OnBehalfOf.NURSE_MIDWIFE.toString())
                || StringUtils.equals(onBehalfOfRole, DEPRECATED_EPSOS_DOCTOR_ROLE)) {
            logger.debug("HCP Identity Assertion OnBehalfOf: '{}'", onBehalfOfRole);
        } else {
            throw new InvalidFieldException("OnBehalfOf 'urn:epsos:names:wp3.4:subject:on-behalf-of' attribute in assertion should be one of followings " +
                    "{'dentist', 'nurse', 'pharmacist', 'physician', 'nurse midwife'}.");
        }
    }

    @Override
    public void XSPARoleValidator(Assertion assertion, String documentClass) throws MissingFieldException, InvalidFieldException {

        String xspaRole = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE);
        // TODO: XSPARole.EPSOS_DOCTOR this role is not valid and considered as workaround
        if (xspaRole.equals(XSPARole.NURSE.toString()) || xspaRole.equals(XSPARole.PHARMACIST.toString())
                || xspaRole.equals(XSPARole.PHYSICIAN.toString()) || xspaRole.equals(XSPARole.NURSE_MIDWIFE.toString())
                || xspaRole.equals(XSPARole.PATIENT.toString()) || StringUtils.equals(xspaRole, DEPRECATED_EPSOS_DOCTOR_ROLE)) {

            logger.debug("HCP Identity Assertion XSPA Role: '{}'", xspaRole);
        } else if (xspaRole.equals(XSPARole.ANCILLARY_SERVICES.toString()) || xspaRole.equals(XSPARole.CLINICAL_SERVICES.toString())) {

            logger.debug("HCP Identity Assertion XSPA Role: '{}'", xspaRole);
            OnBehalfOfValidator(assertion, documentClass);
        } else {

            logger.error("Found XSPA Role: '{}'", xspaRole);
            logger.error("XSPA Role 'urn:oasis:names:tc:xacml:2.0:subject:role' attribute in assertion should be one of followings {'nurse', 'pharmacist', 'physician', 'medical doctor', 'nurse midwife', 'ancillary services' , 'clinical services', 'patient'}.");
            throw new InvalidFieldException("The user role is invalid. It should be one of followings {'nurse', 'pharmacist', 'physician', 'medical doctor', 'nurse midwife', 'ancillary services' , 'clinical services', 'patient'}");
        }
    }

    @Override
    public void XSPASubjectValidatorForHCP(Assertion assertion, String documentClass) throws MissingFieldException, InvalidFieldException {

        String subjectId = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XACML_1_0_SUBJECT_SUBJECT_ID);
        if (StringUtils.isEmpty(subjectId)) {
            throw new InvalidFieldException("XSPA Subject 'urn:oasis:names:tc:xacml:1.0:subject:subject-id' attribute in assertion should be filled.");
        } else if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.info("HCP Identity Assertion XSPA Subject: '{}", subjectId);
        }
    }

    @Override
    public void XSPASubjectValidatorForTRC(Assertion assertion, String documentClass) throws MissingFieldException, InvalidFieldException {

        String resourceId = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XACML_1_0_RESOURCE_RESOURCE_ID);
        if (resourceId.equals("")) {
            throw new InvalidFieldException("XSPA subject 'uurn:oasis:names:tc:xacml:1.0:resource:resource-id' attribute in assertion should be filled.");
        } else if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.info("TRC Assertion XSPA subject: '{}'", resourceId);
        }
    }

    @Override
    public void PurposeOfUseValidator(Assertion assertion, String documentClass) throws MissingFieldException, InsufficientRightsException {

        String resourceId = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_PURPOSEOFUSE);
        if (resourceId.equals("TREATMENT") || resourceId.equals("EMERGENCY")) {
            logger.debug("HCP Identity Assertion XSPA Purpose of Use: '{}'", resourceId);
        } else {
            logger.error("InsufficientRightsException: Purpose of Use provided is not supported");
            throw new InsufficientRightsException();
        }
    }

    @Override
    public void XSPALocalityValidator(Assertion assertion, String documentClass) throws MissingFieldException, InvalidFieldException {

        String environmentLocality = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XSPA_1_0_ENVIRONMENT_LOCALITY);
        if (environmentLocality.equals("")) {
            throw new InvalidFieldException("XSPA Locality 'urn:oasis:names:tc:xspa:1.0:environment:locality' attribute in assertion should be filled.");
        }
        logger.debug("HCP Identity Assertion XSPA Locality: '{}", environmentLocality);
    }

    @Override
    public void XCPDPermissionValidator(Assertion assertion) throws InsufficientRightsException {

        List<XMLObject> permissions = AssertionHelper.getPermissionValuesFromAssertion(assertion);
        for (XMLObject permission : permissions) {
            logger.debug("HCP Identity Assertion XSPD Permission: '{}'", permission.getDOM().getTextContent());
            if (permission.getDOM().getTextContent().equals(URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PRD_006)) {
                logger.debug("Found permission for PRD-006 (Patient Identification and Lookup)");
                return;
            }
        }
        logger.error("InsufficientRightsException");
        throw new InsufficientRightsException();
    }

    @Override
    public void XCAPermissionValidator(Assertion assertion, String documentClass) throws InsufficientRightsException, MissingFieldException {

        switch (documentClass) {
            case Constants.PS_CLASSCODE:
                XCAPermissionValidatorPS(assertion);
                break;
            case Constants.EP_CLASSCODE:
                XCAPermissionValidatorEP(assertion);
                break;
            case Constants.MRO_CLASSCODE:
                XCAPermissionValidatorMro(assertion);
                break;
            default:
                String errorMsg = "Invalid document class code: " + documentClass;
                logger.error(errorMsg);
                // TODO: What to do when wrong class code
                throw new MissingFieldException(errorMsg);
        }
    }

    /**
     * @param assertion
     * @throws InsufficientRightsException
     */
    private void XCAPermissionValidatorPS(Assertion assertion) throws InsufficientRightsException {

        boolean medicalHistory = false;
        boolean vitalSign = false;
        boolean patientMedications = false;
        boolean reviewProblem = false;


        List<XMLObject> permissions = AssertionHelper.getPermissionValuesFromAssertion(assertion);
        String permissionValue;
        String xspaRole;

        //Check allowed roles
        try {
            xspaRole = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE);
        } catch (MissingFieldException ex) {
            logger.error("A MissingFieldException was caugth. The assertion role could not be obtained: '{}'", ex.getMessage(), ex);
            throw new InsufficientRightsException();
        }
        if (!xspaRole.equals(XSPARole.PHYSICIAN.toString()) && !xspaRole.equals(XSPARole.PATIENT.toString())
                && !StringUtils.equals(xspaRole, DEPRECATED_EPSOS_DOCTOR_ROLE)) {
            logger.error("InsufficientRightsException - Unsupported role (named: '{}') tried to access Patient Summary documents.", xspaRole);
            throw new InsufficientRightsException();
        }

        //Check required permissions
        for (XMLObject permission : permissions) {
            if (permission.getDOM() != null) {
                permissionValue = permission.getDOM().getTextContent();
                logger.debug("HCP Identity Assertion XSPA Permission: '{}'", permissionValue);
                switch (permissionValue) {
                    case URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PRD_003:
                        medicalHistory = true;
                        logger.debug("Found permission for PRD-003 (Review Medical History)");
                        break;
                    case URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PRD_005:
                        vitalSign = true;
                        logger.debug("Found permission for PRD-005 (Review Vital Signs/Patient Measurements)");
                        break;
                    case URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PRD_010:
                        patientMedications = true;
                        logger.debug("Found permission for PRD-010 (Review Patient Medications)");
                        break;
                    case URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PRD_016:
                        reviewProblem = true;
                        logger.debug("Found permission for PRD-016 (Review Problems)");
                        break;
                    default:
                        logger.warn("No permission found!!!");
                        break;
                }
            }
        }

        if (medicalHistory && vitalSign && patientMedications && reviewProblem) {
            return;
        }
        logger.error("InsufficientRightsException");
        throw new InsufficientRightsException();
    }

    /**
     * XCA for order service (ePrescription)
     *
     * @param assertion the SAML Assertion
     * @throws InsufficientRightsException
     */
    private void XCAPermissionValidatorEP(Assertion assertion) throws InsufficientRightsException {

        boolean reviewExistingOrders = false;
        boolean patientMedications = false;

        List<XMLObject> permissions = AssertionHelper.getPermissionValuesFromAssertion(assertion);
        String xspaRole;

        //Check allowed roles
        try {
            xspaRole = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE);
        } catch (MissingFieldException ex) {
            logger.error("A MissingFieldException was caugth. The assertion role could not be obtained: '{}'", ex.getMessage(), ex);
            throw new InsufficientRightsException();
        }
        if (!xspaRole.equals(XSPARole.PHARMACIST.toString())) {
            logger.error("InsufficientRightsException - Unsupported (named: '{}) role tried to access ePrescriptions documents.", xspaRole);
            throw new InsufficientRightsException();
        }

        //Check required permissions
        for (XMLObject permission : permissions) {
            if (permission.getDOM() != null) {
                logger.debug("HCP Identity Assertion XSPA Permission: '{}'", permission.getDOM().getTextContent());
                if (permission.getDOM().getTextContent().equals(URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PRD_004)) {
                    reviewExistingOrders = true;
                    logger.debug("Found permission for PRD-004 (Review Existing Orders)");
                } else if (permission.getDOM().getTextContent().equals(URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PRD_010)) {
                    patientMedications = true;
                    logger.debug("Found permission for PRD-010 (Review Patient Medications)");
                }
            }
        }

        if (reviewExistingOrders && patientMedications) {
            return;
        }
        logger.error("InsufficientRightsException");
        throw new InsufficientRightsException();
    }

    /**
     * XCA validator for MRO service, currently using the same validator for eP.
     *
     * @param assertion
     * @throws InsufficientRightsException
     */
    private void XCAPermissionValidatorMro(Assertion assertion) throws InsufficientRightsException {

        XCAPermissionValidatorEP(assertion);
    }

    @Override
    public void XDRPermissionValidator(Assertion assertion, String documentClass) throws InsufficientRightsException, MissingFieldException {

        switch (documentClass) {
            //  eDispensation document
            case Constants.ED_CLASSCODE:
                break;
            //  HCER is not supported currently in eHDSI project.
            case Constants.HCER_CLASSCODE:
                XDRPermissionValidatorEncounterReport(assertion);
                break;
            // CONSENT is not supported currently in eHDSI project.
            case Constants.CONSENT_CLASSCODE:
                XDRPermissionValidatorConsent(assertion);
                break;
            // TODO: What to do when wrong class code?
            default:
                logger.error("Invalid document class code: '{}'", documentClass);
                throw new MissingFieldException("Invalid document class code: " + documentClass);
        }
    }

    /**
     * XDR validation for HCER service
     *
     * @param assertion the SAML Assertion
     * @throws InsufficientRightsException
     */
    private void XDRPermissionValidatorDispense(Assertion assertion) throws InsufficientRightsException {

        //  TODO: add support for discard operation
        boolean recordMedicationAdministrationRecord = false;

        List<XMLObject> permissions = AssertionHelper.getPermissionValuesFromAssertion(assertion);
        String xspaRole;

        //Check allowed roles
        try {
            xspaRole = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE);
        } catch (MissingFieldException ex) {
            logger.error("A MissingFieldException was caugth. The assertion role could not be obtained: '{}'", ex.getMessage(), ex);
            throw new InsufficientRightsException();
        }
        if (!xspaRole.equals(XSPARole.PHARMACIST.toString()) && !xspaRole.equals(XSPARole.PHYSICIAN.toString())) {
            logger.error("InsufficientRightsException - Unsupported role (named: '{}') tried to submit eDispensations or HCER documents.", xspaRole);
            throw new InsufficientRightsException();
        }

        //Check required permissions
        for (XMLObject permission : permissions) {
            logger.debug("HCP Identity Assertion XSPA Permission: '{}'", permission.getDOM().getTextContent());
            if (permission.getDOM().getTextContent().equals(URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PPD_046)) {
                recordMedicationAdministrationRecord = true;
                logger.debug("Found permission for PPD-046 (Record Medication Administration Record)");
            }
        }

        if (!recordMedicationAdministrationRecord) {
            logger.error("InsufficientRightsException");
            throw new InsufficientRightsException();
        }
    }

    /**
     * XDR validation for HCER service
     *
     * @param assertion the SAML Assertion
     * @throws InsufficientRightsException
     */
    private void XDRPermissionValidatorEncounterReport(Assertion assertion) throws InsufficientRightsException {

        XDRPermissionValidatorDispense(assertion);
    }

    /**
     * XDR for patient consent
     *
     * @param assertion the SAML Assertion
     * @throws InsufficientRightsException
     */
    private void XDRPermissionValidatorConsent(Assertion assertion) throws InsufficientRightsException {

        //  TODO: add support for consent discard operation
        boolean recordMedicationAdministrationRecord = false;

        List<XMLObject> permissions = AssertionHelper.getPermissionValuesFromAssertion(assertion);
        String xspaRole;

        //Check allowed roles
        try {
            xspaRole = getAttributeFromAssertion(assertion, URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE);
        } catch (MissingFieldException ex) {
            logger.error("A MissingFieldException was caugth. The assertion role could not be obtained: '{}'", ex.getMessage(), ex);
            throw new InsufficientRightsException();
        }
        if (!xspaRole.equals(XSPARole.PHARMACIST.toString()) && !xspaRole.equals(XSPARole.PHYSICIAN.toString())) {
            logger.error("InsufficientRightsException - Unsupported role (named: '{}') tried to submit consent documents.", xspaRole);
            throw new InsufficientRightsException();
        }

        //  Check required permissions
        for (XMLObject permission : permissions) {
            if (permission.getDOM() != null) {
                logger.debug("HCP Identity Assertion XSPA Permission: '{}'", permission.getDOM().getTextContent());
                if (permission.getDOM().getTextContent().equals(URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_HL7_PERMISSION_PPD_032)) {
                    recordMedicationAdministrationRecord = true;
                    logger.debug("Found permission for PPD-032 (New Consents and Authorizations)");
                }
            }
        }

        if (!recordMedicationAdministrationRecord) {
            logger.error("InsufficientRightsException");
            throw new InsufficientRightsException();
        }
    }

    @Override
    public boolean isConsentGiven(String patientId, String countryId) {
        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.debug("Checking consent of patient '{}' for country '{}'", patientId, countryId);
            loggerClinical.debug("Consent is Valid by default of patient '{}' from country '{}'", patientId, countryId);
        }
        return true;
    }
}
