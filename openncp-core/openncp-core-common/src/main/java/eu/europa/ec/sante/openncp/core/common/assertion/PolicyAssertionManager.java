package eu.europa.ec.sante.openncp.core.common.assertion;

import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.assertion.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.common.ClassCode;
import org.opensaml.saml.saml2.core.Assertion;

public interface PolicyAssertionManager {

    void healthcareFacilityValidator(Assertion assertion) throws MissingFieldException, InvalidFieldException;

    boolean isConsentGiven(String patientId, String countryId);

    void onBehalfOfValidator(Assertion assertion) throws MissingFieldException, InvalidFieldException;

    default void purposeOfUseValidator(Assertion assertion, ClassCode classCode) throws MissingFieldException, InsufficientRightsException {
        purposeOfUseValidator(assertion);
    }

    void purposeOfUseValidator(Assertion assertion) throws MissingFieldException, InsufficientRightsException;

    void xcaPermissionvalidator(Assertion assertion, ClassCode classCode) throws InsufficientRightsException, MissingFieldException;

    void xcpdPermissionValidator(Assertion assertion) throws InsufficientRightsException;

    void XDRPermissionValidator(Assertion assertion, ClassCode classCode) throws MissingFieldException, InvalidFieldException, InsufficientRightsException;

    void purposeOfUseValidatorForTRC(Assertion assertion) throws MissingFieldException, InsufficientRightsException;

    void xspalocalityvalidator(Assertion assertion) throws MissingFieldException, InvalidFieldException;

    void xspaOrganizationIdValidator(Assertion assertion) throws MissingFieldException, InvalidFieldException;

    void xspaRoleValidator(Assertion assertion) throws MissingFieldException, InvalidFieldException;

    void xspaSubjectValidatorForHCP(Assertion assertion) throws MissingFieldException, InvalidFieldException;

    void xspaSubjectValidatorForTRC(Assertion assertion) throws MissingFieldException, InvalidFieldException;
}
