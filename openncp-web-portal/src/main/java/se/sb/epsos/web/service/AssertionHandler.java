package se.sb.epsos.web.service;

import epsos.ccd.gnomon.auditmanager.*;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditService;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditServiceFactory;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManager;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.util.security.CryptographicConstant;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.XSURI;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sb.epsos.web.auth.AuthenticatedUser;
import se.sb.epsos.web.pages.KeyStoreManager;
import se.sb.epsos.web.pages.KeyStoreManagerImpl;
import se.sb.epsos.web.util.CdaHelper.Validator;
import sun.security.x509.X500Name;
import tr.com.srdc.epsos.util.Constants;
import tr.com.srdc.epsos.util.http.IPUtil;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

import static org.opensaml.saml.saml2.core.NameIDType.UNSPECIFIED;

public class AssertionHandler implements Serializable {

    private static final long serialVersionUID = 5209063407337843010L;
    private static final DatatypeFactory DATATYPE_FACTORY;
    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionHandler.class);

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalArgumentException();
        }
    }

    private Assertion assertion;

    public AssertionHandler() {
    }

    public Assertion createSAMLAssertion(AuthenticatedUser userDetails) throws InitializationException {

        LOGGER.info("[OpenNCP Web Portal] HCP Assertion Creation");
        InitializationService.initialize();
        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

        @SuppressWarnings("unchecked")
        SAMLObjectBuilder<Assertion> nameIdBuilder = (SAMLObjectBuilder<Assertion>) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
        NameID nameId = (NameID) nameIdBuilder.buildObject();
        nameId.setValue(userDetails.getUsername());
        nameId.setFormat(UNSPECIFIED);

        assertion = create(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);

        DateTime now = new DateTime();
        DateTime nowUTC = now.withZone(DateTimeZone.UTC).toDateTime();

        String assertionId = "_" + UUID.randomUUID();
        assertion.setID(assertionId);
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssueInstant(nowUTC.toDateTime());

        Subject subject = create(Subject.class, Subject.DEFAULT_ELEMENT_NAME);
        assertion.setSubject(subject);
        subject.setNameID(nameId);

        SubjectConfirmation subjectConf = create(SubjectConfirmation.class, SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        subjectConf.setMethod(SubjectConfirmation.METHOD_SENDER_VOUCHES);
        assertion.getSubject().getSubjectConfirmations().add(subjectConf);

        Conditions conditions = create(Conditions.class, Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(nowUTC.toDateTime());
        conditions.setNotOnOrAfter(nowUTC.toDateTime().plusHours(4));
        assertion.setConditions(conditions);

        String countryCode = ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_CODE");
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue("urn:idp:" + countryCode + ":countryB");
        issuer.setNameQualifier("urn:epsos:wp34:assertions");
        assertion.setIssuer(issuer);

        AuthnStatement authnStatement = create(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(nowUTC.toDateTime());
        assertion.getAuthnStatements().add(authnStatement);

        AuthnContext authnContext = create(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
        AuthnContextClassRef authnContextClassRef = create(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        authnContextClassRef.setAuthnContextClassRef(AuthnContext.X509_AUTHN_CTX);
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnStatement.setAuthnContext(authnContext);

        AttributeStatement attributeStatement = create(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);

        Attribute attrPID = createAttribute(builderFactory, "XSPA subject",
                "urn:oasis:names:tc:xacml:1.0:subject:subject-id", userDetails.getCommonName(), "", "");
        attributeStatement.getAttributes().add(attrPID);

        //TODO fix multiple roles??
        String role = AssertionHandlerConfigManager.getRoleDisplayName(userDetails.getRoles().get(0));
        Attribute attrPID_1 = createAttribute(builderFactory, "XSPA role", "urn:oasis:names:tc:xacml:2.0:subject:role", role, "", "");
        attributeStatement.getAttributes().add(attrPID_1);

        Attribute attrPID_3 = createAttribute(builderFactory, "XSPA Organization",
                "urn:oasis:names:tc:xspa:1.0:subject:organization", userDetails.getOrganizationName(), "", "");
        attributeStatement.getAttributes().add(attrPID_3);

        Attribute attrPID_4 = createAttribute(builderFactory, "XSPA Organization ID",
                "urn:oasis:names:tc:xspa:1.0:subject:organization-id", Constants.OID_PREFIX + userDetails.getOrganizationId(), "AA", "");
        attributeStatement.getAttributes().add(attrPID_4);

        Attribute attrPID_5 = createAttribute(builderFactory, "epSOS Healthcare Facility Type",
                "urn:epsos:names:wp3.4:subject:healthcare-facility-type", AssertionHandlerConfigManager.getFacilityType(userDetails.getRoles().get(0)), "", "");
        attributeStatement.getAttributes().add(attrPID_5);

        Attribute attrPID_6 = createAttribute(builderFactory, "XSPA Purpose Of Use",
                "urn:oasis:names:tc:xspa:1.0:subject:purposeofuse", AssertionHandlerConfigManager.getPurposeOfUse(), "", "");
        attributeStatement.getAttributes().add(attrPID_6);

        Attribute attrPID_7 = createAttribute(builderFactory, "XSPA Locality",
                "urn:oasis:names:tc:xspa:1.0:environment:locality", userDetails.getOrganizationName(), "", "");
        attributeStatement.getAttributes().add(attrPID_7);

        Attribute attrPID_8 = createAttribute(builderFactory, "Hl7 Permissions",
                "urn:oasis:names:tc:xspa:1.0:subject:hl7:permission");
        Set<String> permissions = new HashSet<>();
        for (String r : userDetails.getRoles()) {
            permissions.addAll(AssertionHandlerConfigManager.getPermissions(r));
        }

        String permissionPrefix = AssertionHandlerConfigManager.getPermissionsPrefix();
        for (String permission : permissions) {
            AddAttributeValue(builderFactory, attrPID_8, permissionPrefix + permission, "", "");
        }

        attributeStatement.getAttributes().add(attrPID_8);

        assertion.getStatements().add(attributeStatement);

        try {
            sendHPAuthenticationAudit(userDetails, assertion);
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }

        return assertion;
    }

    public boolean isExpired(Assertion assertion) {

        if (assertion.getConditions().getNotBefore() != null && assertion.getConditions().getNotBefore().isAfterNow()) {
            return true;
        }
        return assertion.getConditions().getNotOnOrAfter() != null
                && (assertion.getConditions().getNotOnOrAfter().isBeforeNow() || assertion.getConditions().getNotOnOrAfter().isEqualNow());
    }

    protected AuditService getAuditService() {
        return AuditServiceFactory.getInstance();
    }

    protected String getPrivateKeyAlias() {
        return NcpServiceConfigManager.getPrivateKeyAlias("assertion");
    }

    protected String getPrivateKeystoreLocation() {
        return NcpServiceConfigManager.getPrivateKeystoreLocation("assertion");
    }

    protected String getPrivateKeyPassword() {
        return NcpServiceConfigManager.getPrivateKeyPassword("assertion");
    }

    protected ConfigurationManager getConfigurationManager() {
        return ConfigurationManagerFactory.getConfigurationManager();
    }

    public void sendHPAuthenticationAudit(AuthenticatedUser userDetails, Assertion assertion) {

        String serviceUserId = getCertificateTlsCommonName();
        String securityHeader = "[No security header provided]";
        String requestParticipantObjectID = Constants.UUID_PREFIX + assertion.getID();
        String responseParticipantObjectID = Constants.UUID_PREFIX + assertion.getID();

        String sourceIP = IPUtil.getPrivateServerIp();
        String pointOfCareUserId = userDetails.getOrganizationName();
        String pointOfCareRoleId = "Other";
        String userIdAlias = assertion.getSubject().getNameID().getSPProvidedID();
        String humanRequesterUserId = StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "" + "<" + assertion.getSubject().getNameID().getValue()
                + "@" + assertion.getIssuer().getValue() + ">";
        String humanRequesterRoleId = AssertionHandlerConfigManager.getRoleDisplayName(userDetails.getRoles().get(0));
        String humanRequesterAlternativeUserId = userDetails.getCommonName();
        String auditServiceSourceId = ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_PRINCIPAL_SUBDIVISION");
        String eventTargetObjectId = Constants.UUID_PREFIX + assertion.getID();

        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        XMLGregorianCalendar eventLogDateTime = DATATYPE_FACTORY.newXMLGregorianCalendar(c);

        // Preparing HCP assertions creation EventLog.
        EventLog eventLog = EventLog.createEventLogHCPIdentity(TransactionName.epsosHcpAuthentication, EventActionCode.EXECUTE,
                eventLogDateTime, EventOutcomeIndicator.FULL_SUCCESS, pointOfCareUserId, pointOfCareRoleId, humanRequesterUserId,
                humanRequesterRoleId, humanRequesterAlternativeUserId, serviceUserId, serviceUserId, auditServiceSourceId,
                eventTargetObjectId, requestParticipantObjectID, securityHeader.getBytes(StandardCharsets.UTF_8),
                responseParticipantObjectID, securityHeader.getBytes(StandardCharsets.UTF_8), sourceIP, sourceIP, NcpSide.NCP_B);
        eventLog.setEventType(EventType.epsosHcpAuthentication);

        // Sending audit message to the ATNA repository.
        AuditServiceFactory.getInstance().write(eventLog, "13", "2");
    }

    private String getCertificateTlsCommonName() {

        String certTlsAlias = getTlsCertificateAlias();
        LOGGER.debug("KEY_ALIAS: '{}'", certTlsAlias);
        String keystoreTlsLocation = getTlsKeystoreLocation();
        LOGGER.debug("KEYSTORE_LOCATION: '{}'", keystoreTlsLocation);
        String keystoreTlsPassword = getTlsCertificatePassword();
        LOGGER.debug("KEY_STORE_PASS: '{}'", StringUtils.isNotBlank(keystoreTlsPassword) ? "******" : "N/A");

        if (Validator.isNull(certTlsAlias)) {
            LOGGER.error("Problem reading configuration parameters");
            return "TLS CN not available";
        }
        java.security.cert.Certificate certificate;
        try (FileInputStream is = new FileInputStream(keystoreTlsLocation)) {

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(is, keystoreTlsPassword == null ? null : keystoreTlsPassword.toCharArray());
            // Get certificate
            certificate = keystore.getCertificate(certTlsAlias);
            if (certificate instanceof X509Certificate) {

                // Get subject
                X509Certificate x509Certificate = (X509Certificate) certificate;
                return ((X500Name) x509Certificate.getSubjectDN()).getCommonName();
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | java.io.IOException | java.security.cert.CertificateException e) {
            LOGGER.error("{}: '{}'", e.getClass(), e.getMessage(), e);
        }
        return "TLS CN not available";
    }

    private String getTlsCertificateAlias() {
        return NcpServiceConfigManager.getPrivateKeyAlias("tls");
    }

    private String getTlsKeystoreLocation() {
        return NcpServiceConfigManager.getPrivateKeystoreLocation("tls");
    }

    private String getTlsCertificatePassword() {
        return NcpServiceConfigManager.getPrivateKeyPassword("tls");
    }

    public Attribute createAttribute(XMLObjectBuilderFactory builderFactory, String FriendlyName, String oasisName) {
        Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(FriendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);
        return attrPID;
    }

    private Attribute AddAttributeValue(XMLObjectBuilderFactory builderFactory, Attribute attribute, String value,
                                        String namespace, String xmlschema) {

        XMLObjectBuilder<Assertion> stringBuilder = (XMLObjectBuilder<Assertion>) builderFactory.getBuilder(XSString.TYPE_NAME);
        XSString attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
        attrValPID.setValue(value);
        attribute.getAttributeValues().add(attrValPID);
        return attribute;
    }

    private Attribute createAttribute(XMLObjectBuilderFactory builderFactory, String FriendlyName, String oasisName,
                                      String value, String namespace, String xmlschema) {

        Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(FriendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);

        XMLObjectBuilder<Assertion> stringBuilder;

        if (StringUtils.isBlank(namespace)) {
            XSString attrValPID;
            stringBuilder = (XMLObjectBuilder<Assertion>) builderFactory.getBuilder(XSString.TYPE_NAME);
            attrValPID = (XSString) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            attrValPID.setValue(value);
            attrPID.getAttributeValues().add(attrValPID);
        } else {
            XSURI attrValPID;
            stringBuilder = (XMLObjectBuilder<Assertion>) builderFactory.getBuilder(XSURI.TYPE_NAME);
            attrValPID = (XSURI) stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSURI.TYPE_NAME);
            attrValPID.setValue(value);
            attrPID.getAttributeValues().add(attrValPID);
        }

        return attrPID;
    }

    public <T> T create(Class<T> cls, QName qname) {
        return (T) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(qname).buildObject(qname);
    }

    /**
     * @param assertion
     * @throws Exception
     */
    public void signSAMLAssertion(SignableSAMLObject assertion) throws Exception {

        LOGGER.info("[OpenNCP Web Portal] Assertion Signature");
        KeyStoreManager keyManager = new KeyStoreManagerImpl();
        X509Certificate certificate = keyManager.getCertificate();
        KeyPair privateKeyPair = keyManager.getPrivateKey();

        PrivateKey privateKey = privateKeyPair.getPrivate();

        Signature signature = (Signature) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(Signature.DEFAULT_ELEMENT_NAME).buildObject(Signature.DEFAULT_ELEMENT_NAME);
        BasicX509Credential signingCredential = CredentialSupport.getSimpleCredential(certificate, privateKey);
        signature.setSigningCredential(signingCredential);
        signature.setSignatureAlgorithm(CryptographicConstant.ALGO_ID_SIGNATURE_RSA_SHA256);
        signature.setCanonicalizationAlgorithm(CryptographicConstant.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        KeyInfo keyInfo = (KeyInfo) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME).buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data data = (X509Data) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(X509Data.DEFAULT_ELEMENT_NAME).buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        org.opensaml.xmlsec.signature.X509Certificate x509Certificate = (org.opensaml.xmlsec.signature.X509Certificate) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME)
                .buildObject(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME);

        String value = org.apache.xml.security.utils.Base64.encode(signingCredential.getEntityCertificate().getEncoded());
        x509Certificate.setValue(value);
        data.getX509Certificates().add(x509Certificate);
        keyInfo.getX509Datas().add(data);
        signature.setKeyInfo(keyInfo);

        assertion.setSignature(signature);
        XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
        Signer.signObject(signature);
    }

    public Assertion getAssertion() {
        return assertion;
    }

    public String getOrganizationId() {
        return getAttributeValue("urn:oasis:names:tc:xspa:1.0:subject:organization-id");
    }

    public String getFacilityType() {
        return getAttributeValue("urn:epsos:names:wp3.4:subject:healthcare-facility-type");
    }

    private String getAttributeValue(String key) {

        List<AttributeStatement> attrStatements = assertion.getAttributeStatements();
        for (AttributeStatement stat : attrStatements) {
            List<Attribute> attributes = stat.getAttributes();
            for (Attribute attr : attributes) {
                if (attr.getName().equals(key)) {
                    return attr.getAttributeValues().get(0).getDOM().getTextContent();
                }
            }
        }
        return null;
    }
}
