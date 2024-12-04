package eu.europa.ec.sante.openncp.core.common.fhir.audit;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import org.opensaml.saml.saml2.core.Assertion;
import org.w3c.dom.Element;

@Domain
public interface AuditSecurityInfo {

    Assertion getAssertion();

    Element getSamlAsRoot();

    String getRequestIp();

    String getHostIp();

    static AuditSecurityInfo from(final Assertion assertion, final Element samlAsRoot, final String requestIp, final String hostIp) {
        return ImmutableAuditSecurityInfo.builder()
                .assertion(assertion)
                .samlAsRoot(samlAsRoot)
                .requestIp(requestIp)
                .hostIp(hostIp)
                .build();
    }

}
