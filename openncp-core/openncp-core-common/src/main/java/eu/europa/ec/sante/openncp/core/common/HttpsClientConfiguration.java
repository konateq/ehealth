package eu.europa.ec.sante.openncp.core.common;

import eu.europa.ec.sante.openncp.common.audit.ssl.ImmutableKeystoreDetails;
import eu.europa.ec.sante.openncp.common.audit.ssl.KeystoreDetails;
import eu.europa.ec.sante.openncp.common.configuration.util.Constants;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.util.ResourceUtils;

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

        final SSLContextBuilder builder = SSLContextBuilder.create();
        builder.setKeyStoreType("JKS");
        builder.setKeyManagerFactoryAlgorithm("SunX509");
        builder.loadKeyMaterial(ResourceUtils.getFile(Constants.SC_KEYSTORE_PATH),
                Constants.SC_KEYSTORE_PASSWORD.toCharArray(),
                Constants.SC_PRIVATEKEY_PASSWORD.toCharArray());
        builder.loadTrustMaterial(ResourceUtils.getFile(Constants.TRUSTSTORE_PATH),
                Constants.TRUSTSTORE_PASSWORD.toCharArray(), TrustAllStrategy.INSTANCE);

        return builder.build();
    }


    public static HttpClient getDefaultSSLClient() throws UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {

        final SSLConnectionSocketFactory sslConnectionSocketFactory = buildSSLConnectionSocketFactory();
        final HttpClientBuilder builder = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).setUserAgent("OpenNCP http client");
        
        return builder.build();
    }

    public static SSLConnectionSocketFactory buildSSLConnectionSocketFactory() throws UnrecoverableKeyException, CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {

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

        final SSLContext sslContext = SslContextBuilder.build(serviceConsumerKeystoreDetails, trustStoreKeystoreDetails);
        return new SSLConnectionSocketFactory(
                sslContext, new String[]{"TLSv1.2", "TLSv1.3"}, null, NoopHostnameVerifier.INSTANCE);
    }
}
