package eu.europa.ec.sante.openncp.application.client.connector.integrationtests.util;

import eu.europa.ec.sante.openncp.application.client.connector.assertion.AssertionService;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.ImmutableTrcAssertionRequest;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.STSClientException;
import eu.europa.ec.sante.openncp.application.client.connector.assertion.TrcAssertionRequest;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.key.DefaultKeyStoreManager;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.AssertionMarshaller;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssertionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionUtils.class);

    public static Assertion createClinicalAssertion(final KeyStoreManager keyStoreManager, final String username, final String fullName,
                                              final String email) {
        final List<String> permissions = new ArrayList<>();
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-003");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-004");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-005");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-006");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-010");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PRD-016");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PPD-032");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PPD-033");
        permissions.add("urn:oasis:names:tc:xspa:1.0:subject:hl7:permission:PPD-046");

        final AssertionUtils.Concept conceptRole = new AssertionUtils.Concept();
        conceptRole.setCode("221");
        conceptRole.setCodeSystemId("2.16.840.1.113883.2.9.6.2.7");
        conceptRole.setCodeSystemName("ISCO");
        conceptRole.setDisplayName("Medical Doctors");

        return createHCPAssertion(keyStoreManager, fullName, email, "BE", "Belgium", "homecommid", conceptRole,
                "eHealth OpenNCP EU Portal", "urn:hl7ii:1.2.3.4:ABCD", "Resident Physician", "TREATMENT",
                "eHDSI EU Testing MedCare Center", permissions, null);
    }

    private static Assertion createHCPAssertion(KeyStoreManager keyStoreManager, final String fullName, final String email, final String countryCode,
                                               final String countryName, final String homeCommId, final AssertionUtils.Concept role, final String organization,
                                               final String organizationId, final String facilityType, final String purposeOfUse,
                                               final String locality, final List<String> permissions, final String onBehalfId) {

        Assertion assertion = null;
        try {

            final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

            // Create the NameIdentifier
            final SAMLObjectBuilder nameIdBuilder = (SAMLObjectBuilder) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
            assert nameIdBuilder != null;
            final var nameId = (NameID) nameIdBuilder.buildObject();
            nameId.setValue(email);
            nameId.setFormat(NameID.EMAIL);

            assertion = create(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
            final var issuedInstant = DateTime.now();
            final String assId = "_" + UUID.randomUUID();
            assertion.setID(assId);
            assertion.setVersion(SAMLVersion.VERSION_20);
            assertion.setIssueInstant(issuedInstant);

            final Subject subject = create(Subject.class, Subject.DEFAULT_ELEMENT_NAME);
            assertion.setSubject(subject);
            subject.setNameID(nameId);

            // Create and add Subject Confirmation
            final SubjectConfirmation subjectConf = create(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
            subjectConf.setMethod(SubjectConfirmation.METHOD_SENDER_VOUCHES);
            assertion.getSubject().getSubjectConfirmations().add(subjectConf);

            // Create and add conditions
            final Conditions conditions = create(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);

            conditions.setNotBefore(issuedInstant);

            final AudienceRestriction audienceRestriction = create(AudienceRestriction.class, AudienceRestriction.DEFAULT_ELEMENT_NAME);
            final Audience audience = create(Audience.class, Audience.DEFAULT_ELEMENT_NAME);
            audience.setAudienceURI("urn:ehdsi:assertions.audience:x-border");
            audienceRestriction.getAudiences().add(audience);
            conditions.getAudienceRestrictions().add(audienceRestriction);

            // According to Spec
            conditions.setNotOnOrAfter(issuedInstant.plus(Duration.standardHours(4)));
            assertion.setConditions(conditions);

            final var issuer = new IssuerBuilder().buildObject();
            issuer.setValue("urn:idp:" + countryCode + ":countryB");
            issuer.setNameQualifier("urn:ehdsi:assertions:hcp");
            assertion.setIssuer(issuer);

            // Add and create the authentication statement
            final AuthnStatement authStmt = create(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
            authStmt.setAuthnInstant(issuedInstant);
            assertion.getAuthnStatements().add(authStmt);

            // Create and add AuthnContext
            final AuthnContext authnContext = create(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
            final AuthnContextClassRef authnContextClassRef = create(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
            //  Based on National Requirements and implementation this value might need to be updated.
            authnContextClassRef.setAuthnContextClassRef(AuthnContext.SMARTCARD_PKI_AUTHN_CTX);
            authnContext.setAuthnContextClassRef(authnContextClassRef);
            authStmt.setAuthnContext(authnContext);

            final AttributeStatement attrStmt = create(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);

            // Set HC Identifier
            final var attrHCID = createAttribute(builderFactory, "HCI Identifier", "urn:ihe:iti:xca:2010:homeCommunityId", "urn:oid:" + homeCommId,
                    "");
            attrStmt.getAttributes().add(attrHCID);

            // Set NP Identifier
            final var attrNPID = createAttribute(builderFactory, "NPI Identifier", "urn:oasis:names:tc:xspa:1.0:subject:npi", countryName, "");
            attrStmt.getAttributes().add(attrNPID);

            // XSPA Subject
            final var attrPID = createAttribute(builderFactory, "XSPA Subject", "urn:oasis:names:tc:xspa:1.0:subject:subject-id", fullName, "");
            attrStmt.getAttributes().add(attrPID);

            // XSPA Role
            final var structuralRole = createAttributeXSPARole(builderFactory, "XSPA Role", "urn:oasis:names:tc:xacml:2.0:subject:role", role);
            attrStmt.getAttributes().add(structuralRole);

            // XSPA Organization - Optional Field (eHDSI SAML Profile 2.2.0)
            if (StringUtils.isNotBlank(organization)) {
                final var attrPID3 = createAttribute(builderFactory, "XSPA Organization", "urn:oasis:names:tc:xspa:1.0:subject:organization",
                        organization, "");
                attrStmt.getAttributes().add(attrPID3);
            }

            // XSPA Organization ID - Optional Field (eHDSI SAML Profile 2.2.0)
            if (StringUtils.isNotBlank(organizationId)) {
                final var attrPID4 = createAttribute(builderFactory, "XSPA Organization ID", "urn:oasis:names:tc:xspa:1.0:subject:organization-id",
                        organizationId, "");
                attrStmt.getAttributes().add(attrPID4);
            }
            // // On behalf of
            if (StringUtils.isNotBlank(onBehalfId)) {
                final var attrPID41 = createAttribute(builderFactory, "OnBehalfOf", "urn:epsos:names:wp3.4:subject:on-behalf-of", onBehalfId,
                        role.getDisplayName());
                attrStmt.getAttributes().add(attrPID41);
                attrStmt.getAttributes().add(attrPID41);
            }

            // eHealth DSI Healthcare Facility Type
            // var attrPID5 = createAttribute(builderFactory, "eHealth DSI Healthcare Facility Type",
            final var attrPID5 = createAttribute(builderFactory, "eHealth DSI Healthcare Facility Type",
                    "urn:ehdsi:names:subject:healthcare-facility-type", facilityType, "");
            attrStmt.getAttributes().add(attrPID5);

            // XSPA Purpose of Use
            final var attrPID6 = createAttributePurposeOfUse(builderFactory, "XSPA Purpose Of Use","urn:oasis:names:tc:xspa:1.0:subject:purposeofuse", purposeOfUse);
            attrStmt.getAttributes().add(attrPID6);

            // XSPA Locality
            final var attrPID7 = createAttribute(builderFactory, "XSPA Locality", "urn:oasis:names:tc:xspa:1.0:environment:locality", locality, "");
            attrStmt.getAttributes().add(attrPID7);

            // HL7 Permissions
            final var attrPID8 = createAttribute("Hl7 Permissions", "urn:oasis:names:tc:xspa:1.0:subject:hl7:permission");
            for (final Object permission : permissions) {
                AddAttributeValue(builderFactory, attrPID8, permission.toString());
            }
            attrStmt.getAttributes().add(attrPID8);

            assertion.getStatements().add(attrStmt);
            signSAMLAssertion(keyStoreManager, assertion, "gazelle.ncp-signature.openncp.dg-sante.eu");
            LOGGER.info("AssertionId: '{}'", assertion.getID());

            final var marshaller = new AssertionMarshaller();
            final Element element = marshaller.marshall(assertion);
            assertion.setDOM(element);
            final Document document = element.getOwnerDocument();
            final String hcpa = XmlUtils.getDocumentAsXml(document, false);
            LOGGER.info("#### HCPA Start\n '{}' \n#### HCPA End", hcpa);
        } catch (final Exception e) {
            LOGGER.error(ExceptionUtils.getStackTrace(e));
        }
        return assertion;
    }

    public static Assertion createTRCAssertion(final AssertionService assertionService, final ConfigurationManager configurationManager, final Assertion clinicalAssertion, final PatientId patient, final String purposeOfUse) throws STSClientException, MarshallingException, MalformedURLException {
        final String patientId = patient.getExtension() + "^^^&" + patient.getRoot() + "&ISO";
        final URL trcServerUrl = new URL(configurationManager.getProperty("secman.sts.url"));
        final TrcAssertionRequest assertionRequest = ImmutableTrcAssertionRequest.builder().location(trcServerUrl).assertion(clinicalAssertion).checkForHostname(false).validationEnabled(false).purposeOfUse(purposeOfUse).patientId(patientId).build();
        final Assertion assertionTRC = assertionService.request(assertionRequest);
        final var marshaller = new AssertionMarshaller();
        final Element element = marshaller.marshall(assertionTRC);
        final Document document = element.getOwnerDocument();
        LOGGER.info("TRC Assertion: '{}'\n'{}'", assertionTRC.getID(), XmlUtils.getDocumentAsXml(document, false));
        return assertionTRC;
    }

    private static <T> T create(final Class<T> cls, final QName qname) {
        LOGGER.info("Creating class {}", cls.getName());
        return (T) (XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(qname)).buildObject(qname);
    }

    private static void signSAMLAssertion(KeyStoreManager keyStoreManager, final SignableSAMLObject signableSAMLObject, final String keyAlias) throws Exception {

        LOGGER.info("method signSAMLAssertion('{}')", keyAlias);

        final String signatureKeystorePath = Constants.NCP_SIG_KEYSTORE_PATH;
        final String signatureKeystorePassword = Constants.NCP_SIG_KEYSTORE_PASSWORD;
        final String truststorePath = Constants.TRUSTSTORE_PATH;
        final String truststorePassword = Constants.TRUSTSTORE_PASSWORD;
        final String signatureKeyAlias = Constants.NCP_SIG_PRIVATEKEY_ALIAS;
        final String signatureKeyPassword = Constants.NCP_SIG_PRIVATEKEY_PASSWORD;

        final KeyStoreManager keyManager = new DefaultKeyStoreManager(signatureKeystorePath,
                signatureKeystorePassword,
                truststorePath,
                truststorePassword,
                signatureKeyAlias,
                signatureKeyPassword);

        final X509Certificate signatureCertificate;
        PrivateKey privateKey = null;

        if (keyAlias == null) {
            signatureCertificate = (X509Certificate) keyManager.getDefaultCertificate();
        } else {
            final var keyStore = KeyStore.getInstance("JKS");
            final var file = new File(signatureKeystorePath);
            keyStore.load(new FileInputStream(file), signatureKeystorePassword.toCharArray());
            privateKey = (PrivateKey) keyStore.getKey(signatureKeyAlias, signatureKeyPassword.toCharArray());
            signatureCertificate = (X509Certificate) keyManager.getCertificate(keyAlias);
        }

        LOGGER.info("Keystore & Signature Certificate loaded: '{}'", signatureCertificate.getSerialNumber());

        final Signature sig = (Signature) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                .buildObject(Signature.DEFAULT_ELEMENT_NAME);
        final BasicX509Credential signingCredential = CredentialSupport.getSimpleCredential(signatureCertificate, privateKey);

        sig.setSigningCredential(signingCredential);
        sig.setSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        sig.setCanonicalizationAlgorithm("http://www.w3.org/2001/10/xml-exc-c14n#");

        final var keyInfo = (KeyInfo) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME)
                .buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        final X509Data data = (X509Data) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(X509Data.DEFAULT_ELEMENT_NAME)
                .buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        final var x509Certificate = (org.opensaml.xmlsec.signature.X509Certificate) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(
                        org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME)
                .buildObject(
                        org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME);

        final var value = Base64.encodeBase64String(signingCredential.getEntityCertificate().getEncoded());
        x509Certificate.setValue(value);
        data.getX509Certificates().add(x509Certificate);
        keyInfo.getX509Datas().add(data);
        sig.setKeyInfo(keyInfo);

        signableSAMLObject.setSignature(sig);
        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(signableSAMLObject).marshall(signableSAMLObject);

        try {
            Signer.signObject(sig);
        } catch (final SignatureException e) {
            throw new Exception(e);
        }
    }

    private static Attribute createAttribute(final String friendlyName, final String oasisName) {

        final Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(friendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);
        return attrPID;
    }

    private static Attribute AddAttributeValue(final XMLObjectBuilderFactory builderFactory, final Attribute attribute, final String value) {

        final XMLObjectBuilder stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
        final XSString attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrValPID.setValue(value);
        attribute.getAttributeValues().add(attrValPID);
        return attribute;
    }

    private static Attribute createAttribute(final XMLObjectBuilderFactory builderFactory, final String FriendlyName, final String oasisName,
                                             final String value, final String namespace) {

        final Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(FriendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);
        // Create and add the Attribute Value

        final XMLObjectBuilder stringBuilder;

        if (StringUtils.isBlank(namespace)) {
            final XSString attrValPID;
            stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
            attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            attrValPID.setValue(value);
            attrPID.getAttributeValues().add(attrValPID);
        } else {
            final XSURI attrValPID;
            stringBuilder = builderFactory.getBuilder(XSURI.TYPE_NAME);
            attrValPID = (XSURI) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSURI.TYPE_NAME);
            attrValPID.setValue(value);
            attrPID.getAttributeValues().add(attrValPID);
        }

        return attrPID;
    }

    private static Attribute createAttributeXSPARole(final XMLObjectBuilderFactory builderFactory, final String FriendlyName, final String oasisName,
                                                     final AssertionUtils.Concept conceptRole) {

        final Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(FriendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);
        // Create and add the Attribute Value

        final XMLObjectBuilder<XSAny> xsAnyBuilder = (XMLObjectBuilder<XSAny>) builderFactory.getBuilder(XSAny.TYPE_NAME);
        final XSAny role = xsAnyBuilder.buildObject("urn:hl7-org:v3", "Role", "");
        role.getUnknownAttributes().put(new QName("code"), conceptRole.getCode());
        role.getUnknownAttributes().put(new QName("codeSystem"), conceptRole.getCodeSystem());
        role.getUnknownAttributes().put(new QName("codeSystemName"), conceptRole.getCodeSystemName());
        role.getUnknownAttributes().put(new QName("displayName"), conceptRole.getDisplayName());
        final XSAny roleAttributeValue = xsAnyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        roleAttributeValue.getUnknownXMLObjects().add(role);
        attrPID.getAttributeValues().add(roleAttributeValue);
        return attrPID;
    }

    private static Attribute createAttributePurposeOfUse(final XMLObjectBuilderFactory builderFactory, final String FriendlyName,
                                                         final String oasisName, final String value) {

        final Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(FriendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);
        // Create and add the Attribute Value

        final XMLObjectBuilder<XSAny> xsAnyBuilder = (XMLObjectBuilder<XSAny>) builderFactory.getBuilder(XSAny.TYPE_NAME);
        final XSAny pou = xsAnyBuilder.buildObject("urn:hl7-org:v3", "PurposeOfUse", "");
        pou.getUnknownAttributes().put(new QName("code"), value);
        pou.getUnknownAttributes().put(new QName("codeSystem"), "3bc18518-d305-46c2-a8d6-94bd59856e9e");
        pou.getUnknownAttributes().put(new QName("codeSystemName"), "eHDSI XSPA PurposeOfUse");
        pou.getUnknownAttributes().put(new QName("displayName"), value);
        final XSAny pouAttributeValue = xsAnyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        pouAttributeValue.getUnknownXMLObjects().add(pou);
        attrPID.getAttributeValues().add(pouAttributeValue);
        return attrPID;
    }

    public static class Concept {

        private String code;

        private String displayName;
        private String codeSystem;

        private String codeSystemName;

        public Concept() {
        }

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(final String displayName) {
            this.displayName = displayName;
        }

        public String getCodeSystem() {
            return codeSystem;
        }

        public void setCodeSystemId(final String codeSystem) {
            this.codeSystem = codeSystem;
        }

        public String getCodeSystemName() {
            return codeSystemName;
        }

        public void setCodeSystemName(final String codeSystemName) {
            this.codeSystemName = codeSystemName;
        }
    }
}
