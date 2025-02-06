package eu.europa.ec.sante.openncp.core.client.ihe.service;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.core.client.ihe.xcpd.XcpdInitGateway;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IdentificationServiceTest {
    @InjectMocks
    private IdentificationService identificationService;

    @Mock
    private XcpdInitGateway xcpdInitGateway;

    /**
     * Method under test:
     * {@link IdentificationService#findIdentityByTraits(PatientDemographics, Map, String)}
     */
    @Test
    public void testFindIdentityByTraits() throws NoPatientIdDiscoveredException, ParseException {
        // Arrange
        ArrayList<PatientDemographics> patientDemographicsList = new ArrayList<>();


        PatientDemographics patient = new PatientDemographics();
        patient.setAdministrativeGender(PatientDemographics.Gender.FEMALE);
        patient.setBirthDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        patient.setCity("Oxford");
        patient.setCountry("GB");
        patient.setEmail("jane.doe@example.org");
        patient.setFamilyName("Family Name");
        patient.setGivenName("Given Name");
        patient.setHomeCommunityId("42");
        patient.setId("42");
        patient.setIdList(new ArrayList<>());
        patient.setPostalCode("Postal Code");
        patient.setStreetAddress("42 Main St");
        patient.setTelephone("6625550144");

        when(xcpdInitGateway.patientDiscovery(patient,
                new HashMap<>(), "GB")).thenReturn(patientDemographicsList);

        // Act
        List<PatientDemographics> actualFindIdentityByTraitsResult = identificationService.findIdentityByTraits(patient,
                new HashMap<>(), "GB");

        // Assert
        verify(xcpdInitGateway).patientDiscovery(isA(PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualFindIdentityByTraitsResult.isEmpty());
        assertSame(patientDemographicsList, actualFindIdentityByTraitsResult);
    }

    /**
     * Method under test:
     * {@link IdentificationService#findIdentityByTraits(PatientDemographics, Map, String)}
     */
    @Test
    public void testFindIdentityByTraitsNoPatient() throws NoPatientIdDiscoveredException, ParseException {
        PatientDemographics patient = new PatientDemographics();
        patient.setAdministrativeGender(PatientDemographics.Gender.FEMALE);
        patient.setBirthDate(Date.from(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()));
        patient.setCity("Oxford");
        patient.setCountry("GB");
        patient.setEmail("jane.doe@example.org");
        patient.setFamilyName("Family Name");
        patient.setGivenName("Given Name");
        patient.setHomeCommunityId("42");
        patient.setId("42");
        patient.setIdList(new ArrayList<>());
        patient.setPostalCode("Postal Code");
        patient.setStreetAddress("42 Main St");
        patient.setTelephone("6625550144");

        when(xcpdInitGateway.patientDiscovery(patient,
                new HashMap<>(), "GB")).thenThrow(new NoPatientIdDiscoveredException(OpenNCPErrorCode.ERROR_GENERIC, "An error occurred"));


        assertThrows(NoPatientIdDiscoveredException.class,
                () -> identificationService.findIdentityByTraits(patient, new HashMap<>(), "GB"));
        verify(xcpdInitGateway).patientDiscovery(isA(PatientDemographics.class), isA(Map.class), eq("GB"));
    }
}
