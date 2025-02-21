package eu.europa.ec.sante.openncp.core.common.fhir.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.common.ServerContext;
import eu.europa.ec.sante.openncp.core.common.assertion.saml.SAML2Validator;
import eu.europa.ec.sante.openncp.core.common.assertion.validation.NokAssertionValidation;
import eu.europa.ec.sante.openncp.core.common.fhir.audit.AuditSecurityInfo;
import eu.europa.ec.sante.openncp.core.common.fhir.context.EuRequestDetails;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Interceptor(order = Integer.MIN_VALUE)
@Component
public class NokInterceptor implements FhirCustomInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NokInterceptor.class);

    private final SAML2Validator saml2Validator;
    private final ServerContext serverContext;
    private final NokAssertionValidation nokAssertionValidation;

    public NokInterceptor(final NokAssertionValidation nokAssertionValidation, final ServerContext serverContext) {
        this.nokAssertionValidation = nokAssertionValidation;
        this.saml2Validator = Validate.notNull(saml2Validator, "saml2Validator must not be null");
        this.serverContext = Validate.notNull(serverContext, "serverContext must not be null");
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void validateTrcAssertionIfApplicable(final RequestDetails theRequestDetails, final ServletRequestDetails servletRequestDetails, final RestOperationTypeEnum restOperationTypeEnum) {
        final EuRequestDetails euRequestDetails = EuRequestDetails.of(theRequestDetails);

        final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final AuditSecurityInfo auditSecurityInfo = (AuditSecurityInfo) usernamePasswordAuthenticationToken.getDetails();

        auditSecurityInfo.getSamlDetails().getAssertionDetails(AssertionType.NOK)
                .ifPresent(nokAssertionValidation::validate);
    }
}
