package eu.europa.ec.sante.openncp.common.security.key;

import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.common.util.CertificatesDataHolder;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public final class DefaultKeyStoreManager implements KeyStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultKeyStoreManager.class);

    private final String KEYSTORE_LOCATION;
    private final String TRUSTSTORE_LOCATION;
    private final String KEYSTORE_PASSWORD;
    private final String TRUSTSTORE_PASSWORD;
    private final String PRIVATEKEY_ALIAS;
    private final String PRIVATEKEY_PASSWORD;
    private KeyStore keyStore;
    private KeyStore trustStore;

    public DefaultKeyStoreManager(final String keyStoreLocation, final String keystorePassword, final String truststoreLocation,
                                  final String truststorePassword, final String privateKeyAlias, final String privateKeyPassword) {

        // Constants Initialization
        this.KEYSTORE_LOCATION = Validate.notBlank(keyStoreLocation);
        this.TRUSTSTORE_LOCATION = Validate.notBlank(truststoreLocation);
        this.KEYSTORE_PASSWORD = Validate.notBlank(keystorePassword);
        this.TRUSTSTORE_PASSWORD = Validate.notBlank(truststorePassword);
        this.PRIVATEKEY_ALIAS = Validate.notBlank(privateKeyAlias);
        this.PRIVATEKEY_PASSWORD = Validate.notBlank(privateKeyPassword);

        this.keyStore = this.getKeyStore();
        this.trustStore = this.getTrustStore();
    }

    public static DefaultKeyStoreManager forConsumer(final CertificatesDataHolder certificatesDataHolder) {
        Validate.notNull(certificatesDataHolder, "certificatesDataHolder must not be null");

        final CertificatesDataHolder.CertificateData serviceConsumerData = certificatesDataHolder.getServiceConsumerData().orElseThrow(() -> new RuntimeException("serviceConsumerData must not be null"));
        final CertificatesDataHolder.CertificateData truststoreData = certificatesDataHolder.getTrustoreData();

        return new DefaultKeyStoreManager(
                serviceConsumerData.getKeystorePath(),
                serviceConsumerData.getKeystorePassword(),
                truststoreData.getKeystorePath(),
                truststoreData.getKeystorePassword(),
                serviceConsumerData.getPrivateKeyAlias().orElse(null),
                serviceConsumerData.getPrivateKeyPassword().orElse(null));
    }

    public static DefaultKeyStoreManager forProvider(final CertificatesDataHolder certificatesDataHolder) {
        Validate.notNull(certificatesDataHolder, "certificatesDataHolder must not be null");

        final CertificatesDataHolder.CertificateData serviceProviderData = certificatesDataHolder.getServiceProviderData().orElseThrow(() -> new RuntimeException("serviceProviderData must not be null"));
        final CertificatesDataHolder.CertificateData truststoreData = certificatesDataHolder.getTrustoreData();

        return new DefaultKeyStoreManager(
                serviceProviderData.getKeystorePath(),
                serviceProviderData.getKeystorePassword(),
                truststoreData.getKeystorePath(),
                truststoreData.getKeystorePassword(),
                serviceProviderData.getPrivateKeyAlias().orElseThrow(() -> new RuntimeException("private key alias must not be empty")),
                serviceProviderData.getPrivateKeyPassword().orElseThrow(() -> new RuntimeException("private key password must not be empty")));
    }

    public static DefaultKeyStoreManager forSignature(final CertificatesDataHolder certificatesDataHolder) {
        Validate.notNull(certificatesDataHolder, "certificatesDataHolder must not be null");

        final CertificatesDataHolder.CertificateData signatureData = certificatesDataHolder.getSignatureData().orElseThrow(() -> new RuntimeException("signatureData must not be null"));
        final CertificatesDataHolder.CertificateData truststoreData = certificatesDataHolder.getTrustoreData();

        return new DefaultKeyStoreManager(
                signatureData.getKeystorePath(),
                signatureData.getKeystorePassword(),
                truststoreData.getKeystorePath(),
                truststoreData.getKeystorePassword(),
                signatureData.getPrivateKeyAlias().orElseThrow(() -> new RuntimeException("private key alias must not be empty")),
                signatureData.getPrivateKeyPassword().orElseThrow(() -> new RuntimeException("private key password must not be empty")));
    }



    @Override
    public KeyPair getPrivateKey(final String alias, final char[] password) throws SMgrException {

        try {

            // Get private key
            final Key key = this.keyStore.getKey(alias, password);
            if (key instanceof PrivateKey) {
                // Get certificate of public key
                final Certificate cert = this.keyStore.getCertificate(alias);

                // Get public key
                final PublicKey publicKey = cert.getPublicKey();

                // Return a key pair
                return new KeyPair(publicKey, (PrivateKey) key);
            }
        } catch (final UnrecoverableKeyException e) {
            LOGGER.error(null, e);
            throw new SMgrException("Key with alias:" + alias + " is unrecoverable", e);
        } catch (final NoSuchAlgorithmException e) {
            LOGGER.error(null, e);
            throw new SMgrException("Key with alias:" + alias + " uses an incompatible algorithm", e);
        } catch (final KeyStoreException e) {
            LOGGER.error(null, e);
            throw new SMgrException("Key with alias:" + alias + " not found", e);
        }
        return null;
    }

    @Override
    public KeyStore getKeyStore() {

        try {
            this.keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            try (final InputStream keystoreStream = new FileInputStream(this.KEYSTORE_LOCATION)) {
                this.keyStore.load(keystoreStream, this.KEYSTORE_PASSWORD.toCharArray());

                return this.keyStore;
            }
        } catch (final IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException ex) {
            LOGGER.error(null, ex);
        }
        return null;
    }

    @Override
    public Certificate getCertificate(final String alias) throws SMgrException {

        try {
            return this.keyStore.getCertificate(alias);
        } catch (final KeyStoreException ex) {
            LOGGER.error(null, ex);
            throw new SMgrException("Certificate with alias: " + alias + " not found in keystore", ex);
        }
    }

    @Override
    public KeyStore getTrustStore() {

        try {
            this.trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (final InputStream keystoreStream = new FileInputStream(this.TRUSTSTORE_LOCATION)) {
                this.trustStore.load(keystoreStream, this.TRUSTSTORE_PASSWORD.toCharArray());
                return this.trustStore;
            }
        } catch (final IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException ex) {
            LOGGER.error(null, ex);
        }
        return null;
    }

    @Override
    public KeyPair getDefaultPrivateKey() throws SMgrException {
        return this.getPrivateKey(this.PRIVATEKEY_ALIAS, this.PRIVATEKEY_PASSWORD.toCharArray());
    }

    @Override
    public Certificate getDefaultCertificate() throws SMgrException {
        return this.getCertificate(this.PRIVATEKEY_ALIAS);
    }
}
