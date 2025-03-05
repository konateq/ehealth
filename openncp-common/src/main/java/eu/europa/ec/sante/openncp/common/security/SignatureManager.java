package eu.europa.ec.sante.openncp.common.security;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.common.validation.util.security.CryptographicConstant;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.xmlsec.signature.support.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * The NCP Signature Manager is a JAVA library for applying and verifying detached digital signatures on XML documents
 * and for applying and verifying enveloped signatures on SAML assertions and Audit Trail Messages
 *
 * @author Jerry Dimitriou <jerouris at netsmart.gr>
 */
@Component
public class SignatureManager {

    private final Logger logger = LoggerFactory.getLogger(SignatureManager.class);
    private final KeyStoreManager keyManager;
    private String signatureAlgorithm;
    private String digestAlgorithm;

    public SignatureManager(final KeyStoreManager keyStoreManager) {
        this.keyManager = Validate.notNull(keyStoreManager);
        init();
    }

    private void init() {

        signatureAlgorithm = CryptographicConstant.ALGO_ID_SIGNATURE_RSA_SHA256;
        digestAlgorithm = CryptographicConstant.ALGO_ID_DIGEST_SHA256;
    }

    /**
     * Verifies the enveloped SAML signature and checks that the Assertion is signed against that signature.
     * This method returns nothing when the signature is valid.
     * When the signature is not valid though, it throws an SMgrException with the Error Message that caused
     * the signature to fail validation.
     *
     * @param assertion The SAML Assertion that will be validated by the method.
     * @throws SMgrException When the validation of the signature fails
     */
    public void verifySAMLAssertion(final Assertion assertion) throws SMgrException {
        try {
            final var profileValidator = new SAMLSignatureProfileValidator();
            final var assertionSignature = assertion.getSignature();
            try {
                profileValidator.validate(assertionSignature);
            } catch (final SignatureException e) {
                // Indicates signature did not conform to SAML Signature profile
                throw new SMgrException("SAML Signature Profile Validation: " + e.getMessage());
            }

            final X509Certificate cert;
            final List<X509Certificate> certificates = KeyInfoSupport.getCertificates(assertionSignature.getKeyInfo());
            for (final X509Certificate certificate : certificates) {
                logger.debug("Certificate: '{}'", certificate.getIssuerX500Principal().getName());
            }
            if (certificates.size() == 1) {
                cert = certificates.get(0);
            } else {
                throw new SMgrException("More than one certificate found in KeyInfo");
            }

            final var basicX509Credential = new BasicX509Credential(cert);

            try {
                SignatureValidator.validate(assertionSignature, basicX509Credential);
            } catch (final SignatureException e) {
                // Indicates signature was not cryptographically valid, or possibly a processing error
                throw new SMgrException("Signature Validation: " + e.getMessage());
            }
            final var certificateValidator = new CertificateValidator(keyManager.getTrustStore());
            certificateValidator.validateCertificate(cert);

            final TrustManagerFactory trustManagerFactory;
            try {
                trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            } catch (final NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not create the trust manager factory", e);
            }

            try {
                trustManagerFactory.init(keyManager.getTrustStore());
            } catch (final KeyStoreException e) {
                throw new RuntimeException("Could not initialize the truststore", e);
            }

            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            X509TrustManager x509TrustManager = (X509TrustManager) trustManagers[0];

            x509TrustManager.checkClientTrusted(certificates.toArray(new X509Certificate[0]), "RSA");
        } catch (final CertificateException ex) {
            logger.error(null, ex);
        }
    }

    /**
     * Verifies the enveloped XML signature and checks that the XML Document is signed against that signature.
     * This method returns nothing when the signature is valid. When the signature is not valid though,
     * it throws an SMgrException with the Error Message that caused the signature to fail validation.
     *
     * @param doc The XML Document that will be validated.
     * @throws SMgrException When the validation of the signature fails
     */
    public void verifyEnvelopedSignature(final Document doc) throws SMgrException {

        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setXIncludeAware(false);
            dbf.setNamespaceAware(true);
            final var xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");

            final NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

            if (nl.getLength() == 0) {
                throw new SMgrException("Cannot find Signature element");
            } // and document context.

            final var certificateValidator = new CertificateValidator(keyManager.getTrustStore());

            final var valContext = new DOMValidateContext(certificateValidator, nl.item(0));

            // Unmarshal the XMLSignature.
            final var signature = xmlSignatureFactory.unmarshalXMLSignature(valContext);

            // Validate the XMLSignature.
            final boolean coreValidity = signature.validate(valContext);

            if (!coreValidity) {
                throw new SMgrException("Invalid Signature: Mathematical check failed");
            }
        } catch (final XMLSignatureException | MarshalException | ParserConfigurationException ex) {
            throw new SMgrException("Signature Invalid: " + ex.getMessage(), ex);
        }
    }

    /**
     * Signs a SAML Object using the private key with alias <i>keyAlias</i>.
     * Uses the OpenSAML2 library.
     *
     * @param as          The Signable SAML Object that is going to be signed. Usually a SAML Assertion
     * @param keyAlias    The NCP Trust Store Key Alias of the private key that will be used for signing.
     * @param keyPassword Password of the Signature certificate Key.
     * @throws SMgrException When signing fails
     * @see SignableSAMLObject
     */
    public void signSAMLAssertion(final SignableSAMLObject as, final String keyAlias, final char[] keyPassword) throws SMgrException {

        final KeyPair keyPair;
        final X509Certificate cert;
        //check if we must use the default key
        if (keyAlias == null) {
            keyPair = keyManager.getDefaultPrivateKey();
            cert = (X509Certificate) keyManager.getDefaultCertificate();
        } else {
            keyPair = keyManager.getPrivateKey(keyAlias, keyPassword);
            cert = (X509Certificate) keyManager.getCertificate(keyAlias);
        }

        final XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final var signature = (Signature) builderFactory.getBuilder(Signature.DEFAULT_ELEMENT_NAME).buildObject(Signature.DEFAULT_ELEMENT_NAME);
        final Credential signingCredential = CredentialSupport.getSimpleCredential(cert, keyPair.getPrivate());

        signature.setSigningCredential(signingCredential);
        signature.setSignatureAlgorithm(signatureAlgorithm);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

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

        final String value;
        try {
            value = org.apache.commons.codec.binary.Base64.encodeBase64String(
                    ((BasicX509Credential) signingCredential).getEntityCertificate().getEncoded());
        } catch (final CertificateEncodingException e) {
            throw new SMgrException(e.getMessage(), e);
        }
        x509Certificate.setValue(value);
        data.getX509Certificates().add(x509Certificate);
        keyInfo.getX509Datas().add(data);
        signature.setKeyInfo(keyInfo);

        as.setSignature(signature);
        try {
            final var marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
            marshallerFactory.getMarshaller(as).marshall(as);
        } catch (final MarshallingException e) {
            throw new SMgrException(e.getMessage(), e);
        }
        try {
            Signer.signObject(signature);
        } catch (final SignatureException ex) {
            throw new SMgrException(ex.getMessage(), ex);
        }
    }

    /**
     * Signs an XML document using the default private key as it is configured in the Configuration Manager.
     * Uses enveloped XML Signatures
     *
     * @param doc The Document that is going to be signed. be used for signing.
     * @throws SMgrException When signing fails
     */
    public void signXMLWithEnvelopedSig(final Document doc) throws SMgrException {
        signXMLWithEnvelopedSig(doc, null, null);
    }

    /**
     * Signs an XML document using the private key with alias <i>keyAlias</i>.
     * Uses enveloped XML Signatures
     *
     * @param doc         The Document that is going to be signed.
     * @param keyAlias    The NCP Trust Store Key Alias of the private key that
     *                    will be used for signing.
     * @param keyPassword
     * @throws SMgrException When signing fails
     */
    public void signXMLWithEnvelopedSig(final Document doc, final String keyAlias, final char[] keyPassword) throws SMgrException {

        final KeyPair kp;
        final X509Certificate cert;

        if (keyAlias == null) {
            kp = keyManager.getDefaultPrivateKey();
            cert = (X509Certificate) keyManager.getDefaultCertificate();
        } else {
            kp = keyManager.getPrivateKey(keyAlias, keyPassword);
            cert = (X509Certificate) keyManager.getCertificate(keyAlias);
        }

        try {
            final String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
            final var xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName)
                                                                                                   .getDeclaredConstructor()
                                                                                                   .newInstance());
            final var reference = xmlSignatureFactory.newReference("", xmlSignatureFactory.newDigestMethod(digestAlgorithm, null),
                                                                   Collections.singletonList(xmlSignatureFactory.newTransform(Transform.ENVELOPED,
                                                                                                                              (XMLStructure) null)),
                                                                   null, null);

            final var signedInfo = xmlSignatureFactory.newSignedInfo(
                    xmlSignatureFactory.newCanonicalizationMethod(CryptographicConstant.ALGO_ID_C14N_EXCL_WITH_COMMENTS,
                                                                  (C14NMethodParameterSpec) null),
                    xmlSignatureFactory.newSignatureMethod(signatureAlgorithm, null), Collections.singletonList(reference));

            final var keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();

            final List<Serializable> x509Content = new ArrayList<>();
            x509Content.add(cert.getSubjectX500Principal().getName(X500Principal.RFC1779));
            x509Content.add(cert);
            final var x509Data = keyInfoFactory.newX509Data(x509Content);

            final var keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

            final var domSignContext = new DOMSignContext(kp.getPrivate(), doc.getDocumentElement());
            final var xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);
            xmlSignature.sign(domSignContext);
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchAlgorithmException |
                       InvalidAlgorithmParameterException | MarshalException | XMLSignatureException | NoSuchMethodException |
                       InvocationTargetException ex) {
            throw new SMgrException(ex.getMessage(), ex);
        }
    }

    /**
     * Signs a Signable SAML Object using the default key that is configured in the Configuration Manager.
     * Uses the OpenSAML2 library.
     *
     * @param trc The Signable SAML Object that is going to be signed. Usually a SAML Assertion.
     * @throws SMgrException When signing fails
     * @see SignableSAMLObject
     */
    public void signSAMLAssertion(final Assertion trc) throws SMgrException {
        signSAMLAssertion(trc, null, null);
    }
}
