package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.common.IpInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpInformationInterceptor extends InterceptorAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(IpInformationInterceptor.class);

    @Override
    public boolean incomingRequestPreProcessed(final HttpServletRequest theRequest, final HttpServletResponse theResponse) {
        LOGGER.info("Extracting the ip information.");

        String requestIp = theRequest.getHeader("X-FORWARDED-FOR");
        if (requestIp == null) {
            requestIp = theRequest.getRemoteAddr();
        }

        InetAddress hostIp = null;
        try {
            hostIp = InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }

        final IpInformation ipInformation = IpInformation.from(requestIp, hostIp.getHostAddress());
        LogContext.setIpInformation(ipInformation);

        return true;
    }
}
