package eu.europa.ec.sante.openncp.application.client.config;

import eu.europa.ec.sante.openncp.api.client.interceptor.AssertionReportingInterceptor;
import eu.europa.ec.sante.openncp.api.client.interceptor.*;
import eu.europa.ec.sante.openncp.api.client.interceptor.HcpAssertionValidationInterceptor;
import eu.europa.ec.sante.openncp.common.Constant;
import eu.europa.ec.sante.openncp.common.configuration.ConfigurationManager;
import eu.europa.ec.sante.openncp.core.client.api.ClientServicePortType;
import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.wss4j.common.crypto.JasyptPasswordEncryptor;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

@Configuration
public class WebServiceConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceConfig.class);

    @Bean
    public LoggingFeature loggingFeature() {
        final LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        loggingFeature.setVerbose(true);
        return loggingFeature;
    }

    @Bean
    public PasswordEncryptor passwordEncryptor() {
        // We can just use a random password since this class is used to encrypt and decrypt the actual password.
        return new JasyptPasswordEncryptor(UUID.randomUUID().toString());
    }


    @Bean
    public Merlin signatureCrypto(final ConfigurationManager configurationManager, final PasswordEncryptor passwordEncryptor) throws WSSecurityException, IOException {
        final Properties cryptoProperties = new Properties();
        cryptoProperties.put("org.apache.ws.security.crypto.merlin.truststore.file",
                configurationManager.getProperty(Constant.TRUSTSTORE_PATH));
        cryptoProperties.put("org.apache.ws.security.crypto.merlin.truststore.type", "jks");
        cryptoProperties.put("org.apache.ws.security.crypto.merlin.truststore.password",
                configurationManager.getProperty(Constant.TRUSTSTORE_PASSWORD));
        return new Merlin(cryptoProperties, Thread.currentThread().getContextClassLoader(), passwordEncryptor);
    }

    @Bean
    public Endpoint endpoint(final Bus bus,
                             final ClientServicePortType clientConnectorServicePortType,
                             final LoggingFeature loggingFeature,
                             final AssertionValidationInterceptor assertionValidationInterceptor,
                             final Merlin signatureCrypto) {
        final EndpointImpl endpoint = new EndpointImpl(bus, clientConnectorServicePortType);
        endpoint.getFeatures().add(loggingFeature);
        endpoint.getFeatures().add(new WSAddressingFeature());

        endpoint.getProperties().put(SecurityConstants.SIGNATURE_CRYPTO, signatureCrypto);
        endpoint.getInInterceptors().add(new AssertionsInInterceptor());
        endpoint.getInInterceptors().add(new TransportTokenInInterceptor());
        endpoint.getInInterceptors().add(new AssertionReportingInterceptor());
        endpoint.getInInterceptors().add(new AssertionReportingInterceptor());
        endpoint.getInInterceptors().add(assertionValidationInterceptor);
        endpoint.getOutFaultInterceptors().add(new MyHealthEuSoapFaultInterceptor());
        endpoint.publish("/" + clientConnectorServicePortType.getClass().getAnnotation(WebService.class).serviceName());

        return endpoint;
    }
}
