package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Sets a correlation id to the request header that should propagate through all the connected services.
 */
@Interceptor(order = Integer.MIN_VALUE + 1)
@Component
public class CountryCodeInterceptor implements FhirCustomInterceptor, IClientInterceptor, HandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryCodeInterceptor.class);
    public static final String COUNTRY_CODE_HEADER_KEY = "CountryCode";

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public void setIncomingCountryCodeToLogContext(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
        final String countryCode = httpServletRequest.getHeader(COUNTRY_CODE_HEADER_KEY);
        LogContext.setCountryCode(countryCode);
        httpServletResponse.setHeader(COUNTRY_CODE_HEADER_KEY, StringEscapeUtils.escapeJava(LogContext.getCountryCode()));
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {
        setIncomingCountryCodeToLogContext(request, response);
        return true;
    }

    @Override
    public void interceptRequest(final IHttpRequest theRequest) {
        theRequest.addHeader(COUNTRY_CODE_HEADER_KEY, StringEscapeUtils.escapeJava(LogContext.getCountryCode()));
    }

    @Override
    public void interceptResponse(final IHttpResponse theResponse) throws IOException {

    }
}
