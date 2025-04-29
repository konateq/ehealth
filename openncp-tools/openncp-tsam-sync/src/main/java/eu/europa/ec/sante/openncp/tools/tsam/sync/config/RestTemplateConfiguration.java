package eu.europa.ec.sante.openncp.tools.tsam.sync.config;

import eu.europa.ec.sante.openncp.tools.tsam.sync.cts.support.SimpleClientHttpRequestFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

    private final ProxyProperties proxyProperties;

    public RestTemplateConfiguration(ProxyProperties proxyProperties) {
        this.proxyProperties = proxyProperties;
    }

    @Bean(destroyMethod="")
    public HttpClient httpClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        if (!StringUtils.isEmpty(proxyProperties.getHost())) {
            int port = -1;
            if (proxyProperties.getPort() != null) {
                port = proxyProperties.getPort();
            }
            HttpHost proxy = new HttpHost(proxyProperties.getHost(), port);
            builder.setProxy(proxy);

            if (!StringUtils.isEmpty(proxyProperties.getUsername()) && !StringUtils.isEmpty(proxyProperties.getPassword())) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(proxy),
                        new UsernamePasswordCredentials(proxyProperties.getUsername(), proxyProperties.getPassword()));
                builder.setDefaultCredentialsProvider(credentialsProvider);
            }
        }

        return builder.build();
    }

    @Bean(destroyMethod="")
    public RestTemplate restTemplate() {
        return new RestTemplate(new SimpleClientHttpRequestFactory(httpClient()));
    }
}
