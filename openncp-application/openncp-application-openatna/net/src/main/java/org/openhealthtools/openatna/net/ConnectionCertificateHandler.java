package org.openhealthtools.openatna.net;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * A simple class for dealing with certificate management.
 * <p/>
 * <p/>
 * This is a single point of entry for the entire suite of ssl key and certificate handling functions.
 * Basically you should use it as a static class for obtaining KeyManager s and TrustManager s.
 * KeyManagers hold keys and as such require passwords.
 * TrustManagers on the other hand only hold certificates and as such do not require passwords.
 * You should also use it to obtain the KeyStore needed to generate the KeyManager s and TrustManager s.
 * <p/>
 * <p/>
 * Once everything is complete, this should only be used by the ConnectionFactory and should never be called directly.
 * <p/>
 * <p/>
 * (Actually that means that these should all be protected and not public, but I think that would break the unit tests.)
 * <p/>
 */
public class ConnectionCertificateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionCertificateHandler.class);
    private static final Logger LOGGER_CLINICAL = LoggerFactory.getLogger("LOGGER_CLINICAL");
    private static final String SERVER_EHEALTH_MODE = "server.ehealth.mode";
    private static final String PRODUCTION = "PRODUCTION";

    private ConnectionCertificateHandler() {
    }

    /**
     * Creates a key/trust store and loads in the corresponding file.
     */
    public static KeyStore createKeyStore(final URL url, final String password)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (url == null) {
            throw new IllegalArgumentException("Keystore url may not be null");
        }
        LOGGER.debug("Initializing key store");
        KeyStore keystore;
        if (url.getFile().endsWith(".p12")) {
            keystore = KeyStore.getInstance("pkcs12");
        } else {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        }
        keystore.load(url.openStream(), password != null ? password.toCharArray() : null);
        return keystore;
    }

    /**
     * Creates Key Managers from a keystore.
     */
    public static KeyManager[] createKeyManagers(final KeyStore keystore, final String password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        LOGGER.debug("Initializing key manager");
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, password != null ? password.toCharArray() : null);
        return keyManagerFactory.getKeyManagers();
    }

    /**
     * Creates Trust Managers from a truststore.
     */
    public static TrustManager[] createTrustManagers(final KeyStore keystore, SecureConnectionDescription scd)
            throws KeyStoreException, NoSuchAlgorithmException {
        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        LOGGER.debug("Initializing trust manager");
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keystore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        for (int i = 0; i < trustManagers.length; i++) {
            if (trustManagers[i] instanceof X509TrustManager) {
                trustManagers[i] = new LoggedX509TrustManager((X509TrustManager) trustManagers[i], scd);
            }
        }
        return trustManagers;
    }

    /**
     * Returns out keystore certificate chain.
     *
     * @param keystore Keystore to print out.
     * @throws KeyStoreException If the keystore is broken.
     */
    public static String getKeyCertDN(KeyStore keystore) throws KeyStoreException {

        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate[] certs = keystore.getCertificateChain(alias);
            if (certs != null) {
                StringBuilder message = new StringBuilder("Certificate chain '" + alias + "':");
                int i = 1;
                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate) {
                        X509Certificate x509Certificate = (X509Certificate) cert;
                        message.append("\n Certificate ").append(i++).append(":");
                        message.append("\n  Subject DN: ").append(x509Certificate.getSubjectDN());
                        message.append("\n  Signature Algorithm: ").append(x509Certificate.getSigAlgName());
                        message.append("\n  Valid from: ").append(x509Certificate.getNotBefore());
                        message.append("\n  Valid until: ").append(x509Certificate.getNotAfter());
                        message.append("\n  Issuer: ").append(x509Certificate.getIssuerDN());
                    }
                }
                if (!StringUtils.equals(System.getProperty(SERVER_EHEALTH_MODE), PRODUCTION) && LOGGER_CLINICAL.isDebugEnabled()) {
                    LOGGER_CLINICAL.debug(message.toString());
                }
            }
        }
        return "";
    }

    /**
     * For debugging only.  Prints out keystore certificate chain.
     *
     * @param keystore Keystore to print out.
     * @throws KeyStoreException If the keystore is broken.
     */
    public static void printTrustCerts(KeyStore keystore) throws KeyStoreException {

        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            String message = "Trusted certificate '" + alias + "':";
            Certificate trustedCertificate = keystore.getCertificate(alias);
            if (trustedCertificate instanceof X509Certificate) {
                X509Certificate certificate = (X509Certificate) trustedCertificate;
                message += "\n  Subject DN: " + certificate.getSubjectDN();
                message += "\n  Signature Algorithm: " + certificate.getSigAlgName();
                message += "\n  Valid from: " + certificate.getNotBefore();
                message += "\n  Valid until: " + certificate.getNotAfter();
                message += "\n  Issuer: " + certificate.getIssuerDN();
            }
            if (!StringUtils.equals(System.getProperty(SERVER_EHEALTH_MODE), PRODUCTION)
                    && LOGGER_CLINICAL.isDebugEnabled()) {
                LOGGER_CLINICAL.debug(message);
            }
        }
    }

    /**
     * For debugging only.  Prints out keystore certificate chain.
     *
     * @param keystore Keystore to print out.
     * @throws KeyStoreException If the keystore is broken.
     */
    public static void printKeyCertificates(KeyStore keystore) throws KeyStoreException {

        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate[] certs = keystore.getCertificateChain(alias);
            if (certs != null) {
                StringBuilder message = new StringBuilder("Certificate chain '" + alias + "':");
                int i = 1;
                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate) {
                        X509Certificate x509Certificate = (X509Certificate) cert;
                        message.append("\n Certificate ").append(i++).append(":");
                        message.append("\n  Subject DN: ").append(x509Certificate.getSubjectDN());
                        message.append("\n  Signature Algorithm: ").append(x509Certificate.getSigAlgName());
                        message.append("\n  Valid from: ").append(x509Certificate.getNotBefore());
                        message.append("\n  Valid until: ").append(x509Certificate.getNotAfter());
                        message.append("\n  Issuer: ").append(x509Certificate.getIssuerDN());
                    }
                }
                if (!StringUtils.equals(System.getProperty(SERVER_EHEALTH_MODE), PRODUCTION) && LOGGER_CLINICAL.isDebugEnabled()) {
                    LOGGER_CLINICAL.debug(message.toString());
                }
            }
        }
    }
}
