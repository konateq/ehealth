package eu.europa.ec.sante.openncp.core.common.fhir.config;

import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<HandlerInterceptor> interceptors;

    public WebConfig(final List<HandlerInterceptor> interceptors) {
        this.interceptors = Validate.notNull(interceptors, "interceptors must not be null");
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        interceptors.forEach(interceptor ->
                registry.addInterceptor(interceptor).addPathPatterns("/dicom/**")
        );
    }
}
