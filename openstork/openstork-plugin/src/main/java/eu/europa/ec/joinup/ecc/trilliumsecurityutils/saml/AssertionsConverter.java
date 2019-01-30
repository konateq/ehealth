package eu.europa.ec.joinup.ecc.trilliumsecurityutils.saml;

import epsos.ccd.netsmart.securitymanager.SamlTRCIssuer;
import epsos.ccd.netsmart.securitymanager.SignatureManager;
import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import epsos.ccd.netsmart.securitymanager.key.KeyStoreManager;
import epsos.ccd.netsmart.securitymanager.key.impl.DefaultKeyStoreManager;
import epsos.ccd.netsmart.securitymanager.sts.client.TRCAssertionRequest;
import eu.epsos.assertionvalidator.AssertionHelper;
import eu.epsos.assertionvalidator.PolicyManagerInterface;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.AssertionMarshaller;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.xml.*;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.XSURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tr.com.srdc.epsos.data.model.PatientId;
import tr.com.srdc.epsos.securityman.exceptions.InsufficientRightsException;
import tr.com.srdc.epsos.securityman.exceptions.MissingFieldException;
import tr.com.srdc.epsos.util.Constants;

import javax.xml.namespace.QName;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * This class contains several conversion methods to convert epSOS Specific SAML Assertions into KP exchange required assertions.
 *
 * @author Marcelo Fonseca <marcelo.fonseca@iuz.pt>
 */
public class AssertionsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionsConverter.class);
    private static final String PORTAL_DOCTOR_PERMISSIONS = "PORTAL_DOCTOR_PERMISSIONS";
    private static final String PORTAL_PHARMACIST_PERMISSIONS = "PORTAL_PHARMACIST_PERMISSIONS";
    private static final String PORTAL_NURSE_PERMISSIONS = "PORTAL_NURSE_PERMISSIONS";
    private static final String PORTAL_ADMIN_PERMISSIONS = "PORTAL_ADMIN_PERMISSIONS";
    private static final String PORTAL_PATIENT_PERMISSIONS = "PORTAL_PATIENT_PERMISSIONS";

    private static <T> T create(Class<T> cls, QName qname) {
        return (T) org.opensaml.Configuration.getBuilderFactory()
                .getBuilder(qname).buildObject(qname);
    }

    public static Assertion createPlainTRCA(String purpose, Assertion idAs, String patientId) throws SMgrException {

        Assertion trc;
        SamlTRCIssuer issuer = new SamlTRCIssuer();
        trc = issuer.issueTrcToken(idAs, patientId, purpose, null);
        return trc;
    }

    public static Assertion createTRCA(String purpose, Assertion idAs, String root, String extension) throws Exception {

        Assertion trc;
        LOGGER.debug("Try to create TRCA for patient : '{}'", extension);
        String pat;
        pat = extension + "^^^&" + root + "&ISO";
        LOGGER.info("TRCA Patient ID : '{}'", pat);
        LOGGER.info("Assertion ID: '{}'", idAs.getID());
        LOGGER.info("SECMAN URL: '{}'", ConfigurationManagerFactory.getConfigurationManager().getProperty("secman.sts.url"));
        TRCAssertionRequest req1 = new TRCAssertionRequest.Builder(idAs, pat).PurposeOfUse(purpose).build();
        trc = req1.request();

        AssertionMarshaller marshaller = new AssertionMarshaller();
        Element element;
        element = marshaller.marshall(trc);


        if (LOGGER.isDebugEnabled()) {
            Document document = element.getOwnerDocument();
            //String trca = Util.getDocumentAsXml(document, false);
            LOGGER.info("#### TRCA Start");
            //LOGGER.info(trca);
            LOGGER.info("#### TRCA End");
        }

        LOGGER.debug("TRCA CREATED: '{}'", trc.getID());
        LOGGER.debug("TRCA WILL BE STORED TO SESSION: '{}'", trc.getID());
        return trc;
    }

    public static Assertion createEpsosAssertion(Assertion kpAssertion) throws Exception {

        // TODO get more attributes from the assertion and fill the new one, like role, treatment ...
        String role = AssertionUtils.getRoleFromKPAssertion(kpAssertion);
        boolean isPhysician = role.equalsIgnoreCase("doctor");
        boolean isPharmacist = role.equalsIgnoreCase("pharmacist");
        boolean isNurse = role.equalsIgnoreCase("nurse");
        boolean isAdministrator = role.equalsIgnoreCase("admin");
        boolean isPatient = role.equalsIgnoreCase("patient");

        if (isPhysician || isPharmacist || isNurse || isAdministrator || isPatient) {
            LOGGER.info("The role is one of the expected. Continuing ...");
        } else {
            LOGGER.error("The portal role is NOT one of the expected. Break");
            return null;
        }

        String orgName;
        List<String> permissions = new ArrayList<>();

        String username = "TRILLIUM GATEWAY";
        String rolename = "";
        String prefix = "urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:";

        if (isPhysician) {
            rolename = "medical doctor";
            String doctor_perms = ConfigurationManagerFactory.getConfigurationManager().getProperty(PORTAL_DOCTOR_PERMISSIONS);
            String p[] = doctor_perms.split(",");
            for (String aP : p) {
                permissions.add(prefix + aP);
            }
        }
        if (isPharmacist) {
            rolename = "pharmacist";
            String pharm_perms = ConfigurationManagerFactory.getConfigurationManager().getProperty(PORTAL_PHARMACIST_PERMISSIONS);
            String p1[] = pharm_perms.split(",");
            for (String aP1 : p1) {
                permissions.add(prefix + aP1);
            }
        }
        if (isNurse) {
            rolename = "nurse";
            String nurse_perms = ConfigurationManagerFactory.getConfigurationManager().getProperty(PORTAL_NURSE_PERMISSIONS);
            String p1[] = nurse_perms.split(",");
            for (String aP1 : p1) {
                permissions.add(prefix + aP1);
            }
        }
        if (isPatient) {
            rolename = "patient";
            String patient_perms = ConfigurationManagerFactory.getConfigurationManager().getProperty(PORTAL_PATIENT_PERMISSIONS);
            String p1[] = patient_perms.split(",");
            for (String aP1 : p1) {
                permissions.add(prefix + aP1);
            }
        }
        if (isAdministrator) {
            rolename = "administrator";
            String admin_perms = ConfigurationManagerFactory.getConfigurationManager().getProperty(PORTAL_ADMIN_PERMISSIONS);
            String p1[] = admin_perms.split(",");
            for (String aP1 : p1) {
                permissions.add(prefix + aP1);
            }
        }

        orgName = "TRILLIUM GATEWAY";
        String poc = "POC Trillium Bidge";
        // fixed for consent creation AuthorInstitution Validation problem
        String orgId = "TRILLIUMGATEWAY.1";
        String orgType;
        if (isPharmacist) {
            orgType = "Pharmacy";
        } else {
            orgType = "Hospital";
        }
        Assertion assertion = createEpsosAssertion(username, rolename, orgName, orgId, orgType, "TREATMENT",
                poc, permissions);

        LOGGER.debug("Getting config manager");
        String KEY_ALIAS = Constants.NCP_SIG_PRIVATEKEY_ALIAS;
        LOGGER.debug("KEY ALIAS: '{}'", KEY_ALIAS);
        String PRIVATE_KEY_PASS = Constants.NCP_SIG_PRIVATEKEY_PASSWORD;

        AssertionUtils.signSAMLAssertion(assertion, KEY_ALIAS, PRIVATE_KEY_PASS.toCharArray());
        AssertionMarshaller marshaller = new AssertionMarshaller();

        Element element;
        element = marshaller.marshall(assertion);

        Document document = element.getOwnerDocument();
        String hcpa = AssertionUtils.getDocumentAsXml(document, false);
        LOGGER.info("#### HCPA Start");
        LOGGER.info(hcpa);
        LOGGER.info("#### HCPA End");

        if (assertion != null) {
            LOGGER.info("The assertion has been created with id: '{}'", assertion.getID());
        } else {
            LOGGER.error("########### Error creating assertion");
        }

        return assertion;
    }

    public static Assertion issueTrcToken(final Assertion hcpIdentityAssertion, String patientID, String purposeOfUse,
                                          List<Attribute> attrValuePair) throws SMgrException {

        KeyStoreManager ksm;
        HashMap<String, String> auditDataMap;

        ksm = new DefaultKeyStoreManager();
        auditDataMap = new HashMap<>();

        try {
            //initializing the map
            auditDataMap.clear();
            XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
            SecureRandomIdentifierGenerator randomGen = new SecureRandomIdentifierGenerator();

            //Doing an indirect copy so, because when cloning, signatures are lost.
            SignatureManager sman = new SignatureManager(ksm);

            try {
                sman.verifySAMLAssertion(hcpIdentityAssertion);
            } catch (SMgrException ex) {
                LOGGER.error("SMgrException: '{}'", ex);
                throw new SMgrException("SAML Assertion Validation Failed: " + ex.getMessage());
            }
            if (hcpIdentityAssertion.getConditions().getNotBefore().isAfterNow()) {
                String msg = "Identity Assertion with ID " + hcpIdentityAssertion.getID() + " can't ne used before " + hcpIdentityAssertion.getConditions().getNotBefore();
                LOGGER.error("SMgrException: '{}'", msg);
                throw new SMgrException(msg);
            }
            if (hcpIdentityAssertion.getConditions().getNotOnOrAfter().isBeforeNow()) {
                String msg = "Identity Assertion with ID " + hcpIdentityAssertion.getID() + " can't be used after " + hcpIdentityAssertion.getConditions().getNotOnOrAfter();
                LOGGER.error("SMgrException: '{}'", msg);
                throw new SMgrException(msg);
            }

            auditDataMap.put("hcpIdAssertionID", hcpIdentityAssertion.getID());

            // Create the assertion
            Assertion trc = create(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
            if (patientID == null) {
                throw new SMgrException("Patiend ID cannot be null");
            }

            auditDataMap.put("patientID", patientID);
            DateTime now = new DateTime();
            DateTime nowUTC = now.withZone(DateTimeZone.UTC).toDateTime();

            trc.setIssueInstant(nowUTC.toDateTime());
            trc.setID("_" + UUID.randomUUID());
            auditDataMap.put("trcAssertionID", trc.getID());

            trc.setVersion(SAMLVersion.VERSION_20);

            // Create and add the Subject
            Subject subject = create(Subject.class, Subject.DEFAULT_ELEMENT_NAME);

            trc.setSubject(subject);
            Issuer issuer = new IssuerBuilder().buildObject();

            String confIssuer = ConfigurationManagerFactory.getConfigurationManager().getProperty("secman.trc.endpoint");

            if (confIssuer.isEmpty()) {

                String countryCode = ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_CODE");
                confIssuer = "urn:initgw:" + countryCode + ":countryB";
                ConfigurationManagerFactory.getConfigurationManager().setProperty("secman.trc.endpoint", confIssuer);
            }
            issuer.setValue(confIssuer);
            trc.setIssuer(issuer);

            NameID nameid = getXspaSubjectFromAttributes(hcpIdentityAssertion.getAttributeStatements());
            trc.getSubject().setNameID(nameid);
            auditDataMap.put("humanRequestorNameID", hcpIdentityAssertion.getSubject().getNameID().getValue());
            auditDataMap.put("humanRequestorSubjectID", nameid.getValue());

            //Create and add Subject Confirmation
            SubjectConfirmation subjectConf = create(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
            subjectConf.setMethod(SubjectConfirmation.METHOD_SENDER_VOUCHES);
            trc.getSubject().getSubjectConfirmations().add(subjectConf);

            //Create and add conditions
            Conditions conditions = create(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
            conditions.setNotBefore(nowUTC.toDateTime());
            conditions.setNotOnOrAfter(nowUTC.toDateTime().plusHours(2));
            trc.setConditions(conditions);

            //Create and add Advice
            Advice advice = create(Advice.class, Advice.DEFAULT_ELEMENT_NAME);
            trc.setAdvice(advice);

            //Create and add AssertionIDRef
            AssertionIDRef aIdRef = create(AssertionIDRef.class, AssertionIDRef.DEFAULT_ELEMENT_NAME);
            aIdRef.setAssertionID(hcpIdentityAssertion.getID());
            advice.getAssertionIDReferences().add(aIdRef);

            //Add and create the authentication statement
            AuthnStatement authStmt = create(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
            authStmt.setAuthnInstant(nowUTC.toDateTime());
            trc.getAuthnStatements().add(authStmt);

            //Creata and add AuthnContext
            AuthnContext ac = create(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
            AuthnContextClassRef accr = create(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
            accr.setAuthnContextClassRef(AuthnContext.PREVIOUS_SESSION_AUTHN_CTX);
            ac.setAuthnContextClassRef(accr);
            authStmt.setAuthnContext(ac);

            // Create the Saml Attribute Statement
            AttributeStatement attrStmt = create(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);
            trc.getStatements().add(attrStmt);

            //Creating the Attribute that holds the Patient ID
            Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
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
            Attribute attrPoU = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
            attrPoU.setFriendlyName("XSPA Purpose Of Use");

            // TODO: Is there a constant for that urn??
            attrPoU.setName("urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
            attrPoU.setNameFormat(Attribute.URI_REFERENCE);
            if (purposeOfUse == null) {
                attrPoU = findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
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

            String poc = ((XSString) Objects.requireNonNull(findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                    "urn:oasis:names:tc:xspa:1.0:subject:organization")).getAttributeValues().get(0)).getValue();

            LOGGER.info("Point of Care: {0}", poc);
            auditDataMap.put("pointOfCare", poc);

            String pocId = ((XSURI) Objects.requireNonNull(findURIInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                    "urn:oasis:names:tc:xspa:1.0:subject:organization-id")).getAttributeValues().get(0)).getValue();

            LOGGER.info("Point of Care: {0}", poc);
            auditDataMap.put("pointOfCareID", pocId);

            String hrRole = ((XSString) Objects.requireNonNull(findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                    "urn:oasis:names:tc:xacml:2.0:subject:role")).getAttributeValues().get(0)).getValue();

            LOGGER.info("HR Role {0}", hrRole);
            auditDataMap.put("humanRequestorRole", hrRole);

            String facilityType = ((XSString) Objects.requireNonNull(findStringInAttributeStatement(hcpIdentityAssertion.getAttributeStatements(),
                    "urn:epsos:names:wp3.4:subject:healthcare-facility-type")).getAttributeValues().get(0)).getValue();

            LOGGER.info("Facility Type {0}", facilityType);
            auditDataMap.put("facilityType", facilityType);

            sman.signSAMLAssertion(trc);
            return trc;
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error(null, ex);
            throw new SMgrException(ex.getMessage());
        }
    }

    protected static Attribute findURIInAttributeStatement(List<AttributeStatement> statements, String attrName) {

        LOGGER.info("Size:{0}", statements.size());
        for (AttributeStatement stmt : statements) {

            for (Attribute attribute : stmt.getAttributes()) {

                if (attribute.getName().equals(attrName)) {

                    LOGGER.info("Attribute Name:{0}", attribute.getName());
                    Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);

                    attr.setFriendlyName(attribute.getFriendlyName());
                    attr.setName(attribute.getNameFormat());
                    attr.setNameFormat(attribute.getNameFormat());

                    XMLObjectBuilder uriBuilder = Configuration.getBuilderFactory().getBuilder(XSURI.TYPE_NAME);
                    XSURI attrVal = (XSURI) uriBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSURI.TYPE_NAME);

                    attrVal.setValue(((XSURI) attribute.getAttributeValues().get(0)).getValue());
                    attr.getAttributeValues().add(attrVal);

                    return attr;
                }
            }
        }
        return null;
    }

    protected static NameID findProperNameID(Subject subject) {

        String format = subject.getNameID().getFormat();
        LOGGER.info("is email?: {0}", format.equals(NameID.EMAIL));
        LOGGER.info("is x509 subject?: {0}", format.equals(NameID.X509_SUBJECT));
        LOGGER.info("is Unspecified?: {0}", format.equals(NameID.UNSPECIFIED));

        NameID n = create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        n.setFormat(format);
        n.setValue(subject.getNameID().getValue());
        return n;
    }

    private static Assertion createEpsosAssertion(String username, String role, String organization, String organizationId,
                                                  String facilityType, String purposeOfUse, String xspaLocality,
                                                  List<String> permissions) {
        // assertion
        Assertion assertion = null;
        try {
            DefaultBootstrap.bootstrap();
            XMLObjectBuilderFactory builderFactory = org.opensaml.Configuration
                    .getBuilderFactory();

            SAMLObjectBuilder<Assertion> builder = (SAMLObjectBuilder<Assertion>) builderFactory
                    .getBuilder(Assertion.DEFAULT_ELEMENT_NAME);

            // Create the NameIdentifier
            SAMLObjectBuilder nameIdBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
            NameID nameId = (NameID) nameIdBuilder.buildObject();
            nameId.setValue(username);
            nameId.setFormat(NameID.UNSPECIFIED);

            assertion = create(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);

            String assId = "_" + UUID.randomUUID().toString();
            assertion.setID(assId);
            assertion.setVersion(SAMLVersion.VERSION_20);
            org.joda.time.DateTime now = new org.joda.time.DateTime();
            DateTime nowUTC = now.withZone(DateTimeZone.UTC).toDateTime();

            assertion.setIssueInstant(nowUTC.toDateTime());

            Subject subject = create(Subject.class, Subject.DEFAULT_ELEMENT_NAME);
            assertion.setSubject(subject);
            subject.setNameID(nameId);

            // Create and add Subject Confirmation
            SubjectConfirmation subjectConf = create(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
            subjectConf.setMethod(SubjectConfirmation.METHOD_SENDER_VOUCHES);
            assertion.getSubject().getSubjectConfirmations().add(subjectConf);

            // Create and add conditions
            Conditions conditions = create(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);

            conditions.setNotBefore(nowUTC.toDateTime());
            conditions.setNotOnOrAfter(nowUTC.toDateTime().plusHours(4));
            assertion.setConditions(conditions);

            String countryCode = ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_CODE");
            Issuer issuer = new IssuerBuilder().buildObject();
            issuer.setValue("urn:idp:" + countryCode + ":countryB");
            issuer.setNameQualifier("urn:epsos:wp34:assertions");
            assertion.setIssuer(issuer);

            // Add and create the authentication statement
            AuthnStatement authStmt = create(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
            authStmt.setAuthnInstant(nowUTC.toDateTime());
            assertion.getAuthnStatements().add(authStmt);

            // Create and add AuthnContext
            AuthnContext ac = create(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
            AuthnContextClassRef accr = create(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
            accr.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);
            ac.setAuthnContextClassRef(accr);
            authStmt.setAuthnContext(ac);

            AttributeStatement attrStmt = create(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);

            // XSPA Subject
            Attribute attrPID = createAttribute(builderFactory, "XSPA subject",
                    "urn:oasis:names:tc:xacml:1.0:subject:subject-id", username, "", "");
            attrStmt.getAttributes().add(attrPID);
            // XSPA Role
            Attribute attrPID_1 = createAttribute(builderFactory, "XSPA role",
                    "urn:oasis:names:tc:xacml:2.0:subject:role", role, "", "");
            attrStmt.getAttributes().add(attrPID_1);
            // HITSP Clinical Speciality
            /*
             * Attribute attrPID_2 =
             * createAttribute(builderFactory,"HITSP Clinical Speciality",
             * "urn:epsos:names:wp3.4:subject:clinical-speciality",role,"","");
             * attrStmt.getAttributes().add(attrPID_2);
             */
            // XSPA Organization
            Attribute attrPID_3 = createAttribute(builderFactory, "XSPA Organization",
                    "urn:oasis:names:tc:xspa:1.0:subject:organization", organization, "", "");
            attrStmt.getAttributes().add(attrPID_3);
            // XSPA Organization ID
            Attribute attrPID_4 = createAttribute(builderFactory, "XSPA Organization ID",
                    "urn:oasis:names:tc:xspa:1.0:subject:organization-id", organizationId, "AA", "");
            attrStmt.getAttributes().add(attrPID_4);

            // // On behalf of
            // Attribute attrPID_4 =
            // createAttribute(builderFactory,"OnBehalfOf",
            // "urn:epsos:names:wp3.4:subject:on-behalf-of",organizationId,role,"");
            // attrStmt.getAttributes().add(attrPID_4);
            // epSOS Healthcare Facility Type
            Attribute attrPID_5 = createAttribute(builderFactory, "epSOS Healthcare Facility Type",
                    "urn:epsos:names:wp3.4:subject:healthcare-facility-type", facilityType, "", "");
            attrStmt.getAttributes().add(attrPID_5);
            // XSPA Purpose of Use
            Attribute attrPID_6 = createAttribute(builderFactory, "XSPA Purpose Of Use",
                    "urn:oasis:names:tc:xspa:1.0:subject:purposeofuse", purposeOfUse, "", "");
            attrStmt.getAttributes().add(attrPID_6);
            // XSPA Locality
            Attribute attrPID_7 = createAttribute(builderFactory, "XSPA Locality",
                    "urn:oasis:names:tc:xspa:1.0:environment:locality", xspaLocality, "", "");
            attrStmt.getAttributes().add(attrPID_7);
            // HL7 Permissions
            Attribute attrPID_8 = createAttribute(builderFactory,
                    "Hl7 Permissions",
                    "urn:oasis:names:tc:xspa:1.0:subject:hl7:permission");
            for (Object permission : permissions) {
                attrPID_8 = AddAttributeValue(builderFactory, attrPID_8, permission.toString(), "", "");
            }
            attrStmt.getAttributes().add(attrPID_8);

            assertion.getStatements().add(attrStmt);

        } catch (ConfigurationException e) {
            LOGGER.error("ConfigurationException: '{}'", e.getMessage(), e);
        }
        return assertion;
    }

    public static Assertion convertIdAssertion(Assertion epsosHcpAssertion, PatientId patientId) {

        if (epsosHcpAssertion == null) {
            LOGGER.error("Provided Assertion is null.");
            return null;
        }

        final String X509_SUBJECT_FORMAT = "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName";

        Assertion result;

        final HCPIAssertionBuilder assertionBuilder;
        final String hcpRole;
        final String hcpId;
        final String hcpSpecialty;
        final String orgId;
        final String orgName;
        final String healthCareFacilityType;
        final String purposeOfUse;
        final String pointOfCare;
        final List<String> permissions;

        LOGGER.info("Converting Assertion.");

        // Initialize Assertions Builder with minimum initial parameters.
        assertionBuilder = new HCPIAssertionBuilder("UID=medical doctor", X509_SUBJECT_FORMAT, "sender-vouches")
                .issuer("O=European HCP,L=Europe,ST=Europe,C=EU", X509_SUBJECT_FORMAT)
                .audienceRestrictions("http://ihe.connecthaton.XUA/X-ServiceProvider-IHE-Connectathons")
                .notOnOrAfter(4);

        // MANDATORY: HCP ID and HCP Role
        try {
            hcpRole = AssertionHelper.getAttributeFromAssertion(epsosHcpAssertion, PolicyManagerInterface.URN_OASIS_NAMES_TC_XACML_2_0_SUBJECT_ROLE);
            hcpId = AssertionHelper.getAttributeFromAssertion(epsosHcpAssertion, PolicyManagerInterface.URN_OASIS_NAMES_TC_XACML_1_0_SUBJECT_SUBJECT_ID);
            assertionBuilder.hcpIdentifier(hcpId).hcpRole(hcpRole);

        } catch (MissingFieldException ex) {
            LOGGER.error("One or more required attributes were not found in the original assertion: '{}'", ex.getMessage(), ex);
            return null;
        }

        // OPTIONAL: HCP Specialty
        try {
            hcpSpecialty = AssertionHelper.getAttributeFromAssertion(epsosHcpAssertion, "urn:epsos:names:wp3.4:subject:clinical-speciality");
            assertionBuilder.hcpSpecialty(hcpSpecialty);

        } catch (MissingFieldException ex) {
            LOGGER.info("Optional attribute not found, proceeding with conversion (HCP Specialty).", ex);
        }

        // OPTIONAL: HCP Organization ID and HCP Organization Name
        try {
            orgId = AssertionHelper.getAttributeFromAssertion(epsosHcpAssertion, "urn:oasis:names:tc:xspa:1.0:subject:organization-id");
            orgName = AssertionHelper.getAttributeFromAssertion(epsosHcpAssertion, "urn:oasis:names:tc:xspa:1.0:subject:organization");
            assertionBuilder.healthCareProfessionalOrganisation(orgId, orgName);

        } catch (MissingFieldException ex) {
            LOGGER.info("Optional attribute not found, proceeding with conversion ( HCP Organization ID and HCP Organization Name.", ex);
        }

        // MANDATORY: HealthCare Facility Type
        try {
            healthCareFacilityType = AssertionHelper.getAttributeFromAssertion(epsosHcpAssertion, "urn:epsos:names:wp3.4:subject:healthcare-facility-type");
            assertionBuilder.healthCareFacilityType(healthCareFacilityType);

        } catch (MissingFieldException ex) {
            LOGGER.error("One or more required attributes were not found in the original assertion", ex);
            return null;
        }

        // MANDATORY: Purpose of Use
        try {
            purposeOfUse = AssertionHelper.getAttributeFromAssertion(epsosHcpAssertion, PolicyManagerInterface.URN_OASIS_NAMES_TC_XSPA_1_0_SUBJECT_PURPOSEOFUSE);
            assertionBuilder.purposeOfUse(purposeOfUse);

        } catch (MissingFieldException ex) {
            LOGGER.error("One or more required attributes were not found in the original assertion", ex);
            return null;

        }

        // MANDATORY: Point Of Care
        try {
            pointOfCare = AssertionHelper.getAttributeFromAssertion(epsosHcpAssertion, "urn:oasis:names:tc:xspa:1.0:environment:locality");
            assertionBuilder.pointOfCare(pointOfCare);

        } catch (MissingFieldException ex) {
            LOGGER.error("One or more required attributes were not found in the original assertion", ex);
            return null;
        }

        // MANDATORY: Patient ID (For eHealth Exchange)
        if (patientId != null && patientId.getfullId() != null && !patientId.getfullId().isEmpty()) {
            assertionBuilder.patientId(patientId.getfullId());

        } else {
            LOGGER.error("One or more required attributes were not found (Patient Id).");
            return null;
        }
        // MANDATORY: Home Community Id (For eHealth Exchange)
        if (Constants.HOME_COMM_ID != null && !Constants.HOME_COMM_ID.isEmpty()) {
            assertionBuilder.homeCommunityId(Constants.HOME_COMM_ID);

        } else {
            LOGGER.error("One or more required attributes were not found (Home Community Id).");
            return null;
        }

        //  OPTIONAL (0..*): Permissions
        try {
            permissions = convertPermissions(AssertionHelper.getPermissionValuesFromAssertion(epsosHcpAssertion));
            assertionBuilder.permissions(permissions);

        } catch (InsufficientRightsException ex) {
            LOGGER.error("An Insufficient Rights error was found while extracting permissions from the assertion.", ex);
            return null;
        }

        // BUILD Assertion
        result = assertionBuilder.build();

        return result;
    }

    public static Assertion convertTrcAssertion(Assertion epsosHcpAssertion) {

        if (epsosHcpAssertion == null) {
            LOGGER.error("Provided Assertion is null.");
            return null;
        }
        final Assertion result;

        LOGGER.info("Conversion not implemented: mirroring TRC Assertion");
        result = epsosHcpAssertion;

        return result;
    }

    private static List<String> convertPermissions(final List<XMLObject> permissions) {

        if (permissions == null || permissions.isEmpty()) {
            LOGGER.error("Provided list is null or empty.");
            return null;
        }

        List<String> result = new ArrayList<>();

        for (XMLObject permission : permissions) {
            result.add(permission.getDOM().getTextContent());
        }

        return result;
    }

    private static Attribute createAttribute(XMLObjectBuilderFactory builderFactory, String friendlyName, String oasisName) {

        Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(friendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);

        return attrPID;
    }

    private static Attribute AddAttributeValue(XMLObjectBuilderFactory builderFactory, Attribute attribute, String value,
                                               String namespace, String xmlschema) {

        XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
        XSString attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrValPID.setValue(value);
        attribute.getAttributeValues().add(attrValPID);

        return attribute;
    }

    private static Attribute createAttribute(XMLObjectBuilderFactory builderFactory, String FriendlyName, String oasisName,
                                             String value, String namespace, String xmlschema) {

        Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(FriendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);
        // Create and add the Attribute Value

        XMLObjectBuilder stringBuilder;

        if (namespace.equals("")) {

            XSString attrValPID;
            stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
            attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            attrValPID.setValue(value);
            attrPID.getAttributeValues().add(attrValPID);
        } else {

            XSURI attrValPID;
            stringBuilder = builderFactory.getBuilder(XSURI.TYPE_NAME);
            attrValPID = (XSURI) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSURI.TYPE_NAME);
            attrValPID.setValue(value);
            attrPID.getAttributeValues().add(attrValPID);
        }

        return attrPID;
    }

    protected static Attribute findStringInAttributeStatement(List<AttributeStatement> statements, String attrName) {

        LOGGER.info("Size:{0}", statements.size());

        for (AttributeStatement stmt : statements) {
            for (Attribute attribute : stmt.getAttributes()) {
                if (attribute.getName().equals(attrName)) {

                    LOGGER.info("Attribute Name: {0}", attribute.getName());
                    Attribute attr = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
                    attr.setFriendlyName(attribute.getFriendlyName());
                    attr.setName(attribute.getNameFormat());
                    attr.setNameFormat(attribute.getNameFormat());

                    XMLObjectBuilder stringBuilder = Configuration.getBuilderFactory().getBuilder(XSString.TYPE_NAME);
                    XSString attrVal = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
                    attrVal.setValue(((XSString) attribute.getAttributeValues().get(0)).getValue());
                    attr.getAttributeValues().add(attrVal);

                    return attr;
                }
            }
        }
        return null;
    }

    protected static NameID getXspaSubjectFromAttributes(List<AttributeStatement> stmts) {

        Attribute xspaSubjectAttribute = findStringInAttributeStatement(stmts, "urn:oasis:names:tc:xacml:1.0:subject:subject-id");

        NameID n = create(NameID.class, NameID.DEFAULT_ELEMENT_NAME);
        n.setFormat(NameID.UNSPECIFIED);
        if (xspaSubjectAttribute != null) {
            n.setValue(((XSString) xspaSubjectAttribute.getAttributeValues().get(0)).getValue());
        }

        return n;
    }
}
