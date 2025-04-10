package eu.europa.ec.sante.openncp.common.util;

import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManagerFactory;
import eu.europa.ec.sante.openncp.common.configuration.StandardProperties;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import eu.europa.ec.sante.openncp.common.security.SslUtil;
import eu.europa.ec.sante.openncp.common.util.proxy.CustomProxySelector;
import eu.europa.ec.sante.openncp.common.util.proxy.ProxyCredentials;
import org.cryptacular.util.CertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;

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


    public static String getTlsCertificateCommonName(final CertificatesDataHolder.CertificateData certificateData, final String host) {
        final Certificate[] certificates = getSSLPeerCertificate(certificateData, host, false);
        if (certificates != null && certificates.length > 0) {
            final X509Certificate cert = (X509Certificate) certificates[0];
            return getCommonName(cert);
        }
        return WARNING_NO_CERTIFICATE_FOUND;
    }

    public static String getTlsCertificateCommonName(final String host) {
        final CertificatesDataHolder.CertificateData certificateData = ImmutableCertificateData.builder()
                .keystorePath(Constants.SC_KEYSTORE_PATH)
                .keystorePassword(Constants.SC_KEYSTORE_PASSWORD)
                .privateKeyAlias(Constants.SC_PRIVATEKEY_ALIAS)
                .privateKeyPassword(Constants.SC_PRIVATEKEY_PASSWORD)
                .build();

        return getTlsCertificateCommonName(certificateData, host);
    }

    private static Certificate[] getSSLPeerCertificate(final CertificatesDataHolder.CertificateData certificateData, final String host, final boolean sslValidation) {

        HttpsURLConnection urlConnection = null;

        if (!sslValidation) {

            final var trustAllCerts = new TrustManager[]{new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] certificates, final String authType) {
                    try {
                        if (this.getAcceptedIssuers().length > 0) {
                            this.getAcceptedIssuers()[0].checkValidity();
                        }
                    } catch (final CertificateExpiredException | CertificateNotYetValidException e) {
                        if (this.getAcceptedIssuers().length > 0) {
                            LOGGER.error("Error: Invalid server certificate : {} : {}", this.getAcceptedIssuers()[0].getSubjectDN().getName(), authType);
                        } else {
                            LOGGER.error("Error: Invalid server certificate : UNKNOWN : {}", authType);
                        }
                    }
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] arg0, final String arg1) {
                    try {
                        if (this.getAcceptedIssuers().length > 0) {
                            this.getAcceptedIssuers()[0].checkValidity();
                        }
                    } catch (final CertificateExpiredException | CertificateNotYetValidException e) {
                        if (this.getAcceptedIssuers().length > 0) {
                            LOGGER.error("Error: Invalid server certificate : {} : {}", this.getAcceptedIssuers()[0].getSubjectDN().getName(), arg1);
                        } else {
                            LOGGER.error("Error: Invalid server certificate : UNKNOWN : {}", arg1);
                        }
                    }
                }
            }
            };


            try (final var keystoreInputStream = getKeystoreInputStream(certificateData.getKeystorePath())) {

                // Install the all-trusting trust manager
                final var keyStore = KeyStore.getInstance("JKS");
                keyStore.load(keystoreInputStream, certificateData.getKeystorePassword().toCharArray());
                final var keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                keyManagerFactory.init(keyStore, certificateData.getKeystorePassword().toCharArray());

                // Install the all-trusting trust manager
                final SSLContext sslContext;
                sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(keyManagerFactory.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

                final URL url;
                url = new URL(host);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setHostnameVerifier((hostname, session) -> session.isValid() && !hostname.isEmpty());
                urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                urlConnection.connect();
                return urlConnection.getServerCertificates();

            } catch (final IOException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException
                           | KeyManagementException | CertificateException e) {
                LOGGER.error("Error: '{}'", e.getMessage(), e);
            } finally {

                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
        return new Certificate[]{};
    }

    private static InputStream getKeystoreInputStream(final String location) {

        try {
            final var file = new File(location);
            if (file.exists()) {
                return new FileInputStream(file);
            }
            final var url = new URL(location);
            return url.openStream();

        } catch (final Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }

        LOGGER.warn("Could not open stream to: '{}'", location);
        return null;
    }

    public static String getCommonNameFromServerCertificate(final String endpoint) {

        LOGGER.debug("Trying to find certificate from : '{}'", endpoint);
        String result = WARNING_NO_CERTIFICATE_FOUND;
        HttpsURLConnection urlConnection = null;

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
        } catch (final IOException e) {
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


    public CustomProxySelector setCustomProxyServerForURLConnection() {

        final CustomProxySelector customProxySelector;
        if (isBehindProxy()) {
            final var proxyCredentials = getProxyCredentials();
            customProxySelector = new CustomProxySelector(ProxySelector.getDefault(), proxyCredentials);
            return customProxySelector;
        }
        return null;
    }
}

