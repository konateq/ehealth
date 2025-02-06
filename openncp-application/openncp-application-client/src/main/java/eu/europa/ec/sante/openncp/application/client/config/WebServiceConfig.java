package eu.europa.ec.sante.openncp.application.client.config;

import eu.europa.ec.sante.openncp.api.client.interceptor.*;
import eu.europa.ec.sante.openncp.core.client.api.ClientServicePortType;
import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

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
    public Endpoint endpoint(final Bus bus,
                             final ClientServicePortType clientConnectorServicePortType,
                             final LoggingFeature loggingFeature,
                             final HcpAssertionValidationInterceptor hcpAssertionValidationInterceptor) {
        final EndpointImpl endpoint = new EndpointImpl(bus, clientConnectorServicePortType);
        endpoint.getFeatures().add(loggingFeature);
        endpoint.getFeatures().add(new WSAddressingFeature());

        endpoint.getProperties().put(SecurityConstants.SIGNATURE_PROPERTIES, "signature.properties");
        endpoint.getInInterceptors().add(new AssertionsInInterceptor());
        endpoint.getInInterceptors().add(new TransportTokenInInterceptor());
        endpoint.getInInterceptors().add(new AssertionReportingInterceptor());
        endpoint.getInInterceptors().add(hcpAssertionValidationInterceptor);

        endpoint.getOutFaultInterceptors().add(new MyHealthEuSoapFaultInterceptor());

        endpoint.publish("/" + clientConnectorServicePortType.getClass().getAnnotation(WebService.class).serviceName());

        return endpoint;
    }
}
