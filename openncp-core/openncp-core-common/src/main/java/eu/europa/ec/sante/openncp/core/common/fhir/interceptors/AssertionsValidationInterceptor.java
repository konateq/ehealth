package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.common.context.ServerContext;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidationResult;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.AssertionValidator;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Interceptor(order = AssertionsReportingInterceptor.ORDER + 1)
@Component
public class AssertionsValidationInterceptor implements FhirCustomInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionsValidationInterceptor.class);

    private final ServerContext serverContext;
    private final AssertionValidator assertionValidator;

    public AssertionsValidationInterceptor(final AssertionValidator assertionValidator, final ServerContext serverContext) {
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");
        this.assertionValidator = Validate.notNull(assertionValidator, "assertionValidator must not be null");
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void validateAssertions(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails, final RestOperationTypeEnum restOperationTypeEnum) {
        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final AuditSecurityInfo auditSecurityInfo = (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();

        final SamlDetails samlDetails = auditSecurityInfo.getSamlDetails();
        samlDetails.getAssertionDetails(AssertionType.HCP).orElseThrow(() -> new AuthenticationException("No valid HCP assertion found"));

        final List<AssertionValidationResult> validationResults = assertionValidator.validate(samlDetails.getAssertions());
        final List<String> failedValidationMessages = validationResults.stream().flatMap(assertionValidationResult -> assertionValidationResult.getFailedValidationMessages().stream()).collect(Collectors.toList());
        if (!failedValidationMessages.isEmpty()) {
            throw new AuthenticationException(String.format("Validation failed for assertions with the following details: %s", StringUtils.join(failedValidationMessages, "\n")));
        }
    }
}
