package se.sb.epsos.web.service.mock;

import epsos.ccd.netsmart.securitymanager.sts.client.TRCAssertionRequest;
import eu.europa.ec.sante.ehdsi.openncp.audit.AuditService;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opensaml.saml.saml2.core.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sb.epsos.shelob.ws.client.jaxws.*;
import se.sb.epsos.web.auth.AuthenticatedUser;
import se.sb.epsos.web.model.*;
import se.sb.epsos.web.service.*;
import se.sb.epsos.web.util.XmlTypeWrapper;
import se.sb.epsos.web.util.XmlUtil;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class NcpServiceFacadeMock implements NcpServiceFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcpServiceFacadeMock.class);

    private NcpServiceFacade serviceFacade;
    private ClientConnectorService webServiceClient;
    private TrcServiceHandler trcServiceHandler;
    private MockSettings settings = withSettings().serializable();

    public NcpServiceFacadeMock() {
        LOGGER.info("Creating NcpServiceFacadeMock");
        webServiceClient = mock(ClientConnectorService.class, settings);
        ClientConnectorServiceService service = mock(ClientConnectorServiceService.class, settings);
        mockInitUserResponse(service);
        mockQueryPatentsResponse(webServiceClient);
        mockSetTRCAssertionResponse(webServiceClient);
        mockQueryDocumentsResponse(webServiceClient);
        mockRetrieveDocumentResponse(webServiceClient);
        mockSubmitDocumentResponse(webServiceClient);

        AuditService auditService = mock(AuditService.class, settings);
        when(auditService.write(Mockito.notNull(), (String) Mockito.notNull(), (String) Mockito.notNull())).thenReturn(true);

        trcServiceHandler = mock(TrcServiceHandler.class, settings);
        mockBuildTrcRequestResponse(trcServiceHandler);

        serviceFacade = new NcpServiceFacadeImpl(service, webServiceClient, trcServiceHandler);
    }

    @Override
    public String about() {
        return getClass().getSimpleName() + " (offline:stubbed)";
    }

    @Override
    public void bindToSession(String sessionId) {
        serviceFacade.bindToSession(sessionId);
    }

    private void mockBuildTrcRequestResponse(TrcServiceHandler trcServiceHandlerMock) {
        try {
            when(trcServiceHandler.buildTrcRequest(any(Assertion.class), anyString(), anyString())).thenAnswer(new Answer<TRCAssertionRequest>() {
                @Override
                public TRCAssertionRequest answer(InvocationOnMock invocation) throws Throwable {
                    final Assertion assertion = (Assertion) invocation.getArguments()[0];
                    String patientId = (String) invocation.getArguments()[1];
                    String purposeOfUse = (String) invocation.getArguments()[2];
                    TRCAssertionRequest request = mock(TRCAssertionRequest.class, settings);
                    when(request.request()).thenReturn(assertion);
                    return request;
                }
            });
        } catch (Exception e) {
            LOGGER.error("Exception: '{}'", e.getMessage(), e);
        }
    }

    private void mockSubmitDocumentResponse(ClientConnectorService webServiceClientMock) {
    }

    private void mockRetrieveDocumentResponse(ClientConnectorService webServiceClientMock) {
        when(webServiceClientMock.retrieveDocument(any(RetrieveDocumentRequest.class))).thenAnswer(new Answer<EpsosDocument>() {

            @Override
            public EpsosDocument answer(InvocationOnMock invocation) {
                RetrieveDocumentRequest req = (RetrieveDocumentRequest) invocation.getArguments()[0];
                String documentId = req.getDocumentId().getDocumentUniqueId();
                byte[] bytes = DocumentCatalog.get(documentId);
                EpsosDocument mock = new EpsosDocument();
                mock.setBase64Binary(bytes);
                return mock;
            }
        });
    }

    private void mockQueryDocumentsResponse(ClientConnectorService webServiceClientMock) {

        when(webServiceClientMock.queryDocuments(any(QueryDocumentRequest.class))).thenAnswer(new Answer<List<EpsosDocument>>() {

            @Override
            public List<EpsosDocument> answer(InvocationOnMock invocation) throws Throwable {

                QueryDocumentRequest request = (QueryDocumentRequest) invocation.getArguments()[0];
                PatientId patientId = request.getPatientId();
                LOGGER.debug("PatientId: " + createLongPatientId(patientId));
                List<EpsosDocument> result = DocumentCatalog.queryEP(createLongPatientId(patientId));

                if (LOGGER.isDebugEnabled()) {
                    for (EpsosDocument epsosDocument : result) {
                        LOGGER.debug("\n" + XmlUtil.marshallJaxbObject(new XmlTypeWrapper<>(epsosDocument)));
                        LOGGER.debug("Doc: " + epsosDocument.getTitle());
                    }
                }
                return result;
            }
        });
    }

    private void mockSetTRCAssertionResponse(ClientConnectorService webServiceClientMock) {

    }

    private void mockQueryPatentsResponse(ClientConnectorService webServiceClientMock) {

        when(webServiceClientMock.queryPatient(any(QueryPatientRequest.class))).thenAnswer(new Answer<List<PatientDemographics>>() {
            @Override
            public List<PatientDemographics> answer(InvocationOnMock invocation) {
                List<PatientDemographics> result = new ArrayList<>();
                QueryPatientRequest query = (QueryPatientRequest) invocation.getArguments()[0];
                String queryId = query.getPatientDemographics().getPatientId().get(0).getExtension() + "^^^&" + query.getPatientDemographics().getPatientId().get(0).getRoot();
                PatientDemographics pat = PatientCatalog.query(queryId);
                if (pat != null) {
                    result.add(pat);
                }
                return result;
            }
        });
    }

    private AuthenticatedUser createUserDetailsMock() {

        AuthenticatedUser user = mock(AuthenticatedUser.class, settings);
        when(user.getUsername()).thenReturn("test");
        when(user.getRoles()).thenReturn(Arrays.asList("ROLE_PHARMACIST"));
        when(user.getCommonName()).thenReturn("Unit Test");
        when(user.getOrganizationName()).thenReturn("TEST");
        when(user.getOrganizationId()).thenReturn("111");
        return user;
    }

    private void mockInitUserResponse(ClientConnectorServiceService service) {

        when(service.getPort(new QName("http://cc.pt.epsos.eu", "ClientConnectorServiceHttpSoap11Endpoint"),
                ClientConnectorService.class)).thenReturn(webServiceClient);
    }

    @Override
    public List<Person> queryForPatient(AuthenticatedUser user, List<PatientIdVO> list, CountryVO country) throws NcpServiceException {
        return serviceFacade.queryForPatient(user, list, country);
    }

    @Override
    public void initServices(AuthenticatedUser user) throws NcpServiceException {
        LOGGER.info("Method call: " + this.getClass().getSimpleName() + ".initUser()");
        serviceFacade.initServices(user);
    }

    @Override
    public void setTRCAssertion(TRC trc, AuthenticatedUser user) {

    }

    @Override
    public List<MetaDocument> queryDocuments(Person person, String doctype, AuthenticatedUser user) throws NcpServiceException {
        return serviceFacade.queryDocuments(person, doctype, user);
    }

    @Override
    public CdaDocument retrieveDocument(MetaDocument doc) throws NcpServiceException {
        return serviceFacade.retrieveDocument(doc);
    }

    private String createLongPatientId(PatientId pid) {
        return pid.getExtension() + "^^^&" + pid.getRoot();
    }

    @Override
    public byte[] submitDocument(Dispensation dispensation, AuthenticatedUser user, Person person, String eD_PageAsString) throws NcpServiceException {
        return serviceFacade.submitDocument(dispensation, user, person, eD_PageAsString);
    }
}
