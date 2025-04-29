package eu.europa.ec.sante.openncp.config;

import eu.europa.ec.sante.openncp.sts.NextOfKinService;
import eu.europa.ec.sante.openncp.sts.STSService;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;

@Configuration
public class WebServiceConfig {

    private static final String BINDING_URI = "http://www.w3.org/2003/05/soap/bindings/HTTP/";

    @Bean(name = Bus.DEFAULT_BUS_ID, destroyMethod="")
    public SpringBus springBus() {
        var springBus = new SpringBus();
        springBus.getFeatures().add(new LoggingFeature());
        return springBus;
    }

//    @Bean
//    public JaxWsProxyFactoryBean jaxWsProxyFactoryBean() {
//        final JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
//        factory.setFeatures(Collections.singletonList(new LoggingFeature()));
//        return factory;
//    }

    @Bean(destroyMethod="")
    public Endpoint endpoint(Bus bus, STSService stsService) {
        EndpointImpl endpoint = new EndpointImpl(bus, stsService);
        endpoint.setBindingUri(BINDING_URI);
        endpoint.publish("/SecurityTokenService");
        return endpoint;
    }

    @Bean(destroyMethod="")
    public Endpoint endpoint2(Bus bus, NextOfKinService nokService) {
        EndpointImpl endpoint = new EndpointImpl(bus, nokService);
        endpoint.setBindingUri(BINDING_URI);
        endpoint.publish("/NextOfKinTokenService");
        return endpoint;
    }
}
