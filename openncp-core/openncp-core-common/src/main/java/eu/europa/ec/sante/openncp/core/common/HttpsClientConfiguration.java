package eu.europa.ec.sante.openncp.core.common;

import eu.europa.ec.sante.openncp.common.audit.ssl.ImmutableKeystoreDetails;
import eu.europa.ec.sante.openncp.common.audit.ssl.KeystoreDetails;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class HttpsClientConfiguration {

    private HttpsClientConfiguration() {
    }

    public static SSLContext buildSSLContext() throws NoSuchAlgorithmException, KeyManagementException, IOException,
            CertificateException, KeyStoreException, UnrecoverableKeyException {

        final KeystoreDetails serviceConsumerKeystoreDetails = ImmutableKeystoreDetails.builder()
                .keystoreLocation(Constants.SC_KEYSTORE_PATH)
                .keystorePassword(Constants.SC_KEYSTORE_PASSWORD)
                .alias(Constants.SC_PRIVATEKEY_ALIAS)
                .keyPassword(Constants.SC_PRIVATEKEY_PASSWORD)
                .build();

        final KeystoreDetails trustStoreKeystoreDetails = ImmutableKeystoreDetails.builder()
                .keystoreLocation(Constants.TRUSTSTORE_PATH)
                .keystorePassword(Constants.TRUSTSTORE_PASSWORD)
                .build();

        return SslContextBuilder.build(serviceConsumerKeystoreDetails, trustStoreKeystoreDetails);
    }


    public static HttpClient getDefaultSSLClient() throws UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {

        final SSLConnectionSocketFactory sslConnectionSocketFactory = buildSSLConnectionSocketFactory();
        final HttpClientBuilder builder = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).setUserAgent("OpenNCP http client");
        
        return builder.build();
    }

    public static SSLConnectionSocketFactory buildSSLConnectionSocketFactory() throws UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {

        final SSLContext sslContext = buildSSLContext();
        return new SSLConnectionSocketFactory(
                sslContext, new String[]{"TLSv1.2", "TLSv1.3"}, null, NoopHostnameVerifier.INSTANCE);
    }
}
