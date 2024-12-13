package eu.europa.ec.sante.openncp.application.client.connector.request;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Domain
public interface DocumentReferenceByIdRequest extends OpenNcpRequest {

    @Override
    Map<AssertionType, Assertion> getAssertions();

    @Override
    String getCountryCode();

    @Override
    PatientId getPatientId();

    String getId();

    default Map<String, Set<String>> getSearchParameters() {
        final Map<String, Set<String>> payload = new HashMap<>();
        payload.put("patient.identifier", Set.of(getPatientId().getRoot() + "|" + getPatientId().getExtension()));
        payload.put("id", Set.of(getId()));
        return payload;
    }
}
