package eu.europa.ec.sante.openncp.core.client.ihe.xca;

import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.FilterParams;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.GenericDocumentCode;
import eu.europa.ec.sante.openncp.core.common.ihe.datamodel.PatientId;
import eu.europa.ec.sante.openncp.core.common.ihe.exception.OpenNCPException;
import eu.europa.ec.sante.openncp.core.common.ihe.transformation.service.CDATransformationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class XcaInitGatewayTest {
    @Mock
    private CDATransformationService cDATransformationService;

    @InjectMocks
    private XcaInitGateway xcaInitGateway;

    /**
     * Method under test:
     * {@link XcaInitGateway#crossGatewayQuery(PatientId, String, List, FilterParams, Map, String)}
     */
    @Test
    public void testCrossGatewayQuery() throws OpenNCPException {
        // Arrange
        PatientId pid = new PatientId("Root", "Extension");

        ArrayList<GenericDocumentCode> documentCodes = new ArrayList<>();

        FilterParams filterParams = new FilterParams();
        filterParams.setCreatedAfter(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setCreatedBefore(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setMaximumSize(3L);

        // Act and Assert
        assertThrows(RuntimeException.class,
                () -> xcaInitGateway.crossGatewayQuery(pid, "GB", documentCodes, filterParams, new HashMap<>(), "Service"));
    }

    /**
     * Method under test:
     * {@link XcaInitGateway#crossGatewayQuery(PatientId, String, List, FilterParams, Map, String)}
     */
    @Test
    public void testCrossGatewayQuery2() throws OpenNCPException {
        // Arrange
        PatientId pid = new PatientId("9.9.9-U", "Extension");

        ArrayList<GenericDocumentCode> documentCodes = new ArrayList<>();

        FilterParams filterParams = new FilterParams();
        filterParams.setCreatedAfter(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setCreatedBefore(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setMaximumSize(3L);

        // Act and Assert
        assertThrows(RuntimeException.class,
                () -> xcaInitGateway.crossGatewayQuery(pid, "GB", documentCodes, filterParams, new HashMap<>(), "Service"));
    }

    /**
     * Method under test:
     * {@link XcaInitGateway#crossGatewayQuery(PatientId, String, List, FilterParams, Map, String)}
     */
    @Test
    public void testCrossGatewayQuery3() throws OpenNCPException {
        // Arrange
        PatientId pid = Mockito.mock(PatientId.class);
        when(pid.getExtension()).thenReturn("Extension");
        when(pid.getRoot()).thenReturn("root");
        ArrayList<GenericDocumentCode> documentCodes = new ArrayList<>();

        FilterParams filterParams = new FilterParams();
        filterParams.setCreatedAfter(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setCreatedBefore(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setMaximumSize(3L);

        // Act and Assert
        assertThrows(RuntimeException.class,
                () -> xcaInitGateway.crossGatewayQuery(pid, "GB", documentCodes, filterParams, new HashMap<>(), "Service"));
        String extension= pid.getExtension();
        String root= pid.getRoot();
        assertEquals("Extension", extension);
        assertEquals("root", root);

    }

    /**
     * Method under test:
     * {@link XcaInitGateway#crossGatewayQuery(PatientId, String, List, FilterParams, Map, String)}
     */
    @Test
    public void testCrossGatewayQuery4() throws OpenNCPException {
        // Arrange
        PatientId pid = mock(PatientId.class);
        when(pid.getExtension()).thenReturn("Extension");
        when(pid.getRoot()).thenReturn("Root");

        GenericDocumentCode genericDocumentCode = new GenericDocumentCode();
        genericDocumentCode.setSchema(" ");
        genericDocumentCode.setValue("42");

        ArrayList<GenericDocumentCode> documentCodes = new ArrayList<>();
        documentCodes.add(genericDocumentCode);

        FilterParams filterParams = new FilterParams();
        filterParams.setCreatedAfter(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setCreatedBefore(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setMaximumSize(3L);

        // Act and Assert
        assertThrows(RuntimeException.class,
                () -> xcaInitGateway.crossGatewayQuery(pid, "GB", documentCodes, filterParams, new HashMap<>(), "Service"));
        String extension= pid.getExtension();
        String root= pid.getRoot();
        assertEquals("Extension", extension);
        assertEquals("Root", root);
    }

    /**
     * Method under test:
     * {@link XcaInitGateway#crossGatewayQuery(PatientId, String, List, FilterParams, Map, String)}
     */
    @Test
    public void testCrossGatewayQuery5() throws OpenNCPException {
        // Arrange
        PatientId pid = mock(PatientId.class);
        when(pid.getExtension()).thenReturn("Extension");
        when(pid.getRoot()).thenReturn("Root");

        GenericDocumentCode genericDocumentCode = new GenericDocumentCode();
        genericDocumentCode.setSchema(" ");
        genericDocumentCode.setValue("42");

        GenericDocumentCode genericDocumentCode2 = new GenericDocumentCode();
        genericDocumentCode2.setSchema("LeafClass");
        genericDocumentCode2.setValue(" ");

        ArrayList<GenericDocumentCode> documentCodes = new ArrayList<>();
        documentCodes.add(genericDocumentCode2);
        documentCodes.add(genericDocumentCode);

        FilterParams filterParams = new FilterParams();
        filterParams.setCreatedAfter(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setCreatedBefore(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setMaximumSize(3L);

        // Act and Assert
        assertThrows(RuntimeException.class,
                () -> xcaInitGateway.crossGatewayQuery(pid, "GB", documentCodes, filterParams, new HashMap<>(), "Service"));
        String extension= pid.getExtension();
        String root= pid.getRoot();
        assertEquals("Extension", extension);
        assertEquals("Root", root);
    }

    /**
     * Method under test:
     * {@link XcaInitGateway#crossGatewayQuery(PatientId, String, List, FilterParams, Map, String)}
     */
    @Test
    public void testCrossGatewayQuery6() throws OpenNCPException {
        // Arrange
        PatientId pid = mock(PatientId.class);
        when(pid.getExtension()).thenReturn(" ");
        when(pid.getRoot()).thenReturn("Root");
        ArrayList<GenericDocumentCode> documentCodes = new ArrayList<>();
        FilterParams filterParams = mock(FilterParams.class);
        when(filterParams.getMaximumSize()).thenReturn(3L);
        when(filterParams.getCreatedAfter())
                .thenReturn(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        when(filterParams.getCreatedBefore())
                .thenReturn(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        doNothing().when(filterParams).setCreatedAfter(Mockito.<Instant>any());
        doNothing().when(filterParams).setCreatedBefore(Mockito.<Instant>any());
        doNothing().when(filterParams).setMaximumSize(Mockito.<Long>any());
        filterParams.setCreatedAfter(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setCreatedBefore(LocalDate.of(1970, 1, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant());
        filterParams.setMaximumSize(3L);

        // Act and Assert
        assertThrows(RuntimeException.class, () -> xcaInitGateway.crossGatewayQuery(pid, "GB", documentCodes, filterParams,
                new HashMap<>(), "java.util.List"));
        String extension= pid.getExtension();
        String root= pid.getRoot();
        Long maximumSize=filterParams.getMaximumSize();
        Instant createdBefore=filterParams.getCreatedBefore();
        Instant createdAfter=filterParams.getCreatedAfter();
        assertEquals(" ", extension);
        assertEquals("Root", root);
        assertNotNull(maximumSize);
        assertNotNull(createdBefore);
        assertNotNull(createdAfter);
    }

}
