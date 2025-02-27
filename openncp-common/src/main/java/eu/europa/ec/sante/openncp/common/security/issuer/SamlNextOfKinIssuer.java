package eu.europa.ec.sante.openncp.common.security.issuer;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.SignatureManager;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.common.security.util.AssertionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
public class SamlNextOfKinIssuer {

    private final Logger logger = LoggerFactory.getLogger(SamlNextOfKinIssuer.class);
    KeyStoreManager keyStoreManager;
    HashMap<String, String> auditDataMap = new HashMap<>();

    /**
     * @param keyStoreManager
     */
    public SamlNextOfKinIssuer(final KeyStoreManager keyStoreManager) {
        this.keyStoreManager = Validate.notNull(keyStoreManager);
    }

    //    /**
    //     * Helper Function that makes it easy to create a new OpenSAML Object, using the default namespace prefixes.
    //     *
    //     * @param <T>   The Type of OpenSAML Class that will be created
    //     * @param cls   the openSAML Class
    //     * @param qname The QName of the Represented XML element.
    //     * @return the new OpenSAML object of type T
    //     */
    //    public static <T> T create(Class<T> cls, QName qname) {
    //        return (T) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(qname).buildObject(qname);
    //    }

    public Assertion issueNextOfKinToken(final Assertion hcpIdentityAssertion, final String doctorId, final String idaReference,
                                         final List<Attribute> attrValuePair) throws SMgrException {

        // Initializing the Map
        auditDataMap.clear();
        final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

        //  Doing an indirect copy so, because when cloning, signatures are lost.
        final var signatureManager = new SignatureManager(keyStoreManager);

        try {
            signatureManager.verifySAMLAssertion(hcpIdentityAssertion);
        } catch (final SMgrException ex) {
            throw new SMgrException("SAML Assertion Validation Failed: " + ex.getMessage());
        }
        final var issuanceInstant = DateTime.now();
        logger.info("Assertion validity: '{}' - '{}'", hcpIdentityAssertion.getConditions().getNotBefore(),
                hcpIdentityAssertion.getConditions().getNotOnOrAfter());
        if (hcpIdentityAssertion.getConditions().getNotBefore().isAfter(issuanceInstant)) {
            final String msg = "Identity Assertion with ID " + hcpIdentityAssertion.getID() + " can't be used before " +
                    hcpIdentityAssertion.getConditions().getNotBefore() + ". Current UTC time is " + issuanceInstant;
            logger.error("SecurityManagerException: '{}'", msg);
            throw new SMgrException(msg);
        }
        if (hcpIdentityAssertion.getConditions().getNotOnOrAfter().isBefore(issuanceInstant)) {
            final String msg = "Identity Assertion with ID " + hcpIdentityAssertion.getID() + " can't be used after " +
                    hcpIdentityAssertion.getConditions().getNotOnOrAfter() + ". Current UTC time is " + issuanceInstant;
            logger.error("SecurityManagerException: '{}'", msg);
            throw new SMgrException(msg);
        }

        auditDataMap.put("hcpIdAssertionID", hcpIdentityAssertion.getID());

        // Create the assertion
        final Assertion assertion = AssertionUtil.create(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setIssueInstant(issuanceInstant);
        assertion.setID("_" + UUID.randomUUID());
        assertion.setVersion(SAMLVersion.VERSION_20);

        // Create and add the Subject
        final Subject subject = AssertionUtil.create(Subject.class, Subject.DEFAULT_ELEMENT_NAME);
        assertion.setSubject(subject);
        final var issuer = new IssuerBuilder().buildObject();
        final String countryCode = Constants.COUNTRY_CODE;
        final String confIssuer = "urn:initgw:" + countryCode + ":countryB";
        issuer.setValue(confIssuer);
        issuer.setNameQualifier("urn:ehdsi:assertions:nok");
        assertion.setIssuer(issuer);

        final NameID nameid = AssertionUtil.create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        nameid.setFormat(NameID.UNSPECIFIED);
        nameid.setValue(doctorId);

        assertion.getSubject().setNameID(nameid);

        final String spProvidedID = hcpIdentityAssertion.getSubject().getNameID().getSPProvidedID();
        final String humanRequestorNameID = StringUtils.isNotBlank(spProvidedID) ? spProvidedID
                : "<" + hcpIdentityAssertion.getSubject().getNameID().getValue() +
                "@" + hcpIdentityAssertion.getIssuer().getValue() + ">";

        auditDataMap.put("humanRequestorNameID", humanRequestorNameID);

        final var subjectIdAttr = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                "urn:oasis:names:tc:xspa:1.0:subject:subject-id");
        final String humanRequesterAlternativeUserID = ((XSString) subjectIdAttr.getAttributeValues().get(0)).getValue();
        auditDataMap.put("humanRequestorSubjectID", humanRequesterAlternativeUserID);

        //Create and add Subject Confirmation
        final SubjectConfirmation subjectConf = AssertionUtil.create(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        subjectConf.setMethod(SubjectConfirmation.METHOD_SENDER_VOUCHES);
        assertion.getSubject().getSubjectConfirmations().add(subjectConf);

        //Create and add conditions
        final Conditions conditions = AssertionUtil.create(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(issuanceInstant);

        final AudienceRestriction audienceRestriction = AssertionUtil.create(AudienceRestriction.class, AudienceRestriction.DEFAULT_ELEMENT_NAME);
        final Audience audience = AssertionUtil.create(Audience.class, Audience.DEFAULT_ELEMENT_NAME);
        audience.setAudienceURI("urn:ehdsi:assertions.audience:x-border");
        audienceRestriction.getAudiences().add(audience);
        conditions.getAudienceRestrictions().add(audienceRestriction);

        conditions.setNotOnOrAfter(issuanceInstant.plus(Duration.standardHours(2)));
        assertion.setConditions(conditions);

        //Create and add Advice
        final Advice advice = AssertionUtil.create(Advice.class, Advice.DEFAULT_ELEMENT_NAME);
        assertion.setAdvice(advice);

        //Create and add AssertionIDRef
        final AssertionIDRef aIdRef = AssertionUtil.create(AssertionIDRef.class, AssertionIDRef.DEFAULT_ELEMENT_NAME);
        aIdRef.setAssertionID(idaReference);
        advice.getAssertionIDReferences().add(aIdRef);

        //Add and create the authentication statement
        final AuthnStatement authStmt = AssertionUtil.create(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
        authStmt.setAuthnInstant(issuanceInstant);
        assertion.getAuthnStatements().add(authStmt);

        //Create and add AuthnContext
        final AuthnContext ac = AssertionUtil.create(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
        final AuthnContextClassRef authnContextClassRef = AssertionUtil.create(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        authnContextClassRef.setAuthnContextClassRef(AuthnContext.PREVIOUS_SESSION_AUTHN_CTX);
        ac.setAuthnContextClassRef(authnContextClassRef);
        authStmt.setAuthnContext(ac);

        // Create the Saml Attribute Statement
        final AttributeStatement attrStmt = AssertionUtil.create(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);
        assertion.getStatements().add(attrStmt);

        //Creating the Attribute that holds the Patient ID
        //        Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        //        attrPID.setFriendlyName("XSPA Subject");
        //        attrPID.setName("urn:oasis:names:tc:xspa:1.0:subject:subject-id");
        //        attrPID.setNameFormat(Attribute.URI_REFERENCE);

        //Create and add the Attribute Value
        // var stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
        //        XSString attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        //        attrValPID.setValue(patientID);
        //        attrPID.getAttributeValues().add(attrValPID);
        //        attrStmt.getAttributes().add(attrPID);

        // Set Next of Kin attributes:
        //        Attribute attributeNextOfKinId = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        //        attributeNextOfKinId.setFriendlyName("XSPA Purpose Of Use");
        //
        //        attributeNextOfKinId.setName("urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
        //        attributeNextOfKinId.setNameFormat(Attribute.URI_REFERENCE);
        //        if (nextOfKinId == null) {
        //            attributeNextOfKinId = SamlIssuerHelper.createAttribute(nextOfKinId, "Purpose Of Use", Attribute.NAME_FORMAT_ATTRIB_NAME,
        //                    "urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
        //            if (attributeNextOfKinId == null) {
        //                throw new SMgrException("Purpose of use not found in the assertion and is not passed as a parameter");
        //            }
        //        } else {
        //            XSString attrValNextOfKinId = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        //            attrValNextOfKinId.setValue(nextOfKinId);
        //            attributeNextOfKinId.getAttributeValues().add(attrValNextOfKinId);
        //        }
        // attrStmt.getAttributes().add(attributeNextOfKinId);
        for (final Attribute attribute : attrValuePair) {
            attrStmt.getAttributes().add(attribute);
        }

        var pointOfCareAttr = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                "urn:oasis:names:tc:xspa:1.0:subject:organization");
        if (pointOfCareAttr != null) {
            final String poc = ((XSString) pointOfCareAttr.getAttributeValues().get(0)).getValue();
            auditDataMap.put("pointOfCare", poc);
        } else {
            pointOfCareAttr = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                    "urn:oasis:names:tc:xspa:1.0:environment:locality");
            final String poc = ((XSString) pointOfCareAttr.getAttributeValues().get(0)).getValue();
            auditDataMap.put("pointOfCare", poc);
        }

        final var pointOfCareIdAttr = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                "urn:oasis:names:tc:xspa:1.0:subject:organization-id");
        if (pointOfCareIdAttr != null) {
            final String pocId = ((XSString) pointOfCareIdAttr.getAttributeValues().get(0)).getValue();
            auditDataMap.put("pointOfCareID", pocId);
        } else {
            auditDataMap.put("pointOfCareID", "No Organization ID - POC information");
        }

        final String hrRole = ((XSString) AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                        "urn:oasis:names:tc:xacml:2.0:subject:role")
                .getAttributeValues()
                .get(0)).getValue();

        auditDataMap.put("humanRequestorRole", hrRole);

        final String facilityType = ((XSString) AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                        "urn:ehdsi:names:subject:healthcare-facility-type")
                .getAttributeValues()
                .get(0)).getValue();

        auditDataMap.put("facilityType", facilityType);

        signatureManager.signSAMLAssertion(assertion);
        if (logger.isDebugEnabled()) {
            logger.debug("Assertion generated at '{}'", assertion.getIssueInstant().toString());
        }

        return assertion;
    }

    /**
     * @return
     */
    public String getPointOfCare() {

        return auditDataMap.get("pointOfCare");
    }

    /**
     * @return
     */
    public String getPointOfCareID() {

        return auditDataMap.get("pointOfCareID");
    }

    /**
     * @return
     */
    public String getHumanRequestorNameId() {

        return auditDataMap.get("humanRequestorNameID");
    }

    /**
     * @return
     */
    public String getHumanRequestorSubjectId() {

        return auditDataMap.get("humanRequestorSubjectID");
    }

    /**
     * @return
     */
    public String getHRRole() {

        return auditDataMap.get("humanRequestorRole");
    }

    public String getFunctionalRole() {

        return auditDataMap.get("humanRequesterFunctionalRole");
    }

    /**
     * @return
     */
    public String getFacilityType() {

        return auditDataMap.get("facilityType");
    }
}
