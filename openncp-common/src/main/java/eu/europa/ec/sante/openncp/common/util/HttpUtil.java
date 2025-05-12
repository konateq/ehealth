package eu.europa.ec.sante.openncp.common.util;

import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerFactory;
import eu.europa.ec.sante.openncp.common.configuration.StandardProperties;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.SslUtil;
import eu.europa.ec.sante.openncp.common.util.proxy.ProxyCredentials;
import org.cryptacular.util.CertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpUtil {

    public static final Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);
    private static final String WARNING_NO_CERTIFICATE_FOUND = "Warning!: No Server certificate found!";

    public static String getHostIpAddress(final String host) {

        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (final UnknownHostException e) {
            return "Server IP Unknown";
        }
    }

    public static String getClientCertificate(final HttpServletRequest request) {

        LOGGER.info("Trying to find certificate from : '{}'", request.getRequestURI());
        final String result;
        final X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

        if (certs != null && certs.length > 0) {
            result = getCommonName(certs[0]);
        } else {
            if ("https".equals(request.getScheme())) {
                LOGGER.warn("This was an HTTPS request, " + "but no client certificate is available");
            } else {
                LOGGER.warn("This was not an HTTPS request, " + "so no client certificate is available");
            }
            result = "Warning!: No Client certificate found!";
        }
        LOGGER.debug("Client Certificate: '{}'", result);
        return result;
    }

    public static String getCommonNameFromServerCertificate(final String endpoint) {

        LOGGER.debug("Trying to find certificate from : '{}'", endpoint);
        String result = WARNING_NO_CERTIFICATE_FOUND;

        final CertificatesDataHolder certificatesDataHolder = CertificatesDataHolder.builder()
                .trustoreData(CertificatesDataHolder.CertificateData.builder()
                        .keystorePath(Constants.TRUSTSTORE_PATH)
                        .keystorePassword(Constants.TRUSTSTORE_PASSWORD)
                        .build())
                .serviceConsumerData(CertificatesDataHolder.CertificateData.builder()
                        .keystorePath(Constants.SC_KEYSTORE_PATH)
                        .keystorePassword(Constants.SC_KEYSTORE_PASSWORD)
                        .privateKeyAlias(Constants.SC_PRIVATEKEY_ALIAS)
                        .privateKeyPassword(Constants.SC_PRIVATEKEY_PASSWORD)
                        .build())
                .build();

        try {
            if (endpoint.startsWith("https")) {
                final var sslSocketFactory = SslUtil.getSSLSocketFactory(certificatesDataHolder);
                final URL url = new URL(endpoint);
                final String host = url.getHost();
                int port = url.getPort() == -1 ? 443 : url.getPort();

                try (final SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port)) {
                    socket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
                    socket.startHandshake();
                    final Certificate[] certs = socket.getSession().getPeerCertificates();
                    //Get the first certificate
                    if (certs != null && certs.length > 0) {
                        final X509Certificate cert = (X509Certificate) certs[0];
                        result = getCommonName(cert);
                    }
                }
            }
        } catch (final Throwable e) {
            LOGGER.error(String.format("Error fetching the server certificate at [%s] with exception message [%s]", endpoint, e.getMessage()), e);
        }
        LOGGER.debug("Server Certificate: '{}'", result);
        return result;
    }

    public static String getSubjectDN(final CertificatesDataHolder certificatesDataHolder, final boolean isProvider) {
        final CertificatesDataHolder.CertificateData certificateData;
        if (isProvider) {
            certificateData = certificatesDataHolder.getServiceProviderData().orElseThrow(() -> new RuntimeException("ServiceProviderData must not be empty"));
        } else {
            certificateData = certificatesDataHolder.getServiceConsumerData().orElseThrow(() -> new RuntimeException("ServiceConsumerData must not be empty"));
        }

        try (final var inputStream = new FileInputStream(certificateData.getKeystorePath())) {
            final var keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(inputStream, certificateData.getKeystorePassword().toCharArray());
            final Certificate cert = keystore.getCertificate(certificateData.getPrivateKeyAlias().orElseThrow(() -> new RuntimeException("Private key alias must not be empty")));

            if (cert instanceof X509Certificate) {
                final var x509Certificate = (X509Certificate) cert;
                return getCommonName(x509Certificate);
            }
        } catch (final KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            LOGGER.error("{}: '{}'", e.getClass(), e.getMessage(), e);
        }
        return "";
    }

    public static String getSubjectDN(final boolean isProvider) {
        final CertificatesDataHolder certificatesDataHolder = CertificatesDataHolder.builder()
                .trustoreData(CertificatesDataHolder.CertificateData.builder()
                        .keystorePath(Constants.TRUSTSTORE_PATH)
                        .keystorePassword(Constants.TRUSTSTORE_PASSWORD)
                        .build())
                .serviceProviderData(CertificatesDataHolder.CertificateData.builder()
                        .keystorePath(Constants.SP_KEYSTORE_PATH)
                        .keystorePassword(Constants.SP_KEYSTORE_PASSWORD)
                        .privateKeyAlias(Constants.SP_PRIVATEKEY_ALIAS)
                        .privateKeyPassword(Constants.SP_PRIVATEKEY_PASSWORD)
                        .build())
                .serviceConsumerData(CertificatesDataHolder.CertificateData.builder()
                        .keystorePath(Constants.SC_KEYSTORE_PATH)
                        .keystorePassword(Constants.SC_KEYSTORE_PASSWORD)
                        .privateKeyAlias(Constants.SC_PRIVATEKEY_ALIAS)
                        .privateKeyPassword(Constants.SC_PRIVATEKEY_PASSWORD)
                        .build())
                .build();

        return getSubjectDN(certificatesDataHolder, isProvider);
    }

    public static boolean isBehindProxy() {

        return Boolean.parseBoolean(ConfigurationManagerFactory.getConfigurationManager().getProperty(StandardProperties.HTTP_PROXY_USED));
    }

    public static ProxyCredentials getProxyCredentials() {

        final var credentials = new ProxyCredentials();
        credentials.setProxyAuthenticated(Boolean.parseBoolean(ConfigurationManagerFactory.getConfigurationManager().getProperty(StandardProperties.HTTP_PROXY_USED)));
        credentials.setHostname(ConfigurationManagerFactory.getConfigurationManager().getProperty(StandardProperties.HTTP_PROXY_HOST));
        credentials.setPassword(ConfigurationManagerFactory.getConfigurationManager().getProperty(StandardProperties.HTTP_PROXY_PASSWORD));
        credentials.setPort(ConfigurationManagerFactory.getConfigurationManager().getProperty(StandardProperties.HTTP_PROXY_PORT));
        credentials.setUsername(ConfigurationManagerFactory.getConfigurationManager().getProperty(StandardProperties.HTTP_PROXY_USERNAME));
        return credentials;
    }

    private static String getCommonName(final java.security.cert.X509Certificate cert) {
        return CertUtil.subjectCN(cert);
    }
}

