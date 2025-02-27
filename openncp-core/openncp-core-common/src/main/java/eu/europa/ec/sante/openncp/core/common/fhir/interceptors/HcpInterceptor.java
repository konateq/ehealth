package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidationResult;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidator;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Interceptor
@Component
public class HcpInterceptor implements FhirCustomInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HcpInterceptor.class);
    private final ServerContext serverContext;
    private final AssertionValidator assertionValidator;


    public HcpInterceptor(final ServerContext serverContext, final AssertionValidator assertionValidator) {
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");
        this.assertionValidator = Validate.notNull(assertionValidator, "assertionValidator must not be null");
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void validateTrcAssertionIfApplicable(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails, final RestOperationTypeEnum restOperationTypeEnum) {
        final EuRequestDetails euRequestDetails = EuRequestDetails.of(theRequestDetails);
        if (euRequestDetails.getRestOperationType() == RestOperationTypeEnum.METADATA) {
            LOGGER.debug("The request was a METADATA request, skipping HCP assertion verification");
            return;
        }

        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final AuditSecurityInfo auditSecurityInfo = (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();

        final SamlDetails samlDetails = auditSecurityInfo.getSamlDetails();
        final List<AssertionDetails> assertions = samlDetails.getAssertions();
        final AssertionDetails hcpAssertion = samlDetails.getAssertionDetails(AssertionType.HCP).orElseThrow(() -> new AuthenticationException("No valid HCP assertion found"));

        final Optional<AssertionValidationResult> validationResult = assertionValidator.validate(hcpAssertion, assertions);
        if (validationResult.isPresent()) {
            final List<String> failedValidationMessages = validationResult.get().getFailedValidationMessages();
            if (!failedValidationMessages.isEmpty()) {
                throw new AuthenticationException(String.format("Validation failed for assertion [%s]: %s", hcpAssertion.getAssertionType(), StringUtils.join(failedValidationMessages, "\n")));
            }
        }
    }
}
