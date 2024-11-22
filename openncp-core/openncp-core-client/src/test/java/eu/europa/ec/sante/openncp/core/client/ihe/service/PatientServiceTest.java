package eu.europa.ec.sante.openncp.core.client.ihe.service;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.ec.sante.openncp.common.error.OpenNCPErrorCode;
import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.ihe.xca.XcaInitGateway;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.OrCDDocumentMetaData;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xsd.ihe.iti.xds_b._2007.RetrieveDocumentSetResponseType;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCAException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;

@RunWith(MockitoJUnitRunner.class)
public class PatientServiceTest {
    @InjectMocks
    private PatientService patientService;

    @Mock
    private XcaInitGateway xcaInitGateway;

    /**
     * Method under test:
     * {@link PatientService#retrieve(XDSDocument, String, String, String, Map)}
     */
    @Test
    public void testRetrieve() throws XCAException, UnsupportedEncodingException {
        // Arrange
        RetrieveDocumentSetResponseType.DocumentResponse documentResponse = new RetrieveDocumentSetResponseType.DocumentResponse();
        documentResponse.setDocument("AXAXAXAX".getBytes("UTF-8"));
        documentResponse.setDocumentUniqueId("42");
        documentResponse.setHomeCommunityId("42");
        documentResponse.setMimeType("42");
        documentResponse.setRepositoryUniqueId("42");
        when(xcaInitGateway.crossGatewayRetrieve(Mockito.<XDSDocument>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<String>any(), Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any()))
                .thenReturn(documentResponse);

        GenericDocumentCode classCode = new GenericDocumentCode();
        classCode.setSchema("Schema");
        classCode.setValue("42");

        GenericDocumentCode formatCode = new GenericDocumentCode();
        formatCode.setSchema("Schema");
        formatCode.setValue("42");

        XDSDocument document = new XDSDocument();
        document.setAtcCode("Atc Code");
        document.setAtcText("Atc Text");
        document.setAuthors(new ArrayList<>());
        document.setClassCode(classCode);
        document.setCreationTime("Creation Time");
        document.setDescription("The characteristics of someone or something");
        document.setDispensable(true);
        document.setDocumentUniqueId("42");
        document.setDoseFormCode("Dose Form Code");
        document.setDoseFormText("Dose Form Text");
        document.setEventTime("Event Time");
        document.setFormatCode(formatCode);
        document.setHcid("Hcid");
        document.setHealthcareFacility("Healthcare Facility");
        document.setId("42");
        document.setMimeType("Mime Type");
        document.setName("Name");
        document.setPDF(true);
        document
                .setReasonOfHospitalisation(new OrCDDocumentMetaData.ReasonOfHospitalisation("Code", "Coding Scheme", "Text"));
        document.setRepositoryUniqueId("42");
        document.setSize("Size");
        document.setStrength("Strength");
        document.setSubstitution("Substitution");

        // Act
        RetrieveDocumentSetResponseType.DocumentResponse actualRetrieveResult = patientService.retrieve(document, "42",
                "GB", "en", new HashMap<>());

        // Assert
        verify(xcaInitGateway).crossGatewayRetrieve(isA(XDSDocument.class), eq("42"), eq("GB"), eq("en"), isA(Map.class),
                eq("PatientService"));
        assertSame(documentResponse, actualRetrieveResult);
    }

    /**
     * Method under test:
     * {@link PatientService#retrieve(XDSDocument, String, String, String, Map)}
     */
    @Test
    public void testRetrieve2() throws XCAException {
        // Arrange
        when(xcaInitGateway.crossGatewayRetrieve(Mockito.<XDSDocument>any(), Mockito.<String>any(), Mockito.<String>any(),
                Mockito.<String>any(), Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any()))
                .thenThrow(new XCAException(OpenNCPErrorCode.ERROR_GENERIC, "An error occurred", "Context"));

        GenericDocumentCode classCode = new GenericDocumentCode();
        classCode.setSchema("Schema");
        classCode.setValue("42");

        GenericDocumentCode formatCode = new GenericDocumentCode();
        formatCode.setSchema("Schema");
        formatCode.setValue("42");

        XDSDocument document = new XDSDocument();
        document.setAtcCode("Atc Code");
        document.setAtcText("Atc Text");
        document.setAuthors(new ArrayList<>());
        document.setClassCode(classCode);
        document.setCreationTime("Creation Time");
        document.setDescription("The characteristics of someone or something");
        document.setDispensable(true);
        document.setDocumentUniqueId("42");
        document.setDoseFormCode("Dose Form Code");
        document.setDoseFormText("Dose Form Text");
        document.setEventTime("Event Time");
        document.setFormatCode(formatCode);
        document.setHcid("Hcid");
        document.setHealthcareFacility("Healthcare Facility");
        document.setId("42");
        document.setMimeType("Mime Type");
        document.setName("Name");
        document.setPDF(true);
        document
                .setReasonOfHospitalisation(new OrCDDocumentMetaData.ReasonOfHospitalisation("Code", "Coding Scheme", "Text"));
        document.setRepositoryUniqueId("42");
        document.setSize("Size");
        document.setStrength("Strength");
        document.setSubstitution("Substitution");

        // Act and Assert
        assertThrows(XCAException.class, () -> patientService.retrieve(document, "42", "GB", "en", new HashMap<>()));
        verify(xcaInitGateway).crossGatewayRetrieve(isA(XDSDocument.class), eq("42"), eq("GB"), eq("en"), isA(Map.class),
                eq("PatientService"));
    }
}
