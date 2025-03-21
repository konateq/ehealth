package eu.europa.ec.sante.openncp.api.client.interceptor;

import eu.europa.ec.sante.openncp.common.NcpSide;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.common.validation.OpenNCPValidation;
import eu.europa.ec.sante.openncp.core.common.AssertionDetails;
import eu.europa.ec.sante.openncp.core.common.SamlDetails;
import eu.europa.ec.sante.openncp.core.common.SecurityContext;
import eu.europa.ec.sante.openncp.core.common.SecurityContextProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor that will report all the assertions present on the request if needed by the configuration.
 */
public class AssertionReportingInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssertionReportingInterceptor.class);

    public AssertionReportingInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    public void handleMessage(final Message message) {
        if (!OpenNCPValidation.isValidationEnable()) {
            LOGGER.info("OpenNCP validation is disabled, not reporting any assertions.");
            return;
        }

        LOGGER.info("Reporting assertions");
        final SecurityContext securityContext = SecurityContextProvider.getSecurityContext().orElseThrow(() -> new RuntimeException("AssertionContext is null"));
        final SamlDetails samlDetails = securityContext.getSamlDetails().orElseThrow(() -> new RuntimeException("SamlDetails is null"));

        samlDetails.getAssertion(AssertionType.HCP)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> OpenNCPValidation.validateHCPAssertion(assertion, NcpSide.NCP_B));
        samlDetails.getAssertion(AssertionType.TRC)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> OpenNCPValidation.validateTRCAssertion(assertion, NcpSide.NCP_B));
        samlDetails.getAssertion(AssertionType.NOK)
                .map(AssertionDetails::getAssertion)
                .ifPresent(assertion -> OpenNCPValidation.validateNoKAssertion(assertion, NcpSide.NCP_B));
    }

    public void handleFault(final Message messageParam) {
        //empty
    }
}
