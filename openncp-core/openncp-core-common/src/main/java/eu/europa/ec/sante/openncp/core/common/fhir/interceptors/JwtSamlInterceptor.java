package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.fhir.context.JwtToken;
import eu.europa.ec.sante.openncp.core.common.fhir.security.TokenProvider;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.XSDValidationException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML2Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.config.InitializationException;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

@Interceptor(order = Integer.MIN_VALUE)
@Component
public class JwtSamlInterceptor implements FhirCustomInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtSamlInterceptor.class);

    private final TokenProvider tokenProvider;
    private final SAML2Validator saml2Validator;
    private final ServerContext serverContext;

    static {
        try {
            org.opensaml.core.config.InitializationService.initialize();
        } catch (final InitializationException e) {
            throw new RuntimeException("Could not initialize the opensaml InitializationService", e);
        }
    }

    public JwtSamlInterceptor(final TokenProvider tokenProvider, final SAML2Validator saml2Validator, final ServerContext serverContext) {
        this.tokenProvider = Validate.notNull(tokenProvider, "tokenProvider must not be null");
        this.saml2Validator = Validate.notNull(saml2Validator, "saml2Validator must not be null");
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public void incomingRequestPreProcessed(final HttpServletRequest theRequest, final HttpServletResponse theResponse) {
        LOGGER.info("Validating the incoming JWT bearer token from the request.");
        final Optional<JwtToken> jwtToken = JwtToken.extractFrom(theRequest);
        if (jwtToken.isEmpty()) {
            LOGGER.error("No jwt token found in request with serverContext [{}] \n the request summary: \n {}", serverContext, getRequestSummary(theRequest));
            throw new AuthenticationException(String.format("A bearer token is mandatory to initiate a request to [%s].", serverContext.getNcpSide().getName()));
        }


        final DecodedJWT jwt = tokenProvider.verifyToken(jwtToken.get().getToken());
        final SamlDetails samlDetails = SamlDetails.of(jwt);

        validateAssertions(samlDetails);

        final String ipAddress = Objects.requireNonNullElseGet(theRequest.getHeader("X-FORWARDED-FOR"), theRequest::getRemoteAddr);
        final InetAddress hostIp;
        try {
            hostIp = InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }

        final AuditSecurityInfo auditSecurityInfo = AuditSecurityInfo.from(samlDetails, ipAddress, hostIp.getHostAddress());
        addAssertionToSecurityContext(auditSecurityInfo);
        LogContext.setAuthorization(jwtToken.map(JwtToken::getAuthorizationHeaderValue).orElse(null));
    }


    public void addAssertionToSecurityContext(final AuditSecurityInfo auditSecurityInfo) {
        final Assertion hcpAssertion = auditSecurityInfo.getSamlDetails().getHcpAssertion()
                .map(AssertionDetails::getAssertion)
                .orElseThrow(() -> new AuthenticationException("A HCP assertion is mandatory."));
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(hcpAssertion.getSubject().getNameID().getValue(), hcpAssertion.getIssuer().getValue(), null);
        authentication.setDetails(auditSecurityInfo);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void validateAssertions(final SamlDetails samlDetails) {
        final Assertion hcpAssertion = samlDetails.getHcpAssertion()
                .map(AssertionDetails::getAssertion)
                .orElseThrow(() -> new AuthenticationException("A HCP assertion is mandatory."));

        if (OpenNCPValidation.isValidationEnable()) {
            OpenNCPValidation.validateHCPAssertion(hcpAssertion, serverContext.getNcpSide());
            samlDetails.getAssertion(AssertionType.TRC)
                    .map(AssertionDetails::getAssertion)
                    .ifPresent(trcAssertion -> OpenNCPValidation.validateTRCAssertion(trcAssertion, serverContext.getNcpSide()));
            samlDetails.getAssertion(AssertionType.NOK)
                    .map(AssertionDetails::getAssertion)
                    .ifPresent(trcAssertion -> OpenNCPValidation.validateNOKAssertion(trcAssertion, serverContext.getNcpSide()));
        }

        try {
            saml2Validator.validateHCPHeader(hcpAssertion);
        } catch (final MissingFieldException | InsufficientRightsException | InvalidFieldException | SMgrException |
                       XSDValidationException e) {
            throw new AuthenticationException("Invalid HCP assertion.", e);
        }
    }

    private String getRequestSummary(final HttpServletRequest request) {
        final StringBuilder summary = new StringBuilder();

        // Basic request details
        summary.append("HTTP Method: ").append(request.getMethod()).append(StringUtils.LF)
                .append("Request URI: ").append(request.getRequestURI()).append(StringUtils.LF)
                .append("Protocol: ").append(request.getProtocol()).append(StringUtils.LF)
                .append("Remote Address: ").append(request.getRemoteAddr()).append(StringUtils.LF + StringUtils.LF);

        // Headers
        summary.append("Headers:\n");
        Collections.list(request.getHeaderNames())
                .forEach(headerName -> {
                    final String headerValues = String.join(",", Collections.list(request.getHeaders(headerName)));
                    summary.append(String.format(" - Header [%s] - Value [%s]", headerName, headerValues)).append(StringUtils.LF);
                });

        // Parameters
        summary.append("\nParameters:\n");
        Collections.list(request.getParameterNames())
                .forEach(paramName -> {
                    final String paramValues = String.join(",", request.getParameterValues(paramName));
                    summary.append(String.format(" - Parameter [%s] - Value [%s]", paramName, paramValues)).append(StringUtils.LF);
                });

        return summary.toString();
    }
}
