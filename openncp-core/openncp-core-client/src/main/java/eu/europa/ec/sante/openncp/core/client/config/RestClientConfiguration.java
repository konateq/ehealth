package eu.europa.ec.sante.openncp.core.client.config;

import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.audit.ssl.ImmutableKeystoreDetails;
import eu.europa.ec.sante.openncp.common.audit.ssl.KeystoreDetails;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.core.common.SslContextBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

@Configuration
public class RestClientConfiguration {
    @Bean
    public SSLContext sslContextForServiceConsumer(final ConfigurationManager configurationManager) throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException, KeyManagementException {
        final KeystoreDetails serviceConsumerKeystoreDetails = ImmutableKeystoreDetails.builder()
                .keystoreLocation(configurationManager.getProperty(Constant.SC_KEYSTORE_PATH))
                .keystorePassword(configurationManager.getProperty(Constant.SC_KEYSTORE_PASSWORD))
                .alias(configurationManager.getProperty(Constant.SC_PRIVATEKEY_ALIAS))
                .keyPassword(configurationManager.getProperty(Constant.SC_PRIVATEKEY_PASSWORD))
                .build();

        final KeystoreDetails trustStoreKeystoreDetails = ImmutableKeystoreDetails.builder()
                .keystoreLocation(configurationManager.getProperty(Constant.TRUSTSTORE_PATH))
                .keystorePassword(configurationManager.getProperty(Constant.TRUSTSTORE_PASSWORD))
                .build();
        return SslContextBuilder.build(serviceConsumerKeystoreDetails, trustStoreKeystoreDetails);
    }

    @Bean
    @Profile("!production")
    public RestTemplate restTemplateInsecure(@Qualifier("sslContextForServiceConsumer") final SSLContext sslContext) {
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        final SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new NoopHostnameVerifier()
        );

        final PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(
                        RegistryBuilder.<ConnectionSocketFactory>create()
                                .register("https", sslConnectionSocketFactory)
                                .build()
                );

        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);

        final CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .setConnectionManager(connectionManager)
                .build();

        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);

        return new RestTemplate(factory);
    }

    @Bean
    @Profile("production")
    public RestTemplate restTemplateSecure(@Qualifier("sslContextForServiceConsumer") final SSLContext sslContext) {
        final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        // Standard connection pool for production
        final PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);

        // Production HttpClient with default SSL handling
        final CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setConnectionManager(connectionManager)
                .build();

        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);

        return new RestTemplate(factory);
    }
}
