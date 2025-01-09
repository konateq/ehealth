package eu.europa.ec.sante.openncp.application.client.connector.request;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.Map;

public interface OpenNcpRequest {
    Map<AssertionType, Assertion> getAssertions();

    String getCountryCode();

    PatientId getPatientId();
}
