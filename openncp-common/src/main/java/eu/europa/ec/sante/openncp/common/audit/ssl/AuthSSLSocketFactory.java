package eu.europa.ec.sante.openncp.common.audit.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;

public class AuthSSLSocketFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthSSLSocketFactory.class);
    private final X509TrustManager defaultTrustManager;
    private KeystoreDetails details = null;
    private KeystoreDetails truststore = null;
    private SSLContext sslcontext = null;

    /**
     * @param details
     * @param truststore
     * @param defaultTrustManager
     * @throws IOException
     */
    public AuthSSLSocketFactory(final KeystoreDetails details, final KeystoreDetails truststore, final X509TrustManager defaultTrustManager) {
        super();

        if (details != null) {
            this.details = details;
        }
        if (truststore != null) {
            this.truststore = truststore;
        }
        if (defaultTrustManager == null) {
            LOGGER.debug("Using SUN default trust manager");
            this.defaultTrustManager = KeystoreManager.getDefaultTrustManager();
        } else {
            this.defaultTrustManager = defaultTrustManager;
        }
    }

    /**
     * @param details
     * @param truststore
     */
    public AuthSSLSocketFactory(final KeystoreDetails details, final KeystoreDetails truststore) {
        this(details, truststore, null);
    }

    /**
     * @param details
     * @param defaultTrustManager
     */
    public AuthSSLSocketFactory(final KeystoreDetails details, final X509TrustManager defaultTrustManager) {
        this(details, null, defaultTrustManager);
    }

    /**
     * @param details
     */
    public AuthSSLSocketFactory(final KeystoreDetails details) {
        this(details, null, null);
    }

    /**
     * @param details
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    private static KeyStore createKeyStore(final KeystoreDetails details)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        if (details.getKeystoreLocation() == null) {
            throw new IllegalArgumentException("Keystore location may not be null");
        }

        LOGGER.debug("Initializing key store");
        final KeyStore keystore = KeyStore.getInstance(details.getKeystoreType());

        try (final InputStream is = getKeystoreInputStream(details.getKeystoreLocation())) {

            if (is == null) {
                throw new IOException("Could not open stream to " + details.getKeystoreLocation());
            }
            final String password = details.getKeystorePassword();
            keystore.load(is, password != null ? password.toCharArray() : null);
        }
        return keystore;
    }

    /**
     * @param location
     * @return
     */
    private static InputStream getKeystoreInputStream(final String location) {

        try {
            final File file = new File(location);
            if (file.exists()) {
                return Files.newInputStream(file.toPath());
            }
            final URL url = new URL(location);
            return url.openStream();

        } catch (final Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }

        LOGGER.warn("Could not open stream to: '{}'", location);
        return null;
    }

    /**
     * @param keystore
     * @param details
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableKeyException
     */
    private KeyManager[] createKeyManagers(final KeyStore keystore, final KeystoreDetails details)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {

        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        LOGGER.debug("Initializing key manager");
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(details.getAlgType());
        final String password = details.getKeyPassword().orElseGet(details::getKeystorePassword);
        keyManagerFactory.init(keystore, password.toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    /**
     * @param truststore
     * @param keystore
     * @param defaultTrustManager
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     */
    private TrustManager[] createTrustManagers(final KeystoreDetails truststore, final KeyStore keystore, final X509TrustManager defaultTrustManager)
            throws KeyStoreException, NoSuchAlgorithmException {

        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keystore);
        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        for (final TrustManager trustmanager : trustManagers) {

            if (trustmanager instanceof X509TrustManager) {
                return new TrustManager[]{
                        new AuthSSLX509TrustManager((X509TrustManager) trustmanager, defaultTrustManager, new ArrayList<>(truststore.getAuthorizedDNs()))};
            }
        }
        return trustManagers;
    }

    /**
     * @return
     * @throws IOException
     */
    private SSLContext createSSLContext() throws IOException {

        try {
            KeyManager[] keyManagers = null;
            TrustManager[] trustManagers = null;
            if (this.details != null) {
                final KeyStore keystore = createKeyStore(details);
                final Enumeration aliases = keystore.aliases();
                while (aliases.hasMoreElements()) {
                    final String alias = (String) aliases.nextElement();
                    final Certificate[] certs = keystore.getCertificateChain(alias);
                    if (certs != null) {
                        LOGGER.debug("Certificate chain: '{}'", alias);
                        for (int c = 0; c < certs.length; c++) {
                            if (certs[c] instanceof X509Certificate) {
                                final X509Certificate cert = (X509Certificate) certs[c];
                                LOGGER.debug("Certificate '{}':", (c + 1));
                                LOGGER.debug("   Subject DN: '{}'", cert.getSubjectDN());
                                LOGGER.debug("   Serial Number: '{}'", cert.getSerialNumber());
                                LOGGER.debug("   Signature Algorithm: '{}'", cert.getSigAlgName());
                                LOGGER.debug("   Valid from: '{}'", cert.getNotBefore());
                                LOGGER.debug("   Valid until: '{}'", cert.getNotAfter());
                                LOGGER.debug("   Issuer: '{}'", cert.getIssuerDN());
                            }
                        }
                    }
                }
                keyManagers = createKeyManagers(keystore, details);
            }
            if (this.truststore != null) {
                final KeyStore keystore = createKeyStore(truststore);
                final Enumeration aliases = keystore.aliases();
                while (aliases.hasMoreElements()) {
                    final String alias = (String) aliases.nextElement();
                    LOGGER.debug("Trusted certificate: '{}':", alias);
                    final Certificate trustedCert = keystore.getCertificate(alias);
                    if (trustedCert instanceof X509Certificate) {
                        final X509Certificate cert = (X509Certificate) trustedCert;
                        LOGGER.debug("   Subject DN: '{}'", cert.getSubjectDN());
                        LOGGER.debug("   Serial Number: '{}'", cert.getSerialNumber());
                        LOGGER.debug("   Signature Algorithm: '{}'", cert.getSigAlgName());
                        LOGGER.debug("   Valid from: '{}'", cert.getNotBefore());
                        LOGGER.debug("   Valid until: '{}'", cert.getNotAfter());
                        LOGGER.debug("   Issuer: '{}'", cert.getIssuerDN());
                    }
                }
                trustManagers = createTrustManagers(truststore, keystore, defaultTrustManager);
            }
            if (trustManagers == null) {
                LOGGER.debug("Created Trust Managers from the default...");
                trustManagers = new TrustManager[]{defaultTrustManager};
            }

            final SSLContext localSSLContext = SSLContext.getInstance("TLSv1.2");
            localSSLContext.init(keyManagers, trustManagers, null);
            return localSSLContext;
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error("NoSuchAlgorithmException: '{}'", e.getMessage(), e);
            throw new IOException("Unsupported algorithm exception: " + e.getMessage());
        } catch (final KeyStoreException e) {
            LOGGER.error("KeyStoreException: '{}'", e.getMessage(), e);
            throw new IOException("Keystore exception: " + e.getMessage());
        } catch (final GeneralSecurityException e) {
            LOGGER.error("GeneralSecurityException: '{}'", e.getMessage(), e);
            throw new IOException("Key management exception: " + e.getMessage());
        } catch (final IOException e) {
            LOGGER.error("IOException: '{}'", e.getMessage(), e);
            throw new IOException("I/O error reading keystore/truststore file: " + e.getMessage());
        }
    }

    /**
     * @return
     * @throws IOException
     */
    public SSLContext getSSLContext() throws IOException {
        if (this.sslcontext == null) {
            this.sslcontext = createSSLContext();
        }
        return this.sslcontext;
    }

    /**
     * @param host
     * @param port
     * @return
     * @throws IOException
     */
    public Socket createSecureSocket(final String host, final int port) throws IOException {
        return getSSLContext().getSocketFactory().createSocket(host, port);
    }

    /**
     * @param port
     * @param mutualAuth
     * @return
     * @throws IOException
     */
    public ServerSocket createServerSocket(final int port, final boolean mutualAuth) throws IOException {
        final ServerSocket ss = getSSLContext().getServerSocketFactory().createServerSocket(port);
        if (mutualAuth) {
            ((SSLServerSocket) ss).setNeedClientAuth(true);
        }
        return ss;
    }

    /**
     * @return
     */
    public boolean isSecured() {
        return true;
    }
}
