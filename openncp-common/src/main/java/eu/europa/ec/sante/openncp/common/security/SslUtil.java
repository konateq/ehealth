package eu.europa.ec.sante.openncp.common.security;

import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.key.DefaultKeyStoreManager;
import eu.europa.ec.sante.openncp.common.security.key.KeyStoreManager;
import eu.europa.ec.sante.openncp.common.util.CertificatesDataHolder;
import org.apache.commons.lang3.Validate;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class SslUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslUtil.class);

    private SslUtil() {
    }

    public static SSLContext createSSLContext(final SslProperties sslProperties) {
        Validate.notNull(sslProperties);
        final PrivateKeyStrategy privatek = (map, socket) -> sslProperties.getCertificateAlias();

        // Trust own CA and all self-signed certs
        SSLContext sslcontext = null;
        try {
            //must be the same as SC_KEYSTORE_PASSWORD
            sslcontext = SSLContexts.custom()
                    .loadKeyMaterial(new File(sslProperties.getKeystorePath()),
                            sslProperties.getKeystorePassword().toCharArray(),
                            sslProperties.getCertificatePassword().toCharArray(),
                            privatek)
                    .loadTrustMaterial(new File(sslProperties.getTruststorePath()),
                            sslProperties.getTruststorePassword().toCharArray(),
                            new TrustSelfSignedStrategy())
                    .build();
        } catch (final NoSuchAlgorithmException ex) {
            LOGGER.error(String.format("NoSuchAlgorithmException: %s", ex.getMessage()), ex);
        } catch (final KeyStoreException ex) {
            LOGGER.error(String.format("KeyStoreException: %s", ex.getMessage()), ex);
        } catch (final CertificateException ex) {
            LOGGER.error(String.format("CertificateException: %s", ex.getMessage()), ex);
        } catch (final IOException ex) {
            LOGGER.error(String.format("IOException: %s", ex.getMessage()), ex);
        } catch (final KeyManagementException ex) {
            LOGGER.error(String.format("KeyManagementException: %s", ex.getMessage()), ex);
        } catch (final UnrecoverableKeyException ex) {
            LOGGER.error(String.format("UnrecoverableKeyException: %s", ex.getMessage()), ex);
        }
        return sslcontext;
    }

    public static SSLSocketFactory getSSLSocketFactory(final CertificatesDataHolder certificatesDataHolder) {
        final KeyStoreManager keyStoreManager = DefaultKeyStoreManager.forConsumer(certificatesDataHolder);

        try { String sigKeystorePassword = Constants.NCP_SIG_KEYSTORE_PASSWORD;
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            var keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStoreManager.getKeyStore(), sigKeystorePassword.toCharArray());
            var trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStoreManager.getTrustStore());
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext.getSocketFactory(); }

        catch (KeyManagementException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
            return null;
        }
    }


}
