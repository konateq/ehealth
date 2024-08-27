package epsos.ccd.netsmart.securitymanager;

import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import epsos.ccd.netsmart.securitymanager.key.KeyStoreManager;
import epsos.ccd.netsmart.securitymanager.key.impl.DefaultKeyStoreManager;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.util.OpenNCPConstants;
import eu.europa.ec.sante.ehdsi.openncp.util.ServerMode;
import eu.europa.ec.sante.openncp.securitymanager.AssertionUtil;
import eu.europa.ec.sante.openncp.securitymanager.TwoFactorAuthentication;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * The TRC Assertion issuer is a sub-component that issues Treatment Relationship Assertions as specified in eHDSI SAML
 * Profile specification document. It makes use of the Signature Manager for signing the assertions.
 * An audit trail entry is written after the successful issuance of a TRC assertion.
 */
public class SamlTRCIssuer {

    private final Logger logger = LoggerFactory.getLogger(SamlTRCIssuer.class);
    private final Logger loggerClinical = LoggerFactory.getLogger("LOGGER_CLINICAL");

    KeyStoreManager keyStoreManager;
    HashMap<String, String> auditDataMap;

    public SamlTRCIssuer() {

        keyStoreManager = new DefaultKeyStoreManager();
        auditDataMap = new HashMap<>();
    }

    /**
     * @param keyStoreManager
     */
    public SamlTRCIssuer(KeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

//    /**
//     * Helper Function that makes it easy to create a new OpenSAML Object, using
//     * the default namespace prefixes.
//     *
//     * @param <T>   The Type of OpenSAML Class that will be created
//     * @param cls   the openSAML Class
//     * @param qname The Qname of the Represented XML element.
//     * @return the new OpenSAML object of type T
//     */
//    public static <T> T create(Class<T> cls, QName qname) {
//        return (T) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(qname).buildObject(qname);
//    }

    /**
     * @param idaReference
     * @param sessionNotOnOrAfter
     * @param authnInstant
     * @param notOnOrAfter
     * @param notBefore
     * @param doctorId
     * @param patientID
     * @param purposeOfUse
     */
    public Assertion issueTrcTokenUnsigned(String purposeOfUse, String patientID, String doctorId, DateTime notBefore,
                                           DateTime notOnOrAfter, DateTime authnInstant, DateTime sessionNotOnOrAfter,
                                           String idaReference) throws SMgrException {

        try {
            //initializing the map
            XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

            // Create the assertion
            Assertion trc = AssertionUtil.create(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
            if (patientID == null) {
                throw new SMgrException("Patiend ID cannot be null");
            }
            var issuanceInstant = Instant.now();
            trc.setIssueInstant(issuanceInstant);
            trc.setID("_" + UUID.randomUUID());
            trc.setVersion(SAMLVersion.VERSION_20);

            // Create and add the Subject
            Subject subject = AssertionUtil.create(Subject.class, Subject.DEFAULT_ELEMENT_NAME);
            trc.setSubject(subject);
            var issuer = new IssuerBuilder().buildObject();
            String countryCode = ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_CODE");
            String confIssuer = "urn:initgw:" + countryCode + ":countryB";

            issuer.setValue(confIssuer);
            trc.setIssuer(issuer);

            NameID nameid = AssertionUtil.create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
            nameid.setFormat(NameID.UNSPECIFIED);
            nameid.setValue(doctorId);

            trc.getSubject().setNameID(nameid);

            //Create and add Subject Confirmation
            SubjectConfirmation subjectConf = AssertionUtil.create(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
            subjectConf.setMethod(SubjectConfirmation.METHOD_SENDER_VOUCHES);
            trc.getSubject().getSubjectConfirmations().add(subjectConf);

            //Create and add conditions
            Conditions conditions = AssertionUtil.create(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
            conditions.setNotBefore(issuanceInstant);

            AudienceRestriction audienceRestriction = AssertionUtil.create(AudienceRestriction.class, AudienceRestriction.DEFAULT_ELEMENT_NAME);
            Audience audience = AssertionUtil.create(Audience.class, Audience.DEFAULT_ELEMENT_NAME);
            audience.setURI("urn:ehdsi:assertions.audience:x-border");
            audienceRestriction.getAudiences().add(audience);
            conditions.getAudienceRestrictions().add(audienceRestriction);

            conditions.setNotOnOrAfter(issuanceInstant.plus(Duration.ofHours(2))); // According to Spec
            trc.setConditions(conditions);

            //Create and add Advice
            Advice advice = AssertionUtil.create(Advice.class, Advice.DEFAULT_ELEMENT_NAME);
            trc.setAdvice(advice);

            //Create and add AssertionIDRef
            AssertionIDRef aIdRef = AssertionUtil.create(AssertionIDRef.class, AssertionIDRef.DEFAULT_ELEMENT_NAME);
            aIdRef.setValue(idaReference);
            advice.getAssertionIDReferences().add(aIdRef);

            //Add and create the authentication statement
            AuthnStatement authStmt = AssertionUtil.create(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
            authStmt.setAuthnInstant(issuanceInstant);
            trc.getAuthnStatements().add(authStmt);

            //Create and add AuthnContext
            AuthnContext ac = AssertionUtil.create(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
            AuthnContextClassRef accr = AssertionUtil.create(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
            accr.setURI(AuthnContext.PREVIOUS_SESSION_AUTHN_CTX);
            ac.setAuthnContextClassRef(accr);
            authStmt.setAuthnContext(ac);

            // Create the Saml Attribute Statement
            AttributeStatement attrStmt = AssertionUtil.create(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);
            trc.getStatements().add(attrStmt);

            //Creating the Attribute that holds the Patient ID
            Attribute attrPID = AssertionUtil.create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
            attrPID.setFriendlyName("XSPA Subject");

            // TODO: Is there a constant for that urn??
            attrPID.setName("urn:oasis:names:tc:xacml:1.0:resource:resource-id");
            attrPID.setNameFormat(Attribute.URI_REFERENCE);

            //Create and add the Attribute Value
            XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
            XSString attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            attrValPID.setValue(patientID);
            attrPID.getAttributeValues().add(attrValPID);
            attrStmt.getAttributes().add(attrPID);

            //Creating the Attribute that holds the Purpose of Use
            Attribute attrPoU = AssertionUtil.create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
            attrPoU.setFriendlyName("XSPA Purpose Of Use");

            // TODO: Is there a constant for that urn??
            attrPoU.setName("urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
            attrPoU.setNameFormat(Attribute.URI_REFERENCE);
            if (purposeOfUse == null) {
                attrPoU = AssertionUtil.createAttributePurposeOfUse(purposeOfUse, "Purpose Of Use", Attribute.NAME_FORMAT_ATTRIB_NAME,
                        "urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
                if (attrPoU == null) {
                    throw new SMgrException("Purpose of use not found in the assertion and is not passed as a parameter");
                }
            } else {
                XSString attrValPoU = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
                attrValPoU.setValue(purposeOfUse);
                attrPoU.getAttributeValues().add(attrValPoU);
            }
            attrStmt.getAttributes().add(attrPoU);

            return trc;
        } catch (Exception ex) {
            throw new SMgrException(ex.getMessage());
        }
    }

    public Assertion issueTrcToken(final Assertion hcpIdentityAssertion, String patientID, String purposeOfUse,
                                   List<Attribute> attrValuePair) throws SMgrException {
        return issueTrcToken(hcpIdentityAssertion, patientID, purposeOfUse, StringUtils.EMPTY, StringUtils.EMPTY, attrValuePair);
    }

    /**
     * Issues a SAML TRC Assertion that proves the treatment relationship between the HCP and the Patient.
     * The Identity of the HCP is provided by the hcpIdentityAssertion. The identity of the patient is inferred from the patientID.
     *
     * @param hcpIdentityAssertion The health care professional Identity SAML Assertion. The method validates
     *                             the assertion using the {@link SignatureManager#verifySAMLAssertion(org.opensaml.saml.saml2.core.Assertion)}.
     * @param patientID            The Patient Id that is required for the TRC Assertion
     * @param purposeOfUse         Purpose of use Variables (e.g. TREATMENT)
     * @param attrValuePair        SAML {@link Attribute} that will be added to the assertion
     * @return the SAML TRC Assertion
     */
    public Assertion issueTrcToken(final Assertion hcpIdentityAssertion, String patientID, String purposeOfUse,
                                   String dispensationPinCode, String prescriptionId, List<Attribute> attrValuePair)
            throws SMgrException {

        if (OpenNCPConstants.NCP_SERVER_MODE != ServerMode.PRODUCTION && loggerClinical.isDebugEnabled()) {
            loggerClinical.debug("Assertion HCP issued: '{}' for Patient: '{}' and Purpose of use: '{}' - Attributes: ",
                    hcpIdentityAssertion.getID(), patientID, purposeOfUse);
        }
        // Initializing the Map
        auditDataMap.clear();
        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

        //  Doing an indirect copy so, because when cloning, signatures are lost.
        var signatureManager = new SignatureManager(keyStoreManager);

        try {
            signatureManager.verifySAMLAssertion(hcpIdentityAssertion);
        } catch (SMgrException ex) {
            throw new SMgrException("SAML Assertion Validation Failed: " + ex.getMessage());
        }

        var issuanceInstant = Instant.now();
        logger.info("Assertion validity: '{}' - '{}'", hcpIdentityAssertion.getConditions().getNotBefore(),
                hcpIdentityAssertion.getConditions().getNotOnOrAfter());
        if (hcpIdentityAssertion.getConditions().getNotBefore().isAfter(issuanceInstant)) {
            String msg = "Identity Assertion with ID " + hcpIdentityAssertion.getID() + " can't be used before " +
                    hcpIdentityAssertion.getConditions().getNotBefore() + ". Current UTC time is " + issuanceInstant;
            logger.error("SecurityManagerException: '{}'", msg);
            throw new SMgrException(msg);
        }
        if (hcpIdentityAssertion.getConditions().getNotOnOrAfter().isBefore(issuanceInstant)) {
            String msg = "Identity Assertion with ID " + hcpIdentityAssertion.getID() + " can't be used after " +
                    hcpIdentityAssertion.getConditions().getNotOnOrAfter() + ". Current UTC time is " + issuanceInstant;
            logger.error("SecurityManagerException: '{}'", msg);
            throw new SMgrException(msg);
        }
        String authnContextClassRef = hcpIdentityAssertion.getAuthnStatements().get(0).getAuthnContext()
                .getAuthnContextClassRef().getURI();
        if (!TwoFactorAuthentication.getAuthTypeValues().contains(authnContextClassRef)) {
            String msg = "Identity Assertion with ID " + hcpIdentityAssertion.getID() + "has authnContextClassRef= " +
                    authnContextClassRef + ". Instead authnContextClassRef must be one of " +
                    TwoFactorAuthentication.getAuthTypeValues().toString();
            logger.error("SecurityManagerException: '{}'", msg);
            throw new SMgrException(msg);
        }
        auditDataMap.put("hcpIdAssertionID", hcpIdentityAssertion.getID());

        // Create the assertion
        Assertion trc = AssertionUtil.create(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);

        if (patientID == null) {
            throw new SMgrException("Patient ID cannot be null");
        }

        auditDataMap.put("patientID", patientID);

        trc.setIssueInstant(issuanceInstant);
        trc.setID("_" + UUID.randomUUID());
        auditDataMap.put("trcAssertionID", trc.getID());

        trc.setVersion(SAMLVersion.VERSION_20);

        // Create and add the Subject
        Subject subject = AssertionUtil.create(Subject.class, Subject.DEFAULT_ELEMENT_NAME);

        trc.setSubject(subject);
        var issuer = new IssuerBuilder().buildObject();

        String countryCode = ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_CODE");
        String confIssuer = "urn:initgw:" + countryCode + ":countryB";
        issuer.setValue(confIssuer);
        issuer.setNameQualifier("urn:ehdsi:assertions:trc");
        trc.setIssuer(issuer);

        //  Set the TRC Assertion Subject element to the same value as the HCP one.
        NameID nameID = AssertionUtil.create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        nameID.setFormat(hcpIdentityAssertion.getSubject().getNameID().getFormat());
        nameID.setValue(hcpIdentityAssertion.getSubject().getNameID().getValue());
        trc.getSubject().setNameID(nameID);

        String spProvidedID = hcpIdentityAssertion.getSubject().getNameID().getSPProvidedID();
        String humanRequestorNameID = StringUtils.isNotBlank(spProvidedID) ? spProvidedID : "<" + hcpIdentityAssertion.getSubject().getNameID().getValue()
                + "@" + hcpIdentityAssertion.getIssuer().getValue() + ">";

        auditDataMap.put("humanRequestorNameID", humanRequestorNameID);

        var subjectIdAttr = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                "urn:oasis:names:tc:xspa:1.0:subject:subject-id");
        String humanRequesterAlternativeUserID = ((XSString) subjectIdAttr.getAttributeValues().get(0)).getValue();
        auditDataMap.put("humanRequestorSubjectID", humanRequesterAlternativeUserID);

        //Create and add Subject Confirmation
        SubjectConfirmation subjectConf = AssertionUtil.create(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        subjectConf.setMethod(SubjectConfirmation.METHOD_SENDER_VOUCHES);
        trc.getSubject().getSubjectConfirmations().add(subjectConf);

        //Create and add conditions according specifications (validity 2 hours)
        Conditions conditions = AssertionUtil.create(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(issuanceInstant);

        AudienceRestriction audienceRestriction = AssertionUtil.create(AudienceRestriction.class, AudienceRestriction.DEFAULT_ELEMENT_NAME);
        Audience audience = AssertionUtil.create(Audience.class, Audience.DEFAULT_ELEMENT_NAME);
        audience.setURI("urn:ehdsi:assertions.audience:x-border");
        audienceRestriction.getAudiences().add(audience);
        conditions.getAudienceRestrictions().add(audienceRestriction);

        conditions.setNotOnOrAfter(issuanceInstant.plus(Duration.ofHours(2)));
        trc.setConditions(conditions);

        //Create and add Advice
        Advice advice = AssertionUtil.create(Advice.class, Advice.DEFAULT_ELEMENT_NAME);
        trc.setAdvice(advice);

        //Create and add AssertionIDRef
        AssertionIDRef aIdRef = AssertionUtil.create(AssertionIDRef.class, AssertionIDRef.DEFAULT_ELEMENT_NAME);
        aIdRef.setValue(hcpIdentityAssertion.getID());
        advice.getAssertionIDReferences().add(aIdRef);

        //Add and create the authentication statement
        AuthnStatement authStmt = AssertionUtil.create(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
        authStmt.setAuthnInstant(issuanceInstant);
        trc.getAuthnStatements().add(authStmt);

        //Create and add AuthnContext
        AuthnContext ac = AssertionUtil.create(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
        AuthnContextClassRef accr = AssertionUtil.create(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        accr.setURI(AuthnContext.PREVIOUS_SESSION_AUTHN_CTX);
        ac.setAuthnContextClassRef(accr);
        authStmt.setAuthnContext(ac);

        // Create the SAML Attribute Statement
        AttributeStatement attrStmt = AssertionUtil.create(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);
        trc.getStatements().add(attrStmt);

        //Creating the Attribute that holds the Patient ID
        Attribute attrPID = AssertionUtil.create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName("XSPA Subject");

        // TODO: Is there a constant for that urn??
        attrPID.setName("urn:oasis:names:tc:xacml:1.0:resource:resource-id");
        attrPID.setNameFormat(Attribute.URI_REFERENCE);

        //Create and add the Attribute Value
        XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
        XSString attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrValPID.setValue(patientID);
        attrPID.getAttributeValues().add(attrValPID);
        attrStmt.getAttributes().add(attrPID);

        //Creating the Attribute that holds the Purpose of Use
        Attribute attrPoU = AssertionUtil.create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPoU.setFriendlyName("XSPA Purpose Of Use");
        // TODO: Is there a constant for that urn??
        attrPoU.setName("urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
        attrPoU.setNameFormat(Attribute.URI_REFERENCE);
        if (purposeOfUse == null) {
            attrPoU = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                    "urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
            if (attrPoU == null) {
                throw new SMgrException("Purpose of Use not found in the assertion and is not passed as a parameter");
            }
        } else {

            XMLObjectBuilder<XSAny> xsAnyBuilder = (XMLObjectBuilder<XSAny>) builderFactory.getBuilder(XSAny.TYPE_NAME);
            XSAny pou = xsAnyBuilder.buildObject("urn:hl7-org:v3", "PurposeOfUse", "");
            pou.getUnknownAttributes().put(new QName("code"), purposeOfUse);
            pou.getUnknownAttributes().put(new QName("codeSystem"), "3bc18518-d305-46c2-a8d6-94bd59856e9e");
            pou.getUnknownAttributes().put(new QName("codeSystemName"), "eHDSI XSPA PurposeOfUse");
            pou.getUnknownAttributes().put(new QName("displayName"), purposeOfUse);
            //pou.setTextContent(purposeOfUse);
            XSAny pouAttributeValue = xsAnyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
            pouAttributeValue.getUnknownXMLObjects().add(pou);
            attrPoU.getAttributeValues().add(pouAttributeValue);
        }
        attrStmt.getAttributes().add(attrPoU);

        if (StringUtils.isNotBlank(dispensationPinCode)) {
            Attribute attributeDispensationPinCode = AssertionUtil.create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
            attributeDispensationPinCode.setFriendlyName("Dispensation Pin Code");
            attributeDispensationPinCode.setName("urn:ehdsi:names:document:document-id:dispensationPinCode");
            attributeDispensationPinCode.setNameFormat(Attribute.URI_REFERENCE);
            XSString attrValPinCode = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            attrValPinCode.setValue(dispensationPinCode);
            attributeDispensationPinCode.getAttributeValues().add(attrValPinCode);
            attrStmt.getAttributes().add(attributeDispensationPinCode);
        }
        if (StringUtils.isNotBlank(prescriptionId)) {
            Attribute attributeDocumentId = AssertionUtil.create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
            attributeDocumentId.setFriendlyName("Prescription ID");
            attributeDocumentId.setName("urn:ehdsi:names:document:document-id:prescriptionId");
            attributeDocumentId.setNameFormat(Attribute.URI_REFERENCE);
            XSString attrValDocumentId = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            attrValDocumentId.setValue(prescriptionId);
            attributeDocumentId.getAttributeValues().add(attrValDocumentId);
            attrStmt.getAttributes().add(attributeDocumentId);
        }
        var pointOfCareAttr = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                "urn:oasis:names:tc:xspa:1.0:subject:organization");
        if (pointOfCareAttr != null) {
            String poc = ((XSString) pointOfCareAttr.getAttributeValues().get(0)).getValue();
            auditDataMap.put("pointOfCare", poc);
        } else {
            pointOfCareAttr = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                    "urn:oasis:names:tc:xspa:1.0:environment:locality");
            String poc = ((XSString) pointOfCareAttr.getAttributeValues().get(0)).getValue();
            auditDataMap.put("pointOfCare", poc);
        }

        var pointOfCareIdAttr = AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                "urn:oasis:names:tc:xspa:1.0:subject:organization-id");
        if (pointOfCareIdAttr != null) {
            String pocId = ((XSString) pointOfCareIdAttr.getAttributeValues().get(0)).getValue();
            auditDataMap.put("pointOfCareID", pocId);
        } else {
            auditDataMap.put("pointOfCareID", "No Organization ID - POC information");
        }

        String hrRole = ((XSString) AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                "urn:oasis:names:tc:xacml:2.0:subject:role").getAttributeValues().get(0)).getValue();

        auditDataMap.put("humanRequestorRole", hrRole);

        String facilityType = ((XSString) AssertionUtil.findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                "urn:ehdsi:names:subject:healthcare-facility-type").getAttributeValues().get(0)).getValue();

        auditDataMap.put("facilityType", facilityType);

        signatureManager.signSAMLAssertion(trc);
        if (logger.isDebugEnabled()) {
            logger.debug("Assertion generated at '{}'", trc.getIssueInstant().toString());
        }

        return trc;
    }

    /**
     * Verifies the signature of the TRC Assertion. The TRC Assertion should be signed and will be validated
     * also against both the patient ID and the HCP Identity that is provided by the IdentityAssertion.
     *
     * @param trcAssertion         The Assertion that is to be validated.
     * @param hcpIdentityAssertion The health care professional Identity SAML
     *                             Assertion. The method validates the assertion using the
     *                             {@link SignatureManager#verifySAMLAssertion(org.opensaml.saml.saml2.core.Assertion)}.
     * @param patientID            The Patient Id that is required for the TRC Assertion
     * @throws SMgrException when the verification fails.
     */
    public void verifyTrcToken(Assertion trcAssertion, Assertion hcpIdentityAssertion, String patientID) throws SMgrException {

        var signatureManager = new SignatureManager(keyStoreManager);
        signatureManager.verifySAMLAssertion(trcAssertion);
        signatureManager.verifySAMLAssertion(hcpIdentityAssertion);
    }

//    /**
//     * @param value
//     * @param friendlyName
//     * @param nameFormat
//     * @param name
//     * @return
//     */
//    protected Attribute createAttribute(String value, String friendlyName, String nameFormat, String name) {
//
//        Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
//        attr.setFriendlyName(friendlyName);
//        attr.setName(name);
//        attr.setNameFormat(nameFormat);
//
//        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
//        XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
//        XSString attrVal = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
//        attrVal.setValue(value);
//        attr.getAttributeValues().add(attrVal);
//        return attr;
//    }

//    /**
//     * @param statements
//     * @param attrName
//     * @return
//     */
//    protected Attribute findURIInAttributeStatement(List<AttributeStatement> statements, String attrName) {
//
//        for (AttributeStatement stmt : statements) {
//            for (Attribute attribute : stmt.getAttributes()) {
//                if (attribute.getName().equals(attrName)) {
//
//                    Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
//                    attr.setFriendlyName(attribute.getFriendlyName());
//                    attr.setName(attribute.getNameFormat());
//                    attr.setNameFormat(attribute.getNameFormat());
//
//                    XMLObjectBuilder uriBuilder = XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSURI.TYPE_NAME);
//                    XSURI attrVal = (XSURI) uriBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSURI.TYPE_NAME);
//                    attrVal.setURI(((XSURI) attribute.getAttributeValues().get(0)).getURI());
//                    attr.getAttributeValues().add(attrVal);
//
//                    return attr;
//                }
//            }
//        }
//        return null;
//    }

//    /**
//     * @param subject
//     * @return
//     */
//    protected NameID findProperNameID(Subject subject) {
//
//        String format = subject.getNameID().getFormat();
//        logger.debug("is email?: '{}'", format.equals(NameID.EMAIL));
//        logger.debug("is x509 subject?: '{}'", format.equals(NameID.X509_SUBJECT));
//        logger.debug("is Unspecified?: '{}'", format.equals(NameID.UNSPECIFIED));
//        NameID n = create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
//        n.setFormat(format);
//        n.setValue(subject.getNameID().getValue());
//
//        return n;
//    }

//    /**
//     * @param stmts
//     * @return
//     */
//    protected NameID getXspaSubjectFromAttributes(List<AttributeStatement> stmts) {
//
//        var xspaSubjectAttribute = AssertionUtil.findStringInAttributeStatement(stmts, "urn:oasis:names:tc:xspa:1.0:subject:subject-id");
//        NameID nameID = create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
//        nameID.setFormat(NameID.UNSPECIFIED);
//        nameID.setValue(((XSString) xspaSubjectAttribute.getAttributeValues().get(0)).getValue());
//
//        return nameID;
//    }

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
