package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.sante.openncp.common.context.LogContext;
import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.security.exception.SMgrException;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.fhir.context.JwtToken;
import eu.europa.ec.sante.openncp.core.common.fhir.security.TokenProvider;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InsufficientRightsException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.InvalidFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.MissingFieldException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.exceptions.XSDValidationException;
import eu.europa.ec.sante.openncp.core.common.ihe.assertionvalidator.saml.SAML2Validator;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

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
        final Map<AssertionType, Assertion> assertionMap = new HashMap<>();

        final SamlDetails hcpSamlDetails = SamlDetails.of(jwt.getClaim(AssertionType.HCP.name())).orElseThrow(() -> new RuntimeException("HCP assertion is mandatory."));
        assertionMap.put(AssertionType.HCP, hcpSamlDetails.getAssertion());

        final Optional<SamlDetails> trcSamlDetails = SamlDetails.of(jwt.getClaim(AssertionType.TRC.name()));
        trcSamlDetails.ifPresent(samlDetails -> assertionMap.put(AssertionType.TRC, samlDetails.getAssertion()));

        final Optional<SamlDetails> nokSamlDetails = SamlDetails.of(jwt.getClaim(AssertionType.NOK.name()));
        nokSamlDetails.ifPresent(samlDetails -> assertionMap.put(AssertionType.NOK, samlDetails.getAssertion()));

        validateAssertions(assertionMap);

        final String ipAddress = Objects.requireNonNullElseGet(theRequest.getHeader("X-FORWARDED-FOR"), theRequest::getRemoteAddr);
        final InetAddress hostIp;
        try {
            hostIp = InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }

        final AuditSecurityInfo auditSecurityInfo = AuditSecurityInfo.from(hcpSamlDetails.getAssertion(), hcpSamlDetails.getElement(), ipAddress, hostIp.getHostAddress());
        addAssertionToSecurityContext(auditSecurityInfo);
        LogContext.setAuthorization(jwtToken.map(JwtToken::getAuthorizationHeaderValue).orElse(null));
    }


    public void addAssertionToSecurityContext(final AuditSecurityInfo auditSecurityInfo) {
        final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(auditSecurityInfo.getAssertion().getSubject().getNameID().getValue(), auditSecurityInfo.getAssertion().getIssuer().getValue(), null);
        authentication.setDetails(auditSecurityInfo);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void validateAssertions(final Map<AssertionType, Assertion> assertionMap) {
        if (OpenNCPValidation.isValidationEnable()) {
            OpenNCPValidation.validateHCPAssertion(assertionMap.get(AssertionType.HCP), serverContext.getNcpSide());
            if (assertionMap.containsKey(AssertionType.TRC)) {
                OpenNCPValidation.validateTRCAssertion(assertionMap.get(AssertionType.TRC), serverContext.getNcpSide());
            }
            if (assertionMap.containsKey(AssertionType.NOK)) {
                OpenNCPValidation.validateNOKAssertion(assertionMap.get(AssertionType.NOK), serverContext.getNcpSide());
            }
        }

        try {
            saml2Validator.validateHCPHeader(assertionMap.get(AssertionType.HCP));
        } catch (final MissingFieldException | InsufficientRightsException | InvalidFieldException | SMgrException |
                       XSDValidationException e) {
            throw new AuthenticationException("Invalid SAML token.", e);
        }

        //validate TRC

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

    @Domain
    interface SamlDetails {
        Claim getClaim();

        Element getElement();

        Assertion getAssertion();

        static Optional<SamlDetails> of(final Claim claim) {
            if (claim == null || claim.isNull()) {
                return Optional.empty();
            }

            final Base64.Decoder decoder = Base64.getDecoder();

            final String claimAsString = claim.asString();
            final String decodedSaml = new String(decoder.decode(claimAsString));

            final Element samlElement;
            try {
                final BasicParserPool ppMgr = new BasicParserPool();
                ppMgr.setNamespaceAware(true);
                if (!ppMgr.isInitialized()) {
                    ppMgr.initialize();
                }
                final InputStream in = new ByteArrayInputStream(decodedSaml.getBytes());
                final Document samlas;
                samlas = ppMgr.parse(in);
                samlElement = samlas.getDocumentElement();
            } catch (final XMLParserException | ComponentInitializationException ex) {
                throw new AuthenticationException(Msg.code(333) + ex.getMessage());
            }

            final Assertion assertion;
            try {
                final UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
                final Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(samlElement);
                assertion = (Assertion) unmarshaller.unmarshall(samlElement);
            } catch (final UnmarshallingException ex) {
                throw new AuthenticationException(Msg.code(333) + ex.getMessage());
            }

            return Optional.of(ImmutableSamlDetails.builder()
                    .claim(claim)
                    .element(samlElement)
                    .assertion(assertion)
                    .build());
        }
    }
}
