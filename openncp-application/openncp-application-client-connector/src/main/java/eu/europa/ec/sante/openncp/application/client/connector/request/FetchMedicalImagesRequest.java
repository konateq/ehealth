package eu.europa.ec.sante.openncp.application.client.connector.request;

import eu.europa.ec.sante.openncp.common.immutables.Domain;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.Map;
import java.util.Optional;

@Domain
public interface FetchMedicalImagesRequest extends OpenNcpRequest {
    @Override
    Map<AssertionType, Assertion> getAssertions();

    @Override
    String getCountryCode();

    @Override
    PatientId getPatientId();

    String getStudyUid();

    Optional<String> getSeriesUid();

    Optional<String> getInstanceUid();
}
