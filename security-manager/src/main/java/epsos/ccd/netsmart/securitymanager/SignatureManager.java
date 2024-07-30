package epsos.ccd.netsmart.securitymanager;

import epsos.ccd.netsmart.securitymanager.exceptions.SMgrException;
import epsos.ccd.netsmart.securitymanager.key.KeyStoreManager;
import epsos.ccd.netsmart.securitymanager.key.impl.DefaultKeyStoreManager;
import eu.europa.ec.sante.ehdsi.openncp.util.security.CryptographicConstant;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

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
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The NCP Signature Manager is a JAVA library for applying and verifying detached digital signatures on XML documents
 * and for applying and verifying enveloped signatures on SAML assertions and Audit Trail Messages
 *
 * @author Jerry Dimitriou <jerouris at netsmart.gr>
 */
public class SignatureManager {

    private final Logger logger = LoggerFactory.getLogger(SignatureManager.class);
    private final KeyStoreManager keyManager;
    private String signatureAlgorithm;
    private String digestAlgorithm;

    public SignatureManager() {

        //Default constructor now defaults to the test keyStoreManager
        keyManager = new DefaultKeyStoreManager();
        init();
    }

    public SignatureManager(KeyStoreManager keyStoreManager) {

        //Constructor for unit testing. Sort of DI
        this.keyManager = keyStoreManager;
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
    public String verifySAMLAssertion(Assertion assertion) throws SMgrException {

        String sigCountryCode = null;

        try {
            var profileValidator = new SAMLSignatureProfileValidator();
            var assertionSignature = assertion.getSignature();
            try {
                profileValidator.validate(assertionSignature);
            } catch (SignatureException e) {
                // Indicates signature did not conform to SAML Signature profile
                throw new SMgrException("SAML Signature Profile Validation: " + e.getMessage());
            }

            X509Certificate cert;
            List<X509Certificate> certificates = KeyInfoSupport.getCertificates(assertionSignature.getKeyInfo());
            for (X509Certificate certificate : certificates) {
                logger.debug("Certificate: '{}'", certificate.getIssuerX500Principal().getName());
            }
            if (certificates.size() == 1) {
                cert = certificates.get(0);
                // Mustafa: When not called through https, we can use the country code of the signature cert
                String certificateDN = cert.getSubjectDN().getName();
                sigCountryCode = certificateDN.substring(certificateDN.indexOf("C=") + 2, certificateDN.indexOf("C=") + 4);

            } else {
                throw new SMgrException("More than one certificate found in KeyInfo");
            }

            var basicX509Credential = new BasicX509Credential(cert);

            try {
                SignatureValidator.validate(assertionSignature, basicX509Credential);

            } catch (SignatureException e) {
                // Indicates signature was not cryptographically valid, or possibly a processing error
                throw new SMgrException("Signature Validation: " + e.getMessage());
            }
            var certificateValidator = new CertificateValidator(keyManager.getTrustStore());
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
        } catch (CertificateException ex) {
            logger.error(null, ex);
        }

        return sigCountryCode;
    }

    /**
     * Verifies the enveloped XML signature and checks that the XML Document is signed against that signature.
     * This method returns nothing when the signature is valid. When the signature is not valid though,
     * it throws an SMgrException with the Error Message that caused the signature to fail validation.
     *
     * @param doc The XML Document that will be validated.
     * @throws SMgrException When the validation of the signature fails
     */
    public void verifyEnvelopedSignature(Document doc) throws SMgrException {

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            var xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM");

            NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

            if (nl.getLength() == 0) {
                throw new SMgrException("Cannot find Signature element");
            } // and document context.

            var certificateValidator = new CertificateValidator(keyManager.getTrustStore());

            var valContext = new DOMValidateContext(certificateValidator, nl.item(0));

            // Unmarshal the XMLSignature.
            var signature = xmlSignatureFactory.unmarshalXMLSignature(valContext);

            // Validate the XMLSignature.
            boolean coreValidity = signature.validate(valContext);

            if (!coreValidity) {
                throw new SMgrException("Invalid Signature: Mathematical check failed");
            }

        } catch (XMLSignatureException | MarshalException ex) {
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
     * @see org.opensaml.saml.common.SignableSAMLObject
     */
    public void signSAMLAssertion(SignableSAMLObject as, String keyAlias, char[] keyPassword) throws SMgrException {

        KeyPair keyPair;
        X509Certificate cert;
        //check if we must use the default key
        if (keyAlias == null) {
            keyPair = keyManager.getDefaultPrivateKey();
            cert = (X509Certificate) keyManager.getDefaultCertificate();

        } else {
            keyPair = keyManager.getPrivateKey(keyAlias, keyPassword);
            cert = (X509Certificate) keyManager.getCertificate(keyAlias);
        }

        XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
        var signature = (Signature) builderFactory.getBuilder(Signature.DEFAULT_ELEMENT_NAME).buildObject(Signature.DEFAULT_ELEMENT_NAME);
        Credential signingCredential = CredentialSupport.getSimpleCredential(cert, keyPair.getPrivate());

        signature.setSigningCredential(signingCredential);
        signature.setSignatureAlgorithm(signatureAlgorithm);
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        var keyInfo = (KeyInfo) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(KeyInfo.DEFAULT_ELEMENT_NAME).buildObject(KeyInfo.DEFAULT_ELEMENT_NAME);
        X509Data data = (X509Data) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(X509Data.DEFAULT_ELEMENT_NAME).buildObject(X509Data.DEFAULT_ELEMENT_NAME);
        var x509Certificate = (org.opensaml.xmlsec.signature.X509Certificate) XMLObjectProviderRegistrySupport.getBuilderFactory()
                .getBuilder(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME).buildObject(org.opensaml.xmlsec.signature.X509Certificate.DEFAULT_ELEMENT_NAME);

        String value;
        try {
            value = org.apache.commons.codec.binary.Base64.encodeBase64String(((BasicX509Credential) signingCredential).getEntityCertificate().getEncoded());
        } catch (CertificateEncodingException e) {
            throw new SMgrException(e.getMessage(), e);
        }
        x509Certificate.setValue(value);
        data.getX509Certificates().add(x509Certificate);
        keyInfo.getX509Datas().add(data);
        signature.setKeyInfo(keyInfo);

        as.setSignature(signature);
        try {
            var marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory();
            marshallerFactory.getMarshaller(as).marshall(as);
        } catch (MarshallingException e) {
            throw new SMgrException(e.getMessage(), e);
        }
        try {
            Signer.signObject(signature);
        } catch (SignatureException ex) {
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
    public void signXMLWithEnvelopedSig(Document doc) throws SMgrException {
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
    public void signXMLWithEnvelopedSig(Document doc, String keyAlias, char[] keyPassword) throws SMgrException {

        KeyPair kp;
        X509Certificate cert;

        if (keyAlias == null) {
            kp = keyManager.getDefaultPrivateKey();
            cert = (X509Certificate) keyManager.getDefaultCertificate();
        } else {
            kp = keyManager.getPrivateKey(keyAlias, keyPassword);
            cert = (X509Certificate) keyManager.getCertificate(keyAlias);

        }

        try {
            String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
            var xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM",
                    (Provider) Class.forName(providerName).getDeclaredConstructor().newInstance());
            var reference = xmlSignatureFactory.newReference("",
                    xmlSignatureFactory.newDigestMethod(digestAlgorithm, null),
                    Collections.singletonList(xmlSignatureFactory.newTransform(Transform.ENVELOPED, (XMLStructure) null)),
                    null, null);

            var signedInfo = xmlSignatureFactory.newSignedInfo(xmlSignatureFactory.newCanonicalizationMethod(CryptographicConstant.ALGO_ID_C14N_EXCL_WITH_COMMENTS,
                            (C14NMethodParameterSpec) null), xmlSignatureFactory.newSignatureMethod(signatureAlgorithm, null),
                    Collections.singletonList(reference));

            var keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();

            List<Serializable> x509Content = new ArrayList<>();
            x509Content.add(cert.getSubjectX500Principal().getName(X500Principal.RFC1779));
            x509Content.add(cert);
            var x509Data = keyInfoFactory.newX509Data(x509Content);

            var keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data));

            var domSignContext = new DOMSignContext(kp.getPrivate(), doc.getDocumentElement());
            var xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);
            xmlSignature.sign(domSignContext);

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException | MarshalException | XMLSignatureException | NoSuchMethodException
                | InvocationTargetException ex) {
            throw new SMgrException(ex.getMessage(), ex);
        }
    }

    /**
     * Signs a Signable SAML Object using the default key that is configured in the Configuration Manager.
     * Uses the OpenSAML2 library.
     *
     * @param trc The Signable SAML Object that is going to be signed. Usually a SAML Assertion.
     * @throws SMgrException When signing fails
     * @see org.opensaml.saml.common.SignableSAMLObject
     */
    public void signSAMLAssertion(Assertion trc) throws SMgrException {
        signSAMLAssertion(trc, null, null);
    }
}
