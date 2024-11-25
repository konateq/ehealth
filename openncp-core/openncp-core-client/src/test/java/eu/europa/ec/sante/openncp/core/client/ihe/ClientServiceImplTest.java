package eu.europa.ec.sante.openncp.core.client.ihe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.ec.sante.openncp.common.security.AssertionType;
import eu.europa.ec.sante.openncp.core.client.api.DocumentId;
import eu.europa.ec.sante.openncp.core.client.api.EpsosDocument;
import eu.europa.ec.sante.openncp.core.client.api.FilterParams;
import eu.europa.ec.sante.openncp.core.client.api.PatientDemographics;
import eu.europa.ec.sante.openncp.core.client.api.PatientId;
import eu.europa.ec.sante.openncp.core.client.api.QueryDocumentRequest;
import eu.europa.ec.sante.openncp.core.client.api.QueryPatientRequest;
import eu.europa.ec.sante.openncp.core.client.api.ReasonOfHospitalisation;
import eu.europa.ec.sante.openncp.core.client.api.RetrieveDocumentRequest;
import eu.europa.ec.sante.openncp.core.client.api.SubmitDocumentRequest;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.QueryDocumentOperation;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.QueryPatientOperation;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.RetrieveDocumentOperation;
import eu.europa.ec.sante.openncp.core.client.ihe.dto.SubmitDocumentOperation;
import eu.europa.ec.sante.openncp.core.client.ihe.service.DispensationService;
import eu.europa.ec.sante.openncp.core.client.ihe.service.IdentificationService;
import eu.europa.ec.sante.openncp.core.client.ihe.service.OrCDService;
import eu.europa.ec.sante.openncp.core.client.ihe.service.OrderService;
import eu.europa.ec.sante.openncp.core.client.ihe.service.PatientService;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.OrCDDocumentMetaData;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.QueryResponse;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocument;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.xds.XDSDocumentAssociation;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.NoPatientIdDiscoveredException;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.XCAException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;

@RunWith(MockitoJUnitRunner.class)
public class ClientServiceImplTest {
    @InjectMocks
    private ClientServiceImpl clientServiceImpl;

    @Mock
    private DispensationService dispensationService;

    @Mock
    private IdentificationService identificationService;

    @Mock
    private OrCDService orCDService;

    @Mock
    private OrderService orderService;

    @Mock
    private PatientService patientService;

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments() throws XCAException {
        // Arrange
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(new ArrayList<>());
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(), Mockito.<List<GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");

        QueryDocumentRequest queryDocumentRequest = new QueryDocumentRequest();
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act
        List<EpsosDocument> actualQueryDocumentsResult = clientServiceImpl.queryDocuments(queryDocumentOperation);

        // Assert
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("42"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
        assertTrue(actualQueryDocumentsResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments2() {
        // Arrange
        FilterParams value = new FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        PatientId value2 = new PatientId();
        value2.setExtension("42");
        value2.setRoot("42");

        QueryDocumentRequest queryDocumentRequest = new QueryDocumentRequest();
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenThrow(new ClientConnectorException("An error occurred"));
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryDocuments(queryDocumentOperation));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments3() throws XCAException {
        // Arrange
        GenericDocumentCode classCode = new GenericDocumentCode();
        classCode.setSchema("queryDocuments");
        classCode.setValue("42");

        GenericDocumentCode formatCode = new GenericDocumentCode();
        formatCode.setSchema("queryDocuments");
        formatCode.setValue("42");

        XDSDocument cdaPDF = new XDSDocument();
        cdaPDF.setAtcCode("queryDocuments");
        cdaPDF.setAtcText("queryDocuments");
        cdaPDF.setAuthors(new ArrayList<>());
        cdaPDF.setClassCode(classCode);
        cdaPDF.setCreationTime("queryDocuments");
        cdaPDF.setDescription("The characteristics of someone or something");
        cdaPDF.setDispensable(true);
        cdaPDF.setDocumentUniqueId("42");
        cdaPDF.setDoseFormCode("queryDocuments");
        cdaPDF.setDoseFormText("queryDocuments");
        cdaPDF.setEventTime("queryDocuments");
        cdaPDF.setFormatCode(formatCode);
        cdaPDF.setHcid("queryDocuments");
        cdaPDF.setHealthcareFacility("queryDocuments");
        cdaPDF.setId("42");
        cdaPDF.setMimeType("queryDocuments");
        cdaPDF.setName("queryDocuments");
        cdaPDF.setPDF(true);
        cdaPDF.setReasonOfHospitalisation(
                new OrCDDocumentMetaData.ReasonOfHospitalisation("queryDocuments", "queryDocuments", "queryDocuments"));
        cdaPDF.setRepositoryUniqueId("42");
        cdaPDF.setSize("queryDocuments");
        cdaPDF.setStrength("queryDocuments");
        cdaPDF.setSubstitution("queryDocuments");

        GenericDocumentCode classCode2 = new GenericDocumentCode();
        classCode2.setSchema("queryDocuments");
        classCode2.setValue("42");

        GenericDocumentCode formatCode2 = new GenericDocumentCode();
        formatCode2.setSchema("queryDocuments");
        formatCode2.setValue("42");

        XDSDocument cdaXML = new XDSDocument();
        cdaXML.setAtcCode("queryDocuments");
        cdaXML.setAtcText("queryDocuments");
        cdaXML.setAuthors(new ArrayList<>());
        cdaXML.setClassCode(classCode2);
        cdaXML.setCreationTime("queryDocuments");
        cdaXML.setDescription("The characteristics of someone or something");
        cdaXML.setDispensable(true);
        cdaXML.setDocumentUniqueId("42");
        cdaXML.setDoseFormCode("queryDocuments");
        cdaXML.setDoseFormText("queryDocuments");
        cdaXML.setEventTime("queryDocuments");
        cdaXML.setFormatCode(formatCode2);
        cdaXML.setHcid("queryDocuments");
        cdaXML.setHealthcareFacility("queryDocuments");
        cdaXML.setId("42");
        cdaXML.setMimeType("queryDocuments");
        cdaXML.setName("queryDocuments");
        cdaXML.setPDF(true);
        cdaXML.setReasonOfHospitalisation(
                new OrCDDocumentMetaData.ReasonOfHospitalisation("queryDocuments", "queryDocuments", "queryDocuments"));
        cdaXML.setRepositoryUniqueId("42");
        cdaXML.setSize("queryDocuments");
        cdaXML.setStrength("queryDocuments");
        cdaXML.setSubstitution("queryDocuments");

        XDSDocumentAssociation xdsDocumentAssociation = new XDSDocumentAssociation();
        xdsDocumentAssociation.setCdaPDF(cdaPDF);
        xdsDocumentAssociation.setCdaXML(cdaXML);

        ArrayList<XDSDocumentAssociation> documentAssociations = new ArrayList<>();
        documentAssociations.add(xdsDocumentAssociation);

        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(documentAssociations);
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(), Mockito.<List<GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");

        QueryDocumentRequest queryDocumentRequest = new QueryDocumentRequest();
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryDocuments(queryDocumentOperation));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("42"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments4() throws XCAException {
        // Arrange
        GenericDocumentCode classCode = new GenericDocumentCode();
        classCode.setSchema("queryDocuments");
        classCode.setValue("42");

        GenericDocumentCode formatCode = new GenericDocumentCode();
        formatCode.setSchema("queryDocuments");
        formatCode.setValue("42");

        XDSDocument cdaPDF = new XDSDocument();
        cdaPDF.setAtcCode("queryDocuments");
        cdaPDF.setAtcText("queryDocuments");
        cdaPDF.setAuthors(new ArrayList<>());
        cdaPDF.setClassCode(classCode);
        cdaPDF.setCreationTime("queryDocuments");
        cdaPDF.setDescription("The characteristics of someone or something");
        cdaPDF.setDispensable(true);
        cdaPDF.setDocumentUniqueId("42");
        cdaPDF.setDoseFormCode("queryDocuments");
        cdaPDF.setDoseFormText("queryDocuments");
        cdaPDF.setEventTime("queryDocuments");
        cdaPDF.setFormatCode(formatCode);
        cdaPDF.setHcid("queryDocuments");
        cdaPDF.setHealthcareFacility("queryDocuments");
        cdaPDF.setId("42");
        cdaPDF.setMimeType("queryDocuments");
        cdaPDF.setName("queryDocuments");
        cdaPDF.setPDF(true);
        cdaPDF.setReasonOfHospitalisation(
                new OrCDDocumentMetaData.ReasonOfHospitalisation("queryDocuments", "queryDocuments", "queryDocuments"));
        cdaPDF.setRepositoryUniqueId("42");
        cdaPDF.setSize("queryDocuments");
        cdaPDF.setStrength("queryDocuments");
        cdaPDF.setSubstitution("queryDocuments");

        GenericDocumentCode classCode2 = new GenericDocumentCode();
        classCode2.setSchema("queryDocuments");
        classCode2.setValue("42");

        GenericDocumentCode formatCode2 = new GenericDocumentCode();
        formatCode2.setSchema("queryDocuments");
        formatCode2.setValue("42");

        XDSDocument cdaXML = new XDSDocument();
        cdaXML.setAtcCode("queryDocuments");
        cdaXML.setAtcText("queryDocuments");
        cdaXML.setAuthors(new ArrayList<>());
        cdaXML.setClassCode(classCode2);
        cdaXML.setCreationTime("queryDocuments");
        cdaXML.setDescription("The characteristics of someone or something");
        cdaXML.setDispensable(true);
        cdaXML.setDocumentUniqueId("42");
        cdaXML.setDoseFormCode("queryDocuments");
        cdaXML.setDoseFormText("queryDocuments");
        cdaXML.setEventTime("queryDocuments");
        cdaXML.setFormatCode(formatCode2);
        cdaXML.setHcid("queryDocuments");
        cdaXML.setHealthcareFacility("queryDocuments");
        cdaXML.setId("42");
        cdaXML.setMimeType("queryDocuments");
        cdaXML.setName("queryDocuments");
        cdaXML.setPDF(true);
        cdaXML.setReasonOfHospitalisation(
                new OrCDDocumentMetaData.ReasonOfHospitalisation("queryDocuments", "queryDocuments", "queryDocuments"));
        cdaXML.setRepositoryUniqueId("42");
        cdaXML.setSize("queryDocuments");
        cdaXML.setStrength("queryDocuments");
        cdaXML.setSubstitution("queryDocuments");

        XDSDocumentAssociation xdsDocumentAssociation = new XDSDocumentAssociation();
        xdsDocumentAssociation.setCdaPDF(cdaPDF);
        xdsDocumentAssociation.setCdaXML(cdaXML);

        GenericDocumentCode classCode3 = new GenericDocumentCode();
        classCode3.setSchema("{} | {}");
        classCode3.setValue("queryDocuments");

        GenericDocumentCode formatCode3 = new GenericDocumentCode();
        formatCode3.setSchema("{} | {}");
        formatCode3.setValue("queryDocuments");

        XDSDocument cdaPDF2 = new XDSDocument();
        cdaPDF2.setAtcCode("{} | {}");
        cdaPDF2.setAtcText("{} | {}");
        cdaPDF2.setAuthors(new ArrayList<>());
        cdaPDF2.setClassCode(classCode3);
        cdaPDF2.setCreationTime("{} | {}");
        cdaPDF2.setDescription("queryDocuments");
        cdaPDF2.setDispensable(false);
        cdaPDF2.setDocumentUniqueId("queryDocuments");
        cdaPDF2.setDoseFormCode("{} | {}");
        cdaPDF2.setDoseFormText("{} | {}");
        cdaPDF2.setEventTime("{} | {}");
        cdaPDF2.setFormatCode(formatCode3);
        cdaPDF2.setHcid("{} | {}");
        cdaPDF2.setHealthcareFacility("{} | {}");
        cdaPDF2.setId("queryDocuments");
        cdaPDF2.setMimeType("{} | {}");
        cdaPDF2.setName("{} | {}");
        cdaPDF2.setPDF(false);
        cdaPDF2.setReasonOfHospitalisation(
                new OrCDDocumentMetaData.ReasonOfHospitalisation("queryDocuments", "queryDocuments", "queryDocuments"));
        cdaPDF2.setRepositoryUniqueId("queryDocuments");
        cdaPDF2.setSize("{} | {}");
        cdaPDF2.setStrength("{} | {}");
        cdaPDF2.setSubstitution("{} | {}");

        GenericDocumentCode classCode4 = new GenericDocumentCode();
        classCode4.setSchema("{} | {}");
        classCode4.setValue("queryDocuments");

        GenericDocumentCode formatCode4 = new GenericDocumentCode();
        formatCode4.setSchema("{} | {}");
        formatCode4.setValue("queryDocuments");

        XDSDocument cdaXML2 = new XDSDocument();
        cdaXML2.setAtcCode("{} | {}");
        cdaXML2.setAtcText("{} | {}");
        cdaXML2.setAuthors(new ArrayList<>());
        cdaXML2.setClassCode(classCode4);
        cdaXML2.setCreationTime("{} | {}");
        cdaXML2.setDescription("queryDocuments");
        cdaXML2.setDispensable(false);
        cdaXML2.setDocumentUniqueId("queryDocuments");
        cdaXML2.setDoseFormCode("{} | {}");
        cdaXML2.setDoseFormText("{} | {}");
        cdaXML2.setEventTime("{} | {}");
        cdaXML2.setFormatCode(formatCode4);
        cdaXML2.setHcid("{} | {}");
        cdaXML2.setHealthcareFacility("{} | {}");
        cdaXML2.setId("queryDocuments");
        cdaXML2.setMimeType("{} | {}");
        cdaXML2.setName("{} | {}");
        cdaXML2.setPDF(false);
        cdaXML2.setReasonOfHospitalisation(
                new OrCDDocumentMetaData.ReasonOfHospitalisation("queryDocuments", "queryDocuments", "queryDocuments"));
        cdaXML2.setRepositoryUniqueId("queryDocuments");
        cdaXML2.setSize("{} | {}");
        cdaXML2.setStrength("{} | {}");
        cdaXML2.setSubstitution("{} | {}");

        XDSDocumentAssociation xdsDocumentAssociation2 = new XDSDocumentAssociation();
        xdsDocumentAssociation2.setCdaPDF(cdaPDF2);
        xdsDocumentAssociation2.setCdaXML(cdaXML2);

        ArrayList<XDSDocumentAssociation> documentAssociations = new ArrayList<>();
        documentAssociations.add(xdsDocumentAssociation2);
        documentAssociations.add(xdsDocumentAssociation);

        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(documentAssociations);
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(), Mockito.<List<GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");

        QueryDocumentRequest queryDocumentRequest = new QueryDocumentRequest();
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryDocuments(queryDocumentOperation));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("42"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments5() throws XCAException {
        // Arrange
        eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode classCode = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode();
        classCode.setSchema("");
        classCode.setValue("");

        eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode formatCode = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode();
        formatCode.setSchema("");
        formatCode.setValue("");

        XDSDocument cdaPDF = new XDSDocument();
        cdaPDF.setAtcCode("");
        cdaPDF.setAtcText("");
        cdaPDF.setAuthors(new ArrayList<>());
        cdaPDF.setClassCode(classCode);
        cdaPDF.setCreationTime("");
        cdaPDF.setDescription("42");
        cdaPDF.setDispensable(true);
        cdaPDF.setDocumentUniqueId("");
        cdaPDF.setDoseFormCode("");
        cdaPDF.setDoseFormText("");
        cdaPDF.setEventTime("");
        cdaPDF.setFormatCode(formatCode);
        cdaPDF.setHcid("");
        cdaPDF.setHealthcareFacility("");
        cdaPDF.setId("");
        cdaPDF.setMimeType("");
        cdaPDF.setName("");
        cdaPDF.setPDF(true);
        cdaPDF
                .setReasonOfHospitalisation(new OrCDDocumentMetaData.ReasonOfHospitalisation("Code", "Coding Scheme", "Text"));
        cdaPDF.setRepositoryUniqueId("");
        cdaPDF.setSize("");
        cdaPDF.setStrength("");
        cdaPDF.setSubstitution("");

        eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode classCode2 = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode();
        classCode2.setSchema("");
        classCode2.setValue("");

        eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode formatCode2 = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode();
        formatCode2.setSchema("");
        formatCode2.setValue("");

        XDSDocument cdaXML = new XDSDocument();
        cdaXML.setAtcCode("");
        cdaXML.setAtcText("");
        cdaXML.setAuthors(new ArrayList<>());
        cdaXML.setClassCode(classCode2);
        cdaXML.setCreationTime("");
        cdaXML.setDescription("42");
        cdaXML.setDispensable(true);
        cdaXML.setDocumentUniqueId("");
        cdaXML.setDoseFormCode("");
        cdaXML.setDoseFormText("");
        cdaXML.setEventTime("");
        cdaXML.setFormatCode(formatCode2);
        cdaXML.setHcid("");
        cdaXML.setHealthcareFacility("");
        cdaXML.setId("");
        cdaXML.setMimeType("");
        cdaXML.setName("");
        cdaXML.setPDF(true);
        cdaXML
                .setReasonOfHospitalisation(new OrCDDocumentMetaData.ReasonOfHospitalisation("Code", "Coding Scheme", "Text"));
        cdaXML.setRepositoryUniqueId("");
        cdaXML.setSize("");
        cdaXML.setStrength("");
        cdaXML.setSubstitution("");

        XDSDocumentAssociation xdsDocumentAssociation = new XDSDocumentAssociation();
        xdsDocumentAssociation.setCdaPDF(cdaPDF);
        xdsDocumentAssociation.setCdaXML(cdaXML);

        ArrayList<XDSDocumentAssociation> documentAssociations = new ArrayList<>();
        documentAssociations.add(xdsDocumentAssociation);

        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(documentAssociations);
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(),
                Mockito.<List<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");

        eu.europa.ec.sante.openncp.core.client.api.FilterParams filterParams = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        filterParams.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        filterParams.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        filterParams.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId patientId = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        patientId.setExtension("42");
        patientId.setRoot("42");
        QueryDocumentRequest queryDocumentRequest = mock(QueryDocumentRequest.class);
        when(queryDocumentRequest.getFilterParams()).thenReturn(filterParams);
        when(queryDocumentRequest.getPatientId()).thenReturn(patientId);
        when(queryDocumentRequest.getCountryCode()).thenReturn("GB");
        when(queryDocumentRequest.getClassCode()).thenReturn(new ArrayList<>());
        doNothing().when(queryDocumentRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryDocumentRequest)
                .setFilterParams(Mockito.<eu.europa.ec.sante.openncp.core.client.api.FilterParams>any());
        doNothing().when(queryDocumentRequest)
                .setPatientId(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientId>any());
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act
        List<EpsosDocument> actualQueryDocumentsResult = clientServiceImpl.queryDocuments(queryDocumentOperation);

        // Assert
        verify(queryDocumentRequest).getCountryCode();
        verify(queryDocumentRequest).setCountryCode(eq("42"));
        verify(queryDocumentRequest).getClassCode();
        verify(queryDocumentRequest).getFilterParams();
        verify(queryDocumentRequest).getPatientId();
        verify(queryDocumentRequest).setFilterParams(isA(eu.europa.ec.sante.openncp.core.client.api.FilterParams.class));
        verify(queryDocumentRequest).setPatientId(isA(eu.europa.ec.sante.openncp.core.client.api.PatientId.class));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("GB"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
        assertEquals(2, actualQueryDocumentsResult.size());
        EpsosDocument getResult = actualQueryDocumentsResult.get(0);
        List<EpsosDocument> associatedDocuments = getResult.getAssociatedDocuments();
        assertEquals(1, associatedDocuments.size());
        EpsosDocument getResult2 = associatedDocuments.get(0);
        assertEquals("", getResult2.getAtcCode());
        EpsosDocument getResult3 = actualQueryDocumentsResult.get(1);
        List<EpsosDocument> associatedDocuments2 = getResult3.getAssociatedDocuments();
        assertEquals(1, associatedDocuments2.size());
        EpsosDocument getResult4 = associatedDocuments2.get(0);
        assertEquals("", getResult4.getAtcCode());
        assertEquals("", getResult.getAtcCode());
        assertEquals("", getResult3.getAtcCode());
        assertEquals("", getResult2.getAtcText());
        assertEquals("", getResult4.getAtcText());
        assertEquals("", getResult.getAtcText());
        assertEquals("", getResult3.getAtcText());
        assertEquals("", getResult2.getDoseFormCode());
        assertEquals("", getResult4.getDoseFormCode());
        assertEquals("", getResult.getDoseFormCode());
        assertEquals("", getResult3.getDoseFormCode());
        assertEquals("", getResult2.getDoseFormText());
        assertEquals("", getResult4.getDoseFormText());
        assertEquals("", getResult.getDoseFormText());
        assertEquals("", getResult3.getDoseFormText());
        assertEquals("", getResult2.getHcid());
        assertEquals("", getResult4.getHcid());
        assertEquals("", getResult.getHcid());
        assertEquals("", getResult3.getHcid());
        assertEquals("", getResult2.getMimeType());
        assertEquals("", getResult4.getMimeType());
        assertEquals("", getResult.getMimeType());
        assertEquals("", getResult3.getMimeType());
        assertEquals("", getResult2.getRepositoryId());
        assertEquals("", getResult4.getRepositoryId());
        assertEquals("", getResult.getRepositoryId());
        assertEquals("", getResult3.getRepositoryId());
        assertEquals("", getResult2.getStrength());
        assertEquals("", getResult4.getStrength());
        assertEquals("", getResult.getStrength());
        assertEquals("", getResult3.getStrength());
        assertEquals("", getResult2.getSubstitution());
        assertEquals("", getResult4.getSubstitution());
        assertEquals("", getResult.getSubstitution());
        assertEquals("", getResult3.getSubstitution());
        assertEquals("", getResult2.getUuid());
        assertEquals("", getResult4.getUuid());
        assertEquals("", getResult.getUuid());
        assertEquals("", getResult3.getUuid());
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode classCode3 = getResult2.getClassCode();
        assertEquals("", classCode3.getNodeRepresentation());
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode classCode4 = getResult4.getClassCode();
        assertEquals("", classCode4.getNodeRepresentation());
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode classCode5 = getResult.getClassCode();
        assertEquals("", classCode5.getNodeRepresentation());
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode classCode6 = getResult3.getClassCode();
        assertEquals("", classCode6.getNodeRepresentation());
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode formatCode3 = getResult2.getFormatCode();
        assertEquals("", formatCode3.getNodeRepresentation());
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode formatCode4 = getResult4.getFormatCode();
        assertEquals("", formatCode4.getNodeRepresentation());
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode formatCode5 = getResult.getFormatCode();
        assertEquals("", formatCode5.getNodeRepresentation());
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode formatCode6 = getResult3.getFormatCode();
        assertEquals("", formatCode6.getNodeRepresentation());
        assertEquals("", classCode3.getSchema());
        assertEquals("", classCode4.getSchema());
        assertEquals("", classCode5.getSchema());
        assertEquals("", classCode6.getSchema());
        assertEquals("", formatCode3.getSchema());
        assertEquals("", formatCode4.getSchema());
        assertEquals("", formatCode5.getSchema());
        assertEquals("", formatCode6.getSchema());
        assertEquals("", classCode3.getValue());
        assertEquals("", classCode4.getValue());
        assertEquals("", classCode5.getValue());
        assertEquals("", classCode6.getValue());
        assertEquals("", formatCode3.getValue());
        assertEquals("", formatCode4.getValue());
        assertEquals("", formatCode5.getValue());
        assertEquals("", formatCode6.getValue());
        assertEquals("42", getResult2.getDescription());
        assertEquals("42", getResult4.getDescription());
        assertEquals("42", getResult.getDescription());
        assertEquals("42", getResult3.getDescription());
        ReasonOfHospitalisation reasonOfHospitalisation = getResult2.getReasonOfHospitalisation();
        assertEquals("Code", reasonOfHospitalisation.getCode());
        ReasonOfHospitalisation reasonOfHospitalisation2 = getResult4.getReasonOfHospitalisation();
        assertEquals("Code", reasonOfHospitalisation2.getCode());
        ReasonOfHospitalisation reasonOfHospitalisation3 = getResult.getReasonOfHospitalisation();
        assertEquals("Code", reasonOfHospitalisation3.getCode());
        ReasonOfHospitalisation reasonOfHospitalisation4 = getResult3.getReasonOfHospitalisation();
        assertEquals("Code", reasonOfHospitalisation4.getCode());
        assertEquals("Text", reasonOfHospitalisation.getText());
        assertEquals("Text", reasonOfHospitalisation2.getText());
        assertEquals("Text", reasonOfHospitalisation3.getText());
        assertEquals("Text", reasonOfHospitalisation4.getText());
        assertNull(getResult2.getBase64Binary());
        assertNull(getResult4.getBase64Binary());
        assertNull(getResult.getBase64Binary());
        assertNull(getResult3.getBase64Binary());
        assertNull(getResult2.getSubmissionSetId());
        assertNull(getResult4.getSubmissionSetId());
        assertNull(getResult.getSubmissionSetId());
        assertNull(getResult3.getSubmissionSetId());
        assertNull(getResult2.getTitle());
        assertNull(getResult4.getTitle());
        assertNull(getResult.getTitle());
        assertNull(getResult3.getTitle());
        assertNull(getResult2.getSize());
        assertNull(getResult4.getSize());
        assertNull(getResult.getSize());
        assertNull(getResult3.getSize());
        assertNull(getResult2.getCreationDate());
        assertNull(getResult4.getCreationDate());
        assertNull(getResult.getCreationDate());
        assertNull(getResult3.getCreationDate());
        assertNull(getResult2.getEventDate());
        assertNull(getResult4.getEventDate());
        assertNull(getResult.getEventDate());
        assertNull(getResult3.getEventDate());
        assertTrue(getResult2.isDispensable());
        assertTrue(getResult4.isDispensable());
        assertTrue(getResult.isDispensable());
        assertTrue(getResult3.isDispensable());
        assertTrue(getResult2.getAssociatedDocuments().isEmpty());
        assertTrue(getResult4.getAssociatedDocuments().isEmpty());
        assertTrue(getResult2.getAuthors().isEmpty());
        assertTrue(getResult4.getAuthors().isEmpty());
        assertTrue(getResult.getAuthors().isEmpty());
        assertTrue(getResult3.getAuthors().isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments6() throws XCAException {
        // Arrange
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(new ArrayList<>());
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(),
                Mockito.<List<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");
        eu.europa.ec.sante.openncp.core.client.api.FilterParams filterParams = mock(
                eu.europa.ec.sante.openncp.core.client.api.FilterParams.class);
        when(filterParams.getMaximumSize()).thenReturn(null);
        when(filterParams.getCreatedAfter()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(filterParams.getCreatedBefore()).thenReturn(new GregorianCalendar(1, 1, 1));
        doNothing().when(filterParams).setCreatedAfter(Mockito.<Calendar>any());
        doNothing().when(filterParams).setCreatedBefore(Mockito.<Calendar>any());
        doNothing().when(filterParams).setMaximumSize(Mockito.<BigInteger>any());
        filterParams.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        filterParams.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        filterParams.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId patientId = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        patientId.setExtension("42");
        patientId.setRoot("42");
        QueryDocumentRequest queryDocumentRequest = mock(QueryDocumentRequest.class);
        when(queryDocumentRequest.getFilterParams()).thenReturn(filterParams);
        when(queryDocumentRequest.getPatientId()).thenReturn(patientId);
        when(queryDocumentRequest.getCountryCode()).thenReturn("GB");
        when(queryDocumentRequest.getClassCode()).thenReturn(new ArrayList<>());
        doNothing().when(queryDocumentRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryDocumentRequest)
                .setFilterParams(Mockito.<eu.europa.ec.sante.openncp.core.client.api.FilterParams>any());
        doNothing().when(queryDocumentRequest)
                .setPatientId(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientId>any());
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act
        List<EpsosDocument> actualQueryDocumentsResult = clientServiceImpl.queryDocuments(queryDocumentOperation);

        // Assert
        verify(queryDocumentRequest).getCountryCode();
        verify(queryDocumentRequest).setCountryCode(eq("42"));
        verify(filterParams, atLeast(1)).getCreatedAfter();
        verify(filterParams, atLeast(1)).getCreatedBefore();
        verify(filterParams).getMaximumSize();
        verify(filterParams).setCreatedAfter(isA(Calendar.class));
        verify(filterParams).setCreatedBefore(isA(Calendar.class));
        verify(filterParams).setMaximumSize(isA(BigInteger.class));
        verify(queryDocumentRequest).getClassCode();
        verify(queryDocumentRequest).getFilterParams();
        verify(queryDocumentRequest).getPatientId();
        verify(queryDocumentRequest).setFilterParams(isA(eu.europa.ec.sante.openncp.core.client.api.FilterParams.class));
        verify(queryDocumentRequest).setPatientId(isA(eu.europa.ec.sante.openncp.core.client.api.PatientId.class));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("GB"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
        assertTrue(actualQueryDocumentsResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments7() throws XCAException {
        // Arrange
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(new ArrayList<>());
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(),
                Mockito.<List<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");
        eu.europa.ec.sante.openncp.core.client.api.FilterParams filterParams = mock(
                eu.europa.ec.sante.openncp.core.client.api.FilterParams.class);
        when(filterParams.getMaximumSize()).thenReturn(BigInteger.valueOf(1L));
        when(filterParams.getCreatedAfter()).thenReturn(null);
        when(filterParams.getCreatedBefore()).thenReturn(new GregorianCalendar(1, 1, 1));
        doNothing().when(filterParams).setCreatedAfter(Mockito.<Calendar>any());
        doNothing().when(filterParams).setCreatedBefore(Mockito.<Calendar>any());
        doNothing().when(filterParams).setMaximumSize(Mockito.<BigInteger>any());
        filterParams.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        filterParams.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        filterParams.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId patientId = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        patientId.setExtension("42");
        patientId.setRoot("42");
        QueryDocumentRequest queryDocumentRequest = mock(QueryDocumentRequest.class);
        when(queryDocumentRequest.getFilterParams()).thenReturn(filterParams);
        when(queryDocumentRequest.getPatientId()).thenReturn(patientId);
        when(queryDocumentRequest.getCountryCode()).thenReturn("GB");
        when(queryDocumentRequest.getClassCode()).thenReturn(new ArrayList<>());
        doNothing().when(queryDocumentRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryDocumentRequest)
                .setFilterParams(Mockito.<eu.europa.ec.sante.openncp.core.client.api.FilterParams>any());
        doNothing().when(queryDocumentRequest)
                .setPatientId(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientId>any());
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act
        List<EpsosDocument> actualQueryDocumentsResult = clientServiceImpl.queryDocuments(queryDocumentOperation);

        // Assert
        verify(queryDocumentRequest).getCountryCode();
        verify(queryDocumentRequest).setCountryCode(eq("42"));
        verify(filterParams).getCreatedAfter();
        verify(filterParams, atLeast(1)).getCreatedBefore();
        verify(filterParams, atLeast(1)).getMaximumSize();
        verify(filterParams).setCreatedAfter(isA(Calendar.class));
        verify(filterParams).setCreatedBefore(isA(Calendar.class));
        verify(filterParams).setMaximumSize(isA(BigInteger.class));
        verify(queryDocumentRequest).getClassCode();
        verify(queryDocumentRequest).getFilterParams();
        verify(queryDocumentRequest).getPatientId();
        verify(queryDocumentRequest).setFilterParams(isA(eu.europa.ec.sante.openncp.core.client.api.FilterParams.class));
        verify(queryDocumentRequest).setPatientId(isA(eu.europa.ec.sante.openncp.core.client.api.PatientId.class));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("GB"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
        assertTrue(actualQueryDocumentsResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments8() throws XCAException {
        // Arrange
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(new ArrayList<>());
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(),
                Mockito.<List<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");
        eu.europa.ec.sante.openncp.core.client.api.FilterParams filterParams = mock(
                eu.europa.ec.sante.openncp.core.client.api.FilterParams.class);
        when(filterParams.getMaximumSize()).thenReturn(BigInteger.valueOf(1L));
        when(filterParams.getCreatedAfter()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(filterParams.getCreatedBefore()).thenReturn(null);
        doNothing().when(filterParams).setCreatedAfter(Mockito.<Calendar>any());
        doNothing().when(filterParams).setCreatedBefore(Mockito.<Calendar>any());
        doNothing().when(filterParams).setMaximumSize(Mockito.<BigInteger>any());
        filterParams.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        filterParams.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        filterParams.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId patientId = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        patientId.setExtension("42");
        patientId.setRoot("42");
        QueryDocumentRequest queryDocumentRequest = mock(QueryDocumentRequest.class);
        when(queryDocumentRequest.getFilterParams()).thenReturn(filterParams);
        when(queryDocumentRequest.getPatientId()).thenReturn(patientId);
        when(queryDocumentRequest.getCountryCode()).thenReturn("GB");
        when(queryDocumentRequest.getClassCode()).thenReturn(new ArrayList<>());
        doNothing().when(queryDocumentRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryDocumentRequest)
                .setFilterParams(Mockito.<eu.europa.ec.sante.openncp.core.client.api.FilterParams>any());
        doNothing().when(queryDocumentRequest)
                .setPatientId(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientId>any());
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act
        List<EpsosDocument> actualQueryDocumentsResult = clientServiceImpl.queryDocuments(queryDocumentOperation);

        // Assert
        verify(queryDocumentRequest).getCountryCode();
        verify(queryDocumentRequest).setCountryCode(eq("42"));
        verify(filterParams, atLeast(1)).getCreatedAfter();
        verify(filterParams).getCreatedBefore();
        verify(filterParams, atLeast(1)).getMaximumSize();
        verify(filterParams).setCreatedAfter(isA(Calendar.class));
        verify(filterParams).setCreatedBefore(isA(Calendar.class));
        verify(filterParams).setMaximumSize(isA(BigInteger.class));
        verify(queryDocumentRequest).getClassCode();
        verify(queryDocumentRequest).getFilterParams();
        verify(queryDocumentRequest).getPatientId();
        verify(queryDocumentRequest).setFilterParams(isA(eu.europa.ec.sante.openncp.core.client.api.FilterParams.class));
        verify(queryDocumentRequest).setPatientId(isA(eu.europa.ec.sante.openncp.core.client.api.PatientId.class));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("GB"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
        assertTrue(actualQueryDocumentsResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments9() throws XCAException {
        // Arrange
        eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode classCode = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode();
        classCode.setSchema("42");
        classCode.setValue("Value");

        eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode formatCode = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode();
        formatCode.setSchema("42");
        formatCode.setValue("Value");

        XDSDocument cdaPDF = new XDSDocument();
        cdaPDF.setAtcCode("42");
        cdaPDF.setAtcText("42");
        cdaPDF.setAuthors(new ArrayList<>());
        cdaPDF.setClassCode(classCode);
        cdaPDF.setCreationTime("42");
        cdaPDF.setDescription("Description");
        cdaPDF.setDispensable(true);
        cdaPDF.setDocumentUniqueId("Document Unique Id");
        cdaPDF.setDoseFormCode("42");
        cdaPDF.setDoseFormText("42");
        cdaPDF.setEventTime("42");
        cdaPDF.setFormatCode(formatCode);
        cdaPDF.setHcid("42");
        cdaPDF.setHealthcareFacility("42");
        cdaPDF.setId("Id");
        cdaPDF.setMimeType("42");
        cdaPDF.setName("42");
        cdaPDF.setPDF(true);
        cdaPDF
                .setReasonOfHospitalisation(new OrCDDocumentMetaData.ReasonOfHospitalisation("Code", "Coding Scheme", "Text"));
        cdaPDF.setRepositoryUniqueId("Repository Unique Id");
        cdaPDF.setSize("42");
        cdaPDF.setStrength("42");
        cdaPDF.setSubstitution("42");

        eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode classCode2 = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode();
        classCode2.setSchema("42");
        classCode2.setValue("Value");

        eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode formatCode2 = new eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode();
        formatCode2.setSchema("42");
        formatCode2.setValue("Value");

        XDSDocument cdaXML = new XDSDocument();
        cdaXML.setAtcCode("42");
        cdaXML.setAtcText("42");
        cdaXML.setAuthors(new ArrayList<>());
        cdaXML.setClassCode(classCode2);
        cdaXML.setCreationTime("42");
        cdaXML.setDescription("Description");
        cdaXML.setDispensable(true);
        cdaXML.setDocumentUniqueId("Document Unique Id");
        cdaXML.setDoseFormCode("42");
        cdaXML.setDoseFormText("42");
        cdaXML.setEventTime("42");
        cdaXML.setFormatCode(formatCode2);
        cdaXML.setHcid("42");
        cdaXML.setHealthcareFacility("42");
        cdaXML.setId("Id");
        cdaXML.setMimeType("42");
        cdaXML.setName("42");
        cdaXML.setPDF(true);
        cdaXML
                .setReasonOfHospitalisation(new OrCDDocumentMetaData.ReasonOfHospitalisation("Code", "Coding Scheme", "Text"));
        cdaXML.setRepositoryUniqueId("Repository Unique Id");
        cdaXML.setSize("42");
        cdaXML.setStrength("42");
        cdaXML.setSubstitution("42");

        XDSDocumentAssociation xdsDocumentAssociation = new XDSDocumentAssociation();
        xdsDocumentAssociation.setCdaPDF(cdaPDF);
        xdsDocumentAssociation.setCdaXML(cdaXML);

        ArrayList<XDSDocumentAssociation> documentAssociations = new ArrayList<>();
        documentAssociations.add(xdsDocumentAssociation);

        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(documentAssociations);
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(),
                Mockito.<List<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");
        eu.europa.ec.sante.openncp.core.client.api.FilterParams filterParams = mock(
                eu.europa.ec.sante.openncp.core.client.api.FilterParams.class);
        when(filterParams.getMaximumSize()).thenReturn(BigInteger.valueOf(1L));
        when(filterParams.getCreatedAfter()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(filterParams.getCreatedBefore()).thenReturn(new GregorianCalendar(1, 1, 1));
        doNothing().when(filterParams).setCreatedAfter(Mockito.<Calendar>any());
        doNothing().when(filterParams).setCreatedBefore(Mockito.<Calendar>any());
        doNothing().when(filterParams).setMaximumSize(Mockito.<BigInteger>any());
        filterParams.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        filterParams.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        filterParams.setMaximumSize(BigInteger.valueOf(1L));
        eu.europa.ec.sante.openncp.core.client.api.PatientId patientId = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientId.class);
        when(patientId.getExtension()).thenReturn("Extension");
        when(patientId.getRoot()).thenReturn("Root");
        doNothing().when(patientId).setExtension(Mockito.<String>any());
        doNothing().when(patientId).setRoot(Mockito.<String>any());
        patientId.setExtension("42");
        patientId.setRoot("42");
        QueryDocumentRequest queryDocumentRequest = mock(QueryDocumentRequest.class);
        when(queryDocumentRequest.getFilterParams()).thenReturn(filterParams);
        when(queryDocumentRequest.getPatientId()).thenReturn(patientId);
        when(queryDocumentRequest.getCountryCode()).thenReturn("GB");
        when(queryDocumentRequest.getClassCode()).thenReturn(new ArrayList<>());
        doNothing().when(queryDocumentRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryDocumentRequest)
                .setFilterParams(Mockito.<eu.europa.ec.sante.openncp.core.client.api.FilterParams>any());
        doNothing().when(queryDocumentRequest)
                .setPatientId(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientId>any());
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryDocuments(queryDocumentOperation));
        verify(queryDocumentRequest).getCountryCode();
        verify(queryDocumentRequest).setCountryCode(eq("42"));
        verify(filterParams, atLeast(1)).getCreatedAfter();
        verify(filterParams, atLeast(1)).getCreatedBefore();
        verify(filterParams, atLeast(1)).getMaximumSize();
        verify(filterParams).setCreatedAfter(isA(Calendar.class));
        verify(filterParams).setCreatedBefore(isA(Calendar.class));
        verify(filterParams).setMaximumSize(isA(BigInteger.class));
        verify(patientId).getExtension();
        verify(patientId).getRoot();
        verify(patientId).setExtension(eq("42"));
        verify(patientId).setRoot(eq("42"));
        verify(queryDocumentRequest).getClassCode();
        verify(queryDocumentRequest).getFilterParams();
        verify(queryDocumentRequest).getPatientId();
        verify(queryDocumentRequest).setFilterParams(isA(eu.europa.ec.sante.openncp.core.client.api.FilterParams.class));
        verify(queryDocumentRequest).setPatientId(isA(eu.europa.ec.sante.openncp.core.client.api.PatientId.class));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("GB"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments10() {
        // Arrange
        FilterParams value = new FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        PatientId value2 = new PatientId();
        value2.setExtension("42");
        value2.setRoot("42");
        FilterParams filterParams = mock(FilterParams.class);
        doNothing().when(filterParams).setCreatedAfter(Mockito.<Calendar>any());
        doNothing().when(filterParams).setCreatedBefore(Mockito.<Calendar>any());
        doNothing().when(filterParams).setMaximumSize(Mockito.<BigInteger>any());
        filterParams.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        filterParams.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        filterParams.setMaximumSize(BigInteger.valueOf(1L));
        PatientId patientId = mock(PatientId.class);
        doNothing().when(patientId).setExtension(Mockito.<String>any());
        doNothing().when(patientId).setRoot(Mockito.<String>any());
        patientId.setExtension("42");
        patientId.setRoot("42");

        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode genericDocumentCode = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        genericDocumentCode.setNodeRepresentation("42");
        genericDocumentCode.setSchema("42");
        genericDocumentCode.setValue("42");

        ArrayList<eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode> genericDocumentCodeList = new ArrayList<>();
        genericDocumentCodeList.add(genericDocumentCode);
        QueryDocumentRequest queryDocumentRequest = mock(QueryDocumentRequest.class);
        when(queryDocumentRequest.getFilterParams()).thenReturn(filterParams);
        when(queryDocumentRequest.getPatientId()).thenReturn(patientId);
        when(queryDocumentRequest.getCountryCode()).thenReturn("GB");
        when(queryDocumentRequest.getClassCode()).thenReturn(genericDocumentCodeList);
        doNothing().when(queryDocumentRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryDocumentRequest).setFilterParams(Mockito.<FilterParams>any());
        doNothing().when(queryDocumentRequest).setPatientId(Mockito.<PatientId>any());
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryDocuments(queryDocumentOperation));
        verify(queryDocumentRequest).getCountryCode();
        verify(queryDocumentRequest).setCountryCode(eq("42"));
        verify(filterParams).setCreatedAfter(isA(Calendar.class));
        verify(filterParams).setCreatedBefore(isA(Calendar.class));
        verify(filterParams).setMaximumSize(isA(BigInteger.class));
        verify(patientId).setExtension(eq("42"));
        verify(patientId).setRoot(eq("42"));
        verify(queryDocumentRequest).getClassCode();
        verify(queryDocumentRequest).getFilterParams();
        verify(queryDocumentRequest).getPatientId();
        verify(queryDocumentRequest).setFilterParams(isA(FilterParams.class));
        verify(queryDocumentRequest).setPatientId(isA(PatientId.class));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryDocuments(QueryDocumentOperation)}
     */
    @Test
    public void testQueryDocuments11() throws XCAException {
        // Arrange
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setDocumentAssociations(new ArrayList<>());
        queryResponse.setFailureMessages(new ArrayList<>());
        when(orCDService.list(Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId>any(),
                Mockito.<String>any(),
                Mockito.<List<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode>>any(),
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams>any(),
                Mockito.<Map<AssertionType, Assertion>>any())).thenReturn(queryResponse);

        eu.europa.ec.sante.openncp.core.client.api.FilterParams value = new eu.europa.ec.sante.openncp.core.client.api.FilterParams();
        value.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        value.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        value.setMaximumSize(BigInteger.valueOf(1L));

        eu.europa.ec.sante.openncp.core.client.api.PatientId value2 = new eu.europa.ec.sante.openncp.core.client.api.PatientId();
        value2.setExtension("42");
        value2.setRoot("42");
        eu.europa.ec.sante.openncp.core.client.api.FilterParams filterParams = mock(
                eu.europa.ec.sante.openncp.core.client.api.FilterParams.class);
        when(filterParams.getMaximumSize()).thenReturn(BigInteger.valueOf(1L));
        when(filterParams.getCreatedAfter()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(filterParams.getCreatedBefore()).thenReturn(new GregorianCalendar(1, 1, 1));
        doNothing().when(filterParams).setCreatedAfter(Mockito.<Calendar>any());
        doNothing().when(filterParams).setCreatedBefore(Mockito.<Calendar>any());
        doNothing().when(filterParams).setMaximumSize(Mockito.<BigInteger>any());
        filterParams.setCreatedAfter(new GregorianCalendar(1, 1, 1));
        filterParams.setCreatedBefore(new GregorianCalendar(1, 1, 1));
        filterParams.setMaximumSize(BigInteger.valueOf(1L));
        eu.europa.ec.sante.openncp.core.client.api.PatientId patientId = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientId.class);
        when(patientId.getExtension()).thenReturn("Extension");
        when(patientId.getRoot()).thenReturn("Root");
        doNothing().when(patientId).setExtension(Mockito.<String>any());
        doNothing().when(patientId).setRoot(Mockito.<String>any());
        patientId.setExtension("42");
        patientId.setRoot("42");

        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode genericDocumentCode = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        genericDocumentCode.setNodeRepresentation("42");
        genericDocumentCode.setSchema("42");
        genericDocumentCode.setValue("42");

        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode genericDocumentCode2 = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        genericDocumentCode2.setNodeRepresentation("queryDocuments");
        genericDocumentCode2.setSchema("queryDocuments");
        genericDocumentCode2.setValue("queryDocuments");

        ArrayList<eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode> genericDocumentCodeList = new ArrayList<>();
        genericDocumentCodeList.add(genericDocumentCode2);
        genericDocumentCodeList.add(genericDocumentCode);
        QueryDocumentRequest queryDocumentRequest = mock(QueryDocumentRequest.class);
        when(queryDocumentRequest.getFilterParams()).thenReturn(filterParams);
        when(queryDocumentRequest.getPatientId()).thenReturn(patientId);
        when(queryDocumentRequest.getCountryCode()).thenReturn("GB");
        when(queryDocumentRequest.getClassCode()).thenReturn(genericDocumentCodeList);
        doNothing().when(queryDocumentRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryDocumentRequest)
                .setFilterParams(Mockito.<eu.europa.ec.sante.openncp.core.client.api.FilterParams>any());
        doNothing().when(queryDocumentRequest)
                .setPatientId(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientId>any());
        queryDocumentRequest.setCountryCode("42");
        queryDocumentRequest.setFilterParams(value);
        queryDocumentRequest.setPatientId(value2);
        QueryDocumentOperation queryDocumentOperation = mock(QueryDocumentOperation.class);
        when(queryDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryDocumentOperation.getRequest()).thenReturn(queryDocumentRequest);

        // Act
        List<EpsosDocument> actualQueryDocumentsResult = clientServiceImpl.queryDocuments(queryDocumentOperation);

        // Assert
        verify(queryDocumentRequest).getCountryCode();
        verify(queryDocumentRequest).setCountryCode(eq("42"));
        verify(filterParams, atLeast(1)).getCreatedAfter();
        verify(filterParams, atLeast(1)).getCreatedBefore();
        verify(filterParams, atLeast(1)).getMaximumSize();
        verify(filterParams).setCreatedAfter(isA(Calendar.class));
        verify(filterParams).setCreatedBefore(isA(Calendar.class));
        verify(filterParams).setMaximumSize(isA(BigInteger.class));
        verify(patientId).getExtension();
        verify(patientId).getRoot();
        verify(patientId).setExtension(eq("42"));
        verify(patientId).setRoot(eq("42"));
        verify(queryDocumentRequest).getClassCode();
        verify(queryDocumentRequest).getFilterParams();
        verify(queryDocumentRequest).getPatientId();
        verify(queryDocumentRequest).setFilterParams(isA(eu.europa.ec.sante.openncp.core.client.api.FilterParams.class));
        verify(queryDocumentRequest).setPatientId(isA(eu.europa.ec.sante.openncp.core.client.api.PatientId.class));
        verify(queryDocumentOperation).getAssertions();
        verify(queryDocumentOperation, atLeast(1)).getRequest();
        verify(orCDService).list(isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId.class), eq("GB"),
                isA(List.class), isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams.class), isA(Map.class));
        assertTrue(actualQueryDocumentsResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient() {
        // Arrange
        PatientDemographics value = new PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");

        QueryPatientRequest queryPatientRequest = new QueryPatientRequest();
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryPatient(queryPatientOperation));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient2() {
        // Arrange
        PatientDemographics value = new PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");

        QueryPatientRequest queryPatientRequest = new QueryPatientRequest();
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenThrow(new ClientConnectorException("An error occurred"));
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryPatient(queryPatientOperation));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient3() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient4() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient5() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient6() {
        // Arrange
        PatientDemographics value = new PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        PatientDemographics patientDemographics = mock(PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("(U)99");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest).setPatientDemographics(Mockito.<PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryPatient(queryPatientOperation));
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest).setPatientDemographics(isA(PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient7() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient8() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient9() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient10() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient11() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient12() {
        // Arrange
        PatientDemographics value = new PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        PatientDemographics patientDemographics = mock(PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("U@U.UUUU");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest).setPatientDemographics(Mockito.<PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.queryPatient(queryPatientOperation));
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest).setPatientDemographics(isA(PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient13() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient14() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(null);
        when(patientDemographics.getPatientId()).thenReturn(new ArrayList<>());
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#queryPatient(QueryPatientOperation)}
     */
    @Test
    public void testQueryPatient15() throws NoPatientIdDiscoveredException {
        // Arrange
        when(identificationService.findIdentityByTraits(
                Mockito.<eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics>any(),
                Mockito.<Map<AssertionType, Assertion>>any(), Mockito.<String>any())).thenReturn(new ArrayList<>());

        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics value = new eu.europa.ec.sante.openncp.core.client.api.PatientDemographics();
        value.setAdministrativeGender("42");
        value.setBirthDate(new GregorianCalendar(1, 1, 1));
        value.setCity("42");
        value.setCountry("42");
        value.setEmail("42");
        value.setFamilyName("42");
        value.setGivenName("42");
        value.setPostalCode("42");
        value.setStreetAddress("42");
        value.setTelephone("42");

        PatientId patientId = new PatientId();
        patientId.setExtension("U@U.UUUU");
        patientId.setRoot("U@U.UUUU");

        ArrayList<PatientId> patientIdList = new ArrayList<>();
        patientIdList.add(patientId);
        eu.europa.ec.sante.openncp.core.client.api.PatientDemographics patientDemographics = mock(
                eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class);
        when(patientDemographics.getAdministrativeGender()).thenReturn("");
        when(patientDemographics.getCity()).thenReturn("Oxford");
        when(patientDemographics.getCountry()).thenReturn("GB");
        when(patientDemographics.getEmail()).thenReturn("jane.doe@example.org");
        when(patientDemographics.getFamilyName()).thenReturn("Family Name");
        when(patientDemographics.getGivenName()).thenReturn("Given Name");
        when(patientDemographics.getPostalCode()).thenReturn("Postal Code");
        when(patientDemographics.getStreetAddress()).thenReturn("42 Main St");
        when(patientDemographics.getTelephone()).thenReturn("6625550144");
        when(patientDemographics.getBirthDate()).thenReturn(new GregorianCalendar(1, 1, 1));
        when(patientDemographics.getPatientId()).thenReturn(patientIdList);
        doNothing().when(patientDemographics).setAdministrativeGender(Mockito.<String>any());
        doNothing().when(patientDemographics).setBirthDate(Mockito.<Calendar>any());
        doNothing().when(patientDemographics).setCity(Mockito.<String>any());
        doNothing().when(patientDemographics).setCountry(Mockito.<String>any());
        doNothing().when(patientDemographics).setEmail(Mockito.<String>any());
        doNothing().when(patientDemographics).setFamilyName(Mockito.<String>any());
        doNothing().when(patientDemographics).setGivenName(Mockito.<String>any());
        doNothing().when(patientDemographics).setPostalCode(Mockito.<String>any());
        doNothing().when(patientDemographics).setStreetAddress(Mockito.<String>any());
        doNothing().when(patientDemographics).setTelephone(Mockito.<String>any());
        patientDemographics.setAdministrativeGender("42");
        patientDemographics.setBirthDate(new GregorianCalendar(1, 1, 1));
        patientDemographics.setCity("42");
        patientDemographics.setCountry("42");
        patientDemographics.setEmail("42");
        patientDemographics.setFamilyName("42");
        patientDemographics.setGivenName("42");
        patientDemographics.setPostalCode("42");
        patientDemographics.setStreetAddress("42");
        patientDemographics.setTelephone("42");
        QueryPatientRequest queryPatientRequest = mock(QueryPatientRequest.class);
        when(queryPatientRequest.getPatientDemographics()).thenReturn(patientDemographics);
        when(queryPatientRequest.getCountryCode()).thenReturn("GB");
        doNothing().when(queryPatientRequest).setCountryCode(Mockito.<String>any());
        doNothing().when(queryPatientRequest)
                .setPatientDemographics(Mockito.<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics>any());
        queryPatientRequest.setCountryCode("42");
        queryPatientRequest.setPatientDemographics(value);
        QueryPatientOperation queryPatientOperation = mock(QueryPatientOperation.class);
        when(queryPatientOperation.getAssertions()).thenReturn(new HashMap<>());
        when(queryPatientOperation.getRequest()).thenReturn(queryPatientRequest);

        // Act
        List<eu.europa.ec.sante.openncp.core.client.api.PatientDemographics> actualQueryPatientResult = clientServiceImpl
                .queryPatient(queryPatientOperation);

        // Assert
        verify(queryPatientRequest).getCountryCode();
        verify(queryPatientRequest).setCountryCode(eq("42"));
        verify(patientDemographics).getAdministrativeGender();
        verify(patientDemographics, atLeast(1)).getBirthDate();
        verify(patientDemographics, atLeast(1)).getCity();
        verify(patientDemographics, atLeast(1)).getCountry();
        verify(patientDemographics, atLeast(1)).getEmail();
        verify(patientDemographics, atLeast(1)).getFamilyName();
        verify(patientDemographics, atLeast(1)).getGivenName();
        verify(patientDemographics, atLeast(1)).getPatientId();
        verify(patientDemographics, atLeast(1)).getPostalCode();
        verify(patientDemographics, atLeast(1)).getStreetAddress();
        verify(patientDemographics, atLeast(1)).getTelephone();
        verify(patientDemographics).setAdministrativeGender(eq("42"));
        verify(patientDemographics).setBirthDate(isA(Calendar.class));
        verify(patientDemographics).setCity(eq("42"));
        verify(patientDemographics).setCountry(eq("42"));
        verify(patientDemographics).setEmail(eq("42"));
        verify(patientDemographics).setFamilyName(eq("42"));
        verify(patientDemographics).setGivenName(eq("42"));
        verify(patientDemographics).setPostalCode(eq("42"));
        verify(patientDemographics).setStreetAddress(eq("42"));
        verify(patientDemographics).setTelephone(eq("42"));
        verify(queryPatientRequest).getPatientDemographics();
        verify(queryPatientRequest)
                .setPatientDemographics(isA(eu.europa.ec.sante.openncp.core.client.api.PatientDemographics.class));
        verify(queryPatientOperation).getAssertions();
        verify(queryPatientOperation, atLeast(1)).getRequest();
        verify(identificationService).findIdentityByTraits(
                isA(eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientDemographics.class), isA(Map.class), eq("GB"));
        assertTrue(actualQueryPatientResult.isEmpty());
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#retrieveDocument(RetrieveDocumentOperation)}
     */
    @Test
    public void testRetrieveDocument() {
        // Arrange
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode value = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        value.setNodeRepresentation("42");
        value.setSchema("42");
        value.setValue("42");

        DocumentId value2 = new DocumentId();
        value2.setDocumentUniqueId("42");
        value2.setRepositoryUniqueId("42");

        RetrieveDocumentRequest retrieveDocumentRequest = new RetrieveDocumentRequest();
        retrieveDocumentRequest.setClassCode(value);
        retrieveDocumentRequest.setCountryCode("42");
        retrieveDocumentRequest.setDocumentId(value2);
        retrieveDocumentRequest.setHomeCommunityId("42");
        retrieveDocumentRequest.setTargetLanguage("42");
        RetrieveDocumentOperation retrieveDocumentOperation = mock(RetrieveDocumentOperation.class);
        when(retrieveDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(retrieveDocumentOperation.getRequest()).thenReturn(retrieveDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.retrieveDocument(retrieveDocumentOperation));
        verify(retrieveDocumentOperation).getAssertions();
        verify(retrieveDocumentOperation).getRequest();
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#retrieveDocument(RetrieveDocumentOperation)}
     */
    @Test
    public void testRetrieveDocument2() {
        // Arrange
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode value = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        value.setNodeRepresentation("42");
        value.setSchema("42");
        value.setValue("42");

        DocumentId value2 = new DocumentId();
        value2.setDocumentUniqueId("42");
        value2.setRepositoryUniqueId("42");

        RetrieveDocumentRequest retrieveDocumentRequest = new RetrieveDocumentRequest();
        retrieveDocumentRequest.setClassCode(value);
        retrieveDocumentRequest.setCountryCode("42");
        retrieveDocumentRequest.setDocumentId(value2);
        retrieveDocumentRequest.setHomeCommunityId("42");
        retrieveDocumentRequest.setTargetLanguage("42");
        RetrieveDocumentOperation retrieveDocumentOperation = mock(RetrieveDocumentOperation.class);
        when(retrieveDocumentOperation.getAssertions()).thenThrow(new ClientConnectorException("An error occurred"));
        when(retrieveDocumentOperation.getRequest()).thenReturn(retrieveDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.retrieveDocument(retrieveDocumentOperation));
        verify(retrieveDocumentOperation).getAssertions();
        verify(retrieveDocumentOperation).getRequest();
    }

    /**
     * Method under test: {@link ClientServiceImpl#sayHello(String)}
     */
    @Test
    public void testSayHello() {
        // Arrange, Act and Assert
        assertEquals("Hello Who", clientServiceImpl.sayHello("Who"));
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#submitDocument(SubmitDocumentOperation)}
     */
    @Test
    public void testSubmitDocument() throws UnsupportedEncodingException {
        // Arrange
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode value = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        value.setNodeRepresentation("42");
        value.setSchema("42");
        value.setValue("42");

        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode value2 = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        value2.setNodeRepresentation("42");
        value2.setSchema("42");
        value2.setValue("42");

        ReasonOfHospitalisation value3 = new ReasonOfHospitalisation();
        value3.setCode("42");
        value3.setText("42");

        EpsosDocument value4 = new EpsosDocument();
        value4.setAtcCode("42");
        value4.setAtcText("42");
        value4.setBase64Binary("AXAXAXAX".getBytes("UTF-8"));
        value4.setClassCode(value);
        value4.setCreationDate(new GregorianCalendar(1, 1, 1));
        value4.setDescription("42");
        value4.setDispensable(true);
        value4.setDoseFormCode("42");
        value4.setDoseFormText("42");
        value4.setEventDate(new GregorianCalendar(1, 1, 1));
        value4.setFormatCode(value2);
        value4.setHcid("42");
        value4.setMimeType("42");
        value4.setReasonOfHospitalisation(value3);
        value4.setRepositoryId("42");
        value4.setSize(BigInteger.valueOf(1L));
        value4.setStrength("42");
        value4.setSubmissionSetId("42");
        value4.setSubstitution("42");
        value4.setTitle("42");
        value4.setUuid("42");

        PatientDemographics value5 = new PatientDemographics();
        value5.setAdministrativeGender("42");
        value5.setBirthDate(new GregorianCalendar(1, 1, 1));
        value5.setCity("42");
        value5.setCountry("42");
        value5.setEmail("42");
        value5.setFamilyName("42");
        value5.setGivenName("42");
        value5.setPostalCode("42");
        value5.setStreetAddress("42");
        value5.setTelephone("42");

        SubmitDocumentRequest submitDocumentRequest = new SubmitDocumentRequest();
        submitDocumentRequest.setCountryCode("42");
        submitDocumentRequest.setDocument(value4);
        submitDocumentRequest.setPatientDemographics(value5);
        SubmitDocumentOperation submitDocumentOperation = mock(SubmitDocumentOperation.class);
        when(submitDocumentOperation.getAssertions()).thenReturn(new HashMap<>());
        when(submitDocumentOperation.getRequest()).thenReturn(submitDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.submitDocument(submitDocumentOperation));
        verify(submitDocumentOperation).getAssertions();
        verify(submitDocumentOperation).getRequest();
    }

    /**
     * Method under test:
     * {@link ClientServiceImpl#submitDocument(SubmitDocumentOperation)}
     */
    @Test
    public void testSubmitDocument2() throws UnsupportedEncodingException {
        // Arrange
        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode value = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        value.setNodeRepresentation("42");
        value.setSchema("42");
        value.setValue("42");

        eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode value2 = new eu.europa.ec.sante.openncp.core.client.api.GenericDocumentCode();
        value2.setNodeRepresentation("42");
        value2.setSchema("42");
        value2.setValue("42");

        ReasonOfHospitalisation value3 = new ReasonOfHospitalisation();
        value3.setCode("42");
        value3.setText("42");

        EpsosDocument value4 = new EpsosDocument();
        value4.setAtcCode("42");
        value4.setAtcText("42");
        value4.setBase64Binary("AXAXAXAX".getBytes("UTF-8"));
        value4.setClassCode(value);
        value4.setCreationDate(new GregorianCalendar(1, 1, 1));
        value4.setDescription("42");
        value4.setDispensable(true);
        value4.setDoseFormCode("42");
        value4.setDoseFormText("42");
        value4.setEventDate(new GregorianCalendar(1, 1, 1));
        value4.setFormatCode(value2);
        value4.setHcid("42");
        value4.setMimeType("42");
        value4.setReasonOfHospitalisation(value3);
        value4.setRepositoryId("42");
        value4.setSize(BigInteger.valueOf(1L));
        value4.setStrength("42");
        value4.setSubmissionSetId("42");
        value4.setSubstitution("42");
        value4.setTitle("42");
        value4.setUuid("42");

        PatientDemographics value5 = new PatientDemographics();
        value5.setAdministrativeGender("42");
        value5.setBirthDate(new GregorianCalendar(1, 1, 1));
        value5.setCity("42");
        value5.setCountry("42");
        value5.setEmail("42");
        value5.setFamilyName("42");
        value5.setGivenName("42");
        value5.setPostalCode("42");
        value5.setStreetAddress("42");
        value5.setTelephone("42");

        SubmitDocumentRequest submitDocumentRequest = new SubmitDocumentRequest();
        submitDocumentRequest.setCountryCode("42");
        submitDocumentRequest.setDocument(value4);
        submitDocumentRequest.setPatientDemographics(value5);
        SubmitDocumentOperation submitDocumentOperation = mock(SubmitDocumentOperation.class);
        when(submitDocumentOperation.getAssertions()).thenThrow(new ClientConnectorException("An error occurred"));
        when(submitDocumentOperation.getRequest()).thenReturn(submitDocumentRequest);

        // Act and Assert
        assertThrows(ClientConnectorException.class, () -> clientServiceImpl.submitDocument(submitDocumentOperation));
        verify(submitDocumentOperation).getAssertions();
        verify(submitDocumentOperation).getRequest();
    }
}
