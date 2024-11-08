package eu.europa.ec.sante.openncp.application.client.config;

import eu.europa.ec.sante.openncp.api.client.interceptor.AssertionsInInterceptor;
import eu.europa.ec.sante.openncp.api.client.interceptor.SamlAssertionInterceptor;
import eu.europa.ec.sante.openncp.api.client.interceptor.TransportTokenInInterceptor;
import eu.europa.ec.sante.openncp.core.client.api.ClientServicePortType;
import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

@Configuration
public class WebServiceConfig {

    @Bean
    public LoggingFeature loggingFeature() {
        final LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        loggingFeature.setVerbose(true);
        return loggingFeature;
    }

    @Bean
    public Endpoint endpoint(final Bus bus, final ClientServicePortType clientConnectorServicePortType,
                             final LoggingFeature loggingFeature) {
        final EndpointImpl endpoint = new EndpointImpl(bus, clientConnectorServicePortType);
        endpoint.getFeatures().add(loggingFeature);
        endpoint.getFeatures().add(new WSAddressingFeature());

        endpoint.getProperties().put(SecurityConstants.SIGNATURE_PROPERTIES, "signature.properties");

        endpoint.getInInterceptors().add(new AssertionsInInterceptor());
        endpoint.getInInterceptors().add(new TransportTokenInInterceptor());
        endpoint.getInInterceptors().add(new SamlAssertionInterceptor());

        endpoint.publish("/" + clientConnectorServicePortType.getClass().getAnnotation(WebService.class).serviceName());

        return endpoint;
    }
}
