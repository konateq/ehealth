package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.validation.GazelleValidation;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.common.context.ServerContext;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidator;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Interceptor(order = AssertionsReportingInterceptor.ORDER)
@Component
public class AssertionsReportingInterceptor implements FhirCustomInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionsReportingInterceptor.class);

    private final ServerContext serverContext;
    private final AssertionValidator assertionValidator;

    public static final int ORDER = JwtSamlInterceptor.ORDER + 1;

    public AssertionsReportingInterceptor(final AssertionValidator assertionValidator, final ServerContext serverContext) {
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");
        this.assertionValidator = Validate.notNull(assertionValidator, "assertionValidator must not be null");
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void validateAssertions(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails, final RestOperationTypeEnum restOperationTypeEnum) {
        if (!GazelleValidation.isValidationEnable()) {
            LOGGER.info("OpenNCP for [{}] gazelle validation is disabled, not reporting any assertions.", serverContext.getNcpSide());
            return;
        }

        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final AuditSecurityInfo auditSecurityInfo = (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();
        final SamlDetails samlDetails = auditSecurityInfo.getSamlDetails();

        samlDetails.getAssertionDetails(AssertionType.HCP)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> GazelleValidation.logAndValidateHCPAssertion(assertion, NcpSide.NCP_B));
        samlDetails.getAssertionDetails(AssertionType.TRC)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> GazelleValidation.validateTRCAssertion(assertion, NcpSide.NCP_B));
        samlDetails.getAssertionDetails(AssertionType.NOK)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> GazelleValidation.validateNoKAssertion(assertion, NcpSide.NCP_B));
    }
}
