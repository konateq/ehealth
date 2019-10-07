package se.sb.epsos.web.service;

import eu.europa.ec.sante.ehdsi.openncp.audit.AuditService;
import epsos.ccd.gnomon.auditmanager.EventLog;
import eu.europa.ec.sante.ehdsi.openncp.configmanager.ConfigurationManager;
import junit.framework.TestCase;
import org.mockito.MockSettings;
import org.opensaml.core.config.InitializationException;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import se.sb.epsos.web.auth.AuthenticatedUser;

import java.util.Arrays;

import static org.mockito.Mockito.*;

public class AssertionHandlerTest extends TestCase {

    private AssertionHandler assertionHandler;
    private AuthenticatedUser userDetailsMock;
    private Assertion assertionMock;
    private ConfigurationManager configurationManager;
    private AuditService as;
    private MockSettings settings = withSettings().serializable();

    @Override
    protected void setUp() throws Exception {

        super.setUp();
        as = mock(AuditService.class, settings);
        configurationManager = mock(ConfigurationManager.class);

        assertionHandler = new AssertionHandler() {
            private static final long serialVersionUID = 1L;

            protected ConfigurationManager getConfigurationManager() {
                return configurationManager;
            }

            protected AuditService getAuditService() {
                return as;
            }

            protected String getPrivateKeyAlias() {
                return "alias";
            }

            protected String getPrivateKeystoreLocation() {
                return "location";
            }

            protected String getPrivateKeyPassword() {
                return "passwd";
            }
        };
        userDetailsMock = mock(AuthenticatedUser.class, settings);
        assertionMock = mock(Assertion.class, settings);

        when(as.write(any(EventLog.class), eq("13"), eq("2"))).thenAnswer(
                invocation -> {
                    EventLog el = (EventLog) invocation.getArguments()[0];
                    assertEquals("identityProvider::HPAuthentication", el.getEI_TransactionName());
                    assertEquals("pharmacist", el.getHR_RoleID());
                    assertTrue(el.getReqM_ParticipantObjectID().startsWith("urn:uuid:"));
                    assertTrue(el.getResM_ParticipantObjectID().startsWith("urn:uuid:"));
                    return false;
                });

        when(userDetailsMock.getUsername()).thenReturn("username");
        when(userDetailsMock.getOrganizationId()).thenReturn("organizationId");
        when(userDetailsMock.getOrganizationName()).thenReturn("organizationName");
        when(userDetailsMock.getRoles()).thenReturn(Arrays.asList("ROLE_PHARMACIST"));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateAssertion() throws InitializationException, SecurityException {

        Assertion assertion = assertionHandler.createSAMLAssertion(userDetailsMock);
        assertNotNull(assertion);
        assertNotNull(assertion.getIssuer().getValue());
        assertEquals(assertion.getIssuer().getValue(), "urn:idp:EU:countryB");
        assertNotNull(assertion.getSubject().getNameID().getValue());
        assertEquals(assertion.getSubject().getNameID().getValue(), "username");
        assertTrue(assertion.getID().startsWith("_"));
        assertEquals(assertion.getVersion(), SAMLVersion.VERSION_20);
    }

    public void testSendAuditEpsos91() {

        assertionHandler.sendHPAuthenticationAudit(userDetailsMock, assertionMock);
    }
}
