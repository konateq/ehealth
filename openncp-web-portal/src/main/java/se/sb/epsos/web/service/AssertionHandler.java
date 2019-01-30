package se.sb.epsos.web.service;

import epsos.ccd.gnomon.auditmanager.*;
import eu.epsos.validation.datamodel.common.NcpSide;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditServiceFactory;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManager;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.util.security.CryptographicConstant;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.*;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.XSURI;
import org.opensaml.xml.security.SecurityConfiguration;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sb.epsos.web.auth.AuthenticatedUser;
import se.sb.epsos.web.pages.KeyStoreInitializationException;
import se.sb.epsos.web.pages.KeyStoreManager;
import se.sb.epsos.web.pages.KeyStoreManagerImpl;
import se.sb.epsos.web.util.CdaHelper.Validator;
import tr.com.srdc.epsos.util.Constants;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.FileInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

public class AssertionHandler implements Serializable {

    private static final long serialVersionUID = 5209063407337843010L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionHandler.class);
    private AssertionHandlerConfigManager configHandler;
    private Assertion assertion;

    public AssertionHandler(AssertionHandlerConfigManager config) {
        this.configHandler = config;
    }

    AssertionHandler() {
        this(new AssertionHandlerConfigManager());
    }

    public static void main(String[] args) {
        DateTime now = new DateTime();
        DateTime nowUTC = now.withZone(DateTimeZone.UTC).toDateTime();
        LOGGER.debug("NotBefore: '{}'", nowUTC.toDateTime().minusMinutes(1));
        LOGGER.debug("NotOnOrAfter: '{}'", nowUTC.toDateTime().plusHours(2));
    }

    Assertion createSAMLAssertion(AuthenticatedUser userDetails) throws ConfigurationException {

        LOGGER.debug("################################################");
        LOGGER.debug("# createSAMLAssertion() - start                #");
        LOGGER.debug("################################################");
        DefaultBootstrap.bootstrap();
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

        @SuppressWarnings("unchecked")
        SAMLObjectBuilder<Assertion> nameIdBuilder = (SAMLObjectBuilder<Assertion>) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
        NameID nameId = (NameID) nameIdBuilder.buildObject();
        nameId.setValue(userDetails.getUsername());
        nameId.setFormat(NameID.UNSPECIFIED);

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
            permissions.addAll(AssertionHandlerConfigManager.getPersmissions(r));
        }

        String permissionPrefix = AssertionHandlerConfigManager.getPersmissionsPrefix();
        for (String permission : permissions) {
            AddAttributeValue(builderFactory, attrPID_8, permissionPrefix + permission, "", "");
        }

        attributeStatement.getAttributes().add(attrPID_8);

        assertion.getStatements().add(attributeStatement);

        LOGGER.debug("# createSAMLAssertion() - stop ");

        try {
            sendAuditEpsos91(userDetails, assertion);
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }

        return assertion;
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

    void sendAuditEpsos91(AuthenticatedUser userDetails, Assertion assertion) {

        String KEY_ALIAS = getPrivateKeyAlias();
        LOGGER.debug("KEY_ALIAS: '{}'", KEY_ALIAS);
        String KEYSTORE_LOCATION = getPrivateKeystoreLocation();
        LOGGER.debug("KEYSTORE_LOCATION: '{}'", KEYSTORE_LOCATION);
        String KEY_STORE_PASS = getPrivateKeyPassword();
        LOGGER.debug("KEY_STORE_PASS: '{}'", StringUtils.isNotBlank(KEY_STORE_PASS) ? "******" : "N/A");

        final ConfigurationManager configurationManager = getConfigurationManager();

        if (Validator.isNull(KEY_ALIAS)) {
            LOGGER.error("Problem reading configuration parameters");
            return;
        }
        java.security.cert.Certificate cert;
        String name = "";
        try (FileInputStream is = new FileInputStream(KEYSTORE_LOCATION)) {

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(is, KEY_STORE_PASS == null ? null : KEY_STORE_PASS.toCharArray());
            // Get certificate
            cert = keystore.getCertificate(KEY_ALIAS);

            // List the aliases
            Enumeration<String> enum1 = keystore.aliases();
            while (enum1.hasMoreElements()) {
                String alias = enum1.nextElement();

                if (cert instanceof X509Certificate) {
                    X509Certificate x509cert = (X509Certificate) cert;

                    // Get subject
                    Principal principal = x509cert.getSubjectDN();
                    name = principal.getName();

                    // Get issuer
                    principal = x509cert.getIssuerDN();
                    String issuerDn = principal.getName();
                }
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | java.io.IOException | java.security.cert.CertificateException e) {
            LOGGER.error("{}: '{}'", e.getClass(), e.getMessage(), e);
        }

        String secHead = "[No security header provided]";
        String reqm_participantObjectID = Constants.UUID_PREFIX + assertion.getID();
        String resm_participantObjectID = Constants.UUID_PREFIX + assertion.getID();

        InetAddress sourceIP;
        String sourceHost;
        try {
            sourceIP = InetAddress.getLocalHost();
            sourceHost = sourceIP.getHostAddress();
        } catch (UnknownHostException ex) {
            LOGGER.error("UnknownHostException: '{}'", ex.getMessage(), ex);
            sourceHost = "UnknownHost";
        }

        String email = userDetails.getUserId() + "@" + configurationManager.getProperty("ncp.country");

        String PC_UserID = userDetails.getOrganizationName();
        String PC_RoleID = "Other";
        String userIdAlias = assertion.getSubject().getNameID().getSPProvidedID();
        String HR_UserID = StringUtils.isNotBlank(userIdAlias) ? userIdAlias : "" + "<" + assertion.getSubject().getNameID().getValue()
                + "@" + assertion.getIssuer().getValue() + ">";
        String HR_RoleID = AssertionHandlerConfigManager.getRoleDisplayName(userDetails.getRoles().get(0));
        String HR_AlternativeUserID = userDetails.getCommonName();
        String SC_UserID = name;
        String SP_UserID = name;

        String AS_AuditSourceId = configurationManager.getProperty("COUNTRY_PRINCIPAL_SUBDIVISION");
        String ET_ObjectID = Constants.UUID_PREFIX + assertion.getID();

        AuditService asd = getAuditService();
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date());
        XMLGregorianCalendar eventLogDateTime = null;
        try {
            eventLogDateTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException ex) {
            LOGGER.error("DatatypeConfigurationException: '{}'", ex.getMessage(), ex);
        }

        EventLog eventLog = EventLog.createEventLogHCPIdentity(TransactionName.epsosHcpAuthentication, EventActionCode.EXECUTE,
                eventLogDateTime, EventOutcomeIndicator.FULL_SUCCESS, PC_UserID, PC_RoleID, HR_UserID, HR_RoleID, HR_AlternativeUserID,
                SC_UserID, SP_UserID, AS_AuditSourceId, ET_ObjectID, reqm_participantObjectID,
                secHead.getBytes(StandardCharsets.UTF_8), resm_participantObjectID, secHead.getBytes(StandardCharsets.UTF_8),
                sourceHost, sourceHost, NcpSide.NCP_B);
        eventLog.setEventType(EventType.epsosHcpAuthentication);
        asd.write(eventLog, "13", "2");
        LOGGER.debug("################################################");
        LOGGER.debug("# sendAuditEpsos91 - stop                      #");
        LOGGER.debug("################################################");
    }

    public Attribute createAttribute(XMLObjectBuilderFactory builderFactory, String FriendlyName, String oasisName) {
        Attribute attrPID = create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
        attrPID.setFriendlyName(FriendlyName);
        attrPID.setName(oasisName);
        attrPID.setNameFormat(Attribute.URI_REFERENCE);
        return attrPID;
    }

    private Attribute AddAttributeValue(XMLObjectBuilderFactory builderFactory, Attribute attribute, String value, String namespace, String xmlschema) {
        @SuppressWarnings("unchecked")
        XMLObjectBuilder<Assertion> stringBuilder = builderFactory.getBuilder(XSString.TYPE_NAME);
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

    public <T> T create(Class<T> cls, QName qname) {
        return (T) Configuration.getBuilderFactory().getBuilder(qname).buildObject(qname);
    }

    public void signSAMLAssertion(SignableSAMLObject assertion) throws KeyStoreInitializationException, KeyStoreException,
            UnrecoverableKeyException, NoSuchAlgorithmException, SecurityException, MarshallingException, SignatureException {

        LOGGER.debug("################################################");
        LOGGER.debug("# signSAMLAssertion() - start                  #");
        LOGGER.debug("################################################");
        KeyStoreManager keyManager = new KeyStoreManagerImpl();
        X509Certificate certificate = keyManager.getCertificate();
        KeyPair privateKeyPair = keyManager.getPrivateKey();

        PrivateKey privateKey = privateKeyPair.getPrivate();

        Signature signature = (Signature) Configuration.getBuilderFactory().getBuilder(Signature.DEFAULT_ELEMENT_NAME)
                .buildObject(Signature.DEFAULT_ELEMENT_NAME);
        Credential signingCredential = SecurityHelper.getSimpleCredential(certificate, privateKey);
        signature.setSigningCredential(signingCredential);
        signature.setSignatureAlgorithm(CryptographicConstant.ALGO_ID_SIGNATURE_RSA_SHA256);
        signature.setCanonicalizationAlgorithm(CryptographicConstant.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        SecurityConfiguration securityConfiguration = Configuration.getGlobalSecurityConfiguration();
        SecurityHelper.prepareSignatureParams(signature, signingCredential, securityConfiguration, null);

        assertion.setSignature(signature);
        Configuration.getMarshallerFactory().getMarshaller(assertion).marshall(assertion);
        Signer.signObject(signature);
        LOGGER.debug("# signSAMLAssertion() - stop ");
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
