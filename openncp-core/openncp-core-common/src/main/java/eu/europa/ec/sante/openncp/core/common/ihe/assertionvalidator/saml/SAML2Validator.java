package eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.dom.DOMSource;

import eu.europa.ec.sante.openncp.common.ClassCode;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.PolicyAssertionManager;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.XSDValidationException;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.common.xml.SAMLSchemaBuilder;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class SAML2Validator {

    private static final String OASIS_WSSE_SCHEMA_LOC = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final Logger LOGGER = LoggerFactory.getLogger(SAML2Validator.class);

    private final SignatureManager signatureManager;
    private final PolicyAssertionManager policyAssertionManager;

    private SAML2Validator(SignatureManager signatureManager, PolicyAssertionManager policyAssertionManager) {
        this.signatureManager = Validate.notNull(signatureManager, "signatureManager must not be null");
        this.policyAssertionManager = Validate.notNull(policyAssertionManager, "policyAssertionManager must not be null");
    }

    public String validateHCPHeader(final Element soapHeader)
            throws MissingFieldException, InsufficientRightsException, InvalidFieldException, XSDValidationException, SMgrException {

        LOGGER.debug("[SAML] Validating XCPD Header.");
        String sigCountryCode = null;

        final NodeList securityList = soapHeader.getElementsByTagNameNS(OASIS_WSSE_SCHEMA_LOC, "Security");
        final Element security;
        if (securityList.getLength() > 0) {
            security = (Element) securityList.item(0);
        } else {
            throw (new MissingFieldException("Security element is required."));
        }

        final NodeList assertionList = security.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion");
        Element hcpAss;
        Assertion hcpAssertion = null;
        try {
            if (assertionList.getLength() > 0) {
                for (var i = 0; i < assertionList.getLength(); i++) {
                    hcpAss = (Element) assertionList.item(i);
                    // Validate Assertion according to SAML XSD
                    final var schemaBuilder = new SAMLSchemaBuilder(SAMLSchemaBuilder.SAML1Version.SAML_11);
                    schemaBuilder.getSAMLSchema().newValidator().validate(new DOMSource(hcpAss));

                    hcpAssertion = (Assertion) SAML.fromElement(hcpAss);
                    if (StringUtils.equals(hcpAssertion.getIssuer().getNameQualifier(), "urn:ehdsi:assertions:hcp")) {
                        break;
                    }
                }
            }
            if (hcpAssertion == null) {
                throw (new MissingFieldException(OpenNCPErrorCode.ERROR_HPI_AUTHENTICATION_NOT_RECEIVED, "HCP Assertion element is required."));
            } else {
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateHCPAssertion(hcpAssertion, NcpSide.NCP_A);
                }
            }

            sigCountryCode = checkHCPAssertion(hcpAssertion, null);
            //TODO: Next of Kin assertion should be checked
            policyAssertionManager.XCPDPermissionValidator(hcpAssertion);
        } catch (final IOException | UnmarshallingException e) {
            LOGGER.error("{}: '{}'", e.getClass(), e.getMessage());
        } catch (final SAXException e) {
            throw new XSDValidationException(e.getMessage());
        }

        return sigCountryCode;
    }

    public void validateHCPHeader(Assertion assertion) throws MissingFieldException, InsufficientRightsException,
            InvalidFieldException, XSDValidationException, SMgrException {
        LOGGER.debug("[SAML] Validating HCP assertion.");

        try {
            RequiredFieldValidators.validateVersion(assertion);
            RequiredFieldValidators.validateID(assertion);
            RequiredFieldValidators.validateIssueInstant(assertion);
            RequiredFieldValidators.validateIssuer(assertion);
            RequiredFieldValidators.validateSubject(assertion);
            RequiredFieldValidators.validateNameID(assertion);
            RequiredFieldValidators.validateFormat(assertion);
            RequiredFieldValidators.validateSubjectConfirmation(assertion);
            RequiredFieldValidators.validateMethod(assertion);
            RequiredFieldValidators.validateConditions(assertion);
            RequiredFieldValidators.validateNotBefore(assertion);
            RequiredFieldValidators.validateNotOnOrAfter(assertion);
            RequiredFieldValidators.validateAuthnStatement(assertion);
            RequiredFieldValidators.validateAuthnInstant(assertion);
            RequiredFieldValidators.validateAuthnContext(assertion);
            RequiredFieldValidators.validateAuthnContextClassRef(assertion);
            RequiredFieldValidators.validateAttributeStatement(assertion);

            FieldValueValidators.validateVersionValue(assertion);
            FieldValueValidators.validateIssuerValue(assertion);
            FieldValueValidators.validateNameIDValue(assertion);
            FieldValueValidators.validateNotBeforeValue(assertion);
            FieldValueValidators.validateNotOnOrAfterValue(assertion);
            FieldValueValidators.validateTimeSpanForHCP(assertion);
            FieldValueValidators.validateAuthnContextClassRefValueForHCP(assertion);

        } catch (MissingFieldException e){
            throw new MissingFieldException(OpenNCPErrorCode.ERROR_HPI_GENERIC, e.getMessage());
        } catch (InvalidFieldException e){
            throw new InvalidFieldException(OpenNCPErrorCode.ERROR_HPI_GENERIC, e.getMessage());
        }

    }

    public String validateXCAHeader(final Element soapHeader, final ClassCode classCode)
            throws InsufficientRightsException, MissingFieldException, InvalidFieldException, SMgrException {

        LOGGER.debug("[SAML] Validating XCA Header.");
        final String sigCountryCode;

        try {
            // Since the XCA Simulator sends this value wrong, we are trying it as follows for now
            final NodeList securityList = soapHeader.getElementsByTagNameNS(OASIS_WSSE_SCHEMA_LOC, "Security");
            final Element security;
            if (securityList.getLength() > 0) {
                security = (Element) securityList.item(0);
            } else {
                throw (new MissingFieldException("Security element is required."));
            }

            final NodeList assertionList = security.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion");
            Element assertionElement;
            Assertion hcpAssertion = null;
            Assertion trcAssertion = null;

            if (assertionList.getLength() > 0) {
                for (var i = 0; i < assertionList.getLength(); i++) {
                    assertionElement = (Element) assertionList.item(i);
                    if (assertionElement.getAttribute("ID").startsWith("urn:uuid:")) {
                        LOGGER.debug("ncname found!!!");
                        assertionElement.setAttribute("ID", "_" + assertionElement.getAttribute("ID").substring(9));
                    }
                    if (assertionElement.getAttribute("ID").startsWith("urn:uuid:")) {
                        LOGGER.debug("ncname still exist!!!");
                    } else {
                        LOGGER.debug("ncname fixed!!!");
                    }

                    // Validate Assertion according to SAML XSD
                    final var schemaBuilder = new SAMLSchemaBuilder(SAMLSchemaBuilder.SAML1Version.SAML_11);
                    schemaBuilder.getSAMLSchema().newValidator().validate(new DOMSource(assertionElement));
                    final var anAssertion = (Assertion) SAML.fromElement(assertionElement);
                    if (StringUtils.equals(anAssertion.getIssuer().getNameQualifier(), "urn:ehdsi:assertions:hcp")) {
                        hcpAssertion = (Assertion) SAML.fromElement(assertionElement);
                    } else {
                        trcAssertion = (Assertion) SAML.fromElement(assertionElement);
                    }
                }
            }
            if (hcpAssertion == null) {
                throw (new MissingFieldException(OpenNCPErrorCode.ERROR_HPI_AUTHENTICATION_NOT_RECEIVED, "HCP Assertion element is required."));
            } else {
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateHCPAssertion(hcpAssertion, NcpSide.NCP_A);
                }
            }
            if (trcAssertion == null) {
                throw (new MissingFieldException("TRC Assertion element is required."));
            } else {
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateTRCAssertion(trcAssertion, NcpSide.NCP_A);
                }
            }

            sigCountryCode = checkHCPAssertion(hcpAssertion, classCode);
            policyAssertionManager.XCAPermissionValidator(hcpAssertion, classCode);
            checkTRCAssertion(trcAssertion, classCode);
            checkTRCAdviceIdReferenceAgainstHCPId(trcAssertion, hcpAssertion);
            //TODO: Next of Kin assertion should be checked
        } catch (final IOException | UnmarshallingException | SAXException e) {
            LOGGER.error("", e);
            throw new InsufficientRightsException();
        }

        return sigCountryCode;
    }

    public String validateXDRHeader(final Element soapHeader, final ClassCode classCode)
            throws InsufficientRightsException, MissingFieldException, InvalidFieldException, SMgrException {

        LOGGER.debug("[SAML] Validating XDR Header.");
        final String sigCountryCode;

        try {
            final NodeList securityList = soapHeader.getElementsByTagNameNS(OASIS_WSSE_SCHEMA_LOC, "Security");
            final Element security;
            if (securityList.getLength() > 0) {
                security = (Element) securityList.item(0);
            } else {
                throw (new MissingFieldException("Security element is required."));
            }

            final NodeList assertionList = security.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion");
            Element assertionElement;
            Assertion hcpAssertion = null;
            Assertion trcAssertion = null;

            if (assertionList.getLength() > 0) {
                for (var i = 0; i < assertionList.getLength(); i++) {
                    assertionElement = (Element) assertionList.item(i);
                    if (assertionElement.getAttribute("ID").startsWith("urn:uuid:")) {
                        LOGGER.debug("ncname found!!!");
                        assertionElement.setAttribute("ID", "_" + assertionElement.getAttribute("ID").substring(9));
                    }
                    if (assertionElement.getAttribute("ID").startsWith("urn:uuid:")) {
                        LOGGER.debug("ncname still exist!!!");
                    } else {
                        LOGGER.debug("ncname fixed!!!");
                    }

                    // Validate Assertion according to SAML XSD
                    final var schemaBuilder = new SAMLSchemaBuilder(SAMLSchemaBuilder.SAML1Version.SAML_11);
                    schemaBuilder.getSAMLSchema().newValidator().validate(new DOMSource(assertionElement));
                    final var anAssertion = (Assertion) SAML.fromElement(assertionElement);
                    if (StringUtils.equals(anAssertion.getIssuer().getNameQualifier(), "urn:ehdsi:assertions:hcp")) {
                        hcpAssertion = (Assertion) SAML.fromElement(assertionElement);
                    } else {
                        trcAssertion = (Assertion) SAML.fromElement(assertionElement);
                    }
                }
            }
            if (hcpAssertion == null) {
                throw (new MissingFieldException(OpenNCPErrorCode.ERROR_HPI_AUTHENTICATION_NOT_RECEIVED, "HCP Assertion element is required."));
            } else {
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateHCPAssertion(hcpAssertion, NcpSide.NCP_A);
                }
            }
            if (trcAssertion == null) {
                throw (new MissingFieldException("TRC Assertion element is required."));
            } else {
                if (OpenNCPValidation.isValidationEnable()) {
                    OpenNCPValidation.validateTRCAssertion(trcAssertion, NcpSide.NCP_A);
                }
            }

            sigCountryCode = checkHCPAssertion(hcpAssertion, classCode);
            policyAssertionManager.XDRPermissionValidator(hcpAssertion, classCode);
            checkTRCAssertion(trcAssertion, classCode);
            checkTRCAdviceIdReferenceAgainstHCPId(trcAssertion, hcpAssertion);
            //TODO: Next of Kin assertion should be checked
        } catch (final IOException | UnmarshallingException | SAXException e) {
            LOGGER.error("", e);
            throw new InsufficientRightsException();
        }

        return sigCountryCode;
    }

    private void checkTRCAdviceIdReferenceAgainstHCPId(final Assertion trcAssertion, final Assertion hcpAssertion) throws InsufficientRightsException {

        try {
            final String trcFirstReferenceId = trcAssertion.getAdvice().getAssertionIDReferences().get(0).getAssertionID();

            if (trcFirstReferenceId != null && trcFirstReferenceId.equals(hcpAssertion.getID())) {
                LOGGER.info("Assertion id reference equals to id.");
                return /* Least one of TRC Advice IdRef equals to HCP Ids */;
            }
        } catch (final Exception ex) {
            LOGGER.error("Unable to resolve first id reference: '{}'", ex.getMessage(), ex);
        }

        LOGGER.info("checkTRCAdviceIdReferenceAgainstHCPId: ReferenceId does not match. Throw InsufficientRightsException.");
        throw new InsufficientRightsException();
    }

    /**
     * Check if consent is given for patient
     *
     * @param patientId patient ID
     * @param countryId country ID
     * @return true if consent is given, else false.
     */
    public boolean isConsentGiven(final String patientId, final String countryId) {

        return policyAssertionManager.isConsentGiven(patientId, countryId);
    }

    public List<Assertion> getAssertions(final Element soapHeader) {

        LOGGER.info("Retrieving SAML tokens from SOAP Header");
        final NodeList securityList = soapHeader.getElementsByTagNameNS(OASIS_WSSE_SCHEMA_LOC, "Security");

        final Element security = (Element) securityList.item(0);
        final NodeList assertionList = security.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion");
        final List<Assertion> result = new ArrayList<>();

        for (var i = 0; i < assertionList.getLength(); i++) {
            final Element ass = (Element) assertionList.item(i);

            if (ass.getAttribute("ID").startsWith("urn:uuid:")) {
                ass.setAttribute("ID", "_" + ass.getAttribute("ID").substring("urn:uuid:".length()));
            }

            try {
                // Validate Assertion according to SAML XSD
                final var schemaBuilder = new SAMLSchemaBuilder(SAMLSchemaBuilder.SAML1Version.SAML_11);
                schemaBuilder.getSAMLSchema().newValidator().validate(new DOMSource(ass));
                result.add((Assertion) SAML.fromElement(ass));
            } catch (final UnmarshallingException | IOException | SAXException ex) {
                LOGGER.error(null, ex);
            }
        }
        return result;
    }

    private String checkHCPAssertion(final Assertion assertion, final ClassCode classCode)
            throws InsufficientRightsException, SMgrException, MissingFieldException, InvalidFieldException {

        final String sigCountryCode;

        try {
            RequiredFieldValidators.validateVersion(assertion);
            RequiredFieldValidators.validateID(assertion);
            RequiredFieldValidators.validateIssueInstant(assertion);
            RequiredFieldValidators.validateIssuer(assertion);
            RequiredFieldValidators.validateSubject(assertion);
            RequiredFieldValidators.validateNameID(assertion);
            RequiredFieldValidators.validateFormat(assertion);
            RequiredFieldValidators.validateSubjectConfirmation(assertion);
            RequiredFieldValidators.validateMethod(assertion);
            RequiredFieldValidators.validateConditions(assertion);
            RequiredFieldValidators.validateNotBefore(assertion);
            RequiredFieldValidators.validateNotOnOrAfter(assertion);
            RequiredFieldValidators.validateAuthnStatement(assertion);
            RequiredFieldValidators.validateAuthnInstant(assertion);
            RequiredFieldValidators.validateAuthnContext(assertion);
            RequiredFieldValidators.validateAuthnContextClassRef(assertion);
            RequiredFieldValidators.validateAttributeStatement(assertion);
            RequiredFieldValidators.validateSignature(assertion);

            FieldValueValidators.validateVersionValue(assertion);
            FieldValueValidators.validateIssuerValue(assertion);
            FieldValueValidators.validateNameIDValue(assertion);
            FieldValueValidators.validateNotBeforeValue(assertion);
            FieldValueValidators.validateNotOnOrAfterValue(assertion);
            FieldValueValidators.validateTimeSpanForHCP(assertion);
            FieldValueValidators.validateAuthnContextClassRefValueForHCP(assertion);

            policyAssertionManager.XSPASubjectValidatorForHCP(assertion, classCode);
            policyAssertionManager.XSPARoleValidator(assertion, classCode);
            policyAssertionManager.HealthcareFacilityValidator(assertion, classCode);
            policyAssertionManager.PurposeOfUseValidator(assertion, classCode);
            if (classCode != null && classCode.equals(ClassCode.EDD_CLASSCODE)) {
                policyAssertionManager.XSPAOrganizationIdValidator(assertion, classCode);
            }
            policyAssertionManager.XSPALocalityValidator(assertion, classCode);
        } catch (final MissingFieldException e) {
            throw new MissingFieldException(OpenNCPErrorCode.ERROR_HPI_GENERIC, e.getMessage());
        } catch (final InvalidFieldException e) {
            throw new InvalidFieldException(OpenNCPErrorCode.ERROR_HPI_GENERIC, e.getMessage());
        }

        //TODO: [Mustafa, 2012.07.05] The original security manager was extended to return the two-letter country code
        // from the signature, but now in order not to change the security manager in Google Code repo, this is reverted back.
        // Konstantin: committed changes to security manager, in order to provide better support XCA and XDR implementations
        //TODO: Improve Exception management.
        sigCountryCode = signatureManager.verifySAMLAssertion(assertion);

        //TODO EHEALTH-6693 See if needed to incapsulate? ERROR_HPI_GENERIC, WARNING_HPI_GENERIC, ERROR_HPI_INSUFFICIENT_INFORMATION...

        return sigCountryCode;
    }

    public void checkTRCAssertion(final Assertion assertion, final ClassCode classCode)
            throws MissingFieldException, InvalidFieldException, InsufficientRightsException, SMgrException {

        RequiredFieldValidators.validateVersion(assertion);
        RequiredFieldValidators.validateID(assertion);
        RequiredFieldValidators.validateIssuer(assertion);
        RequiredFieldValidators.validateIssueInstant(assertion);
        RequiredFieldValidators.validateSubject(assertion);
        RequiredFieldValidators.validateNameID(assertion);
        RequiredFieldValidators.validateFormat(assertion);
        RequiredFieldValidators.validateSubjectConfirmation(assertion);
        RequiredFieldValidators.validateMethod(assertion);
        RequiredFieldValidators.validateConditions(assertion);
        RequiredFieldValidators.validateNotBefore(assertion);
        RequiredFieldValidators.validateNotOnOrAfter(assertion);
        RequiredFieldValidators.validateAdvice(assertion);
        RequiredFieldValidators.validateAssertionIdRef(assertion);
        RequiredFieldValidators.validateAuthnStatement(assertion);
        RequiredFieldValidators.validateAuthnInstant(assertion);
        RequiredFieldValidators.validateAuthnContext(assertion);
        RequiredFieldValidators.validateAuthnContextClassRef(assertion);
        RequiredFieldValidators.validateAttributeStatement(assertion);
        RequiredFieldValidators.validateSignature(assertion);

        FieldValueValidators.validateVersionValue(assertion);
        FieldValueValidators.validateIssuerValue(assertion);
        FieldValueValidators.validateNameIDValue(assertion);
        FieldValueValidators.validateMethodValue(assertion);
        FieldValueValidators.validateNotBeforeValue(assertion);
        FieldValueValidators.validateNotOnOrAfterValue(assertion);
        FieldValueValidators.validateTimeSpanForTRC(assertion);
        FieldValueValidators.validateAuthnContextClassRefValueForHCP(assertion);

        policyAssertionManager.PurposeOfUseValidatorForTRC(assertion, classCode);
        policyAssertionManager.XSPASubjectValidatorForTRC(assertion, classCode);

        signatureManager.verifySAMLAssertion(assertion);
    }

    public String getCountryCodeFromHCPAssertion(final Element soapHeader) throws MissingFieldException, XSDValidationException, SMgrException {

        String sigCountryCode = null;

        final NodeList securityList = soapHeader.getElementsByTagNameNS(OASIS_WSSE_SCHEMA_LOC, "Security");
        final Element security;
        if (securityList.getLength() > 0) {
            security = (Element) securityList.item(0);
        } else {
            throw (new MissingFieldException("Security element is required."));
        }

        final NodeList assertionList = security.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion");
        Element hcpAss;
        Assertion hcpAssertion = null;
        try {
            if (assertionList.getLength() > 0) {
                for (var i = 0; i < assertionList.getLength(); i++) {
                    hcpAss = (Element) assertionList.item(i);
                    // Validate Assertion according to SAML XSD
                    final var schemaBuilder = new SAMLSchemaBuilder(SAMLSchemaBuilder.SAML1Version.SAML_11);
                    schemaBuilder.getSAMLSchema().newValidator().validate(new DOMSource(hcpAss));

                    hcpAssertion = (Assertion) SAML.fromElement(hcpAss);
                    if (StringUtils.equals(hcpAssertion.getIssuer().getNameQualifier(), "urn:ehdsi:assertions:hcp")) {
                        break;
                    }
                }
            }
            if (hcpAssertion == null) {
                throw (new MissingFieldException("HCP Assertion element is required."));
            }

            sigCountryCode = signatureManager.verifySAMLAssertion(hcpAssertion);
        } catch (final IOException | UnmarshallingException e) {
            LOGGER.error("'{}'", e.getMessage(), e);
        } catch (final SAXException e) {
            throw new XSDValidationException(e.getMessage());
        }

        return sigCountryCode;
    }
}
