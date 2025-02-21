package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.SAML2Validator;
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
public class TrcInterceptor implements FhirCustomInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrcInterceptor.class);

    private final SAML2Validator saml2Validator;
    private final ServerContext serverContext;
    private final AssertionValidator assertionValidator;


    public TrcInterceptor(final SAML2Validator saml2Validator, final ServerContext serverContext, final AssertionValidator assertionValidator) {
        this.saml2Validator = Validate.notNull(saml2Validator, "saml2Validator must not be null");
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");
        this.assertionValidator = Validate.notNull(assertionValidator, "assertionValidator must not be null");
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void validateTrcAssertionIfApplicable(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails, final RestOperationTypeEnum restOperationTypeEnum) {
        final EuRequestDetails euRequestDetails = EuRequestDetails.of(theRequestDetails);
        if (euRequestDetails.getRestOperationType() == RestOperationTypeEnum.METADATA) {
            LOGGER.debug("The request was a METADATA request, skipping TRC assertion verification");
            return;
        }

        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final AuditSecurityInfo auditSecurityInfo = (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();

        final Optional<AssertionDetails> assertionDetails = auditSecurityInfo.getSamlDetails().getAssertionDetails(AssertionType.TRC);
        if (assertionDetails.isPresent()) {
            final Optional<AssertionValidationResult> validationResult = assertionValidator.validate(assertionDetails.get());
            if (validationResult.isPresent()) {
                final List<String> failedValidationMessages = validationResult.get().getFailedValidationMessages();
                if (!failedValidationMessages.isEmpty()) {
                    throw new AuthenticationException(String.format("Validation failed for assertion [%s]: %s", assertionDetails.get().getAssertionType(), StringUtils.join(failedValidationMessages, "\n")));
                }
            }
        } else {
            LOGGER.info("No TRC assertion found: {}", euRequestDetails.getResourceType());
        }
    }
}
