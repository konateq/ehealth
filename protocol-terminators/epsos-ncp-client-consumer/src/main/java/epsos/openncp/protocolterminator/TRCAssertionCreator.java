package epsos.openncp.protocolterminator;

import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.PurposeOfUse;
import eu.europa.ec.sante.ehdsi.openncp.assertionvalidator.saml.SAML;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManagerFactory;
import eu.europa.ec.sante.ehdsi.openncp.util.security.CryptographicConstant;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tr.com.srdc.epsos.util.Constants;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TRCAssertionCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TRCAssertionCreator.class);

    public Assertion createTRCAssertion() {

        XMLSignatureFactory factory;
        KeyStore keyStore;
        KeyPair keyPair;
        KeyInfo keyInfo;

        var saml = new SAML();
        var subject = saml.createSubject("physician the",
                "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified", "sender-vouches");

        // Create assertion
        var assertion = saml.createAssertion(subject);
        String countryCode = ConfigurationManagerFactory.getConfigurationManager().getProperty("COUNTRY_CODE");
        var issuer = saml.create(Issuer.class, Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue("urn:initgw:" + countryCode + ":countryB");
        issuer.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
        issuer.setNameQualifier("urn:ehdsi:assertions:trc");
        assertion.setIssuer(issuer);

        // Set version
        SAMLVersion version = SAMLVersion.VERSION_20;
        assertion.setVersion(version);

        // Set AuthnStatement
        var issueInstant = Instant.now();
        var authnStatement = saml.create(AuthnStatement.class, AuthnStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(issueInstant);
        authnStatement.setSessionNotOnOrAfter(issueInstant.plus(Duration.ofHours(2)));

        // Set AuthnStatement/AuthnContext
        var authnContext = saml.create(AuthnContext.class, AuthnContext.DEFAULT_ELEMENT_NAME);
        var authnContextClassRef = saml.create(AuthnContextClassRef.class, AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        authnContextClassRef.setURI("urn:oasis:names:tc:SAML:2.0:ac:classes:PreviousSession");
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnStatement.setAuthnContext(authnContext);
        assertion.getAuthnStatements().add(authnStatement);

        // Set Advice
        var advice = saml.create(Advice.class, Advice.DEFAULT_ELEMENT_NAME);
        var assertionIDRef = saml.create(AssertionIDRef.class, AssertionIDRef.DEFAULT_ELEMENT_NAME);
        assertionIDRef.setValue(assertion.getID());
        advice.getAssertionIDReferences().add(assertionIDRef);
        assertion.setAdvice(advice);

        // Create AttributeStatement
        var attributeStatement = saml.create(AttributeStatement.class, AttributeStatement.DEFAULT_ELEMENT_NAME);

        // Namespaces
        var ns1 = new Namespace(XMLConstants.W3C_XML_SCHEMA_NS_URI, "xs");
        var ns2 = new Namespace(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");

        // Set patient Identifier
        if (true) {
            Attribute att = saml.create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
            att.setFriendlyName("Patient ID");
            att.setName("urn:oasis:names:tc:xacml:1.0:resource:resource-id");
            att.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:uri");

            XMLObjectBuilder<?> builder = XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSAny.TYPE_NAME);
            XSAny attVal = (XSAny) builder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
            attVal.setTextContent("6212122451^^^&2.16.17.710.815.1000.990.1&ISO");

            attVal.getNamespaceManager().registerNamespaceDeclaration(ns1);
            attVal.getNamespaceManager().registerNamespaceDeclaration(ns2);
            QName attributeName = new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type", "xsi");
            attVal.getUnknownAttributes().put(attributeName, "xs:string");

            att.getAttributeValues().add(attVal);
            attributeStatement.getAttributes().add(att);
        }

        // Set Purpose of Use
        if (true) {
            Attribute att = saml.create(Attribute.class, Attribute.DEFAULT_ELEMENT_NAME);
            att.setFriendlyName("XSPA Purpose of Use");
            att.setName("urn:oasis:names:tc:xspa:1.0:subject:purposeofuse");
            att.setNameFormat("urn:oasis:names:tc:SAML:2.0:attrname-format:uri");

            XMLObjectBuilder<?> builder = XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(XSAny.TYPE_NAME);

            XSAny attVal = (XSAny) builder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
            attVal.setTextContent(PurposeOfUse.TREATMENT.toString());

            attVal.getNamespaceManager().registerNamespaceDeclaration(ns1);
            attVal.getNamespaceManager().registerNamespaceDeclaration(ns2);
            QName attributeName = new QName(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type", "xsi");
            attVal.getUnknownAttributes().put(attributeName, "xs:string");

            att.getAttributeValues().add(attVal);
            attributeStatement.getAttributes().add(att);
        }

        // Set AttributeStatement
        assertion.getAttributeStatements().add(attributeStatement);

        // Set Signature
        try {
            // Set assertion.DOM
            var marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(assertion);
            Element element = marshaller.marshall(assertion);
            assertion.setDOM(element);

            // Set factory
            String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
            factory = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).getDeclaredConstructor().newInstance());

            // Set keyStore
            try (var is = new FileInputStream(Constants.SC_KEYSTORE_PATH)) {
                keyStore = KeyStore.getInstance("JKS");
                keyStore.load(is, Constants.SC_KEYSTORE_PASSWORD.toCharArray());
            }

            var passwordProtection = new PasswordProtection(Constants.SC_PRIVATEKEY_PASSWORD.toCharArray());
            PrivateKeyEntry entry = (PrivateKeyEntry) keyStore.getEntry(Constants.SC_PRIVATEKEY_ALIAS, passwordProtection);

            // Set keyPair
            keyPair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());

            // Set keyInfo
            var keyInfoFactory = factory.getKeyInfoFactory();
            keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(keyInfoFactory.newX509Data
                    (Collections.singletonList(entry.getCertificate()))));

            // Create Signature/SignedInfo/Reference
            List<Transform> lst = new ArrayList<>();
            lst.add(factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
            lst.add(factory.newTransform(CryptographicConstant.ALGO_ID_C14N_EXCL_OMIT_COMMENTS, (TransformParameterSpec) null));
            var reference = factory.newReference("#" + assertion.getID(),
                    factory.newDigestMethod(CryptographicConstant.ALGO_ID_DIGEST_SHA256, null),
                    lst, null, null);

            // Set Signature/SignedInfo
            var signedInfo = factory.newSignedInfo(factory.newCanonicalizationMethod
                    (CryptographicConstant.ALGO_ID_C14N_EXCL_WITH_COMMENTS,
                            (C14NMethodParameterSpec) null), factory.newSignatureMethod
                    (CryptographicConstant.ALGO_ID_SIGNATURE_RSA_SHA256, null), Collections.singletonList(reference));

            // Sign Assertion
            var xmlSignature = factory.newXMLSignature(signedInfo, keyInfo);
            var domSignContext = new DOMSignContext(keyPair.getPrivate(), assertion.getDOM());
            xmlSignature.sign(domSignContext);
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }

        // Set Signature's place
        Node signatureElement = assertion.getDOM().getLastChild();

        var foundIssuer = false;
        Node elementAfterIssuer = null;
        NodeList children = assertion.getDOM().getChildNodes();
        for (var c = 0; c < children.getLength(); ++c) {
            org.w3c.dom.Node child = children.item(c);

            if (foundIssuer) {
                elementAfterIssuer = child;
                break;
            }

            if (child.getNodeType() == Node.ELEMENT_NODE && StringUtils.equals(child.getLocalName(), "Issuer"))
                foundIssuer = true;
        }

        // Place after the Issuer, or as first element if no Issuer:
        if (!foundIssuer || elementAfterIssuer != null) {
            assertion.getDOM().removeChild(signatureElement);
            assertion.getDOM().insertBefore(signatureElement,
                    foundIssuer
                            ? elementAfterIssuer
                            : assertion.getDOM().getFirstChild());
        }

        return assertion;
    }
}
